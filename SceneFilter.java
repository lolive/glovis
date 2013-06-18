// This module defines an interface for a scene filter
//----------------------------------------------------

interface SceneFilter
{
    // In the active cell, step forward one date that hasn't been filtered
    public void nextDate();

    // In the cell specified by the scene, step forward one date that hasn't
    // been filtered
    public void nextDate(Metadata scene);

    // In the active cell, step backward one date that hasn't been filtered
    public void prevDate();

    // In the cell specified by the scene, step backward one date that hasn't
    // been filtered
    public void prevDate(Metadata scene);

    // Jump to a date in or after the target year/month
    public void gotoDate(int targetYear, int targetMonth);

    // filter the data for visible scenes
    public void filter();

    // returns true if another date is available after the current date
    public boolean isNextDateAvailable();

    // returns true if another date is available after the current date for 
    // the indicated scene
    public boolean isNextDateAvailable(Metadata scene);

    // returns true if another date is available before the current date
    public boolean isPrevDateAvailable();

    // returns true if another date is available before the current date for
    // the indicated scene
    public boolean isPrevDateAvailable(Metadata scene);

    // jumps to the first date that hasn't been filtered
    public void gotoFirstDate();

    // jumps to the last date that hasn't been filtered
    public void gotoLastDate();

    // methods to return the first and last year available.  Note that this
    // includes all scene dates, even if they have been filtered out.
    public int getFirstYear();
    public int getLastYear();
}
