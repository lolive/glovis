// CancelLoad.java provides a wrapper class around a boolean value.  A 
// reference to an instance of this class is passed to Map Layer reading
// classes so they can detect when a load is cancelled.
//----------------------------------------------------------------------

class CancelLoad
{
    public boolean cancelled;

    CancelLoad()
    {
        cancelled = false;
    }
}
