// ImageLoader - this class handles the details of loading the browse images
//               from the server.
//
//--------------------------------------------------------------------------
import java.awt.Image;
import java.awt.MediaTracker;

class ImageLoader implements Runnable, WorkMonitor
{
    private ImagePane imagePane;
    private imgViewer applet;
    private Image[] mediatrackerList; // list of images currently added to the
                             // media tracker.  Maintained so they can be
                             // removed.
    private boolean[] isIDFree;     // array to track the free mediatracker IDs
    private Metadata[] scenes;      // references to scenes currently loading
    private final int maxImageFiles = 9; // max number of image files to
                                    // load at once.  Used to size the 
                                    // mediatrackerList and isIDFree arrays

    private Thread loaderThread;    // thread for loading images
    private Object loadLock;        // mutex for exclusive access
    private boolean isLoading;      // images are loading flag
    private boolean isLoadCancelled;// cancel image loading flag
    private boolean killThread;     // flag to indicate the thread should
                                    // be killed
    private int numImagesToLoad;    // total number of images currently loading
    private int currImageLoading;   // current image loading of total number

    // private copies of the parameters passed to loadImages so they can
    // be used by the loading thread
    private Metadata[] zOrder;      // snapshot of the displayed scene z-order
    private int cellsToDisplay;     // number of cells currently displayed
    private int pixelSize;          // current display resolution
    private Sensor currSensor;      // reference to the current sensor
    private int filesToLoad;        // number of image files to load at once

    // create a buffer for storing a load that has been submitted by calling
    // loadImages, but not yet started on by the actual load thread.  This
    // buffer eliminates the need to wait for the image loader thread to 
    // complete a cancel before submitting a new set of images to load (and 
    // therefore eliminates some GUI hangs.
    private class PendingLoad
    {
        Metadata[] zOrder;
        int cellsToDisplay;
        int pixelSize;
        Sensor currSensor;
        int filesToLoad;
        boolean pending;
    }
    private PendingLoad pendingLoad;

    // constructor
    //------------
    public ImageLoader(imgViewer applet, ImagePane imagePane)
    {
        this.applet = applet;
        this.imagePane = imagePane;

        // list of images loaded in the media tracker, and associated IDs and
        // scene references
        mediatrackerList = new Image[maxImageFiles];
        isIDFree = new boolean[maxImageFiles];
        scenes = new Metadata[maxImageFiles];

        // create objects for loading images in a separate thread
        loadLock = new Object();
        pendingLoad = new PendingLoad();
        loaderThread = new Thread(this,"Image Loader Thread");
        loaderThread.start();
    }

    // methods required for the WorkMonitor interface
    //-----------------------------------------------
    public String getWorkLabel() { return "Loading Images"; }
    public boolean isWorking() { return isBusy(); }
    public int getTotalWork() { return numImagesToLoad; }
    public int getWorkComplete() { return currImageLoading; }

    // method to wait for any of the images being tracked by the media
    // tracker to arrive.  When one arrives, the ID is freed and the 
    // method returns.  This allows the loader thread to keep kicking off
    // new image loads while still being somewhat responsive to the user
    // cancelling a load.  If an extremely long time goes by without 
    // receiving an image, some kind of error must have occurred, so 
    // return false to indicate an error.  Normally true is returned.
    //-------------------------------------------------------------------
    private boolean waitForAny(MediaTracker mt)
    {
        boolean receivedOne = false;
        int waitCount = 0; // count of number of times sleep has been called

        if (applet.verboseOutput)
            System.out.println("Waiting for any image");

        // loop until an image is received
        while (!receivedOne)
        {
            boolean waitingForOne = false;

            // check if one of the images has been received
            for (int i = 0; i < filesToLoad; i++)
            {
                // only check IDs that are not free
                if (!isIDFree[i])
                {
                    waitingForOne = true;

                    // ID is in use, so check to see if it is done
                    if (mt.checkID(i))
                    {
                        // FIXME - test code to allow simulating a slow
                        // network connection
                        if (applet.slowdown)
                        {
                            try {loaderThread.sleep(500);}
                            catch (InterruptedException e) {}
                        }

                        // it is done, so free the ID and break out of the 
                        // loop
                        receivedOne = true;
                        scenes[i].image = mediatrackerList[i];
                        mt.removeImage(mediatrackerList[i]);
                        mediatrackerList[i] = null;
                        isIDFree[i] = true;
                        scenes[i] = null;
                        break;
                    }
                }
            }

            // if no allocated IDs were found, issue a warning message and
            // break out of the loop (should only happen if there is a 
            // software bug).
            if (!waitingForOne)
            {
                System.out.println("Warning - nothing to wait for");
                break;
            }

            // if none arrived, sleep for a little while
            if (!receivedOne)
            {
                try {loaderThread.sleep(20);}
                catch (InterruptedException e) {}
                waitCount++;

                // check if we have been waiting for an image to download
                // for an extremely long time (9000 is 3 minutes without
                // receiving a single image - this is an extremely long
                // time even on a slow connection)
                if (waitCount > 9000)
                    return false;
            }
        }

        if (applet.verboseOutput)
            System.out.println("received image");

        return true;
    }

    // method to communicate the images to load to the loading thread
    //---------------------------------------------------------------
    public void loadImages
    (
        ZOrderList zOrder,      // I: zOrder list for load order of images
        int cellsToDisplay,     // I: number of cells to display
        int pixelSize,          // I: pixel resolution in meters
        Sensor currSensor       // I: current sensor reference
    )
    {
        // calculate a guess at the number of files to load at once.  The 
        // goal is to get a good balance between the time it takes to cancel
        // a load on a slow connection and the speed of the download for a fast
        // connection.  Assume that only 75,000 bytes should be loading at
        // once.
        int fileSize = currSensor.getImageFileSize(pixelSize);
        int filesToLoad = 75000/fileSize;
        if (filesToLoad < 2)
            filesToLoad = 2;
        else if (filesToLoad > maxImageFiles)
            filesToLoad = maxImageFiles;

        // get a snapshot of the zOrder since the load thread will need to
        // manipulate it at the same time the GUI thread does
        Metadata[] zo = zOrder.getSnapshot();
        if (zo == null)
            return;

        // set the parameters in the pending load buffer.  Lock the buffer 
        // while loading the parameters to protect against the load thread
        // reading a half filled buffer.
        synchronized (pendingLoad)
        {
            pendingLoad.zOrder = zo;
            pendingLoad.cellsToDisplay = cellsToDisplay;
            pendingLoad.pixelSize = pixelSize;
            pendingLoad.currSensor = currSensor;
            pendingLoad.filesToLoad = filesToLoad;
            pendingLoad.pending = true;
            // if the load thread is still running, make sure it is cancelled
            if (isLoading)
                cancelLoad();
            // notify the load thread that data is ready to be loaded
            pendingLoad.notify();
        }
    }

    // method to cancel a load operation currently underway
    //-----------------------------------------------------
    public void cancelLoad()
    {
        // if currently loading, set the cancel flag and interrupt the thread
        // in case it is waiting in the media tracker.  Note: this should be 
        // called from the same thread as loadImages.  The two methods could
        // be synchronized, but it is unnecessary overhead.
        if (isLoading)
        {
            if (applet.verboseOutput)
                System.out.println("cancelled");
            isLoadCancelled = true;
        }
    }

    // method to stop the load thread when the applet is going out of scope
    //---------------------------------------------------------------------
    public void killThread()
    {
        cancelLoad();
        synchronized(pendingLoad)
        {
            killThread = true;
            pendingLoad.notify();
        }
    }

    // method to wait until the image load is no longer busy. Note that this
    // is hopefully a temporary thing.  It is needed to allow preventing 
    // flushing of images that are currently being loaded (causes applet to
    // hang) and changes to the mosaicCells array.
    public void waitUntilDone()
    {
        if (applet.verboseOutput)
            System.out.println("Waiting for image load to complete");
        synchronized(loadLock)
        {
            if (applet.verboseOutput)
                System.out.println("Image load completed");
        }
        if (applet.verboseOutput)
            System.out.println("Waiting done");
    }

    // method to return whether the load thread is loading images
    //-----------------------------------------------------------
    public boolean isBusy()
    {
        return isLoading || pendingLoad.pending;
    }

    // main method for the loading thread
    //-----------------------------------
    public void run()
    {
        String imgName;         // image file name

        MediaTracker mt = new MediaTracker(imagePane);

        // loop forever, loading images as instructed
        while (true)
        {
            // if the loading flag isn't set, check for a pending load.  Note
            // that the wait call releases the lock.
            while (!isLoading)
            {
                synchronized (pendingLoad)
                {
                    // if no load pending and the thread hasn't been killed,
                    // wait for one of them
                    if (!pendingLoad.pending && !killThread)
                    {
                        try
                        {
                            if (applet.verboseOutput)
                            {
                                System.out.println(
                                        "waiting for load command");
                            }
                            pendingLoad.wait();
                        }
                        catch (InterruptedException e){}
                    }
                    // if load pending now, move the data from the pending
                    // load to the real load
                    if (pendingLoad.pending)
                    {
                        this.zOrder = pendingLoad.zOrder;
                        pendingLoad.zOrder = null;
                        this.cellsToDisplay = pendingLoad.cellsToDisplay;
                        this.pixelSize = pendingLoad.pixelSize;
                        this.currSensor = pendingLoad.currSensor;
                        this.filesToLoad = pendingLoad.filesToLoad;
                        isLoading = true;
                        isLoadCancelled = false;
                        pendingLoad.pending = false;
                    }

                    // return from the routine if the killThread flag is set
                    if (killThread)
                    {
                        loaderThread = null;
                        return;
                    }
                }
            }

            // load the images while holding the load lock (mainly so 
            // waitUntilDone can actually block until the load is complete)
            synchronized (loadLock)
            {
                int scenesLoading = 0;  // count image scenes being loaded
                int zIndex = 0;         // start loading scenes at the top
                                        // of the z-order

                // initialize all IDs as free
                for (int i = 0; i < filesToLoad; i++)
                    isIDFree[i] = true;

                // set up the progress monitoring variables
                currImageLoading = 0;
                numImagesToLoad = 0;
                for (int i = 0; i < zOrder.length; i++)
                {
                    // count scenes that aren't loaded or have the wrong pixel
                    // size loaded 
                    Metadata scene = zOrder[i];
                    if ((scene.image == null) || (scene.imageRes != pixelSize))
                        numImagesToLoad++;
                }

                // loop through the scenes in the z-order, loading each of them
                boolean firstTime = true;
                while (zIndex < zOrder.length)
                {
                    Metadata scene = zOrder[zIndex];
                    zIndex++;
                    Image img;  // temporary image pointer

                    // exit the loop if loading has been cancelled
                    if (isLoadCancelled)
                        break;

                    // do not load a scene if it is not visible
                    if (!scene.visible)
                        continue;

                    // do not load scene if the correct resolution is already
                    // loaded
                    if ((scene.image != null) && (scene.imageRes == pixelSize))
                        continue;

                    // get a free media tracker id
                    int id;
                    for (id = 0; id < filesToLoad; id++)
                    {
                        if (isIDFree[id])
                        {
                            isIDFree[id] = false;
                            break;
                        }
                    }

                    // get the image name for the current scene
                    imgName = currSensor.makeImageName(scene,pixelSize);

                    // flush the scene currently referenced for the
                    // scene to work around a Netscape bug that results in
                    // image resources not being released automatically as 
                    // part of garbage collection
                    img = scene.image;
                    if (img != null)
                    {
                        img.flush();
                        scene.image = null;
                    }
                    if (applet.verboseOutput)
                        System.out.println("Loading image " + imgName);
                    // start the image loading
                    img = applet.getImage(applet.getCodeBase(),imgName);

                    // save the scene and the resolution
                    scenes[id] = scene;
                    scene.imageRes = pixelSize;

                    // add the image to the tracker
                    mt.addImage(img,id);
                    mediatrackerList[id] = img;
                    // start the load for this image
                    mt.checkID(id,true);
                    scenesLoading++;

                    // if the size of the media tracker list is reached,
                    // load the contents of the list
                    if (scenesLoading >= filesToLoad)
                    {
                        if (waitForAny(mt))
                        {
                            scenesLoading--;
                            currImageLoading++;
                        }
                        else
                        {
                            // an error happened while waiting for an image,
                            // so break out of the loop
                            scenesLoading = 0;
                            System.out.println("Error loading images.  Image"
                                               +" load cancelled.");
                            break;
                        }
                    }

                    // only load the topmost scene when in single scene mode
                    if (firstTime)
                    {
                        if (cellsToDisplay == Sensor.SINGLE_SCENE)
                            break;
                        firstTime = false;
                    }
                }

                // clear reference to the z-order snapshot to allow 
                // garbage collection on the referenced scenes if needed
                zOrder = null;

                // wait for all the scenes to show up
                while (scenesLoading > 0)
                {
                    if (waitForAny(mt))
                    {
                        scenesLoading--;
                        currImageLoading++;
                    }
                    else
                    {
                        scenesLoading = 0;
                        System.out.println("Error loading images.  Image"
                                           +" load cancelled.");
                    }
                }

                if (isLoadCancelled)
                {
                    if (applet.verboseOutput)
                        System.out.println("Detected cancel");
                }

                // clear the loading flag and send an event to notify the 
                // load is complete
                isLoading = false;
                imagePane.repaint();
            }
        }
    }
}
