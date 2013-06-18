// This module implements a class to act as a scene filter for scenes that
// are navigated like the Landsat data.
//------------------------------------------------------------------------

class LandsatSceneFilter implements SceneFilter
{
    private MosaicData md;  // reference to mosaic data
    private int firstFilteredDate; // index to first date for the selected
                                   // scene that passes any filters
    private int lastFilteredDate;  // index to last date for the selected scene
                                   // that passes any filters

    // constructor for this filter
    //----------------------------
    LandsatSceneFilter(MosaicData md)
    {
        this.md = md;
    }

    // In the active cell, step forward one date that hasn't been filtered
    //--------------------------------------------------------------------
    public void nextDate()
    {
        nextDate(md.getCurrentCell());
    }

    // In the cell specified by the scene, step forward one date that hasn't
    // been filtered
    //----------------------------------------------------------------------
    public void nextDate(Metadata scene)
    {
        nextDate(md.getCellForScene(scene));
    }

    // method with common parts of going to the next date
    //---------------------------------------------------
    private void nextDate(TOC cell)
    {
        if (cell.numImg >= 1)
        {
            // go forward one date, skipping scenes that are not visible.
            // If it is the last date, don't do anything
            int next = cell.currentDateIndex + 1;
            while ((next < cell.numImg) && !cell.scenes[next].visible)
            {
                next++;
            }
            if (next < cell.numImg) 
            {
                md.drawNewDate(cell.gridCol,cell.gridRow,next);
            }
        }
    }

    // In the active cell, step backward one date that hasn't been filtered
    //----------------------------------------------------------------------
    public void prevDate()
    {
        prevDate(md.getCurrentCell());
    }

    // In the cell specified by the scene, step backward one date that hasn't
    // been filtered
    //-----------------------------------------------------------------------
    public void prevDate(Metadata scene)
    {
        prevDate(md.getCellForScene(scene));
    }

    // method with common parts of going to the prev date
    //---------------------------------------------------
    private void prevDate(TOC cell)
    {
        if (cell.numImg >= 1)
        {
            // go back one date, skipping over scenes that are not visible.
            // When at the first scene, don't move any further
            int next = cell.currentDateIndex - 1;
            while ((next >= 0) && !cell.scenes[next].visible)
            {
                next--;
            }
            if (next >= 0)
            {
                md.drawNewDate(cell.gridCol,cell.gridRow,next);
            }
        }
    }

    // Jump to a date in or after the target year/month
    //-------------------------------------------------
    public void gotoDate(int targetYear, int targetMonth)
    {
        TOC cell = md.getCurrentCell();

        // nothing to do if the current cell isn't valid
        if (!cell.valid)
            return;

        // calculate target date
        int targetDate = targetYear * 10000 + targetMonth * 100;
        int last = lastFilteredDate;

        // if target date is off end of available dates, make the last
        // date the target date
        if (targetDate > cell.scenes[last].date)
            targetDate = cell.scenes[last].date;

        // if target date is off start of available dates, make the first
        // date the target date
        if (targetDate < cell.scenes[0].date)
            targetDate = cell.scenes[0].date;

        int newIndex = 0;

        // look for the target date or the next one after it
        for (int index = 0; index <= last; index++)
        {
            if (cell.scenes[index].visible)
            {
                newIndex = index;
                if (cell.scenes[index].date >= targetDate)
                    break;
            }
        }

        md.drawNewDate(cell.gridCol,cell.gridRow,newIndex);
    }

    // filter the data for the indicated maximum cloud cover
    //------------------------------------------------------
    public void filter()
    {
        // initialize the filter results
        firstFilteredDate = 0;
        lastFilteredDate = 0;

        TOC cell = md.getCurrentCell();

        // if no scene selected or the selected scene doesn't have valid
        // metadata, exit
        if (!cell.valid)
            return;

        // default the first date that passes the filter to the last scene
        // in case they all fail
        firstFilteredDate = cell.numImg - 1;

        // look through the list of scenes from first to last until one is 
        // found that doesn't have too much cloud cover to establish the first
        // date available
        for (int index = 0; index < cell.numImg; index++)
        {
            if (cell.scenes[index].visible)
            {
                firstFilteredDate = index;
                break;
            }
        }

        // look through the list of scenes from last until the first that
        // passed the filter to find one that doesn't have too much cloud cover
        // to establish the last date available
        lastFilteredDate = 0;
        for (int index = cell.numImg - 1; index >= firstFilteredDate; index--)
        {
            if (cell.scenes[index].visible)
            {
                lastFilteredDate = index;
                break;
            }
        }
    }

    // returns true if another date is available after the current date
    //-----------------------------------------------------------------
    public boolean isNextDateAvailable()
    {
        TOC cell = md.getCurrentCell();
        if (cell.currentDateIndex >= lastFilteredDate)
            return false;
        else
            return true;
    }

    // returns true if another date is available before the current date
    //------------------------------------------------=-----------------
    public boolean isPrevDateAvailable()
    {
        TOC cell = md.getCurrentCell();
        if (cell.currentDateIndex <= firstFilteredDate)
            return false;
        else
            return true;

    }

    // returns true if another date is available after the current date for 
    // the indicated scene
    //---------------------------------------------------------------------
    public boolean isNextDateAvailable(Metadata scene)
    {
        TOC cell = md.getCellForScene(scene);
        for (int index = cell.currentDateIndex + 1; index < cell.numImg; 
             index++)
        {
            if (cell.scenes[index].visible)
                return true;
        }
        return false;
    }

    // returns true if another date is available before the current date for
    // the indicated scene
    //------------------------------------------------=---------------------
    public boolean isPrevDateAvailable(Metadata scene)
    {
        TOC cell = md.getCellForScene(scene);
        for (int index = cell.currentDateIndex - 1; index >= 0; index--)
            if (cell.scenes[index].visible)
                return true;
        return false;
    }

    // jumps to the first date that hasn't been filtered
    //--------------------------------------------------
    public void gotoFirstDate()
    {
        TOC cell = md.getCurrentCell();
        md.drawNewDate(cell.gridCol,cell.gridRow,firstFilteredDate);
    }

    // jumps to the last date that hasn't been filtered
    //-------------------------------------------------
    public void gotoLastDate()
    {
        TOC cell = md.getCurrentCell();
        md.drawNewDate(cell.gridCol,cell.gridRow,lastFilteredDate);
    }

    // methods to return the first and last year available.  Note that this
    // includes all scene dates, even if they have been filtered out.
    //---------------------------------------------------------------------
    public int getFirstYear()
    {
        TOC cell = md.getCurrentCell();
        return cell.scenes[0].date/10000;
    }
    public int getLastYear()
    {
        TOC cell = md.getCurrentCell();
        return cell.scenes[cell.numImg-1].date/10000;
    }
}
