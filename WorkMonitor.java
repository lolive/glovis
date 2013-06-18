// WorkMonitor.java defines the interface for monitoring the progress a thread
// is making on its work.
//----------------------------------------------------------------------------

interface WorkMonitor
{
    // method to return the message for display while this work is being done
    public String getWorkLabel();

    // method to return the total work to be done
    public int getTotalWork();

    // method to return the work done done so far
    public int getWorkComplete();

    // method to return whether work is being done
    public boolean isWorking();
}
