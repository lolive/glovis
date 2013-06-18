----------------------------------------------------------------------------
This file documents some of the tricker design points
----------------------------------------------------------------------------


----------------------------------------------------------------------------
Applet Threading
----------------------------------------------------------------------------
- Loading/animation thread synchronization with the GUI thread

    The threads that do work in the background (i.e. loading files over a
    network connection or timing animation) are synchronized with the GUI
    thread by sending repaint messages (via a component's repaint call) to the
    ImagePane.  This is done to make synchronization for the various events
    easier.  For example, after the images for the area being viewed are
    loaded, observers could be notified from a non-GUI thread.  However, if
    that happened and an observer made use of variables that might be changed
    by a different thread, trouble could result (i.e. the same observer update
    method could be running twice at the same time).  So, all significant
    events are communicated by sending a repaint message to the ImagePane
    (since repaint posts a message to the event queue) and the paint method of
    the ImagePane checks whether threads have completed some work.

    The other option is use of the synchronized keyword.  However, that is 
    unsafe since it can easily cause deadlocks or long pauses in the GUI.  It
    can also result in unsafe interaction between threads if the synchronized
    keyword is missed somewhere.  So, care must be taken to not change
    globally visible variables from the background threads.  This requirement
    mainly impacts the MosaicData class and callers of its methods.

- Flushing images and threads

    It was discovered that if a MediaTracker has started an image loading and
    the same image is flushed by another thread, the MediaTracker will never
    receive notification that the image arrived.  I didn't dig too deep
    though.  It could be that the image was added to the MediaTracker,
    flushed, then waited on.  It will also not receive an error for the image.
    This is probably a bug in the Java VM.  It happened on several versions
    of Netscape and Mozilla, so it seems common.  The unfortunate thing is
    that the applet manually flushes images due to a bug in Netscape.  To 
    prevent this situation from happening, before an image is flushed it must
    be guaranteed that the image loader has been cancelled and it is no longer
    loading any images.  The cancellation in the image loader has been made to
    complete quickly, so waiting for an image loader cancel from a GUI thread
    should be acceptable.

