// MapLayers.java implements the container class for all the map layers.
//--------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MapLayers implements Runnable, WorkMonitor, MapLayerLoadingCallback
{
    public boolean layersOn = false;// map layers initally not loaded
    private ImagePane imagePane;    // Image Pane map layers are drawn in
    private imgViewer applet;       // applet

    private MapLayer[] layers;      // array for all the map layers

    // Controls the order to draw the map layers.
    // First railroads, roads, water, admin, country, scene overlay, grid,
    // protected area polygons, protected area points, cities, north arrow,
    // point of interest.
    // The last type drawn shows up on top.
    private int[] drawOrder = {9,10,12,1,4,11,3,8,7,2,5,0,6};

    private boolean needClipping;   // flag to indicate the map layers need to
                                    // be clipped to the display area before
                                    // drawing
    private boolean areLayersValid; // flag to indicate map layers are valid
                                    // for drawing

    // cached values from other objects that are used in the thread to load
    // the map layers
    private int projCode;   // the current projection code

    // variables used to control the thread that loads the map layers
    private Thread loadThread;      // the load thread
    private boolean isLoading;      // flag to indicate map layers are
                                    // currently being loaded
    private CancelLoad isLoadCancelled = new CancelLoad(); // flag to indicate 
                                    // the map layer load should be cancelled
    private boolean isLoadingFiles; // flag to indicate files are actually 
                                    // loading
    private boolean killThread;     // flag to indicate the thread should
                                    // be killed
    private int numMapFilesToLoad;  // total number of map files to load
    private int currMapFileLoading; // current map file loading of the total
    private PointOfInterestMapLayer pointOfInterest;
    private AddressSearchMapLayer addressSearchMapLayer;
    private int normalLayerCount;   // count of the standard map layers
                                    // (without user loaded shapefiles
    private ShapeFileAttributesDialog attributesDialog;
    private ModifyLayerColorsDialog colorsDialog;
    private boolean disableExtraLayerDrawing; // flag to indicate the extra
                                    // map layers are not being drawn
    private SceneOverlayMapLayer sceneOverlayMapLayer; // scene overlay map
                                    // layer object

    // create a buffer for storing a load that has been submitted by calling
    // load, but not yet started on by the actual load thread.  This
    // buffer eliminates the need to wait for the loading thread to complete a
    // cancel before submitting a new set of images to load (and therefore
    // eliminates some GUI hangs).
    private class PendingLoad
    {
        LatLong minboxULdeg;
        LatLong minboxLRdeg;
        Point ulMeters;
        Point lrMeters;
        int projCode;
        boolean areLayersValid;
        boolean pending;
    }
    private PendingLoad pendingLoad;

    // constructor for the MapLayers container
    //----------------------------------------
    public MapLayers(imgViewer imgviewer, ImagePane pane)
    {
        applet = imgviewer;
        imagePane = pane;

        // create and place the map layers in the layers array.  Note that the
        // order of items in this array matches the order the layers are shown
        // in the map layer menu
        layers = new MapLayer[13];
        addressSearchMapLayer = new AddressSearchMapLayer(imgviewer,
                                                          KeyEvent.VK_D);
        layers[0] = addressSearchMapLayer;
        layers[1] = new LineMapLayer(imgviewer, "Admin Boundaries",
                        "Political", Color.WHITE, 1.5F, false, true,
                        KeyEvent.VK_A, false);
        layers[1].setLayerOn(MapLayersConfig.DISPLAY_ADMIN_BOUNDARIES);
        layers[2] = new WorldCitiesMapLayer(imgviewer, Color.YELLOW,
                        KeyEvent.VK_C);
        layers[2].setLayerOn(MapLayersConfig.DISPLAY_WORLD_CITIES);
        layers[3] = new GridMapLayer(imgviewer, new Color(255,226,55),
                        KeyEvent.VK_G);
        layers[3].setLayerOn(MapLayersConfig.DISPLAY_COLLECTION_GRID);
        layers[4] = new LineMapLayer(imgviewer, "Country Boundaries",
                        "Country", Color.WHITE, 3, false, true, KeyEvent.VK_B,
                        true);
        layers[4].setLayerOn(MapLayersConfig.DISPLAY_COUNTRY_BOUNDARIES);
        layers[5] = new NorthArrowMapLayer(imgviewer, KeyEvent.VK_N);
        layers[5].setLayerOn(MapLayersConfig.DISPLAY_NORTH_ARROW);
        pointOfInterest = new PointOfInterestMapLayer(imgviewer,
                                    KeyEvent.VK_I);
        layers[6] = pointOfInterest;
        layers[6].setLayerOn(MapLayersConfig.DISPLAY_POINTS_OF_INTEREST);
        layers[7] = new LineMapLayer(imgviewer, "Protected Area Points",
                        "ProtPoint", Color.YELLOW, 1, true, false,
                        KeyEvent.VK_O, false);
        layers[7].setLayerOn(MapLayersConfig.DISPLAY_PROTECTED_AREA_POINTS);
        layers[8] = new LineMapLayer(imgviewer, "Protected Area Polygons",
                        "ProtArea", Color.YELLOW, 1, true, false,
                        KeyEvent.VK_P, false);
        layers[8].setLayerOn(MapLayersConfig.DISPLAY_PROTECTED_AREA_POLYGONS);
        layers[9] = new LineMapLayer(imgviewer, "Railroads", "Railroad", 
                        new Color(255,0,153), 1, false, false, KeyEvent.VK_L,
                        false);
        layers[9].setLayerOn(MapLayersConfig.DISPLAY_RAILROADS);
        layers[10] = new LineMapLayer(imgviewer, "Roads", "Road", 
                        new Color(255,204,153), 1, false, false,
                        KeyEvent.VK_R, false);
        layers[10].setLayerOn(MapLayersConfig.DISPLAY_ROADS);
        sceneOverlayMapLayer = new SceneOverlayMapLayer(imgviewer,
                        new Color(0, 255, 0), KeyEvent.VK_V);
        layers[11] = sceneOverlayMapLayer;
        layers[11].setLayerOn(MapLayersConfig.DISPLAY_SCENE_LIST_OVERLAY);
        layers[12] = new LineMapLayer(imgviewer, "Water", "Water", 
                        new Color(0,192,227), 1, false, false, KeyEvent.VK_W,
                        true);
        layers[12].setLayerOn(MapLayersConfig.DISPLAY_WATER);
        normalLayerCount = 13;

        // create the shapefile attributes dialog
        attributesDialog 
            = new ShapeFileAttributesDialog(applet.getDialogParent());

        // create the dialog for modify layer colors
        colorsDialog = new ModifyLayerColorsDialog(applet, layers);

        // create threading objects
        pendingLoad = new PendingLoad();
        loadThread = new Thread(this,"Map Layer Loader");
        loadThread.start();
    }

    // method to cleanup resources used
    //---------------------------------
    public void cleanup()
    {
        attributesDialog.dispose();
        attributesDialog = null;
        sceneOverlayMapLayer.cleanup();
        colorsDialog.dispose();
        colorsDialog = null;
    }

    // method to show the shapefile attributes dialog
    //-----------------------------------------------
    public void showAttributes()
    {
        Point loc = applet.getDialogLoc();
        loc.y += 30;
        attributesDialog.setLocation(loc);
        attributesDialog.setVisible(true);
    }

    // method to return the reference to the shapefile attributes dialog
    //------------------------------------------------------------------
    public ShapeFileAttributesDialog getAttributesDialog()
    {
        return attributesDialog;
    }

    // method to show the scene overlay configuration dialog
    //------------------------------------------------------
    public void showSceneOverlayConfiguration()
    {
        sceneOverlayMapLayer.showConfigureDialog();
    }

    // method to show a dialog for modifying the map layer colors
    //-----------------------------------------------------------
    public void showModifyLayerColors()
    {
        // if the dialog isn't visible, reset it to show the current colors
        if (!colorsDialog.isVisible())
            colorsDialog.setColors();

        // set the dialog box location
        Point loc = applet.getDialogLoc();
        loc.y += 30;
        colorsDialog.setLocation(loc);
        colorsDialog.setVisible(true);
    }

    // method to temporarily disable the extra map layers
    //---------------------------------------------------
    public void disableExtraMapLayers(boolean state)
    {
        if (layers.length > normalLayerCount)
        {
            if (layersOn)
            {
                disableExtraLayerDrawing = state;
                applet.imgArea.repaint();
            }
        }
    }

    // methods required for the WorkMonitor interface
    //-----------------------------------------------
    public String getWorkLabel() { return "Loading Map Layers"; }
    public boolean isWorking() { return isBusy(); }
    public int getTotalWork() { return numMapFilesToLoad; }
    public int getWorkComplete() { return currMapFileLoading; }

    // method required for the MapLayerLoadingCallback interface
    public void incrementFileReadCounter() { currMapFileLoading++; }

    // method to return the number of map layers available
    //----------------------------------------------------
    public int getNumberOfLayers()
    {
        return layers.length;
    }

    // method to return the number of map layers available, not including
    // user loaded shapefiles
    //-------------------------------------------------------------------
    public int getNumberOfStandardLayers()
    {
        return normalLayerCount;
    }

    // method to return the map layer at a particular index in the array
    //------------------------------------------------------------------
    public MapLayer getLayerAt(int layerIndex)
    {
        return layers[layerIndex];
    }

    // method to add a map layer to the list of map layers.  Returns the index
    // of the added layer.
    //------------------------------------------------------------------------
    public int addMapLayer(MapLayer layer)
    {
        // grow the map layer arrays by one
        int numLayers = getNumberOfLayers();
        numLayers++;
        MapLayer[] newLayers = new MapLayer[numLayers];
        int[] newDrawOrder = new int[numLayers];
        
        int lastLayer = numLayers - 1;
        for (int i = 0; i < lastLayer; i++)
        {
            newLayers[i] = layers[i];
            newDrawOrder[i] = drawOrder[i];
        }
        newLayers[lastLayer] = layer;
        newDrawOrder[lastLayer] = lastLayer;

        drawOrder = newDrawOrder;
        layers = newLayers;

        return lastLayer;
    }

    // method to return the pointOfInterestMapLayer
    //-------------------------------------------
    public PointOfInterestMapLayer getPointOfInterestMapLayer()
    {
        return pointOfInterest; 
    }

    // method to return the address results map layer
    //-----------------------------------------------
    public AddressSearchMapLayer getAddressSearchMapLayer()
    {
        return addressSearchMapLayer;
    }

    // method to display the map layers
    //---------------------------------
    public void showLayers()
    {
        layersOn = true;
        disableExtraLayerDrawing = false;
        startLoad(areLayersValid);
    }

    // method to clear the layers from the display
    // -------------------------------------------
    public void clearLayers()
    {
        layersOn = false;
        disableExtraLayerDrawing = false;
        imagePane.clear();
        imagePane.repaint();
    }

    // method to cancel a currently running map layer load
    //----------------------------------------------------
    public void cancelLoad()
    {
        // note: it is assumed that this method and the load method
        // will always be called from the same thread since no synchronization
        // is used here.
        if (isLoading)
        {
            // flag the load thread to cancel the load
            isLoadCancelled.cancelled = true;
        }
    }

    // method to return true if map layer files are being loaded
    //----------------------------------------------------------
    public boolean isBusy()
    {
        return isLoadingFiles;
    }

    // method to load the map layer files from the server
    //---------------------------------------------------
    public void load(Point ulMeters, Point lrMeters, 
                     ProjectionTransformation proj, int projCode)
    {
        LatLong ulLatLong = new LatLong(0.0,0.0);
        LatLong lrLatLong = new LatLong(0.0,0.0);

        // only need to calculate the minimum bounding box for 
        // non-sinusoidal projections
        if (projCode != CreateProjection.SINUSOIDAL)
        {
            // convert the projection extents to lat/long since that is how the 
            // map layers are tiled
            MinBox.calculateMinBox(ulMeters, lrMeters, proj, ulLatLong,
                                   lrLatLong);
        }

        // load the pending load structure with relevant info
        synchronized (pendingLoad)
        {
            // if map layers are already being loaded, cancel the current load
            // if the area to load has changed
            boolean doCancel = false;
            if (isLoading)
            {
                if ((ulLatLong.latitude != pendingLoad.minboxULdeg.latitude)
                   || (ulLatLong.longitude !=pendingLoad.minboxULdeg.longitude)
                   || (lrLatLong.latitude != pendingLoad.minboxLRdeg.latitude)
                   || (lrLatLong.longitude !=pendingLoad.minboxLRdeg.longitude)
                   || (ulMeters.x != pendingLoad.ulMeters.x)
                   || (ulMeters.y != pendingLoad.ulMeters.y)
                   || (lrMeters.x != pendingLoad.lrMeters.x)
                   || (lrMeters.y != pendingLoad.lrMeters.y)
                   || (projCode != pendingLoad.projCode))
                {
                    doCancel = true;
                }
            }

            pendingLoad.minboxULdeg = ulLatLong;
            pendingLoad.minboxLRdeg = lrLatLong;
            pendingLoad.ulMeters = ulMeters;
            pendingLoad.lrMeters = lrMeters;
            pendingLoad.projCode = projCode;
            pendingLoad.areLayersValid = false;
            pendingLoad.pending = true;

            // if already loading and the area to display has changed, cancel
            // the current load
            if (isLoading && doCancel)
            {
                isLoadCancelled.cancelled = true;
            }
            pendingLoad.notify();
        }
    }

    // helper method to start the map layers loading
    //----------------------------------------------
    private void startLoad(boolean areLayersValid)
    {
        // if the pendingLoad structure isn't initialized yet, do nothing
        if (pendingLoad.ulMeters == null)
            return;

        // setup the pending load like with the current info
        synchronized (pendingLoad)
        {
            if (!pendingLoad.pending)
            {
                // Note: most parameters in the pendingLoad are expected to
                // be available from the last call to the load method
                pendingLoad.areLayersValid = 
                    areLayersValid && this.areLayersValid;
                pendingLoad.pending = true;
            }

            needClipping = true;
            if (isLoading)
                isLoadCancelled.cancelled = true;
            if (layersOn)
                pendingLoad.notify();
        }
    }

    // method to stop the load thread when the applet is going out of scope
    //---------------------------------------------------------------------
    public void killThread()
    {
        synchronized(pendingLoad)
        {
            cancelLoad();
            killThread = true;
            pendingLoad.notify();
        }
    }
    
    // thread to implement loading of map layers
    //------------------------------------------
    public void run()
    {
        Point ulMeters = new Point(0,0); // upper left X/Y in meters
        Point lrMeters = new Point(0,0); // lower right X/Y in meters

        // loop forever - if the thread is asked to end, it will break out
        // of the loop
        while (true)
        {
            LatLong minboxULdeg = null; // upper left lat/long (minbox)
            LatLong minboxLRdeg = null; // lower right lat/long (minbox)

            // if the loading flag isn't set, release the load lock and 
            // wait to be notified that map layers need loading
            while (!isLoading)
            {
                synchronized (pendingLoad)
                {
                    // if no pending load and the thread hasn't been killed,
                    // wait for one of them
                    if (!pendingLoad.pending && !killThread)
                    {
                        try 
                        {
                            pendingLoad.wait();
                        }
                        catch (InterruptedException e){}
                    }

                    // if load pending now, move the data from the pending
                    // load to the real load
                    if (pendingLoad.pending)
                    {
                        minboxULdeg = pendingLoad.minboxULdeg;
                        minboxLRdeg = pendingLoad.minboxLRdeg;
                        ulMeters = pendingLoad.ulMeters;
                        lrMeters = pendingLoad.lrMeters;
                        projCode = pendingLoad.projCode;
                        areLayersValid = pendingLoad.areLayersValid;
                        isLoading = true;
                        isLoadCancelled.cancelled = false;
                        pendingLoad.pending = false;
                    }

                    // return from the routine if the killThread flag is set
                    if (killThread)
                    {
                        loadThread = null;
                        return;
                    }
                }
            }

            int numFiles = 0;  // Number of files required to get coverage

            currMapFileLoading = 0;
            numMapFilesToLoad = 0;

            // set the display area for the layers
            for (int ii = 0; ii < layers.length; ii++)
            {
                if (layers[ii].isLayerOn())
                {
                    if (projCode != CreateProjection.SINUSOIDAL)
                    {
                        // use Lat/Longs
                        numFiles +=
                            layers[ii].setDisplayAreaUsingLatLong(minboxULdeg, 
                                minboxLRdeg, projCode);
                    }
                    else
                    {
                        // use Projection Coordinates
                        numFiles +=
                            layers[ii].setDisplayAreaUsingProjCoords(ulMeters, 
                                lrMeters, projCode);
                    }
                }
            }

            // if any files really need to be loaded, turn on the wait
            // indicator
            numMapFilesToLoad = numFiles;
            if (numFiles > 0)
                isLoadingFiles = true;

            if (isLoadingFiles)
                applet.updateBusyIndicators();

            // load the files as needed
            for (int ii = 0; ii < layers.length; ii++)
            {
                if (layers[ii].isLayerOn())
                {
                    layers[ii].read(isLoadCancelled, ulMeters, projCode, this);
                    if (isLoadCancelled.cancelled)
                    {
                        if (applet.verboseOutput)
                            System.out.println("Linework load cancelled");
                        isLoadCancelled.cancelled = false;
                        isLoading = false;
                        isLoadingFiles = false;
                        applet.updateBusyIndicators();
                        break;
                    }
                }
            }

            // update the various flags and notify the imagePane it should
            // repaint to get the map layers
            isLoading = false;
            isLoadingFiles = false;
            needClipping = true;
            areLayersValid = true;
            applet.updateBusyIndicators();
            imagePane.repaint();
        }
    }

    // method to clip the map layers for the currently displayed area
    //---------------------------------------------------------------
    public void clip()
    {
        // do not attempt to clip files while map layers are invalid
        if (!areLayersValid)
            return;

        // cache the upper left corner and pixel size based on whether the
        // full resolution or 3x3 is shown
        Point ul = imagePane.getUpperLeftCorner();
        // if upper left isn't valid, just return
        if (ul == null)
            return;

        needClipping = false;

        // get the current projection
        // FIXME - instead of getting this here, should probably save it
        // when load is called and use that
        ProjectionTransformation proj = imagePane.md.getProjection();
        int pixelSize = imagePane.md.pixelSize;
        Dimension dispSize = imagePane.getSize();

        for (int ii = 0; ii < layers.length; ii++)
        {
            if (layers[ii].isLayerOn())
                layers[ii].clip(ul, pixelSize, dispSize, proj);
        }
    }

    // The findFeatureName searches the visible map layers to see if any of
    // the layers have a named feature under the x/y screen location provided.
    // If a name is found, it is returned.
    //------------------------------------------------------------------------
    public String findFeatureName(int x, int y, boolean updateAttributeWindow)
    {
        // do not attempt to paint while map layers are invalid
        if (!areLayersValid)
            return null;

        if (layersOn) 
        {
            MapLayerFeatureInfo info = null;
            int usedIndex = -1;

            // loop through each map layers, from the top drawn layer to the
            // bottom layer
            for (int type = layers.length - 1; type >= 0; type--)
            {
                // determine which type to draw on this pass
                int currTypeDrawing = drawOrder[type];

                // skip this layer if it isn't turned on
                if (!layers[currTypeDrawing].isLayerOn())
                    continue;

                // check for a feature under the mouse cursor
                MapLayerFeatureInfo newInfo 
                        = layers[currTypeDrawing].findFeatureName(x, y);

                if (newInfo != null)
                {
                    // feature found, so if it has a smaller area than a
                    // previously found feature, make it the new feature to use
                    if ((info == null) || (newInfo.area < info.area))
                    {
                        info = newInfo;
                        usedIndex = currTypeDrawing;
                    }
                }
            }

            if (info != null)
            {
                // a feature was found, so if updating the attribute window, do
                // so now and then return the feature name
                if (updateAttributeWindow)
                    layers[usedIndex].updateAttributeWindow();
                return info.name;
            }
        }

        return null;
    }

    // The paint method draws the current set of map layers to the graphics
    // context passed in
    //---------------------------------------------------------------------
    public void paint(Graphics g)
    {
        // do not attempt to paint while map layers are invalid
        if (!areLayersValid)
            return;

        if (layersOn) 
        {
            // clip the layers to the current display area if needed
            if (needClipping)
                clip();

            // loop through each map layers
            for (int type = 0; type < layers.length; type++)
            {
                // determine which type to draw on this pass
                int currTypeDrawing = drawOrder[type];

                // skip this layer if it isn't turned on
                if (!layers[currTypeDrawing].isLayerOn())
                    continue;

                // check if the extra map layers shouldn't be drawn
                if ((currTypeDrawing >= normalLayerCount) 
                    && disableExtraLayerDrawing)
                    continue;

                layers[currTypeDrawing].draw(g);
            }
        }
    }
}

// The ModifyLayerColorsDialog class implements a dialog box that allows
// selecting the sensors to show for the scene overlay map layer.
//--------------------------------------------------------------------------
class ModifyLayerColorsDialog extends JDialog implements ActionListener
{
    private Color[] colors;     // array of colors currently selected
    private JButton[] colorButtons; // array for the color buttons
    private MapLayer[] layers;  // map layers

    // constructor for the ModifyLayerColorsDialog
    //--------------------------------------------
    public ModifyLayerColorsDialog(imgViewer applet, MapLayer[] layers)
    {
        // create a modal dialog located by the parent component
        super(new JFrame(), "Modify Map Layer Colors", false);

        this.layers = layers;

        // use a border layout
        getContentPane().setLayout(new BorderLayout());

        // place a message at the top of the dialog box
        JLabel label = new JLabel("Modify the map layer colors");
        getContentPane().add(label, "North");

        // create the buttons at the bottom of the dialog
        JPanel buttonPanel = new JPanel();
        JButton defaultButton = new JButton("Default");
        defaultButton.setMnemonic(KeyEvent.VK_D);
        defaultButton.setToolTipText("Reset to default colors");
        buttonPanel.add(defaultButton);
        JButton okButton = new JButton("OK");
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.setToolTipText("Accept changes and close dialog");
        buttonPanel.add(okButton);
        JButton applyButton = new JButton("Apply");
        applyButton.setMnemonic(KeyEvent.VK_A);
        applyButton.setToolTipText("Apply changes");
        buttonPanel.add(applyButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setToolTipText(
                    "Cancel unapplied changes and close dialog");
        buttonPanel.add(cancelButton);
        getContentPane().add(buttonPanel, "South");

        // listen for button presses
        defaultButton.addActionListener(this);
        okButton.addActionListener(this);
        applyButton.addActionListener(this);
        cancelButton.addActionListener(this);

        // build a label and button for each map layer
        JPanel layerPanel = new JPanel();
        colors = new Color[layers.length];
        colorButtons = new JButton[layers.length];
        layerPanel.setLayout(new GridLayout(layers.length, 2));
        for (int i = 0; i < layers.length; i++)
        {
            colors[i] = layers[i].getColor();
            if (colors[i] != null)
            {
                JLabel layerName = new JLabel(layers[i].getName());
                colorButtons[i] = new JButton("Change");
                colorButtons[i].setToolTipText("Change color for "
                        + layers[i].getName() + " layer");
                colorButtons[i].setActionCommand("" + i);
                colorButtons[i].setBackground(colors[i]);
                colorButtons[i].addActionListener(this);
                layerPanel.add(layerName);
                layerPanel.add(colorButtons[i]);
            }
        }

        JScrollPane scrollPane = new JScrollPane(layerPanel);
        getContentPane().add(scrollPane, "Center");

        // set the size of the dialog
        setSize(360,460);
    }

    // method to update the colors shown for the map layers
    //-----------------------------------------------------
    public void setColors()
    {
        for (int i = 0; i < layers.length; i++)
        {
            colors[i] = layers[i].getColor();
            if (colors[i] != null)
                colorButtons[i].setBackground(colors[i]);
        }
    }

    // method to set the colors in the actual map layers
    //--------------------------------------------------
    public void updateMapLayers()
    {
        for (int i = 0; i < layers.length; i++)
        {
            layers[i].setColor(colors[i]);
        }
    }

    // actionPerformed handles the button presses in the dialog
    //---------------------------------------------------------
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();

        if (command.equals("OK"))
        {
            updateMapLayers();
            setVisible(false);
        }
        if (command.equals("Apply"))
        {
            updateMapLayers();
        }
        else if (command.equals("Cancel"))
        {
            setVisible(false);
        }
        else if (command.equals("Default"))
        {
            // return the colors to their default colors
            for (int i = 0; i < layers.length; i++)
            {
                colors[i] = layers[i].getOriginalColor();
                if (colors[i] != null)
                    colorButtons[i].setBackground(colors[i]);
            }
        }
        else
        {
            try
            {
                int index = Integer.parseInt(command);
                Color color = JColorChooser.showDialog(this,
                   "Choose the color for the " + layers[index].getName()
                   + " map layer", colors[index]);
                if (color != null)
                {
                    colors[index] = color;
                    colorButtons[index].setBackground(color);
                }

            }
            catch (Exception exception)
            {
            }
        }
    }
}
