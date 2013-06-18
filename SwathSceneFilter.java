// This module implements a class to act as a scene filter for scenes that
// are navigated like the Landsat data.
//------------------------------------------------------------------------

class SwathSceneFilter implements SceneFilter
{
    private MosaicData md;     // reference to mosaic data
    private Sensor currSensor; // currently selected sensor
    private imgViewer applet;  // reference to the applet

    // arrays used to track the scenes in swath order.  The cells array
    // holds a key for the column/row of the scene in the inventory grid and 
    // the dates array holds the date index in the TOC.
    private int[] cellsInSwathOrder = null;
    private int[] datesInSwathOrder = null;

    private int swathIndex;    // current location in the swath array
    private TOC[] tocs = null; // saved reference to the TOC
    private int firstFilteredDate; // index to first date for the selected
                                   // scene that passes any filters
    private int lastFilteredDate;  // index to last date for the selected scene
                                   // that passes any filters
    private boolean enableOnlySelected; // flag that onlySelect can be set.
                       // this allows disabling onlySelected for some datasets
    private boolean onlySelected;  // flag to indicate that only the selected
                                   // cell should be considered
    private boolean useVisibleFlag;// flag to indicate the scene metadata flag
                                   // should be used

    // constructor for this filter
    //----------------------------
    SwathSceneFilter(MosaicData md, TOC[] tocs, Sensor currSensor, 
                     imgViewer applet, boolean enableOnlySelected)
    {
        this.md = md;
        this.tocs = tocs;
        this.currSensor = currSensor;
        this.applet = applet;
        this.enableOnlySelected = enableOnlySelected;

        // build an array of scenes in date order over all the TOC's.  This
        // is to allow easily following swaths for sensors like ASTER.
        int totalScenes = 0;
        for (int i = 0; i < tocs.length; i++)
        {
            if (tocs[i].valid)
                totalScenes += tocs[i].numImg;
        }

        cellsInSwathOrder = null;
        datesInSwathOrder = null;

        // build the arrays for date navigation based on swaths
        swathIndex = 0;
        if (totalScenes > 0)
        {
            cellsInSwathOrder = new int[totalScenes];
            datesInSwathOrder = new int[totalScenes];

            // allocate an array for remembering the indices
            int[] indices = new int[tocs.length];

            for (int i = 0; i < totalScenes; i++)
            {
                int currDate = 99999999;
                int currY = -99999999;
                int cellIndex = 0;
                // walk toc array from top to bottom so that the uppermost
                // scene from a date is definitely found
                //-------------------------------------------------------------
                // NOTE: this code depends on the TOC files being build with
                // the scenes with larger Y values being before other scenes on
                // the same date
                // TBD: this code assumes a 3x3 array of cells.  This may not
                //      be true in the future.
                //-------------------------------------------------------------
                for (int j = 0; j < 3; j++)
                {
                    for (int k = 0; k < 3; k++)
                    {
                        int index = k * 3 + j;
                        TOC cell = tocs[index];

                        // if the cell is valid and not all the images have 
                        // been included, consider it as the next possible
                        // scene
                        if (cell.valid && (indices[index] < cell.numImg))
                        {
                            // if the scene date is the earliest one found,
                            // remember it as the probably next one in the 
                            // swatch list
                            Metadata scene = cell.scenes[indices[index]];
                            int tempDate = scene.date;
                            if (tempDate < currDate)
                            {
                                cellIndex = index;
                                currDate = tempDate;
                                currY = scene.ulY;
                            }
                            else if (tempDate == currDate)
                            {
                                // if the scene is on the same date, pick the
                                // one that is closer to the top of the display
                                if (scene.ulY > currY)
                                {
                                    cellIndex = index;
                                    currY = scene.ulY;
                                }
                            }
                        }
                    }
                }

                // save the earliest cell key and date index found since it is
                // the next one in the swath
                cellsInSwathOrder[i] = getHash(tocs[cellIndex]);
                datesInSwathOrder[i] = indices[cellIndex];

                // update the index for this cell
                indices[cellIndex]++;
            }
        }
    }

    // private methods to combine the column/row into a key and extract them
    private final int getHash(TOC cell)
    {
        return ((cell.gridCol + 1000) << 16) + cell.gridRow + 1000;
    }
    private final int getColumn(int index)
    {
        return (index >> 16) - 1000;
    }

    private final int getRow(int index)
    {
        return (index & 0xffff) - 1000;
    }

    // helper method to make sure the swathIndex is up-to-date with the 
    // user selected scene.
    private final void updateSwathIndex()
    {
        // do nothing if no cells available
        if (cellsInSwathOrder == null)
            return;

        // get the hash key for the selected cell
        TOC cell = md.getCurrentCell();
        int targetKey = getHash(cell);
        int targetDateIndex = cell.currentDateIndex;

        // if the key and date index of the swath index does not agree with
        // the current cell, search for the correct one.  This is just an
        // optimization to prevent searching if it isn't needed.
        if ((cellsInSwathOrder[swathIndex] != targetKey) ||
            (datesInSwathOrder[swathIndex] != targetDateIndex))
        {
            int index;
            // TBD - brute force search (should use binary?) (-1 is done so
            // that the last one is returned if nothing else)
            for (index = 0; index < cellsInSwathOrder.length-1; index++)
            {
                if ((targetKey == cellsInSwathOrder[index]) &&
                    (targetDateIndex == datesInSwathOrder[index]))
                    break;
            }
            swathIndex = index;
        }
    }

    // Step forward one date that hasn't been filtered
    //------------------------------------------------
    public void nextDate()
    {
        // do nothing if no cells available
        if (cellsInSwathOrder == null)
            return;

        // get refence to the state of the user defined area
        boolean userAreaClosedFlag 
                    = applet.userDefinedAreaDialog.isUserDefinedAreaClosed();

        // make sure the swath index is current
        updateSwathIndex();

        // move to the next scene
        swathIndex++;
        if (swathIndex >= lastFilteredDate)
            swathIndex = lastFilteredDate;

        TOC targetCell = md.getCurrentCell();
        int targetKey = getHash(targetCell);

        // walk through the scenes until one is found that meets the 
        // visibility and cloud cover restrictions
        for (/* swathIndex already set */ ; swathIndex < lastFilteredDate; 
             swathIndex++)
        {
            int key = cellsInSwathOrder[swathIndex];

            // skip scenes not in the current cell if only displaying a
            // single cell
            if (onlySelected && (key != targetKey))
                continue;

            int dateIndex = datesInSwathOrder[swathIndex];
            int cellNum = md.colRowToCell(getColumn(key),getRow(key));
            if (cellNum == -1)
                continue;

            Metadata scene = tocs[cellNum].scenes[dateIndex];

            // exit the loop on a scene that is visible, and in the user
            // defined area (if set)
            if (scene.visible)
            {
                if (!userAreaClosedFlag || applet.userDefinedAreaDialog.
                        getUserDefinedArea().sceneIntersects(scene))
                {
                        break;
                }
            }
        }

        // update the displayed scene
        int key = cellsInSwathOrder[swathIndex];
        md.drawNewDate(getColumn(key),getRow(key),
                       datesInSwathOrder[swathIndex]);
    }

    // Step backward one date that hasn't been filtered
    //-------------------------------------------------
    public void prevDate()
    {
        // do nothing if no cells available
        if (cellsInSwathOrder == null)
            return;

        // make sure the swath index is current
        updateSwathIndex();

        // get refence to the state of the user defined area
        boolean userAreaClosedFlag 
                = applet.userDefinedAreaDialog.isUserDefinedAreaClosed();

        // move to the previous scene
        swathIndex--;
        if (swathIndex < firstFilteredDate)
            swathIndex = firstFilteredDate;

        TOC targetCell = md.getCurrentCell();
        int targetKey = getHash(targetCell);

        // walk through the scenes until one is found that meets the 
        // visibility and cloud cover restrictions
        for (/* swathIndex already set */ ; swathIndex > firstFilteredDate; 
             swathIndex--)
        {
            int key = cellsInSwathOrder[swathIndex];

            // skip scenes not in the current cell if only displaying a
            // single cell
            if (onlySelected && (key != targetKey))
                continue;

            int dateIndex = datesInSwathOrder[swathIndex];
            int cellNum = md.colRowToCell(getColumn(key),getRow(key));
            if (cellNum == -1)
                continue;

            Metadata scene = tocs[cellNum].scenes[dateIndex];

            // exit the loop on a scene that is visible, and within the user
            // defined area (if set)
            if (scene.visible)
            {
                if (!userAreaClosedFlag || applet.userDefinedAreaDialog.
                        getUserDefinedArea().sceneIntersects(scene))
                {
                    break;
                }
            }
        }
         
        // update the displayed scene
        int key = cellsInSwathOrder[swathIndex];
        md.drawNewDate(getColumn(key),getRow(key),
                       datesInSwathOrder[swathIndex]);
    }

    // Jump to a date in or after the target year/month
    //-------------------------------------------------
    public void gotoDate(int targetYear, int targetMonth)
    {
        // do nothing if no cells available
        if (cellsInSwathOrder == null)
            return;

        // calculate target date
        int targetDate = targetYear * 10000 + targetMonth * 100;

        int newDateIndex = datesInSwathOrder[firstFilteredDate];
        int newIndex = firstFilteredDate;
        
        // get refence to the state of the user defined area
        boolean userAreaClosedFlag 
                = applet.userDefinedAreaDialog.isUserDefinedAreaClosed();

        // get the hash key for the current cell in case only a single
        // cell is displayed
        int currKey = getHash(md.getCurrentCell());

        // look for the target date or the next one after it
        int index;
        for (index = firstFilteredDate; index <= lastFilteredDate; index++)
        {
            // skip scenes that aren't in the displayed cell if only interested
            // in that cell
            if (onlySelected && (currKey != cellsInSwathOrder[index]))
                continue;

            int dateIndex = datesInSwathOrder[index];

            // get the scene metadata for the current scene from the swath
            int key = cellsInSwathOrder[index];
            int cellNum = md.colRowToCell(getColumn(key),getRow(key));
            if (cellNum == -1)
                continue;

            Metadata scene = tocs[cellNum].scenes[dateIndex];

            // only accept scenes that are visible
            if (scene.visible)
            {
                if (!userAreaClosedFlag || applet.userDefinedAreaDialog.
                        getUserDefinedArea().sceneIntersects(scene))
                {
                    newDateIndex = dateIndex;
                    newIndex = index;

                    // if the target date is reached, the search is done
                    if (scene.date >= targetDate)
                        break;
                }
            }
        }

        // update the display with the newly selected scene
        int key = cellsInSwathOrder[newIndex];
        md.drawNewDate(getColumn(key),getRow(key),newDateIndex);
    }

    // filter the data for the indicated maximum cloud cover
    //------------------------------------------------------
    public void filter()
    {
        // do nothing if no cells available
        if (cellsInSwathOrder == null)
            return;

        // set the onlySelected flag if only a single cell is being displayed
        onlySelected = false;
        useVisibleFlag = false;
        int cellsToDisplay = currSensor.getNumCellsAtResolution(md.pixelSize);
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
            onlySelected = enableOnlySelected;
        else if (cellsToDisplay == 1)
            useVisibleFlag = true;
        
        // get refence to the state of the user defined area
        boolean userAreaClosedFlag 
                = applet.userDefinedAreaDialog.isUserDefinedAreaClosed();

        // set the target hash key for displaying a single cell
        int targetKey = getHash(md.getCurrentCell());

        // initialize the filter results
        firstFilteredDate = cellsInSwathOrder.length - 1;
        lastFilteredDate = 0;

        // look through the list of scenes from first to last until one is 
        // found that doesn't have too much cloud cover to establish the first
        // date available
        for (int index = 0; index < cellsInSwathOrder.length; index++)
        {
            int key = cellsInSwathOrder[index];

            // skip scenes not in the current cell if only displaying a single
            // cell
            if (onlySelected && (key != targetKey))
                continue;

            int dateIndex = datesInSwathOrder[index];
            int cellNum = md.colRowToCell(getColumn(key),getRow(key));
            if (cellNum == -1)
                continue;

            Metadata scene = tocs[cellNum].scenes[dateIndex];

            // only accept scenes that are visible and with the user defined
            // area (if set)
            if (scene.visible)
            {
                if (!userAreaClosedFlag || applet.userDefinedAreaDialog.
                        getUserDefinedArea().sceneIntersects(scene))
                {
                    firstFilteredDate = index;
                    break;
                }
            }
        }

        // look through the list of scenes from last until the first that
        // passed the filter to find one that doesn't have too much cloud cover
        // to establish the last date available
        for (int index = cellsInSwathOrder.length - 1; 
             index >= firstFilteredDate; index--)
        {
            int key = cellsInSwathOrder[index];

            // skip scenes not in the current cell if only displaying a single
            // cell
            if (onlySelected && (key != targetKey))
                continue;

            int dateIndex = datesInSwathOrder[index];
            int cellNum = md.colRowToCell(getColumn(key),getRow(key));
            if (cellNum == -1)
                continue;

            Metadata scene = tocs[cellNum].scenes[dateIndex];

            // only accept scenes that are visible and within the user defined
            // area (if set)
            if (scene.visible)
            {
                if (!userAreaClosedFlag || applet.userDefinedAreaDialog.
                        getUserDefinedArea().sceneIntersects(scene))
                {
                    lastFilteredDate = index;
                    break;
                }
            }
        }
    }

    // returns true if another date is available after the current date
    //-----------------------------------------------------------------
    public boolean isNextDateAvailable()
    {
        updateSwathIndex();

        if (swathIndex >= lastFilteredDate)
            return false;
        else
            return true;
    }

    // returns true if another date is available before the current date
    //------------------------------------------------=-----------------
    public boolean isPrevDateAvailable()
    {
        updateSwathIndex();

        if (swathIndex <= firstFilteredDate)
            return false;
        else
            return true;
    }


    // methods for the next/prev date based on an input scene are not 
    // implemented for this filter since they would not serve any value.
    //------------------------------------------------------------------
    public void nextDate(Metadata scene)
    {
    }
    public void prevDate(Metadata scene)
    {
    }
    public boolean isNextDateAvailable(Metadata scene)
    {
        return false;
    }
    public boolean isPrevDateAvailable(Metadata scene)
    {
        return false;
    }

    // jumps to the first date that hasn't been filtered
    //--------------------------------------------------
    public void gotoFirstDate()
    {
        // do nothing if no cells available
        if (cellsInSwathOrder == null)
            return;

        int dateIndex = datesInSwathOrder[firstFilteredDate];
        int key = cellsInSwathOrder[firstFilteredDate];
        md.drawNewDate(getColumn(key),getRow(key),dateIndex);
    }

    // jumps to the last date that hasn't been filtered
    //-------------------------------------------------
    public void gotoLastDate()
    {
        // do nothing if no cells available
        if (cellsInSwathOrder == null)
            return;

        int dateIndex = datesInSwathOrder[lastFilteredDate];
        int key = cellsInSwathOrder[lastFilteredDate];
        md.drawNewDate(getColumn(key),getRow(key),dateIndex);
    }

    // methods to return the first and last year available.  Note that this
    // includes all scene dates, even if they have been filtered out.
    public int getFirstYear()
    {
        int key = cellsInSwathOrder[0];
        int cellNum = md.colRowToCell(getColumn(key),getRow(key));
        if (cellNum != -1)
        {
            TOC cell = tocs[cellNum];
            return cell.scenes[0].date/10000;
        }
        else
            return 0;
    }
    public int getLastYear()
    {
        int key = cellsInSwathOrder[cellsInSwathOrder.length - 1];
        int cellNum = md.colRowToCell(getColumn(key),getRow(key));
        if (cellNum != -1)
        {
            TOC cell = tocs[cellNum];
            return cell.scenes[cell.numImg-1].date/10000;
        }
        else
            return 0;
    }
}
