//  TOC (Table of Contents) object
//
//  The Table of Contents is where we store all the meta & geoData.
//  This is an ASCII file, located in the same directory tree as the imagery 
//  on the server.  
//
//  Files are indexed on disk in directories by sensor, gridCol, gridRow
//  File format:
//    - line 1: 
//        gridCol, gridRow, projCode, hasLines flag (not be used anymore since
//        lines are available everywhere), hasMetrics (not used anymore),
//        numImg
//    - numImg lines of metadata for each scene containing (all ints):
//        date in format YYYYMMDD
//        julian day of year if julian date available for the sensor
//        ulX coordinate
//        ulY coordinate
//        cloud cover percentage
//        entity ID
//        quality values (vary by sensor)
//        offsets for each scene if the sensor has them (sample,line order)
//        
//  Note: gridCol is the equivalent of WRS path and gridRow is the equivalent
//        of WRS row.
//--------------------------------------------------------------------------- 
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;


public class TOC
{
    public Metadata[] scenes;   // array of metadata for each scene
    public String cellDir;      // URL to this cell directory
    public boolean hasMetrics;  // flags metrics are available
    public boolean hasLines;    // flags linework is available
    public boolean valid;       // flag for this TOC valid
    public int gridCol;         // column number for this cell in the grid
    public int gridRow;         // row number for this cell in the grid
    public int projCode;        // projection code
    public int numImg;          // number of images/dates in this cell
    public int minX;            // min X coordinate of all scenes in cell
    public int maxX;            // max X coordinate of all scenes in cell
    public int minY;            // min Y coordinate of all scenes in cell
    public int maxY;            // max Y coordinate of all scenes in cell
    public int currentDateIndex;// index to the currently selected date

    private Point centerProjCoords; // cell center projection coordinates
    private NavigationModel navModel; // navigation model to use for this cell
    private ProjectionTransformation savedProj; // saved projection reference
    private URL appletURL;      // URL for applet for base of sensor data
    private boolean sceneCornersSet; // flag to indicate the scene corners
                                     // have been calculated
    private int[] lineOffset;   // temporary line offsets for 4 corners
    private int[] sampOffset;   // temporary sample offsets for 4 corners
    Dimension maxSceneSize;     // maximum scene dimensions in meters
    private Sensor currSensor;  // stores the sensor for the TOC

    // Constructor for the Table of Contents class
    //--------------------------------------------
    TOC(URL appletCodeBase, int gridCol, int gridRow)
    {
        appletURL = appletCodeBase;
        this.gridCol = gridCol;
        this.gridRow = gridRow;
        // flag no date selected yet
        centerProjCoords = null;
        navModel = null;
        savedProj = null;

        currentDateIndex = -1;
        valid = false;
        sceneCornersSet = false;
        lineOffset = new int[4];
        sampOffset = new int[4];
    }

    // Copy constructor for the Table of Contents class
    //-------------------------------------------------
    TOC(TOC toc)
    {
        scenes = new Metadata[toc.numImg]; 
        for (int i = 0; i < scenes.length; i++)
        {
            scenes[i] = new Metadata(toc.scenes[i]);            
        }
        cellDir = new String(toc.cellDir);
        hasMetrics = toc.hasMetrics;
        hasLines = toc.hasLines;
        valid = toc.valid;
        gridCol = toc.gridCol;
        gridRow = toc.gridRow;
        projCode = toc.projCode;
        numImg = toc.numImg;
        minX = toc.minX;
        maxX = toc.maxX;
        minY = toc.minY;
        maxY = toc.maxY;
        currentDateIndex = toc.currentDateIndex;
        centerProjCoords = toc.centerProjCoords;
        navModel = toc.navModel;
        savedProj = toc.savedProj;
        appletURL = toc.appletURL;
        sceneCornersSet = toc.sceneCornersSet;
        lineOffset = toc.lineOffset;
        sampOffset = toc.sampOffset;
        maxSceneSize = toc.maxSceneSize;
        currSensor = toc.currSensor;       
    }
 
    // Read TOC routine
    //-----------------
    public void read(Sensor currSensor) 
    {
        String dataLine;                  // A line of data from the TOC file...
        int tempVal;
        double offsetRes = currSensor.getOffsetResolution();
        this.currSensor = currSensor;

        // save the navigation model for this sensor
        navModel = currSensor.navModel;

        // assume read will fail
        valid = false;
        sceneCornersSet = false;
        numImg = 0;
        maxSceneSize = currSensor.getNominalSceneSize();

        // Build cell directory name
        cellDir = currSensor.getCellDirectory(gridCol,gridRow);

        BufferedReader data = null;
        try 
        {
            // open the TOC file
            URL tocURL = new URL(appletURL, cellDir+"/TOC");
            InputStream is = tocURL.openStream();
            data = new BufferedReader(new InputStreamReader(is));

            // Get the gridCol, gridRow, projection code, ancilary file flags,
            // and cloud cover index info for this gridCol/gridRow
            //---------------------------------------------------------
            dataLine = data.readLine();
            if (dataLine == null) 
            {
                System.out.println(
                    "Error reading Table of Contents file for gridCol/gridRow "
                    + gridCol + "/" + gridRow);
                data.close();
                return;
            }
            try 
            {
                StringTokenizer st = new StringTokenizer(dataLine,",");
                tempVal = navModel.getColumnNumberFromString(st.nextToken());
                if (tempVal != gridCol) 
                {
                    System.out.println(
                        "Error in TOC file -- incorrect Path specified.");
                    System.out.println(" " + tempVal + " " + gridCol + "\n");
                    data.close();
                    return;
                }
                tempVal = navModel.getRowNumberFromString(st.nextToken());
                if (tempVal != gridRow) 
                {
                    System.out.println(
                        "Error in TOC file -- incorrect Row specified.");
                    data.close();
                    return;
                }
                projCode = Integer.parseInt(st.nextToken());
                tempVal = Integer.parseInt(st.nextToken());
                if (tempVal == 1) 
                    hasLines = true;
                else 
                    hasLines = false;
                tempVal = Integer.parseInt(st.nextToken());
                if (tempVal == 1) 
                    hasMetrics = true;
                else 
                    hasMetrics = false;
                numImg = Integer.parseInt(st.nextToken());
            }
            catch (NoSuchElementException e) 
            {
                System.out.println("Exception:  "+ e.getMessage());
                data.close();
                return;
            }
            catch (NumberFormatException e) 
            {
                System.out.println("Exception:  "+ e.getMessage());
                data.close();
                return;
            }

            // Get the date specific info for each image over this
            // gridCol/gridRow
            //----------------------------------------------------
            scenes = new Metadata[numImg];

            for (int i=0; i<numImg; i++) 
            {
                dataLine = data.readLine();
                if (dataLine == null) 
                {
                    System.out.println(
                    "Error reading Table of Contents file for gridCol/gridRow "
                        + gridCol + "/" + gridRow);
                    data.close();
                    return;
                }
                try 
                {
                    scenes[i] = new Metadata(dataLine, currSensor,gridCol,
                                             gridRow);

                    scenes[i].calculateSceneCenter(offsetRes);
                }
                catch (NoSuchElementException e) 
                {
                    System.out.println("Exception:  "+ e.getMessage());
                    data.close();
                    return;
                }
                catch (NumberFormatException e) 
                {
                    System.out.println("Exception:  "+ e.getMessage());
                    data.close();
                    return;
                }
            }
            
            // close the file (Apple Mac Java VM's don't properly free 
            // resources unless the files are closed)
            data.close();
        } 
        catch(IOException e) 
        {
            // if the data file was opened, close it and issue a message.
            // if it was never successfully opened, skip issuing a message
            // since the file probably just doesn't exist.
            if (data != null)
            {
                try {data.close();} catch (Exception e1){};
                System.out.println("Exception:  "+e.getMessage());
            }
            return;
        }

        // calculate the min/max X/Y extents of this gridCol/gridRow.
        findCoordinateExtents(offsetRes);

        // if any images are available, the cell is valid
        if (numImg > 0)
            valid = true;
    }

    // helper method to calculate the coordinate extents for the cell
    //---------------------------------------------------------------
    private void findCoordinateExtents(double offsetRes)
    {
        // calculate the min/max X/Y extents of this gridCol/gridRow.
        // Done here once on reading TOC instead of repeatedly elsewhere.
        // Also calculate the size of the largest scene in the cell.
        if (numImg > 0)
        {
            int height = 0;
            int width = 0;

            Metadata scene = scenes[0];

            minY = scene.ulY;
            maxY = scene.ulY;
            minX = scene.ulX;
            maxX = scene.ulX;

            for (int i = 0; i < numImg; i++)
            {
                // find the min/max X/Y for the current scene
                scene = scenes[i];
                int sceneMinX, sceneMaxX, sceneMinY, sceneMaxY;
                sceneMinX = scene.ulX;
                sceneMaxX = scene.ulX;
                sceneMinY = scene.ulY;
                sceneMaxY = scene.ulY;
                for (int j = 0; j < 4; j++)
                {
                    int temp = scene.ulY 
                             - (int)Math.round(scene.lineOffset[j] * offsetRes);
                    if (temp < sceneMinY)
                        sceneMinY = temp;
                    if (temp > sceneMaxY)
                        sceneMaxY = temp;
                    temp = scene.ulX 
                         + (int)Math.round(scene.sampOffset[j] * offsetRes);
                    if (temp < sceneMinX)
                        sceneMinX = temp;
                    if (temp > sceneMaxX)
                        sceneMaxX = temp;
                    
                }

                // update the cell min/max X/Y from the current scene
                if (sceneMinY < minY)
                    minY = sceneMinY;
                if (sceneMaxY > maxY)
                    maxY = sceneMaxY;
                if (sceneMinX < minX)
                    minX = sceneMinX;
                if (sceneMaxX > maxX)
                    maxX = sceneMaxX;

                // update the width and height from the current scene
                int temp = sceneMaxX - sceneMinX;
                if (temp > width)
                    width = temp;
                temp = sceneMaxY - sceneMinY;
                if (temp > height)
                    height = temp;
            }

            // set the maximum scene size to the size calculated
            maxSceneSize = new Dimension(width,height);
        }
    }

    // method to clear the indicated filter flag for all the scenes in the TOC
    //------------------------------------------------------------------------
    public void clearSceneFilters(int flag)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].clearFilter(flag);
    }

    // method to filter the scenes in the TOC to the viewport passed in
    //-----------------------------------------------------------------
    public void filterScenesToViewport(Polygon viewport)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToViewport(viewport);

    }

    // method to filter the scenes in the TOC to meet a cloud cover restriction
    //-------------------------------------------------------------------------
    public void filterScenesToCloudCover(int maxCloudCover)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToCloudCover(maxCloudCover);
    }

    // method to filter the scenes in the TOC to a date range
    //-------------------------------------------------------
    public void filterScenesToDateRange(int startYear, int endYear,
                                        int startMonth, int endMonth)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToDateRange(startYear,endYear,startMonth,endMonth);
    }
    
    // method to filter the scenes in the TOC to a grid column & grid row range
    //--------------------------------------------------------------------
    public void filterScenesToGridColRowRange(int startGridCol, int endGridCol,
                                              int startGridRow, int endGridRow)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToGridColRowRange(startGridCol,endGridCol,
                                              startGridRow,endGridRow);
    }
    
    // method to filter the scenes in the TOC to those in Scene List
    //-------------------------------------------------------------------
    public void filterScenesToSceneList(boolean filterEnabled)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToSceneList(filterEnabled);
    }
   
    // method to filter the hidden scenes in the TOC
    //-------------------------------------------------------------------
    public void filterToHiddenScene()
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
        {
             scenes[i].filterToHiddenScene();
        }
    }
  
    // method to filter the scenes in the TOC to those in the user defined area
    //-------------------------------------------------------------------------
    public void filterScenesToUserArea
    (
        boolean filterEnabled,
        UserDefinedAreaDialog userDefinedAreaDialog
    )
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToUserArea(filterEnabled, userDefinedAreaDialog);
    }

    // method to filter the scenes in the TOC to the minimum Quality value
    //--------------------------------------------------------------------
    public void filterScenesToQuality(int minQuality)
    {
        if (!valid)
            return;
        
        // make sure sensor supports quality values
        if (currSensor.numQualityValues > 0)
        {
            for (int i = 0; i < numImg; i++)
                scenes[i].filterToQuality(minQuality);
        }
    }
        
    // method to filter the scenes in the TOC based on the data version
    //-----------------------------------------------------------------
    public void filterScenesToDataVersion(String dataVersion)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToDataVersion(dataVersion);
    }

    // method to filter the scenes in the TOC based on whether they are
    // currently downloadable
    //-----------------------------------------------------------------
    public void filterScenesToDownloadable(boolean filterEnabled)
    {
        if (!valid)
            return;

        for (int i = 0; i < numImg; i++)
            scenes[i].filterToDownloadable(filterEnabled);
    }

    // method to look for a specific scene in the TOC by entity id.  Returns
    // the index to the scene if it is found, otherwise -1.
    //----------------------------------------------------------------------
    public int findScene(Metadata scene)
    {
        int foundAt = -1;

        if (valid)
        {
            // find the scene in the scene array
            for (int i = 0; i < numImg; i++)
            {
                Metadata currScene = scenes[i];

                // use the date as a first level, fast test
                if (scene.date == currScene.date)
                {
                    // the date agrees, so check the whole entity ID
                    if (scene.entityID.equals(scenes[i].entityID))
                    {
                        foundAt = i;
                        break;
                    }
                }
            }
        }

        return foundAt;
    }
  
    // method to find the nearest visible date to the passed in date
    //--------------------------------------------------------------
    public int findDate(int year, int jDate)
    {
        int foundIndex = -1;
        int foundDays = 10000000;

        // find the scene in the scene array
        for (int i = 0; i < numImg; i++)
        {
            Metadata scene = scenes[i];
            
            if (scene.visible)
            {
                int days = scene.daysBetween(year, jDate);
                if (days < foundDays)
                {
                    foundDays = days;
                    foundIndex = i;
                }
            }
        }
        return foundIndex;
    }

    // method to find the nearest visible date to the current date index
    //------------------------------------------------------------------
    public void findClosestVisibleDate()
    {
        // do not do anything if the currentDateIndex isn't valid
        if ((currentDateIndex < 0) || (currentDateIndex > numImg))
            return;

        // if the current scene is visible, nothing to do
        if (scenes[currentDateIndex].visible)
            return;

        int foundBefore = -1;
        int foundAfter = -1;

        // look for the first visible scene before the current date
        for (int i = currentDateIndex; i >= 0; i--)
        {
            if (scenes[i].visible)
            {
                foundBefore = i;
                break;
            }
        }

        // look for the first visible scene after the current date
        for (int i = currentDateIndex + 1; i < numImg; i++)
        {
            if (scenes[i].visible)
            {
                foundAfter = i;
                break;
            }
        }

        if ((foundBefore != -1) && (foundAfter == -1))
        {
            // only found a visible scene before the current one, so use it
            currentDateIndex = foundBefore;
        }
        else if ((foundBefore == -1) && (foundAfter != -1))
        {
            // only found a visible scene after the current one, so use it
            currentDateIndex = foundAfter;
        }
        else if ((foundBefore != -1) && (foundAfter != -1))
        {
            // visible scenes before and after the current one, so look for the
            // closest date
            int daysBefore = scenes[currentDateIndex].daysBetween(
                        scenes[foundBefore].year, scenes[foundBefore].jDate);
            int daysAfter = scenes[currentDateIndex].daysBetween(
                        scenes[foundAfter].year, scenes[foundAfter].jDate);

            if (daysBefore < daysAfter)
                currentDateIndex = foundBefore;
            else
                currentDateIndex = foundAfter;
        }
    }
    
    // method to find the nearest visible date to the target date
    //------------------------------------------------------------------
    public void findSceneClosestToDate(Metadata targetScene)
    {
        // do not do anything if the currentDateIndex isn't valid
        if ((currentDateIndex < 0) || (currentDateIndex > numImg))
            return;

        // if the current scene is visible & in the target date, nothing to do
        if ((scenes[currentDateIndex].visible) && 
            (scenes[currentDateIndex].date == targetScene.date))
            return;
        
        // find date closest to the target scene
        int foundIndex = findDate(targetScene.year, targetScene.jDate);
        
        if (foundIndex != -1)
            currentDateIndex = foundIndex;
    }

    // method to add all the scenes from the indicated sensor on the requested
    // date to the vector passed in.  The scenes are inserted into the vector
    // in order from the top of the display to the bottom of the display.
    // Note that the sensor parameter is needed to properly handle the Landsat
    // combined dataset since TM and MSS scenes can be on the same date.
    //------------------------------------------------------------------------
    public void getScenesOnDate(int date, Sensor sensor, Vector scenesOnDate)
    {
        // nothing to do if no valid scenes
        if (!valid)
            return;

        // get the first index with the requested date and sensor
        int startIndex = getIndexOfDate(date, sensor);

        // if no scenes with the date were found, return without adding
        // anything to the scenesOnDate vector
        if (startIndex == -1)
            return;

        // find the last index with the requested date
        int endIndex = startIndex;
        for (int i = startIndex; i < numImg; i++)
        {
            if (scenes[i].date != date)
                break;
            endIndex = i;
        }

        // insert the scenes from the correct sensor with the requested date
        // into the vector in the correct order
        for (int i = startIndex; i <= endIndex; i++)
        {
            Metadata scene = scenes[i];

            // only take scenes that are the requested sensor (to handle case
            // of the Landsat combined dataset)
            if (sensor != scene.getSensor())
                continue;

            // find the correct location to insert the scene so the scenes 
            // are in order from the top of the display to the bottom
            int targetY = scene.ulY;
            int index;
            for (index = 0; index < scenesOnDate.size(); index++)
            {
                Metadata listScene = (Metadata)scenesOnDate.elementAt(index);
                if (targetY > listScene.ulY)
                    break;
            }
            scenesOnDate.insertElementAt(scene, index);
        }
    }

    // method to get the first index of the scene on a given date from the 
    // indicated sensor.  Returns -1 when a scene is not found on that date.
    // Note that the sensor parameter is needed to properly handle the Landsat
    // combined dataset since TM and MSS scenes can be on the same date.
    //------------------------------------------------------------------------
    public int getIndexOfDate(int date, Sensor sensor)
    {
        // nothing to do if no valid scenes
        if (!valid)
            return -1;

        for (int i = 0; i < numImg; i++)
        {
            Metadata scene = scenes[i];

            // if the date hasn't been reached yet, go to the next scene
            if (scene.date < date)
                continue;
            else if (date == scene.date)
            {
                if (sensor == scene.getSensor())
                {
                    // a matching date and sensor was found, so return its index
                    return i;
                }
            }
            else
            {
                // the date is after the target date, so break out of the loop
                // (since the scenes are from oldest to newest order, no
                // scenes matching the date will be found anymore)
                break;
            }
        }

        // no matching date and sensor found
        return -1;
    }

    // method to return the projection coordinate for this cell's center
    //------------------------------------------------------------------
    public Point getCenterProjCoords(ProjectionTransformation proj)
    {
        // return cached value if it is valid and available
        if ((savedProj == proj) && (centerProjCoords != null))
            return centerProjCoords;

        // save the projection
        savedProj = proj;

        // convert the grid coordinate to a projection
        centerProjCoords = navModel.gridToProjCoords(gridCol,gridRow,proj);

        return centerProjCoords;
    }

    // method to set the scene corners for all the read scenes
    //--------------------------------------------------------
    public void setSceneCorners(ProjectionTransformation proj)
    {
        if (valid && !sceneCornersSet)
        {
            sceneCornersSet = true;

            for (int index = 0; index < numImg; index++)
            {
                scenes[index].setSceneCorners(proj);
            }
        }
    }

    // add the scenes of an existing TOC to this TOC
    //----------------------------------------------
    public void add(TOC newCell)
    {
        if (!newCell.valid)
            return;

        // store starting scenes
        Metadata[] oldScenes = scenes;
        Metadata[] newScenes = newCell.scenes;
        int newScenesIndex = 0;
        int oldScenesIndex = 0;
        int oldScenesCount = numImg;
        int newScenesCount = newCell.numImg;

        numImg = oldScenesCount + newScenesCount;

        // create new array for old and new scenes
        scenes = new Metadata[numImg];

        // combine new and old scenes, using the fact that they are in
        // oldest to newest order
        for (int i = 0; i < numImg; i++)
        {
            if (newScenesIndex == newScenesCount)
            {
                // all the scenes in the new list have been copied already,
                // so copy the scenes from the old list
                scenes[i] = oldScenes[oldScenesIndex];
                oldScenesIndex++;
            }
            else if (oldScenesIndex == oldScenesCount)
            {
                // all the scenes in the old list have been copied already,
                // so copy the scenes from the new list
                scenes[i] = newScenes[newScenesIndex];
                newScenesIndex++;
            }
            else if (oldScenes[oldScenesIndex].date < 
                                            newScenes[newScenesIndex].date)
            {
                // the scene from the old list is older, so include it next
                scenes[i] = oldScenes[oldScenesIndex];
                oldScenesIndex++; 
            }
            else
            {
                // the scene from the new list is older, so include it next
                scenes[i] = newScenes[newScenesIndex];
                newScenesIndex++; 
            }
        }

        // update the valid flag
        if (numImg > 0)
            valid = true;
    }

    // method to remove the scenes that are not downloadable.  The original
    // use for this routine is to convert a Landsat SLC-off scene list to 
    // the downloadable subset for Landsat L1T.
    //---------------------------------------------------------------------
    public void eliminateUndownloadableScenes()
    {
        if (valid)
        {
            // count the number of downloadable scenes
            int num = 0;
            for (int i = 0; i < numImg; i++)
            {
                if (scenes[i].isDownloadable)
                    num++;
            }

            if (num == 0)
            {
                // no scenes left, so indicate the TOC is invalid and free
                // up the current scenes
                valid = false;
                for (int i = 0; i < numImg; i++)
                    scenes[i].cleanup();
                scenes = null;
                numImg = 0;
            }
            else
            {
                // there are downloadable scenes, so copy them to a new scenes
                // array that just holds them
                Metadata[] newScenes = new Metadata[num];
                int destIndex = 0;
                for (int i = 0; i < numImg; i++)
                {
                    if (scenes[i].isDownloadable)
                    {
                        newScenes[destIndex] = scenes[i];
                        destIndex++;
                    }
                    else
                    {
                        // free up the scenes that are not downloadable
                        scenes[i].cleanup();
                        scenes[i] = null;
                    }
                }

                // set the new scenes array
                scenes = newScenes;
                numImg = num;
            }
        }
    }

    // the cleanup method sets all references held by the TOC to null in an
    // attempt to help the Java VM do better garbage collection
    //---------------------------------------------------------------------
    public void cleanup()
    {
        cellDir = null;
        centerProjCoords = null;
        navModel = null;
        savedProj = null;
        currentDateIndex = -1;
        valid = false;
        for (int i = 0; i < numImg; i++)
        {
            if (scenes[i] != null)
            {
                scenes[i].cleanup();
                scenes[i] = null;
            }
        }
        numImg = 0;
    }

    // dump() is a debug routine that writes the contents of the TOC to the
    // Java console.  It is not typically called on a regular basis...
    //---------------------------------------------------------------------
/*
    public void dump() 
    {
        System.out.println("Path/Row of this TOC:  "+gridCol+"/"+gridRow);
        System.out.println("Number of images over this gridCol/gridRow:  "
                            +numImg);
        System.out.println("Projection ID Code:    "+projCode);
        if (hasMetrics) 
            System.out.println(
                "Metrics data available for this gridCol/gridRow");
        else 
            System.out.println(
                    "Metrics data is not available for this gridCol/gridRow");
        if (hasLines) 
            System.out.println(
                "Linework is available for this gridCol/gridRow");
        else 
            System.out.println(
                "Linework is not available for this gridCol/gridRow");
        System.out.println("Bounding image data box:");
        for (int i=0; i<4; i++) 
        {
            System.out.println("  "+sampOffset[i]+","+lineOffset[i]);
        }
        System.out.println("Image Specific records:");
        for (int i=0; i<numImg; i++) 
        {
            System.out.println("   EntityID: " + scenes[i].entityID);
            System.out.println("   Date:  "+scenes[i].date);
            System.out.println("   Julian Date:  "+scenes[i].jDate);
            System.out.println("   Upper Left Image X,Y (projection coords):  "
                + scenes[i].ulX + "," + scenes[i].ulY);
            System.out.println("   Cloud Cover:  "+scenes[i].cloudCover+"%");
        }
        System.out.println("End of Table of Contents for Path/Row:  "
            + gridCol + "/" + gridRow);
    }
*/
}
 
