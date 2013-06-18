//  MosaicData - this class tracks the data for the scenes to display
//
//  Note: this file was created to separate the data representation from
//        the code that displays the data.  Much of this code used to
//        be in the ImagePane class.
//
//  Threading Design Notes:
//      A separate thread is used to load TOC files.  This allows the applet 
//      to remain fairly responsive to the user even if data is being loaded
//      over a slow network connection.  The class also knows when image 
//      files are being loaded by the imageLoader object.
//
//      The design assumes that only two threads interact directly with this
//      class - the GUI thread and the TOC loading thread.  The design attempts
//      to minimize the amount of waiting the GUI thread does so that it can
//      remain responsive and animate a busy indicator while data is being 
//      loaded over the network.
//
//      Be very careful when making changes to this file.  Pay attention to 
//      which thread will be making changes to data members.  Basically, only
//      the GUI thread should be changing most data members so the applet 
//      always sees a consistent state when making updates to the GUI.
//
//      The image member of the Metadata is only updated after a browse image
//      has completed loading in the ImageLoader.  So, it is safe to flush 
//      images any time from this file.  However, since the image loader may
//      complete a load of an image after it should have been flushed here,
//      sometimes resources will be held for the image eventhough it isn't 
//      currently displayed.  That is okay since the final clean up a TOC 
//      is done only after the ImageLoader has completed and any resources that
//      were not free'd due to timing will definitely be free'd then.
//  
//----------------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import javax.swing.JOptionPane;

public class MosaicData extends Observable implements Runnable, WorkMonitor
{
    private imgViewer applet;   // reference to the applet
    private LocatorMap locatorMap; // locator map reference
    public static final Object DISPLAY_MODE_CHANGED = new Object();
            // object passed as part of the observer interface to indicate
            // the display mode changed
    public Point targetXY = new Point();
            // object passed as part of the observer interface to indicate
            // there is a specific target X/Y location (used to set scroll
            // position when scrollbars are present)
    private boolean isTargetXY = false; // flag to indicate the display is 
            // trying to move to a target X/Y

    private final int mosaicWidth = 3;  // width of mosaic array - MUST BE ODD
    private final int mosaicHeight = 3; // height of mosaic array - MUST BE ODD
    private final int mosaicSize = mosaicWidth * mosaicHeight; 
                                        // size of mosaic array
    private final int colCenterIndex = (mosaicWidth - 1) / 2;
                                        // index to center column of mosaic
    private final int rowCenterIndex = (mosaicHeight - 1) / 2;
                                        // index to center row of mosaic
    private final int mosaicCenterIndex = colCenterIndex * mosaicHeight 
                                        + rowCenterIndex;
                                        // index to center cell of mosaic
    private int activeCellIndex = mosaicCenterIndex; // index of the active
                                   // (selected) cell in the mosaicCells array
    private int[] defaultZOrder;  // default Z-order indices
    int gridCol; // grid column of center cell (path for WRS-2)
    int gridRow; // grid row of center cell (row for WRS-2)

    private ProjectionTransformation proj; // Projection transformation object
    private int projectionCode; // current projection code used to display data

    private TOC[] mosaicCells;  // array of TOC's for the mosaic area
    private int cellsToDisplay; // count of cells to display for current 
                        // resolution, or Sensor.SINGLE_SCENE if only one scene
                        // should be displayed.  A value of 1 is not the 
                        // same as SINGLE_SCENE since 1 means it could still
                        // be a mosaic of images in one cell (i.e. ASTER 400m)
    private Sensor currSensor;  // currently selected sensor object
    int pixelSize;              // currently displayed pixel size
    double actualPixelSize; // the actual size of the pixels currently
                            // displayed.  The same as pixelSize for most
                            // sensors, but sometimes different (i.e. MODIS)
                            // since the browse may be a slightly different
                            // resolution than advertised.

    MapLayers mapLayers;    // map layers container

    private DateCache dateCache;// cache of dates for recent areas visited 
                                // where the date is no longer the default date

    int maxCloudCover = 100; // maximum cloud cover to display
    private ZOrderList zOrderList; // list for tracking the Z-order of scenes 
                             // in the mosaic view. 

    private ImagePane pane; // image pane for displaying the mosaic

    private Dimension displaySize; // dimensions needed to display the images

    public MosaicCoords mosaicCoords;// object to hold mosaic coordinate info

    public SceneFilter sceneFilter;// scene filter to use for filtering scenes
                                   // based on things like cloud cover

    private int subCol;     // fractional column step when displaying 1 cell
    private int subRow;     // fractional row step when displaying 1 cell
    public final static int STEPS = 3; // number of fractional steps when
                            // displaying a single cell

    public ImageLoader imageLoader;  // image loader object
    private boolean areImagesLoading;// flag to indicate images are loading
    private int notifyType;          // type of notify last sent to observers

    // defined types of notifies sent to observers
    private static final int NORMAL_NOTIFY = 0;
    private static final int TARGETXY_NOTIFY = 1;
    private static final int DISPLAY_MODE_CHANGE_NOTIFY = 2;

    private Thread loaderThread;     // thread for loading TOC files
    private Object loadLock;         // mutex for exclusive access
    private boolean killThread;      // flag to indicate the thread should
                                     // be killed
    private boolean isLoading;       // TOC files are loading flag
    private boolean isLoadCancelled; // cancel TOC files loading flag
    private TOC[] loadingMosaicCells;// array of TOCs being loaded by the load
                                     // thread
    private int loadingActiveCellIndex; // index into mosaicCells that should 
                                     // be active after new TOCs are loaded
    private boolean loadingPreserveZOrder; // flag to preserve the displayed
                                     // z-order of scenes after new TOCs are 
                                     // loaded
    private boolean[] loadingUsed;   // array of flags to indicate which entries
                                     // of mosaicCells have been reused in the 
                                     // loadingMosaicCells so the correct cells
                                     // can be cleaned up when a new TOC array
                                     // is activated.
    private boolean loadCompleted;   // flag to indicate the TOC load is done
    private boolean tocChangePending;// flag to indicate a change to a new TOC
                                     // array is being loaded
    private boolean resolutionChangePending;// flag that indicates a resolution
                                     // change is pending (delayed due to the
                                     // user changing resolution in the middle
                                     // of a TOC load)
    private Metadata targetScene;    // reference to the target scene when
                                     // showScene is called.  It is stored 
                                     // here temporarily while the inventory
                                     // TOC files load.
    private Metadata refScrollScene; // reference to a scene in the current
                                     // swath when scrolling a full-mosaic
                                     // sensor.  This helps stay on the same
                                     // swath when scrolling up and down.
    private int numTocsToLoad;       // total number of TOC files to load
    private int currTocLoading;      // current TOC loading of total number
    private boolean isCalledFromScrolledData;// flag indicating if display 
                                     //was scrolled
    private Metadata targetDateScene;// current selected scene
    private LatLong targetLatLong;   // target lat/long for gotoLatLong

    // Constructor for the ImagePane.  Allocates array space and sets variables.
    //--------------------------------------------------------------------------
    MosaicData(imgViewer parent, ImagePane paneIn, LocatorMap locatorMap)
    {
        applet = parent;             // Save ptr to parent
        pane = paneIn;
        this.locatorMap = locatorMap;
        
        isCalledFromScrolledData = false;
        
        targetDateScene = null;

        // Setup space for the Table-of-Contents files
        mosaicCells = new TOC[mosaicSize];

        // define the default zorder (from lowest to highest order)
        defaultZOrder = new int[mosaicSize];
        int index = 0;
        for (int col = 0; col < mosaicWidth; col++)
        {
            // skip the middle column
            if (col == colCenterIndex)
                continue;

            for (int row = 0; row < mosaicHeight; row++)
            {
                defaultZOrder[index] = col * mosaicHeight + row;
                index++;
            }
        }
        int col = colCenterIndex;
        for (int row = 0; row < mosaicHeight; row++)
        {
            // skip the middle row
            if (row == rowCenterIndex)
                continue;
            defaultZOrder[index] = col * mosaicHeight + row;
            index++;
        }
        defaultZOrder[index] = mosaicCenterIndex;

        // create the image loader
        imageLoader = new ImageLoader(parent,paneIn);

        // create the date cache for the last 20 scenes changed from the 
        // default date
        dateCache = new DateCache(20);

        // create the mosaic Z-order list
        zOrderList = new ZOrderList();

        mapLayers = new MapLayers(applet,pane);

        sceneFilter = new LandsatSceneFilter(this);

        mosaicCoords = new MosaicCoords();

        // initialize the display size since it is possible to use it before
        // it is set normally
        displaySize = new Dimension(1,1);

        // create objects for threading support
        loadLock = new Object();
        loaderThread = new Thread(this,"TOC Loader Thread");
        loaderThread.start();
    }

    // methods required for the WorkMonitor interface
    //-----------------------------------------------
    public String getWorkLabel() { return "Reading Inventory"; }
    public boolean isWorking() { return isUnstableTOC(); }
    public int getTotalWork() { return numTocsToLoad; }
    public int getWorkComplete() { return currTocLoading; }

    // helper routine to set a default z-order
    //----------------------------------------
    private void setDefaultZOrder()
    {
        TOC cell;

        // if the current sensor is a full mosaic, make the initial z-order
        // be sorted by cloud cover
        if (currSensor.isFullMosaic)
        {
            for (int i = 0; i < mosaicCells.length; i++)
            {
                cell = mosaicCells[defaultZOrder[i]];

                // if the cell is valid, insert the scenes in cloud cover order
                if (cell.valid)
                {
                    for (int j = 0; j < cell.numImg; j++)
                        zOrderList.insertByCloudCover(cell.scenes[j]);
                }
            }
        }
        
        // when default to selected date is enabled set the default scenes
        // to display.
        if (applet.toolsMenu.isDefaultToDateEnabled())
        {
            cell = mosaicCells[activeCellIndex];
            if (cell.valid)
                setDefaultToSelectedDate(cell.scenes[cell.currentDateIndex]);
        }

        // now make sure the current date index in each cell is on top
        for (int i = 0; i < mosaicCells.length; i++)
        {
            cell = mosaicCells[defaultZOrder[i]];

            // if the cell is valid, put the scene on top
            if (cell.valid)
            {
                // then make sure the current date index is on top
                zOrderList.putOnTop(cell.scenes[cell.currentDateIndex]);
            }
        }

        // when in swath mode, make sure the active swath is on top of the
        // other scenes
        if (currSensor.hasSwathMode && applet.toolsMenu.isSwathModeEnabled())
        {
            cell = mosaicCells[activeCellIndex];
            if (cell.valid)
                buildSwath(cell.scenes[cell.currentDateIndex], true);
        }

    }

    // helper routine to set the selected scene in the displayed area
    //-------------------------------------------------------------------
    public void setSelectedScene(Metadata scene)
    {
        // update the selected scene
        updateSelectedScene(scene);

        // notify the observers that the data has changed
        notifyType = MosaicData.NORMAL_NOTIFY;
        setChanged();
        notifyObservers();
    }

    // helper method to put the update the needed items for the currently
    // selected scene
    //-------------------------------------------------------------------
    private void updateSelectedScene(Metadata scene)
    {
        // put the selected scene on top
        zOrderList.putOnTop(scene);

        // find the cell index for the scene
        int cellIndex = colRowToCell(scene.gridCol,scene.gridRow);
        if (cellIndex == -1)
        {
            // bug that should never happen
            System.out.println("Bug detected in updateSelectedScene");
            return;
        }
        TOC cell = mosaicCells[cellIndex];

        // find the scene index so the currentDateIndex can be updated
        int index;
        for (index = 0; index < cell.numImg; index++)
        {
            if (scene == cell.scenes[index])
                break;
        }
        if (index < cell.numImg)
            cell.currentDateIndex = index;
        else
            System.out.println("Bug in updateSelectedScene");

        // set the new active cell and filter the scenes
        activeCellIndex = cellIndex;
        sceneFilter.filter();

        // when in swath mode, make sure the swath for the selected scene
        // is shown and that new scenes are loaded if needed
        if (currSensor.hasSwathMode && applet.toolsMenu.isSwathModeEnabled())
        {
            buildSwath(scene, true);
            mosaicCoordsUpdate();
            loadScenes();
        }
    }

    // helper routine to set the selected cell in the Mosaic.
    // The activeCellIndex value should only be changed by calling this method 
    // unless you are absolutely sure of what you are doing.
    //-------------------------------------------------------------------
    private void setSelectedCell(int cellIndex, boolean preserveZOrder)
    {
        // if not preserving the z-order, select the correct scene to be
        // on top
        if (!preserveZOrder)
        {
            boolean cellSet = false;

            // if a sub-cell step is in effect, make sure the selected scene is 
            // selected from the visible scenes
            if ((subCol != 0) || (subRow != 0))
            {
                zOrderList.top();
                Metadata scene;
                while ((scene = zOrderList.down()) != null)
                {
                    if (scene.visible)
                    {
                        updateSelectedScene(scene);
                        cellSet = true;
                        break;
                    }
                }
            }

            if (!cellSet)
            {
                // set the scene index at the top of the z-order
                TOC cell = mosaicCells[cellIndex];

                if (cell.valid)
                    zOrderList.putOnTop(cell.scenes[cell.currentDateIndex]);

                activeCellIndex = cellIndex;
                sceneFilter.filter();
            }
        }
        else
            sceneFilter.filter();

        // notify the observers that the data has changed
        setChanged();

        // if there is a target X/Y notify observers including that, 
        // otherwise just do a normal notify
        if (isTargetXY)
        {
            notifyType = MosaicData.TARGETXY_NOTIFY;
            notifyObservers(targetXY);
        }
        else
        {
            notifyType = MosaicData.NORMAL_NOTIFY;
            notifyObservers();
        }
    }

    // method to determine if a given scene is in the active swath.  Returns
    // true if the scene is in the active swath.
    //----------------------------------------------------------------------
    private boolean isInActiveSwath(Metadata scene)
    {
        // this assumes that anything with the same date and sensor is in the
        // same swath
        Metadata currScene = getCurrentScene();
        if ((currScene != null) && (currScene.date == scene.date)
            && (currScene.getSensor() == scene.getSensor()))
            return true;
        else
            return false;
    }

    // method to lower the top scene to the bottom of the Z-order and select 
    // a new scene to be the selected scene
    //----------------------------------------------------------------------
    public void lowerScene(Metadata sceneToLower)
    {
        // nothing to do if only a single scene is visible
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
            return;

        // if in swath mode and a scene in the active swath it being lowered,
        // it requires special handling to lower the entire swath
        if (currSensor.hasSwathMode && applet.toolsMenu.isSwathModeEnabled())
        {
            if (isInActiveSwath(sceneToLower))
            {
                // get the list of the scenes in the swath
                Vector swathScenes = getScenesInSwath(sceneToLower);

                // lower all the scenes in the swath
                for (int i = 0; i < swathScenes.size(); i++)
                {
                    Metadata scene = (Metadata)swathScenes.elementAt(i);
                    zOrderList.putOnBottom(scene);
                }

                // get new top scene
                zOrderList.top();
                Metadata topScene = zOrderList.down();

                // make the swath for the new top scene the active swath
                int cellNum = colRowToCell(topScene.gridCol, topScene.gridRow);
                TOC cell = mosaicCells[cellNum];
                int dateIndex = cell.getIndexOfDate(topScene.date,
                                                    topScene.getSensor());
                cell.currentDateIndex = dateIndex;
                activeCellIndex = cellNum;
                buildSwath(topScene, true);

                // load new scenes in case some of the scenes in the new active
                // swath haven't been loaded yet
                mosaicCoordsUpdate();
                loadScenes();

                // return since lowering a scene for this special case has
                // now been entirely handled
                return;
            }
        }

        // get the top scene
        zOrderList.top();
        Metadata topScene = zOrderList.down();

        // if the selected scene is being lowered, look for the scene closest
        // to the top that intersects (if nothing intersects, do not do
        // anything since there is no need to lower the scene)
        if (topScene == sceneToLower)
        {
            Polygon topSceneLocation = topScene.screenLocation;
            Metadata scene;
            boolean intersects = false;

            while ((scene = zOrderList.down()) != null)
            {
                // skip scenes that are not visible
                if (!scene.visible)
                    continue;

                // cache the points in the current scene
                int[] x = scene.screenLocation.xpoints;
                int[] y = scene.screenLocation.ypoints;

                // check each point to see if it intersects with the
                // top scene's location
                for (int i = 0; i < 4; i++)
                {
                    if (topSceneLocation.contains(x[i],y[i]))
                    {
                        intersects = true;
                        break;
                    }
                }
                if (intersects)
                    break;
            }

            // if an intersecting scene was found, make it the selected
            // scene
            if (intersects)
            {
                // put the old scene on the bottom
                zOrderList.putOnBottom(sceneToLower);

                // make the new scene the current scene in its cell and
                int cellNum = colRowToCell(scene.gridCol,scene.gridRow);
                int i;
                for (i = 0; i < mosaicCells[cellNum].numImg; i++)
                    if (mosaicCells[cellNum].scenes[i] == scene)
                        break;
                if (i < mosaicCells[cellNum].numImg)
                    mosaicCells[cellNum].currentDateIndex = i;

                // set the new selected cell, not preserving the Z-order
                setSelectedCell(cellNum, false);
            }
        }
        else
        {
            // not lowering the selected scene, so just put this scene on the
            // bottom and repaint
            zOrderList.putOnBottom(sceneToLower);
            pane.repaint();
        }
    }
    
    // method to select scenes that are close to the date of the target
    // scene and show a mosaic for that date range.
    //----------------------------------------------------------------------
    public void setScenesToDate (Metadata targetSetToScene)
    {
        // looping through each cell
        for (int cellNum = 0; cellNum < mosaicCells.length; cellNum++)
        {
            mosaicCells[cellNum].findSceneClosestToDate(targetSetToScene);
        }  
        
        // rebuild the z-order for the scenes that are visible
        rebuildZOrder();

        // load scenes to match the current selected dates
        mosaicCoordsUpdate();
        loadScenes();

        // notify the observers that the data has changed
        notifyType = MosaicData.NORMAL_NOTIFY;
        setChanged();
        notifyObservers();

    }
    
    // method to set the default to selected date 
    //--------------------------------------------------
    public void setDefaultToSelectedDate(Metadata scene)
    {
        if (applet.toolsMenu.isDefaultToDateEnabled())
        {
            if (targetDateScene == null)
            {
                targetDateScene = scene;
            }
            
            // only update the targetDateScene if the
            // display was not scrolled.
            if (!isCalledFromScrolledData)
            {
                targetDateScene = scene;
            }
            
            //looping through each cell
            for (int cellNum = 0; cellNum < mosaicCells.length; cellNum++)
            {
                mosaicCells[cellNum].findSceneClosestToDate(targetDateScene);
            }  
            
            isCalledFromScrolledData = false;
        }
    }
    
    //method to reset the default scenes displayed
    //--------------------------------------------
    public void resetTargetDate()
    {
        targetDateScene = null;
    }
    
    // method to select the default scene the cell was originally
    // set to.
    //------------------------------------------------------------------
    public void setDefaultScene(Metadata scene)
    {
        // get the cellIndex of the scene
        int cellIndex = colRowToCell(scene.gridCol,scene.gridRow);
        
        TOC cell = mosaicCells[cellIndex];
        // if the TOC is changing, no need to do anything else since the new
        // limit will be enforced when the new TOC is activated
        if (!isUnstableTOC())
        {   
            if (cell.valid)
            {
                // pick the most recent, lowest cloud cover scene
                int index = pickDefaultScene(cellIndex);
                
                // Flush the date index
                flushCurrentDateIndex(cell);
        
                // Set the new date index
                cell.currentDateIndex = index;
                
                // Remove entry from cache 
                dateCache.remove(cell.gridCol,cell.gridRow);
                
                // if in swath mode, pick the scenes in the swaths
                if (currSensor.hasSwathMode && 
                    applet.toolsMenu.isSwathModeEnabled())
                {
                    buildSwath(cell.scenes[cell.currentDateIndex], true);
                }
                
                // when default to selected date is enabled set the default 
                // scenes to display.
                if (applet.toolsMenu.isDefaultToDateEnabled())
                {
                    setDefaultToSelectedDate(
                        cell.scenes[cell.currentDateIndex]);
                }

                // rebuild the z-order for the scenes that are visible
                rebuildZOrder();

                // load scenes to match the current selected dates
                mosaicCoordsUpdate();
                loadScenes();

                // notify the observers that the data has changed
                notifyType = MosaicData.NORMAL_NOTIFY;
                setChanged();
                notifyObservers();
            } 
        }
    }

    // method to add all scene in the user defined area
    //-------------------------------------------------
    public void selectUserAreaScenes(Metadata currentScene)
    {
        // only one image in display, so just add it (if visible)
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            TOC cell = mosaicCells[activeCellIndex];
            Metadata scene = cell.scenes[cell.currentDateIndex];

            if (scene.visible)
            {
                currSensor.sceneList.add(scene);
            }
        }
        else
        {
            // create pointers to User Defined Area values before we get 
            // into for loops below
            UserDefinedArea userDefinedArea = 
                applet.userDefinedAreaDialog.getUserDefinedArea();

            boolean userAreaFilterEnabled = 
                applet.searchLimitDialog.isUserDefinedAreaEnabled();

            // go through each cell and add all scenes that are in the
            // user defined area.
            for (int cellNum = 0; cellNum < mosaicCells.length; cellNum++)
            {
                TOC cell = mosaicCells[cellNum];
                // protect against invalid array checking
                if (cell.valid)
                {
                    // get every visible scene within the user defined area
                    // need to go and check each scene in the cell if it 
                    // intersects the user defined area polygon
                    for (int i = 0; i < cell.numImg; i++)
                    {
                        Metadata scene = cell.scenes[i];

                        // if the user defined area filter has been
                        // enabled, the only scenes visible are those 
                        // within the user defined area. No need to check 
                        // if the scene intersects the user defined polygon
                        if (userAreaFilterEnabled)
                        {
                            if (scene.visible)
                            {
                                currSensor.sceneList.add(scene);
                            }
                        }
                        // filter not enabled, so check each scene if it
                        // intersects the user defined area polygon
                        else
                        {
                            if (scene.visible && 
                                    userDefinedArea.sceneIntersects(scene))
                            {
                                currSensor.sceneList.add(scene);
                            }
                        }
                    }
                }
            }
        }
    }

    // method to add all visible scenes to the scene list
    //---------------------------------------------------
    public void selectAllScenes (Metadata currentScene)
    {
        // only one image in display, so just add it
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            TOC cell = mosaicCells[activeCellIndex];
            Metadata scene = cell.scenes[cell.currentDateIndex];
            
            if (scene.visible)
            {
                currSensor.sceneList.add(scene);
            }
        }
        else //add all current images in mosaicCells to list
        {
            for (int cellNum = 0; cellNum < mosaicCells.length; cellNum++)
            {
                TOC cell = mosaicCells[cellNum];
                // protect against invalid array checking
                if (cell.valid)
                {
                    Metadata scene = cell.scenes[cell.currentDateIndex];
                    if (scene.visible)
                    {
                        currSensor.sceneList.add(scene);
                    }
                }
            }
        }
    }

    // method to add all scenes in the selected swath to the scene list
    //-----------------------------------------------------------------
    public void selectSwathScenes (Metadata currentScene, boolean useHiddenList)
    {
        Vector scenes = getScenesInSwath(currentScene);

        for (int i = 0; i < scenes.size(); i++)
        {
            Metadata scene = (Metadata)scenes.elementAt(i);
            if (scene.visible)
            {
                if (useHiddenList)
                    currSensor.hiddenSceneList.add(scene);
                else 
                    currSensor.sceneList.add(scene);
            }
        }

        // if hiding scenes, make sure the newly selected swath is on top of
        // everything else (needed scenes already being loaded by the hiding
        // mechanism)
        if (useHiddenList)
        {
            zOrderList.top();
            Metadata scene = zOrderList.down();
            buildSwath(scene,true);
        }
    }

    // method to set the ImagePane for the indicated sensor.  Note that the
    // border adjustments are needed since the image extents read from the TOC
    // file are not accurate enough to be visually pleasing.  The values were
    // found by experimentation.
    //------------------------------------------------------------------------
    public void setSensor(Sensor newSensor)
    {
        // cancel any loads in process, making sure to clear the 
        // areImagesLoading flag to prevent a race condition from using stale
        // TOC data
        cancelLoad();
        imageLoader.cancelLoad();
        areImagesLoading = false;
        mapLayers.cancelLoad();

        // flush the date cache when the sensor is switched
        if (currSensor != newSensor)
        {
            dateCache.flush();
            zOrderList.empty();
        }

        // convert the current grid location to lat/long location in case the 
        // navigation model changes with the new sensor.  If the currSensor is
        // null, this is being called during initialization, so just use the
        // new sensor navigation model to establish the location.
        LatLong loc = null;
        if (currSensor == null)
            loc = newSensor.navModel.gridToLatLong(gridCol,gridRow);
        else
        {
            if ((cellsToDisplay == 1) 
                || (cellsToDisplay == Sensor.SINGLE_SCENE))
            {
                // displaying a single cell or single scene, so center the
                // new sensor on the displayed cell
                TOC cell = mosaicCells[activeCellIndex];
                if (cell.valid)
                {
                    loc = currSensor.navModel.gridToLatLong(cell.gridCol,
                                                            cell.gridRow);
                }
            }
            if (loc == null)
            {
                // displaying mosaic, so the location displayed for the 
                // new sensor should be the center of the mosaic
                loc = currSensor.navModel.gridToLatLong(gridCol,gridRow);
            }
        }

        mosaicCoords.invalidate();

        // make all the cells invalid so they aren't used until they are read
        // from the server for the new sensor
        for (int i = 0; i < mosaicCells.length; i++)
            mosaicCells[i].valid = false;

        // update the resolution selection widget so it is current for the
        // selected sensor and get the pixel size for this sensor
        pixelSize = applet.resolutionMenu.setSensor(currSensor, newSensor);

        currSensor = newSensor;
        subCol = 0;
        subRow = 0;

        // if the current sensor mosaics all available data, configure the
        // z-order list for multiple scene mode, otherwise single scene mode
        if (currSensor.isFullMosaic)
            zOrderList.setMultipleSceneMode();
        else
            zOrderList.setSingleSceneMode();

        // get the actual pixel size
        actualPixelSize = currSensor.getActualResolution(pixelSize);

        // get the number of cells to display at the current resolution
        cellsToDisplay = currSensor.getNumCellsAtResolution(pixelSize);

        // convert the lat/long location to the current sensor's grid layout
        Point grid = currSensor.navModel.latLongToGrid(loc.latitude,
                                                       loc.longitude);
        // verify the new sensor can actually move to the grid location
        if (!canMoveToMapArea(grid.x, grid.y))
        {
            // can't move to the grid, so go to the center of the defined area
            GeographicLocatorMapConfig config
                    = locatorMap.getMapConfig(currSensor.locatorMap);
            loc.latitude = (config.topLat + config.bottomLat) / 2.0;
            loc.longitude = (config.leftLon + config.rightLon) / 2.0;
            grid = currSensor.navModel.latLongToGrid(loc.latitude,
                                                     loc.longitude);
        }

        gridCol = grid.x;
        gridRow = grid.y;

        // scroll to the current location for this sensor
        scrollData(gridCol,gridRow,0,0,false,false,false);
    }

    // This routine picks the projection code preferred for the current 
    // scenes being viewed in the mosaic.  It picks the center scene's 
    // proj Code if valid, otherwise it picks the proj Code that is present
    // in more scenes.  The proper thing to do is really have scenes within
    // three scenes of a boundary processed to multiple projections. 
    //---------------------------------------------------------------------
    private int pickPreferredProjCode()
    {
        int code = 1100;

        // if the center scene has valid projection info, use it as the code to
        // display
        if (mosaicCells[mosaicCenterIndex].valid)
        {
            code = mosaicCells[mosaicCenterIndex].projCode;
        }
        else
        {
            /* center scene does not have valid projection info, so select the 
               code with the most scenes in the displayed area */
            int altCode = 1100;
            int codeCount = 0;
            int validCount = 0;
            for (int i = 0; i < mosaicCells.length; i++)
            {
                TOC cell = mosaicCells[i];

                if (cell.valid)
                {
                    if (validCount == 0)
                    {
                        code = cell.projCode;
                        altCode = code;
                    } 
                    validCount++;
                    if (code == cell.projCode)
                        codeCount++;
                    else
                        altCode = cell.projCode;
                }
            }
            // switch to the alternate proj code if the current didn't account
            // for at least half of the scenes that had a valid projection code.
            // This assumes only two codes will be present in an area.
            if (2 * codeCount < validCount)
                code = altCode;
        }
        return code;
    }

    // method to provide access to the projection code
    //------------------------------------------------
    public int getProjectionCode()
    {
        return projectionCode;
    }
    
    // method to pick the most recent, highest quality and lowest 
    // cloud cover scene
    //-----------------------------------------------------------
    private int pickDefaultScene(int cellIndex)
    {
        TOC cell = mosaicCells[cellIndex];
        
        int index = -1;
        final int maxCloudCover = 101;
        final int minQuality = 0;
        int ignoreVisibleIndex = -1;
        // To make quality the primary value in selecting a scene
        final int cloudCoverWeight = 1;
        final int qualityWeight = 101;
        // Start with the worst rating and improve as we find nice scenes
        int bestSceneRating = ((cloudCoverWeight * maxCloudCover) +
                              (qualityWeight * (9 - minQuality)));
        int ignoreVisibleBestSceneRating = bestSceneRating;

        int sceneQuality = 0;
        int sceneCloudCover = 0;
        int sceneRating = 0; // lower rating = better scene

        for (int m = cell.numImg - 1; m >= 0; m--)
        {
            Metadata scene = cell.scenes[m];

            // Find the latest visible image with the high quality and least
            // cloud cover to display
            sceneQuality = scene.getQuality();

            // if quality isn't available, set it to 9 to remove it from the
            // consideration
            if (sceneQuality == -1)
                sceneQuality = 9;

            sceneCloudCover = scene.cloudCover;

            // if the cloud cover isn't available, set it to the max
            if (sceneCloudCover < 0)
                sceneCloudCover = maxCloudCover;

            sceneRating = ((cloudCoverWeight * sceneCloudCover) +
                           (qualityWeight * (9 - sceneQuality)));

            if ((sceneRating < bestSceneRating) && scene.visible)
            {
                bestSceneRating = sceneRating;
                index = m;
            }
            if (sceneRating < ignoreVisibleBestSceneRating)
            {
                ignoreVisibleBestSceneRating = sceneRating;
                ignoreVisibleIndex = m;
            }
        }
       
       // if no visible scene was found, set the index to
       // the lowest cloud cover that isn't visible
       if (index == -1)
           index = ignoreVisibleIndex;

        return index;
    }

    // method to pick the preferred scene to display for each cell.  The end
    // result is that the currentDateIndex is set for each cell of the mosaic.
    //------------------------------------------------------------------------
    private void pickSceneDates()
    {
        for (int i = 0; i < mosaicCells.length; i++)
        {
            TOC cell = mosaicCells[i];

            int index = cell.currentDateIndex;
            
            // if there is a targetScene and it is in this cell, look for the
            // target scene in the cell (using the entity ID)
            if ((targetScene != null) && (targetScene.gridCol == cell.gridCol)
                && (targetScene.gridRow == cell.gridRow))
            {
                index = cell.findScene(targetScene);
                if (index >= 0)
                    cell.currentDateIndex = index;

                // the target scene is no longer needed
                targetScene = null;
            }

            // if there is a selected scene, make sure it is visible
            if (cell.valid && (index != -1))
            {
                Metadata scene = cell.scenes[cell.currentDateIndex];
                if (!scene.visible)
                    index = -1;
            }

            // if no date chosen yet, find the right one to display
            if (cell.valid && (index == -1))
            {
                // look for a recent selection in the date cache
                index = dateCache.lookupDate(cell);
                if ((index == -1) || (index >= cell.numImg) || 
                    !cell.scenes[index].visible)
                {
                    // not in date cache or index from date cache is too big
                    // (might happen if a scene is deleted from the inventory),
                    // so if the user has the current scene in the scene list,
                    // use it
                    index = currSensor.sceneList.contains(cell);
                    if ((index == -1) || (index >= cell.numImg) ||
                        !cell.scenes[index].visible)
                    {
                        // not in list so pick the most recent scene, either
                        // ignoring cloud cover or factoring it in
                        if (!currSensor.useCloudCoverForDefaultScenes)
                        {
                            // this sensor ignores cloud cover for picking
                            // the most recent scene, so pick the most recent
                            // visible scene
                            index = -1;
                            for (int m = cell.numImg - 1; m >= 0; m--)
                            {
                                if (cell.scenes[m].visible)
                                {
                                    index = m;
                                    break;
                                }
                            }
                            // if no scenes are visible, use the latest one
                            if (index == -1)
                                index = cell.numImg - 1;
                        }
                        else
                        {
                           index = pickDefaultScene(i);
                        }
                    }
                }
                
                // flush previous image if loaded when not a full mosaic sensor
                if (!currSensor.isFullMosaic && (index != cell.currentDateIndex)
                    && (cell.currentDateIndex != -1) 
                    && (cell.currentDateIndex < cell.numImg))
                {
                    flushCurrentDateIndex(cell);
                }

                cell.currentDateIndex = index;
            }
        }

        // if displaying a sensor that uses a full mosaic and only showing a 
        // single scene and a target X/Y is in effect, override the single scene
        // based on the scene closest to the target X/Y
        if (currSensor.isFullMosaic && (cellsToDisplay == Sensor.SINGLE_SCENE)
            && isTargetXY)
        {
            double minDist = 1000000000000.0;

            TOC cell = mosaicCells[activeCellIndex];
            for (int i = 0; i < cell.numImg; i++)
            {
                Metadata scene = cell.scenes[i];
                // only consider visible scenes
                if (scene.visible)
                {
                    double x = (scene.ulX - targetXY.x)/1000;
                    double y = (scene.ulY - targetXY.y)/1000;
                    double dist = x * x + y * y;
                    if (dist < minDist)
                    {
                        cell.currentDateIndex = i;
                        minDist = dist;
                    }
                }
            }
        }

        // override the selected scenes with swaths if in swath mode
        pickSwathScenes();
    }

    // helper method to flush the image for the active scene in a cell.
    // Note that this is needed since some Java implementations do not 
    // properly flush image resources when garbage collecting.
    //-----------------------------------------------------------------
    private void flushCurrentDateIndex(TOC cell)
    {
        Metadata scene = cell.scenes[cell.currentDateIndex];
        if (scene.image != null)
        {
            scene.image.flush();
            scene.image = null;
            scene.imageRes = -1;
        }
    }

    // method to pick scenes to display for a swath for each of the columns
    // of the mosaic
    //---------------------------------------------------------------------
    private void pickSwathScenes()
    {
        // if in swath mode, pick the scenes in the swaths
        if (currSensor.hasSwathMode && applet.toolsMenu.isSwathModeEnabled())
        {
            if (currSensor.isFullMosaic)
            {
                // full mosaic sensor, so build a swath for just the active
                // scene, making sure to follow the same swath if scrolling 
                // up or down
                boolean done = false;
                if (refScrollScene != null)
                {
                    // there is a reference scrolling scene, so determine the
                    // scenes in the swath
                    Vector scenes = getScenesInSwath(refScrollScene);
                    refScrollScene = null;

                    // find the first visible scene in the swath and build
                    // a swath using it
                    for (int i = 0; i < scenes.size(); i++)
                    {
                        Metadata scene = (Metadata)scenes.elementAt(i);
                        if (scene.visible)
                        {
                            int cellNum = colRowToCell(scene.gridCol, 
                                                       scene.gridRow);
                            if (cellNum != -1)
                            {
                                activeCellIndex = cellNum;

                                // build the swath using the scene
                                buildSwath(scene,true);

                                // update the current date index for the
                                // active cell
                                TOC cell = mosaicCells[cellNum];
                                for (int index = 0; index < cell.numImg; 
                                     index++)
                                {
                                    if (cell.scenes[index] == scene)
                                    {
                                        cell.currentDateIndex = index;
                                        break;
                                    }
                                }
                                done = true;
                                break;
                            }
                        }
                    }
                }
                // if the swath wasn't built using the reference scroll scene,
                // build it now using the current active scene
                if (!done)
                {
                    TOC cell = mosaicCells[activeCellIndex];
                    if (cell.valid)
                        buildSwath(cell.scenes[cell.currentDateIndex], false);
                }
            }
            else
            {
                // for the non-full mosaic, build a swath in each column of the
                // mosaic
                int activeColIndex = activeCellIndex / mosaicHeight;

                for (int colIndex = 0; colIndex < mosaicHeight; colIndex++)
                {
                    TOC cell;

                    if (activeColIndex == colIndex)
                        cell = mosaicCells[activeCellIndex];
                    else
                        cell = mosaicCells[colIndex * mosaicHeight + 1];
                    if (cell.valid)
                        buildSwath(cell.scenes[cell.currentDateIndex], false);
                }
            }
        }
    }

    // method to build a swath containing the indicated scene
    //-------------------------------------------------------
    private void buildSwath(Metadata targetScene, boolean putOnTop)
    {
        // get the scenes in the same swath as the target scene
        Vector scenesOnDate = getScenesInSwath(targetScene);

        for (int i = 0; i < scenesOnDate.size(); i++)
        {
            Metadata scene = (Metadata)scenesOnDate.elementAt(i);

            // skip the scene if it isn't visible
            if (!scene.visible)
                continue;

            // handle non-full mosaic sensors (like Landsat)
            if (!currSensor.isFullMosaic)
            {
                // get the cell containing the current scene
                TOC cell = getCellForScene(scene);
                if (cell == null)
                    continue;

                // if the scene isn't already the current scene, flush the
                // scene
                if (scene != cell.scenes[cell.currentDateIndex])
                    flushCurrentDateIndex(cell);

                // make the scene the current one for the cell
                int dateIndex = cell.getIndexOfDate(scene.date, 
                                                    scene.getSensor());
                if (dateIndex != -1)
                    cell.currentDateIndex = dateIndex;
            }

            if (putOnTop)
            {
                // put the scene on top of the z-order if requested
                zOrderList.putOnTop(scene);
            }
            else if (!currSensor.isFullMosaic)
            {
                // for non-full mosaics, make sure the z-order contains the
                // correct scene
                zOrderList.changeScene(scene);
            }
        }
        // make sure the target scene is the active one
        if (putOnTop)
        {
            zOrderList.putOnTop(targetScene);
        }
    }

    // return a list of scenes in the same swath as the passed in scene.
    // The returned scenes are in top to bottom order on the display.
    //-----------------------------------------------------------------
    public Vector getScenesInSwath(Metadata scene)
    {
        Vector swathScenes = new Vector();

        // look for any scenes on the same date in any mosaic cell
        for (int index = 0; index < mosaicSize; index++)
        {
            mosaicCells[index].getScenesOnDate(scene.date, scene.getSensor(),
                                               swathScenes);
        }

        return swathScenes;
    }

    // Method to calculate the dimensions of the displayed area
    //---------------------------------------------------------
    private void setDisplaySize()
    {
        int width = 0;
        int height = 0;

        // if only displaying a single scene, use that scene's image size for
        // the display size
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            TOC cell = mosaicCells[activeCellIndex];
            if (cell.valid)
            {
                Metadata scene = cell.scenes[cell.currentDateIndex];
                if (scene.image != null)
                {
                    width = scene.image.getWidth(null);
                    height = scene.image.getHeight(null);
                }
            }
        }
        else if (cellsToDisplay == 1)
        {
            // only displaying one cell, so use the size of the viewport
            Dimension viewportPixels = mosaicCoords.getViewportPixels();
            height = viewportPixels.height;
            width = viewportPixels.width;
        }
        else
        {
            // entire mosaic, so use the previously calculated mosaic size
            Dimension mosaicPixels = mosaicCoords.getMosaicPixels();
            width = mosaicPixels.width;
            height = mosaicPixels.height;
        }

        // remember the dimensions of the data if they are valid
        if ((width > 0) && (height > 0))
            displaySize = new Dimension(width,height);
    }

    // method to return the size of the displayed data
    //------------------------------------------------
    public Dimension getDisplaySize()
    {
        return new Dimension(displaySize);
    }

    // Common method used for loading scenes that will be drawn
    //---------------------------------------------------------
    private void loadScenes() 
    {
        TOC cell;               // current cell being manipulated

        // tell the user imagery is loading
        applet.statusBar.showStatus("Loading Imagery...");

        // do not load the images or map layers if there is a resolution change
        // pending
        if (!resolutionChangePending)
        {
            // load the images for the scenes
            areImagesLoading = true;
            applet.updateBusyIndicators();
            imageLoader.loadImages(zOrderList,cellsToDisplay,pixelSize,
                                   currSensor);
        }
    } 

    // method used to update mosaic coordinates and find min size
    //-----------------------------------------------------------
    private void mosaicCoordsUpdate()
    {
        // calculate the coordinate info for the displayed scenes and pixel size
        Dimension minSize = pane.getParent().getMinimumSize();
        mosaicCoords.update(applet, mosaicCells, currSensor, minSize, proj,
                            cellsToDisplay, activeCellIndex, actualPixelSize,
                            subCol,subRow);
    }

    // method to check if the image load has completed
    //------------------------------------------------
    public void checkForCompletedLoad()
    {
        // if a TOC load completed, follow through with activating the 
        // new TOC files
        if (loadCompleted)
        {
            loadCompleted = false;
            if (tocChangePending)
            {
                tocChangePending = false;
                activateNewTocArray();
            }
            else
            {
                System.out.println("Bug in checkForCompletedLoad");
            }
        }

        // check if just finished loading images
        if (areImagesLoading && !imageLoader.isBusy())
        {
            // just finished loading images, so set the display size
            areImagesLoading = false;
            setDisplaySize();

            // notify the observers that the data has changed
            setChanged();
            if (notifyType == MosaicData.NORMAL_NOTIFY)
                notifyObservers();
            else if (notifyType == MosaicData.TARGETXY_NOTIFY)
                notifyObservers(targetXY);
            else if (notifyType == MosaicData.DISPLAY_MODE_CHANGE_NOTIFY)
                notifyObservers(DISPLAY_MODE_CHANGED);
            else
                System.out.println("Error - unhandled notify type");

            // update the busy indicator state
            applet.updateBusyIndicators();
            applet.statusBar.showStatus("");
        }
    }

    // method to indicate the object is busy loading data over the network
    //--------------------------------------------------------------------
    public boolean isBusy()
    {
        return isLoading || loadCompleted || areImagesLoading 
               || resolutionChangePending;
    }

    // method to indicate the TOC is currently unstable (i.e. loading or 
    // waiting to be activated)
    //-------------------------------------------------------------------
    public boolean isUnstableTOC()
    {
        return isLoading || loadCompleted;
    }

    // Method to initialize the image area.  Only called on applet startup
    // mosaicCells array indices map to the mosaic grid, columns first:
    //    0 3 6
    //    1 4 7
    //    2 5 8
    //--------------------------------------------------------------------
    public void initialize(double startLat, double startLong) 
    {
        Sensor firstSensor = applet.sensorMenu.getCurrentSensor();
        NavigationModel nm = firstSensor.navModel;

        Point gridCell = nm.latLongToGrid(startLat, startLong);
        gridCol = gridCell.x;
        gridRow = gridCell.y;

        // TBD - Should the mosaicCells array size be configurable by sensor?
        URL codebase = CodeBase.getGlovisURL();
        int index = 0;

        // the column center index also tells us the number of grid columns
        // to the right and left that we need in order to populate all columns
        for (int col = colCenterIndex; col >= -colCenterIndex; col--)
        {
            // and similarly for the row center index
            for (int row = -rowCenterIndex; row <= rowCenterIndex; row++)
            {
                mosaicCells[index] = new TOC(codebase, gridCol + col, 
                                             gridRow + row);
                index++;
            }
        }

        // set up the selected sensor which will also load the scenes and 
        // display them
        setSensor(firstSensor);
    } 

    // scroll in the direction indicated
    //----------------------------------
    public void scrollInDirection(int right, int left, int up, int down)
    {
        // if no real movement, return
        if ((left + right + up + down) == 0)
            return;

        int newGridCol = gridCol;
        int newGridRow = gridRow;
        int newSubCol = 0;
        int newSubRow = 0;

        // determine the signs for moving in the "down" and "right" direction
        int downSign = currSensor.navModel.getRowDownDirection();
        int rightSign = currSensor.navModel.getColumnRightDirection();

        // start from the active cell if only displaying a single scene
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            newGridCol = mosaicCells[activeCellIndex].gridCol;
            newGridRow = mosaicCells[activeCellIndex].gridRow;
        }

        // keep track of a reference scene when scrolling a full-mosaic sensor.
        // This allows the same swath to be followed when scrolling up/down.
        refScrollScene = null;
        if (currSensor.isFullMosaic)
        {
            refScrollScene = getCurrentScene();
        }

        // adjust the column and row with the direction commands
        if (cellsToDisplay == 1)
        {
            // displaying a single cell, so allow moving by partial cells
            // use the current center column and row as a starting point since
            // the subCol and subRow are relative to that
            newGridCol = gridCol;
            newGridRow = gridRow;

            // step by a fractional cell
            newSubCol = subCol + rightSign * (right - left);
            newSubRow =  subRow + downSign * (down - up);
            if (newSubCol >= MosaicData.STEPS)
            {
                newSubCol -= MosaicData.STEPS;
                newGridCol++;
            }
            else if (newSubCol <= -MosaicData.STEPS)
            {
                newSubCol += MosaicData.STEPS;
                newGridCol--;
            }
            if (newSubRow >= MosaicData.STEPS)
            {
                newSubRow -= MosaicData.STEPS;
                newGridRow++;
            }
            else if (newSubRow <= -MosaicData.STEPS)
            {
                newSubRow += MosaicData.STEPS;
                newGridRow--;
            }
        }
        else
        {
            // step by a whole cell
            newGridCol += rightSign * (right - left);
            newGridRow += downSign * (down - up);
        }

        NavigationModel nm = currSensor.navModel;

        // prevent wrapping past the end of the grid boundaries if it is not
        // allowed for this navigation model
        boolean wrapAroundNotAllowed = false;
        if (!nm.allowColumnWrapAround())
        {
            if (newGridCol != nm.checkColumnBounds(newGridCol))
                wrapAroundNotAllowed = true;
        }
        if (!nm.allowRowWrapAround())
        {
            if (newGridRow != nm.checkRowBounds(newGridRow))
                wrapAroundNotAllowed = true;
        }
        if (wrapAroundNotAllowed)
        {
            applet.statusBar.showStatus("Cannot move in that direction");
            return;
        }

        // if the new grid cell is valid according to the model, go to it.
        // Otherwise, issue a message to the statusbar
        if (nm.isValidGridCell(newGridCol, newGridRow))
        {
            if (canMoveToMapArea(newGridCol, newGridRow))
            {
                scrollData(newGridCol,newGridRow,newSubCol,newSubRow,false,
                    true,false);
            }
        }
        else
        {
            // the grid cell isn't a legal one
            applet.statusBar.showStatus(nm.getColName() + "=" + newGridCol
                    + ", " + nm.getRowName() + "=" + newGridRow 
                    + " does not contain data!");
        }
    }
  
    // method to validate that the area selected are valid to the locatormap
    // area displayed.
    //-------------------------------------------------------------------
    public boolean canMoveToMapArea(int newGridCol, int newGridRow)
    {
        boolean canMove = true;
        
        GeographicLocatorMapConfig config
                = locatorMap.getMapConfig(currSensor.locatorMap);

        if ((config != null) && config.enforceGeographicBumper
            &&  currSensor.hasGeographicBumper)
        {
            boolean isValidLocatorMapArea = false;

            NavigationModel nm = currSensor.navModel;
            LatLong latLong = nm.gridToLatLong(newGridCol, newGridRow);
            
            if ((latLong.longitude > config.leftLon) 
                && (latLong.latitude < config.topLat)
                && (latLong.longitude < config.rightLon)
                && (latLong.latitude > config.bottomLat))
            {
                isValidLocatorMapArea = true;
            }
            
            if (!isValidLocatorMapArea)
            {
                canMove = false;
                // the grid cell isn't a legal one
                applet.statusBar.showStatus(nm.getColName() + "=" 
                    + nm.getColumnString(newGridCol) + ", " + nm.getRowName()
                    + "=" + nm.getRowString(newGridRow)
                    + " does not contain data!");
            }
        }
        return canMove;
    }

    // method to cancel the loading of TOC files
    //------------------------------------------
    private void cancelLoad()
    {
        if (isLoading)
            isLoadCancelled = true;
    }

    // method to read the TOC files for the current mosaic area.  It
    // communicates with the loading thread to start the TOC files loading.
    //---------------------------------------------------------------------
    private void readTOCs(TOC[] mosaicCells, boolean[] used, int activeCell,
                          boolean preserveZOrder, boolean haveTargetXY)
    {
        // tell the user we've started loading imagery and show the wait cursor
        applet.statusBar.showStatus("Querying Inventory...");

        // wait until the load thread is done so these flags can be changed
        // without being concerned about a race condition.  This will cause
        // the GUI to pause, but loading TOC files is a relatively quick
        // process.  Another option would be to use a double buffer for 
        // communicating with the load thread, but hopefully that won't be
        // necessary.
        synchronized (loadLock)
        {
            // clear the load completed flag just in case the load just
            // completed.  Otherwise there could be a race condition if it
            // had not been handled yet.
            loadCompleted = false;

            // save the targetXY flag
            isTargetXY = haveTargetXY;

            // set the reference for the cells to load
            loadingMosaicCells = mosaicCells;
            loadingUsed = used;
            loadingActiveCellIndex = activeCell;
            loadingPreserveZOrder = preserveZOrder;

            // set the other flags for tracking the state
            tocChangePending = true;
            isLoadCancelled = false;
            isLoading = true;

            // update the busy indicator state
            applet.updateBusyIndicators();

            // notify the load thread that more work has arrived
            loadLock.notify();
        }
    }

    // method to stop the load thread when the applet is going out of scope
    //---------------------------------------------------------------------
    public void killThread()
    {
        cancelLoad();
        imageLoader.killThread();
        synchronized(loadLock)
        {
            killThread = true;
            loadLock.notify();
        }
    }

    // run method for the TOC loader thread
    //-------------------------------------
    public void run()
    {
        // loading the TOC files is done with the loadLock held.  When there
        // is no work to be done, the lock is released by the loadLock.wait
        synchronized (loadLock)
        {
            while (true)
            {
                // wait until some TOC files need to be read
                while (!isLoading)
                {
                    try
                    {
                        loadLock.wait();
                    }
                    catch (InterruptedException e) {}

                    // return from the routine if the killThread flag is set
                    if (killThread)
                    {
                        loaderThread = null;
                        return;
                    }

                }

                // count the TOC files to load
                numTocsToLoad = 0;
                currTocLoading = 0;
                for (int i = 0; i < loadingMosaicCells.length; i++)
                {
                    if (!loadingMosaicCells[i].valid)
                        numTocsToLoad++;
                }

                // read the TOC files that are not yet valid
                for (int i = 0; i < loadingMosaicCells.length; i++)
                {
                    // exit the loop if the load has been cancelled
                    if (isLoadCancelled)
                        break;

                    if (!loadingMosaicCells[i].valid)
                    {
                        // output a reading message if verbose output requested
                        if (applet.verboseOutput)
                        {
                            System.out.println("Reading TOC at " 
                                + loadingMosaicCells[i].gridCol + " " 
                                + loadingMosaicCells[i].gridRow);
                        }

                        currSensor.readTOC(loadingMosaicCells[i]); 

                        // FIXME -  simulate a slow connection if requested
                        if (applet.slowdown)
                        {
                            try {Thread.currentThread().sleep(400);}
                            catch (InterruptedException e) {}
                        }

                        // keep track of how many TOC files have been loaded
                        currTocLoading++;
                    }
                }

                // wait until the image loader has completed the cancel to make
                // sure the new TOC can be set safely (note that any image 
                // loads in progress have been cancelled when the new TOC file
                // load was started)
                imageLoader.waitUntilDone();

                // if the load wasn't cancelled, flag that load is complete and
                // notify the image pane via a repaint command
                if (!isLoadCancelled)
                {
                    loadCompleted = true;
                    isLoading = false;

                    // notify the image pane that the load is complete
                    pane.repaint();
                }

                // make sure the isLoading flag is cleared in case the load
                // was cancelled
                isLoading = false;
            }
        }
    }

    // method to set the center of the viewing area to the passed in X/Y
    // coordiates.  It uses the current projection to convert this to 
    // lat/long.  Note: The X/Y values are assumed to be the upper left
    // corner coordinate for the display.
    // Returns: true if a new area was loaded, false otherwise
    //----------------------------------------------------------------------
    public boolean gotoXY(int x, int y)
    {
        NavigationModel nm = currSensor.navModel;
        // save the target X/Y
        targetXY.x = x;
        targetXY.y = y;

        int newCol;
        int newRow;
        int startCol = gridCol;
        int startRow = gridRow;
        int newSubCol = subCol;
        int newSubRow = subRow;
        double doubleStep = 1.0/MosaicData.STEPS;

        // start from the active cell if only displaying a single scene
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            startCol = mosaicCells[activeCellIndex].gridCol;
            startRow = mosaicCells[activeCellIndex].gridRow;
        }

        // convert the original displayed UL to a path/row
        Point origXY = mosaicCoords.getUpperLeftCorner();

        // adjust the UL X/Y by half the scrollable distance so that scrolling
        // works as expected when scrollbars are present
        Dimension paneSize = pane.getSize();
        Dimension visibleSize = applet.imgScroll.getSize();

        Point adj = new Point((paneSize.width - visibleSize.width)/2,
                              (paneSize.height - visibleSize.height)/2);
        origXY.x += (int)Math.round(adj.x * actualPixelSize);
        origXY.y -= (int)Math.round(adj.y * actualPixelSize);

        // convert the original displayed UL to a path/row
        DoublePoint origDoubleGrid = nm.projToDoubleGrid(origXY, proj);

        // convert the passed in X/Y to a path/row
        Point dragul = new Point(x,y);
        DoublePoint newDoubleGrid = nm.projToDoubleGrid(dragul, proj);

        // calculate the new column/row assuming that the offset from the upper
        // left corner for the old column/row to the center cell will be the
        // same for the new one.  It isn't really, but it should be close 
        // enough that nobody will notice the error.
        double deltaGridCol = origDoubleGrid.x - (startCol 
                                + (subCol * doubleStep));
        double deltaGridRow = origDoubleGrid.y - (startRow 
                                + (subRow * doubleStep));
        double newDoubleGridCol = newDoubleGrid.x - deltaGridCol;
        double newDoubleGridRow = newDoubleGrid.y - deltaGridRow;
        newCol = (int)(newDoubleGridCol + 0.5);
        newRow = (int)(newDoubleGridRow + 0.5);

        if (cellsToDisplay == 1)
        {
            // adjust for the fractional row/column
            double temp = (newDoubleGridCol - newCol) * MosaicData.STEPS;
            if (temp >= 0)
                temp = temp + 0.5;
            else
                temp = temp - 0.5;
            newSubCol = (int)temp;

            temp = (newDoubleGridRow - newRow) * MosaicData.STEPS;
            if (temp >= 0)
                temp = temp + 0.5;
            else
                temp = temp - 0.5;
            newSubRow = (int)temp;
        }

        // limit the new column/row to the legal values
        newCol = nm.checkColumnBounds(newCol);
        newRow = nm.checkRowBounds(newRow);

        // make sure the new grid location is valid
        if (nm.isValidGridCell(newCol, newRow))
        {
            // scroll to the new location if moving to a new grid cell,
            // fractional grid cell, or if potentially a closer scene to the
            // target might be available
            if ((newCol != startCol) || (newRow != startRow) ||
                    (newSubCol != subCol) || (newSubRow != subRow) || 
                    (currSensor.isFullMosaic && 
                     (cellsToDisplay == Sensor.SINGLE_SCENE)))

            {
                if (canMoveToMapArea(newCol, newRow))
                {
                    // scroll the data to the new location, including the
                    // flag for having a targetXY
                    scrollData(newCol,newRow,newSubCol,newSubRow,false,
                        true,true);

                    // a new area was loaded
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                // didn't need to move, so nothing loaded
                return false;
            }
        }
        else
        {
            // grid cell is not valid, so don't move.  Issue a warning message
            // and return false to indicate no move happened
            applet.statusBar.showStatus("Selected tile does not contain data!");
            return false;
        }
    }

    // method to move the display to a given lat/long location
    //--------------------------------------------------------
    public void gotoLatLong(double latitude, double longitude)
    {
        NavigationModel nm = currSensor.navModel;
        Point gridCell = nm.latLongToGrid(latitude, longitude);
        if (canMoveToMapArea(gridCell.x,gridCell.y))
        {
            targetLatLong = new LatLong(latitude, longitude);
            scrollData(gridCell.x,gridCell.y,0,0,false,true,false);
        }
    }

    // method to display a specific scene.  If the scene is not in the 
    // currently displayed mosaic, the display is moved to the correct
    // location.
    //----------------------------------------------------------------
    public void showScene(Metadata scene)
    {
        // filter the scene passed in against the current search limits.
        // If it is not visible, warn the user the scene cannot be viewed
        // with the current search limits and do nothing
        applet.searchLimitDialog.applySearchLimits(scene);

        if (!scene.visible)
        {
            JOptionPane.showMessageDialog(applet.getDialogContainer(),
                    "Scene " + scene.entityID 
                    + " is filtered out by the search limits",
                    "Scene filtered out", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // assume scrolling to a new location will be needed
        boolean needToScroll = true;

        // see if the inventory for the scene's column and row are currently
        // loaded
        int index = colRowToCell(scene.gridCol,scene.gridRow);
        if (index != -1)
        {
            // scene is in loaded inventory, so find it and display it
            TOC cell = mosaicCells[index];

            int sceneIndex = cell.findScene(scene);
            if (sceneIndex >= 0)
            {
                // if the scene is visible, select the correct cell, put it
                // on top, and load the correct date.  Note if the scene is
                // not visible, it can only be because the viewport filtered
                // it out.  In which case, scrolling to the correct location 
                // will be needed.
                if (cell.scenes[sceneIndex].visible)
                {
                    activeCellIndex = index;
                    zOrderList.putOnTop(cell.scenes[sceneIndex]);
                    drawNewDate(scene.gridCol,scene.gridRow,sceneIndex);
                    needToScroll = false;
                }
            }
            else
            {
                System.out.println("Bug!  scene not found in inventory");
            }
        }

        if (needToScroll)
        {
            // scene is not in current mosaic, so move to the correct area
            // and set the target scene reference
            targetScene = scene; // hold this scene while TOCs load
            // if user searched for a scene, that should be the new target date
            targetDateScene = scene;
            scrollData(scene.gridCol,scene.gridRow,0,0,false,true,false);
        }
    }

    // Routine to scroll the make a new grid column/row visible
    //---------------------------------------------------------
    public void scrollData
    (
        int newCol,         // I: grid column to move to
        int newRow,         // I: grid row to move to
        int newSubCol,      // I: fractional column to move to
        int newSubRow,      // I: fractional row to move to
        boolean preserveZOrder, // I: flag to preserve the current Z-order on
                                //    the display
        boolean reuseOldTOC,// I: flag to allow reuse of the current TOC 
                            //    array to speed load times (false only for
                            //    loading a new sensor)
        boolean haveTargetXY// I: flag to indicate there is a target X/Y 
                            //    coordinate
    )
    {
        // notify any threads loading data to cancel the current load since
        // it is going to change
        cancelLoad();
        imageLoader.cancelLoad();
        mapLayers.cancelLoad();
        isCalledFromScrolledData = true;

        NavigationModel nm = currSensor.navModel;

        int[] rows = new int[mosaicHeight];
        int[] cols = new int[mosaicWidth];

        // calculate the new grid columns and rows
        newCol = nm.checkColumnBounds(newCol);
        int right = nm.getColumnRightDirection();
        for (int col = 0; col < mosaicWidth; col++)
        {
            cols[col] = newCol - right * (colCenterIndex - col);
            if (nm.allowColumnWrapAround())
                cols[col] = nm.checkColumnBounds(cols[col]);
        }

        newRow = nm.checkRowBounds(newRow);
        int down = nm.getRowDownDirection();
        for (int row = 0; row < mosaicHeight; row++)
        {
            rows[row] = newRow - down * (rowCenterIndex - row);
            if (nm.allowRowWrapAround())
                rows[row] = nm.checkRowBounds(rows[row]);

        }

        // allocate an array to remember which entries in the mosaicCells
        // array have been reused.  This is done to allow proper cleanup when
        // TOC entries are reused.
        boolean[] used = new boolean[mosaicCells.length];

        // allocate a new TOC array
        TOC[] newTOC = new TOC[mosaicCells.length];

        // if the old TOC array can be reused, look for column/row cells that
        // have already been read.  If they are available, copy the references
        // to them to a new TOC array 
        if (reuseOldTOC)
        {
            for (int i = 0; i < mosaicCells.length; i++)
            {
                newTOC[i] = null;

                int cell_num = colRowToCell(cols[i / mosaicHeight],
                                            rows[i % mosaicHeight]);
                if (cell_num != -1)
                {
                    newTOC[i] = mosaicCells[cell_num];
                    used[cell_num] = true;
                }
            }
        }

        // all the TOC's that can be reused have been copied.  So now, allocate
        // new ones where needed.
        URL codebase = CodeBase.getGlovisURL();
        for(int i = 0; i < newTOC.length; i++)
        {
            // create a new TOC if needed
            if (newTOC[i] == null)
                newTOC[i] = new TOC(codebase,cols[i / mosaicHeight],
                                    rows[i % mosaicHeight]);
        }

        // set the current gridCol/gridRow
        gridCol = newCol;
        gridRow = newRow;
        subCol = newSubCol;
        subRow = newSubRow;

        // default to the new active cell index to be the center cell.
        // However, if the z-order is supposed to be preserved, use the
        // current active cell.
        int newActiveCellIndex = mosaicCenterIndex;
        if (preserveZOrder)
            newActiveCellIndex = activeCellIndex;
        else
        {
            // not preserving the z-order, so empty the ZOrder list since it
            // will be set to a default order after the TOCs are read
            zOrderList.empty();
        }

        // read any new TOC's that are needed
        readTOCs(newTOC, used, newActiveCellIndex, preserveZOrder,
                 haveTargetXY);
    }

    // method to reload all the currently displayed data from the server
    //------------------------------------------------------------------
    public void refreshDisplay()
    {
        scrollData(gridCol, gridRow, subCol, subRow, true, false, false);
    }

    // method to allow easy updating of the display when entering swath mode
    //----------------------------------------------------------------------
    public void updateDisplay()
    {
        scrollData(gridCol, gridRow, subCol, subRow, false, true, false);
    }

    // method to activate a new TOC array when it has finished loading
    //----------------------------------------------------------------
    private void activateNewTocArray()
    {
        if (loadingPreserveZOrder)
        {
            // the z-order is supposed to be preserved so rebuild the z-order
            // list.  It needs to be rebuilt before the old mosaicCells array
            // is cleaned up so that the old scene entity id's are available
            // for finding the same scene in the new array of TOCs.  This
            // only needs to be done for full mosaics since it is the only
            // situation where the entity id is needed to match up scenes.
            if (currSensor.isFullMosaic)
            {
                ZOrderList z = new ZOrderList();
                z.setMultipleSceneMode();
                zOrderList.top();
                Metadata scene;
                boolean first = true;
                // step through the current z-order list, replacing previous 
                // scenes with the currently selected one
                while ((scene = zOrderList.down()) != null)
                {
                    int cellNum = colRowToCell(scene.gridCol,scene.gridRow);
                    TOC cell = loadingMosaicCells[cellNum];
                    int sceneIndex = cell.findScene(scene);
                    Metadata newScene = cell.scenes[sceneIndex];
                    z.putOnBottom(newScene);

                    // for the first scene found, set the cell's current date
                    // index
                    if (first)
                    {
                        loadingMosaicCells[cellNum].currentDateIndex 
                            = sceneIndex;
                        first = false;
                    }
                }
                // activate the rebuilt z-order list
                zOrderList = z;
            }
        }

        // flush any unused TOCs to make sure any resources used are released
        for (int i = 0; i < mosaicCells.length; i++)
        {
            if (!loadingUsed[i])
            {
                mosaicCells[i].cleanup();
                mosaicCells[i] = null;
            }
        }
        loadingUsed = null;

        // switch to the new TOC and scene filter
        mosaicCells = loadingMosaicCells;
        loadingMosaicCells = null;

        // set the active cell index to the value indicated when the load 
        // was started
        activeCellIndex = loadingActiveCellIndex;

        // Pick the preferred projection code for this mosaic area in case we
        // are on a boundary between two different areas
        projectionCode = pickPreferredProjCode();

        // if the projection code is invalid, get the default one for this area
        if (projectionCode == 1100)
        {
            NavigationModel nm = currSensor.navModel;
            LatLong latLong = nm.gridToLatLong(gridCol,gridRow);
            projectionCode = CreateProjection.getDefaultProjectionCode(
                    currSensor, latLong);
        }
        
        // create the projection transformation object
        proj = CreateProjection.fromProjectionNumber(projectionCode);

        // eliminate cells that do not match the preferred projection code
        for (int i = 0; i < mosaicCells.length; i++)
        {
            TOC cell = mosaicCells[i];

            // flag cells with a different projection code as invalid
            if (cell.projCode != projectionCode) 
                cell.valid = false;

            // set the scene corners for all the scenes in the cell
            cell.setSceneCorners(proj);
        }

        // apply the cloud cover, date, scene list, and quality filters to
        // the TOC's
        int startYear = applet.searchLimitDialog.getStartYear();
        int endYear = applet.searchLimitDialog.getEndYear();
        int startMonth = applet.searchLimitDialog.getStartMonth();
        int endMonth = applet.searchLimitDialog.getEndMonth();
        boolean sceneListFilterEnabled = 
                    applet.searchLimitDialog.isSceneListFilterEnabled();
        boolean downloadableFilterEnabled = 
                    applet.searchLimitDialog.isDownloadableFilterEnabled();
        int minQuality = applet.searchLimitDialog.getMinQuality();
        String dataVersion = applet.searchLimitDialog.getDataVersion();
        boolean userAreaFilterEnabled =
                    applet.searchLimitDialog.isUserDefinedAreaEnabled();
        int startGridCol = applet.searchLimitDialog.getStartGridCol();
        int endGridCol = applet.searchLimitDialog.getEndGridCol();
        int startGridRow = applet.searchLimitDialog.getStartGridRow();
        int endGridRow = applet.searchLimitDialog.getEndGridRow();

        for (int i = 0; i < mosaicCells.length; i++)
        {
            mosaicCells[i].filterScenesToCloudCover(maxCloudCover);
            mosaicCells[i].filterScenesToDateRange(startYear,endYear,
                                                   startMonth,endMonth);
            mosaicCells[i].filterScenesToSceneList(sceneListFilterEnabled);
            mosaicCells[i].filterScenesToDownloadable(downloadableFilterEnabled);
            mosaicCells[i].filterScenesToQuality(minQuality);
            mosaicCells[i].filterScenesToDataVersion(dataVersion);
            mosaicCells[i].filterToHiddenScene();
            mosaicCells[i].filterScenesToGridColRowRange(startGridCol,
                                                endGridCol, startGridRow,
                                                endGridRow);
        }

        // if there is a target lat/long set and this is a full mosaic sensor
        // displaying a single scene, search for the scene that is closest to
        // the target lat/long
        if (targetLatLong != null)
        {
            if (currSensor.isFullMosaic
                && (cellsToDisplay == Sensor.SINGLE_SCENE))
            {
                Point location = proj.latLongToProj(targetLatLong);
                if (location != null)
                {
                    TOC cell = mosaicCells[activeCellIndex];
                    if (cell.valid)
                    {
                        double minDist = 1000000000000.0;
                        int indexFound = -1;
                        for (int i = 0; i < cell.numImg; i++)
                        {
                            Metadata scene = cell.scenes[i];
                            // only consider visible scenes
                            if (scene.visible)
                            {
                                double x = (scene.centerXY.x - location.x);
                                double y = (scene.centerXY.y - location.y);
                                double dist = x * x + y * y;
                                if (dist < minDist)
                                {
                                    indexFound = i;
                                    minDist = dist;
                                }
                            }
                        }
                        // if a closest scene was found , make it the target
                        // scene
                        if (indexFound != -1)
                            targetScene = cell.scenes[indexFound];
                    }
                }
            }
            // clear the target lat/long
            targetLatLong = null;
        }

        // the user area filter requires that the scene location polygons be
        // built.  That is done in the mosaicCoordsUpdate method.  This method
        // needs the current date index which is set in pickSceneDates method.
        // But pickSceneDates needs to be called after the filters are done, 
        // which is why the user area filter is seperate from the filters 
        // above. The pickSceneDates is called again after the user area filter
        // because the visible scenes may change
        // kind of a hack just to get around the catch-22 that prevented
        // user area filter from working above.  The targetScene needs to be
        // saved and restored for the second call to pickSceneDates.
        Metadata savedTargetScene = targetScene;
        pickSceneDates();
        mosaicCoordsUpdate();
        targetScene = savedTargetScene;
        for (int i = 0; i < mosaicCells.length; i++)
            mosaicCells[i].filterScenesToUserArea(userAreaFilterEnabled,
                                                  applet.userDefinedAreaDialog);

        // run pick scene dates again now that the rest of the filtering
        // is complete
        pickSceneDates();

        // build a new scene filter for the current area
        sceneFilter = currSensor.getSceneFilter(this,mosaicCells);

        // if preserving the z-order, rebuild the z-order list, otherwise
        // just use the default z-order
        if (loadingPreserveZOrder)
        {
            // only need to rebuild the z-order for non-full mosaics since the
            // full mosaics were handled earlier
            if (!currSensor.isFullMosaic)
                rebuildZOrder();
        }
        else
            setDefaultZOrder();

        // load the images
        loadScenes();

        // set the display size for the current data (note may not be able
        // to in all cases since for single scene mode, the display size may
        // not be known yet - But the routine will determine that)
        setDisplaySize();

        // set the selected cell to the current active cell, preserving the
        // z-order if requested
        setSelectedCell(activeCellIndex, loadingPreserveZOrder);

        // if a resolution change was made while reading TOC files, perform
        // the resolution change now that the TOC load is complete
        if (resolutionChangePending)
        {
            resolutionChangePending = false;
            setResolution(pixelSize);
        }

        // update the busy indicator state
        applet.updateBusyIndicators();
    }

    // method to allow the resolution to be set
    //-----------------------------------------
    public void setResolution(int resolution)
    {
        // set the pixel size and cells to display for this resolution
        pixelSize = resolution;
        actualPixelSize = currSensor.getActualResolution(pixelSize);
        cellsToDisplay = currSensor.getNumCellsAtResolution(pixelSize);

        // cancel any image or map layer loads that are in process
        imageLoader.cancelLoad();
        mapLayers.cancelLoad();

        // if the user has changed the resolution while TOC files are being
        // read, delay the change until the TOC files have been read
        // (otherwise the images would be loaded for the old TOC)
        if (tocChangePending)
        {
            resolutionChangePending = true;
            return;
        }

        // make sure the fractional row and column is reset on a resolution
        // change, otherwise transitioning from ASTER 400m to 140m can show
        // problems
        subCol = 0;
        subRow = 0;

        // If setting the resolution to display a single cell, make sure the 
        // mosiac is centered on the current cell (makes the rest of the code
        // work out much better at the cost of sometimes recentering
        // unexpectedly)
        if (cellsToDisplay == 1)
        {
            // recenter on the active cell and just load the images to display
            // by treating it like a scrolling command
            TOC activeCell = mosaicCells[activeCellIndex];
            scrollData(activeCell.gridCol,activeCell.gridRow,0,0,false,true,
                       false);
        }
        else
        {
            // clear the viewport scene filter since it is only valid when
            // displaying a single cell
            for (int i = 0; i < mosaicCells.length; i++)
                mosaicCells[i].clearSceneFilters(Metadata.VIEWPORT_FILTER);

            // make sure the last selected scene is on top since otherwise
            // the scene may not be (happens when switching from L7 full res to
            // aster 155m, to 1000m - the scene thought to be on top really 
            // isn't since the scenes are loaded in any order)
            // TBD - this should be solveable in a more generic way
            TOC cell = mosaicCells[activeCellIndex];
            if (cell.valid)
            {
                Metadata scene = cell.scenes[cell.currentDateIndex];
                // when default to selected date is enabled set the default 
                // scenes to display.
                if (applet.toolsMenu.isDefaultToDateEnabled())
                {
                    setDefaultToSelectedDate(scene);
                }

                zOrderList.putOnTop(scene);
                // for full mosaic sensors, make sure the swath is correctly 
                // displayed when changing from 400m to 1000m resolution
                if (currSensor.hasSwathMode 
                    && applet.toolsMenu.isSwathModeEnabled())
                {
                    buildSwath(scene, true);
                }
            }

            // not displaying a single cell, so just load the scenes for the
            // current resolution
            mosaicCoordsUpdate();
            loadScenes();

            // set the display size for the current data (note may not be able
            // to in all cases since for single scene mode, the display size may
            // not be known yet - But the routine will determine that)
            setDisplaySize();

            // make sure the scenes are filtered for the new resolution (this is
            // required for some scene filters - mainly swath based filters)
            sceneFilter.filter();

            // notify the observers that the data has changed along with the
            // mode
            notifyType = MosaicData.DISPLAY_MODE_CHANGE_NOTIFY;
            setChanged();
            notifyObservers(DISPLAY_MODE_CHANGED);
        }
    }

    // helper routine to search the TOC list for the requested grid column 
    // and row returns the cell index if the column/row is found.  
    // Otherwise it returns -1
    //--------------------------------------------------------------------
    public int colRowToCell(int col, int row)
    {
        for (int i = 0; i < mosaicCells.length; i++)
        {
            TOC cell = mosaicCells[i];
            if (cell != null && cell.valid)
            {
                if ((cell.gridCol == col) && (cell.gridRow == row))
                    return i;
            }
        }

        // col/row not found
        return -1;
    }

    // method to return the cell that contains the indicated scene.  Returns
    // null if the scene doesn't belong to the current mosaic cells.
    //----------------------------------------------------------------------
    public TOC getCellForScene(Metadata scene)
    {
        int index = colRowToCell(scene.gridCol,scene.gridRow);
        TOC cell = null;
        if (index >= 0)
            cell = mosaicCells[index];
        return cell;
    }

    // return the currently selected cell
    //-----------------------------------
    public TOC getCurrentCell()
    {
        return mosaicCells[activeCellIndex];
    }

    // return the currently selected scene if current cell is valid, or null
    //----------------------------------------------------------------------
    public Metadata getCurrentScene()
    {
        TOC cell = mosaicCells[activeCellIndex];
        if ((cell != null) && cell.valid)
        {
            Metadata scene = cell.scenes[cell.currentDateIndex];
            if (scene.visible)
                return scene;
            else
                return null;
        }
        else
            return null;
    }

    // Load a scene from the server
    //-----------------------------
    private void loadScene(int cell_num, int scene_number)
    {
        TOC cell = mosaicCells[cell_num];
        Metadata scene = cell.scenes[scene_number];

        // save the date for this column/row in the cache since it has changed
        // from the default date
        // Note: scenes should not be added to the date cache for the defaults,
        // so this routine needs to change before it can be called from the
        // routine that loads all the scenes when an area is first entered
        dateCache.add(cell.gridCol,cell.gridRow,scene.date);

        // load images if one isn't already available, or the loaded one is
        // the wrong resolution
        if ((scene.image == null) || (scene.imageRes != pixelSize))
        {
            if (cellsToDisplay == Sensor.SINGLE_SCENE)
            {
                // only displaying a single scene, so the displayed upper left
                // corner changes to match the loaded scene
                mosaicCoords.updateDisplayedUL(scene.ulX,scene.ulY,cell.valid);
            }

            // have the image loader load the new image
            areImagesLoading = true;
            applet.statusBar.showStatus("Loading Imagery...");
            applet.updateBusyIndicators();
            imageLoader.loadImages(zOrderList,cellsToDisplay,pixelSize,
                                   currSensor);
        }
    }    

    // Draw a new date for the current scene at grid column/row and make
    // it the new selected scene
    //------------------------------------------------------------------
    public void drawNewDate(int col, int row, int newDateIndex)
    {
        // do not do anything if loading a new TOC
        if (isLoading || tocChangePending)
            return;

        int cellNum = colRowToCell(col,row);
        if (cellNum != -1)
        {
            TOC cell = mosaicCells[cellNum];

            // flush the current scene if not a full mosaic or in single 
            // scene mode
            if (!currSensor.isFullMosaic || 
                (cellsToDisplay == Sensor.SINGLE_SCENE))
            {
                // flush any scenes not needed anymore
                flushCurrentDateIndex(cell);

                // handle the case where stepping through single scenes for a
                // full mosaic sensor.  The new date may have an image already
                // loaded that needs flushing.
                cell.currentDateIndex = newDateIndex;
                flushCurrentDateIndex(cell);
            }

            // update the current date index
            cell.currentDateIndex = newDateIndex;

            Sensor currSensor = applet.sensorMenu.getCurrentSensor();

            // if a full mosaic sensor, make sure the active cell index is 
            // updated since it may change with a date change
            if (currSensor.isFullMosaic)
                activeCellIndex = cellNum;

            // only draw a new date if the selected cell is valid
            if (cell.valid)
            {
                Metadata scene = cell.scenes[cell.currentDateIndex];

                // if in swath mode, update all the scenes in the swath to
                // the new date
                if (currSensor.hasSwathMode
                    && applet.toolsMenu.isSwathModeEnabled())
                {
                    buildSwath(scene, activeCellIndex == cellNum);
                }
                
                // when default to selected date is enabled set the default 
                // scenes to display.
                if (currSensor.isFullMosaic)
                {
                    zOrderList.putOnTop(scene);
                }
                else if (applet.toolsMenu.isDefaultToDateEnabled())
                {
                    setDefaultToSelectedDate(scene);
                    rebuildZOrder();
                }
                else
                {
                    // make sure this scene is included in the z-order.  For a
                    // full mosaic, always put it on top.  Otherwise, change
                    // the scene leaving it in the current z-order location.
                    zOrderList.changeScene(scene);
                }

                // load the image for the new date
                loadScene(cellNum,cell.currentDateIndex);

                // if displaying a single scene, re-clip the map layers for the
                // current scene since the upper left corner shifts with each
                // new image
                if (cellsToDisplay == Sensor.SINGLE_SCENE)
                    mapLayers.clip();

                // notify the observers that the data has changed
                notifyType = MosaicData.NORMAL_NOTIFY;
                setChanged();
                notifyObservers();
            }
        }
    }

    // method to return the scene reference at the x,y screen location.
    // null is returned if no scene is found.
    //------------------------------------------------------------
    public Metadata findSceneAt(int x, int y)
    {
        Metadata sceneFound = null;
        int imgHit = -1;

        // if displaying more than a single scene, find the selected one
        if (cellsToDisplay != Sensor.SINGLE_SCENE)
        {
            // find out which image was clicked and update its zOrder
            zOrderList.top();
            Metadata scene;
            while ((scene = zOrderList.down()) != null)
            {
                // if the scene isn't visible, don't consider it
                if (!scene.visible)
                    continue;

                // if the scene contains the point, we have a hit
                if (scene.screenLocation.contains(x,y))
                {
                    sceneFound = scene;
                    break;
                }
            }
        }
        else 
        {
            // displaying a single scene at a time, so if the cell is valid
            // return the active scene in the cell
            TOC cell = mosaicCells[activeCellIndex];
            if (cell.valid)
                sceneFound = cell.scenes[cell.currentDateIndex];
        }

        return sceneFound;
    }

    // method to find all the scenes that intersect the x,y screen location.
    // A vector containing the metadata object for each scene intersected is
    // returned.
    //----------------------------------------------------------------------
    public Vector findScenesAt(int x, int y)
    {
        Vector scenes = new Vector();
        Metadata scene;

        // if in single scene mode, can only be one scene found.  So use the
        // singular version of this routine
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            scene = findSceneAt(x,y);
            if (scene != null)
                scenes.addElement(scene);
        }
        else
        {
            // step through the z-order from top to bottom, adding any scenes
            // intersecting the point to the vector
            zOrderList.top();
            while ((scene = zOrderList.down()) != null)
            {
                // if the scene isn't visible, don't consider it
                if (!scene.visible)
                    continue;

                // if the scene contains the point, we have a hit
                if (scene.screenLocation.contains(x,y))
                {
                    scenes.addElement(scene);
                }
            }
        }
        
        return scenes;
    }

    // Calculate the lat/long at a X/Y coordinate in meters using the
    // current projection.  Note that this method should only be called 
    // if the projection is valid (i.e. TOC is valid).
    //-----------------------------------------------------------------
    public LatLong getLatLong(Point xy)
    {
        return proj.projToLatLong(xy.x,xy.y);
    }

    // provide access to the projection transformation object
    //-------------------------------------------------------
    public ProjectionTransformation getProjection()
    {
        return proj;
    }

    // provide access to the array of mosaic cells.  Note: this should not
    // be used unless absolutely necessary!
    //--------------------------------------------------------------------
    public TOC[] getMosaicCells()
    {
        return mosaicCells;
    }
    
    // set the cloud cover limit
    //--------------------------
    public void setCCLimit(int limit)
    {
        maxCloudCover = limit;

        // keep the search limit and scene list dialogs in sync with the cloud
        // cover limit
        applet.searchLimitDialog.setCloudCover(limit);
        applet.sceneListDialog.updateForChangedSearchLimits();

        // if the TOC is changing, no need to do anything else since the new
        // limit will be enforced when the new TOC is activated
        if (isUnstableTOC())
            return;

        // cancel any images being loaded since images may be flushed due to 
        // TOC changes
        imageLoader.cancelLoad();

        // apply the cloud cover limit to the TOCs
        for (int i = 0; i < mosaicCells.length; i++)
        {
            mosaicCells[i].filterScenesToCloudCover(maxCloudCover);
            mosaicCells[i].findClosestVisibleDate();
        }

        // update the swath if needed
        pickSwathScenes();

        // rebuild the z-order for the scenes that are visible
        rebuildZOrder();

        // scan the active scene to find the first and last legal scene
        // given the new cloud cover limit
        sceneFilter.filter();

        // load scenes to match the current filter conditions
        mosaicCoordsUpdate();
        loadScenes();

        // notify the observers that the data has changed
        notifyType = MosaicData.NORMAL_NOTIFY;
        setChanged();
        notifyObservers();
    }

    // provide access to the number of cells to display
    //-------------------------------------------------
    public int getCellsToDisplay()
    {
        return cellsToDisplay;
    }

    // provide access to the index of active cell
    //-------------------------------------------
    public int getActiveCellIndex()
    {
        return activeCellIndex;
    }

    // provide access to subCol
    //-------------------------
    public int getSubCol()
    {
        return subCol;
    }

    // provide access to subRow 
    //-------------------------
    public int getSubRow()
    {
        return subRow;
    }

    // method to make a deep copy of TOCs(mosaicCells)
    //-----------------------------------------------
    public TOC[] copyTOC()
    {
        TOC[] copyOfMosaicCells = new TOC[mosaicSize];
        for (int i = 0; i < copyOfMosaicCells.length; i++)
        {
            copyOfMosaicCells[i] = new TOC(mosaicCells[i]);
        }
        return copyOfMosaicCells;
    }

    // apply date range limits to the TOCs
    //------------------------------------
    public void applyDataLimit(TOC[] toc, int startYear, int endYear, 
                     int startMonth, int endMonth, int maxCloudCover, 
                     int minQuality, String dataVersion, 
                     boolean userAreaFilterEnabled,
                     boolean sceneListFilterEnabled,
                     boolean downloadableFilterEnabled, int startGridCol,
                     int endGridCol, int startGridRow, int endGridRow)                                           
    {
        for (int i = 0; i < toc.length; i++)
        {
            toc[i].filterScenesToDateRange(startYear,endYear,startMonth,
                                           endMonth);
            toc[i].filterScenesToCloudCover(maxCloudCover);
            toc[i].filterScenesToSceneList(sceneListFilterEnabled);
            toc[i].filterScenesToDownloadable(downloadableFilterEnabled);
            toc[i].filterScenesToQuality(minQuality);
            toc[i].filterScenesToDataVersion(dataVersion);
            toc[i].filterToHiddenScene();
            toc[i].filterScenesToUserArea(userAreaFilterEnabled,
                                          applet.userDefinedAreaDialog);
            toc[i].filterScenesToGridColRowRange(startGridCol,endGridCol,
                                                startGridRow,endGridRow);
        }
    }
    
    // find the scene closest to the selected date
    //--------------------------------------------
    public void setSceneToClosestDate(Metadata scene, int jDate, int year)
    {
        int activeCellIndex = colRowToCell(scene.gridCol, scene.gridRow);
        TOC cell = mosaicCells[activeCellIndex]; 
        
        int index = cell.findDate(year, jDate);
        
        if (index != -1)
            drawNewDate(cell.gridCol, cell.gridRow, index);
    }        
    
    // method to set the limits from the search limit dialog box
    //-----------------------------------------------------------
    public void setSearchLimitValues(int startYear, int endYear, int startMonth,
                    int endMonth, int cloudCoverLimit, int minQuality, 
                    String dataVersion, int startGridCol, int endGridCol,
                    int startGridRow, int endGridRow)
    {
        maxCloudCover = cloudCoverLimit;

        boolean sceneListFilterEnabled = 
                        applet.searchLimitDialog.isSceneListFilterEnabled();

        boolean downloadableFilterEnabled = 
                        applet.searchLimitDialog.isDownloadableFilterEnabled();

        boolean userAreaFilterEnabled =
                        applet.searchLimitDialog.isUserDefinedAreaEnabled();

        // update the cloud cover limit in the main applet and update the 
        // scene list dialog with the new limits
        applet.maxCC.setCloudCover(maxCloudCover);
        applet.sceneListDialog.updateForChangedSearchLimits();

        // if the TOC is changing, no need to do anything else since the new
        // limit will be enforced when the new TOC is activated
        if (isUnstableTOC())
            return;

        // cancel any images being loaded since images may be flushed due to 
        // TOC changes
        imageLoader.cancelLoad();

        // update mosaic coordinates
        mosaicCoordsUpdate();
        
        // apply the date range limits to the TOCs
        applyDataLimit(mosaicCells, startYear, endYear, startMonth, endMonth, 
                       maxCloudCover, minQuality, dataVersion, 
                       userAreaFilterEnabled, sceneListFilterEnabled,
                       downloadableFilterEnabled,
                       startGridCol, endGridCol, startGridRow, endGridRow);

        // make sure the selected scene in each TOC is visible.  If not, pick
        // the nearest date that is still visible
        for (int i = 0; i < mosaicCells.length; i++)
            mosaicCells[i].findClosestVisibleDate();

        // update the swath if needed
        pickSwathScenes();
        
        // rebuild the z-order for the scenes that are visible
        rebuildZOrder();

        // scan the active scene to find the first and last legal scene
        // given the date range limit
        sceneFilter.filter();

        // load scenes to match the current filter conditions
        mosaicCoordsUpdate();
        loadScenes();

        // notify the observers that the data has changed
        notifyType = MosaicData.NORMAL_NOTIFY;
        setChanged();
        notifyObservers();
        
    }

    // helper method to rebuild the ZOrderList after potentially picking new
    // scenes as the selected scenes.
    //----------------------------------------------------------------------
    private void rebuildZOrder()
    {
        Metadata scene;

        if (currSensor.isFullMosaic)
        {
            // for a full mosaic, if the top scene is still visible, no further
            // action is needed
            zOrderList.top();
            scene = zOrderList.down();
            if ((scene != null) && !scene.visible)
            {
                // the top scene is not visible, so look for the topmost scene
                // that is still visible and make it the top scene in the
                // z-order
                while ((scene = zOrderList.down()) != null)
                {
                    if (scene.visible)
                    {
                        zOrderList.putOnTop(scene);
                        int cellNum = colRowToCell(scene.gridCol,scene.gridRow);
                        activeCellIndex = cellNum;
                        // make sure the current date index in the cell agrees
                        // with the top scene
                        TOC cell = mosaicCells[activeCellIndex];
                        cell.currentDateIndex = cell.findScene(scene);
                        break;
                    }
                }
            }
        }
        else
        {
            // for non-full mosaic sensors, rebuild the z-order list with
            // the scenes currently shown.  This is needed since the z-order
            // list is based on the scenes in the list, not the cell index.
            ZOrderList z = new ZOrderList();
            z.setSingleSceneMode();
            zOrderList.top();

            // step through the current z-order list, replacing previous 
            // scenes with the currently selected one
            while ((scene = zOrderList.down()) != null)
            {
                int cellNum = colRowToCell(scene.gridCol,scene.gridRow);
                TOC cell = mosaicCells[cellNum];
                z.putOnBottom(cell.scenes[cell.currentDateIndex]);
            }
            // activate the rebuilt z-order list
            zOrderList = z;
        }
    }

    // Return a reference to the z-order list so it can be drawn in the correct
    // bottom to top order.  The z-order list is set to the correct state for
    // the current display options before returning.  This method needs to be
    // called each time the display is drawn.
    //-------------------------------------------------------------------------
    public ZOrderList getZOrder()
    {
        // when showing a single scene, just show the top scene, otherwise 
        // start at the bottom
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            zOrderList.top();
            zOrderList.down();
            // make sure the z-order is still in a valid state since switching
            // directions isn't really legal
            if (zOrderList.up() == null)
            {
                // the z-order isn't in a legal state since there was just one
                // scene in it.  So just start at the bottom.
                zOrderList.bottom();
            }
            else
            {
                // the operation worked, so do it again to get it back to 
                // the original state
                zOrderList.top();
                zOrderList.down();
            }
        }
        else
            zOrderList.bottom();
        return zOrderList;
    }

    // method to check whether a scene can be displayed.  Returns true if
    // it can be displayed.
    //-------------------------------------------------------------------
    public boolean canDisplay(Metadata scene)
    {
        if (!scene.visible)
            return false;

        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            // if displaying a single scene at a time, return false if the
            // current scene isn't from the currently displayed gridCol and
            // row
            TOC cell = mosaicCells[activeCellIndex];
            if ((scene.gridRow != cell.gridRow) || 
                (scene.gridCol != cell.gridCol))
                return false;
        }

        return true;
    }

    // returns true if the scene highlight can be shown for current display 
    // mode
    //---------------------------------------------------------------------
    public boolean canShowHighlight()
    {
        if (cellsToDisplay != Sensor.SINGLE_SCENE)
            return true;
        return false;
    }

    // method to cleanup any resources when the applet is stopped
    //-----------------------------------------------------------
    public void cleanup()
    {
        for (int i = 0; i < mosaicCells.length; i++)
        {
            if (mosaicCells[i] != null)
                mosaicCells[i].cleanup();
        }
    }

    // methods to return information about the current size of the mosaic
    //-------------------------------------------------------------------
    public int getMosaicSize()
    {
        return mosaicSize;
    }
    public int getMosaicWidth()
    {
        return mosaicWidth;
    }
    public int getMosaicHeight()
    {
        return mosaicHeight;
    }
    public int getMosaicCenterIndex()
    {
        return mosaicCenterIndex;
    }
}
