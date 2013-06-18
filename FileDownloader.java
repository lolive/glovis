// Name: FileDownloader
//
// Description: The FileDownloader class implements a utility class to 
//  download files from the server and save them to the local hard drive
//  in a background thread.
//------------------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import javax.swing.ProgressMonitor;
import javax.swing.Timer;

public class FileDownloader implements Runnable, ActionListener
{
    private File destinationDir;    // destination directory on user's drive
    private Metadata[] scenes;      // list of scenes to download
    private imgViewer applet;       // applet reference
    private Sensor currSensor;      // the sensor of the data download
    ProgressMonitor pm;             // progress monitor for showing progress
    String currentSceneID;          // current scene ID for displaying in
                                    // progress monitor
    int currentSceneIndex;          // current scene index for monitor
    Timer refreshTimer;             // timer for updating progress

    // constructor for the FileDownloader
    //-----------------------------------
    public FileDownloader(imgViewer applet, File destinationDir, 
                          Metadata[] scenes)
    {
        this.applet = applet;
        this.destinationDir = destinationDir;
        this.scenes = scenes;
        this.currSensor = applet.sensorMenu.getCurrentSensor();

        // create a progress monitor to keep the user informed of the progress
        // and pop it up if the download takes longer than 500 milliseconds
        pm = new ProgressMonitor(applet, 
                        "Downloading data to " + destinationDir.toString(),
                        "Downloading " + scenes[0].getEntityIDForDisplay() 
                          + " (" + scenes.length + " remaining)", 0,
                        scenes.length);
        pm.setMillisToPopup(500);
    }

    // handle timer events for updating the progress monitor (needs to be
    // done in a timer so the updates happen in the GUI thread)
    //-------------------------------------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        // indicate which scene id is being downloaded in the progress
        // monitor and update the progress
        pm.setNote("Downloading " + currentSceneID + " ("
                   + (scenes.length - currentSceneIndex) + " remaining)");
        pm.setProgress(currentSceneIndex);

        // stop the timer when the download is cancelled or the work is
        // complete
        // if the download wasn't cancelled, set the progress to complete
        if (pm.isCanceled() || (currentSceneIndex >= scenes.length))
        {
            // complete the progress monitor
            pm.setProgress(scenes.length);

            // make sure the progress monitor is closed
            pm.close();

            // stop the timer
            refreshTimer.stop();
            refreshTimer = null;
        }
    }

    // method to create and start the download thread
    //-----------------------------------------------
    public void startDownload()
    {
        // create a thread for doing the download
        Thread downloadThread = new Thread(this, "Download thread");
        downloadThread.start();

        // create the refresh timer for updating the progress monitor
        refreshTimer = new Timer(500, this);
        refreshTimer.start();
    }

    // method to download the files in a separate thread
    //--------------------------------------------------
    public void run()
    {
        // download the files for each of the scenes in the list
        for (int i = 0; i < scenes.length; i++)
        {
            // update the variables for displaying progress
            currentSceneID = scenes[i].getEntityIDForDisplay() ;
            currentSceneIndex = i;

            // get the list of filenames for this scene
            String[] filenames = currSensor.getFilesForScene(scenes[i]);
            String browseName = null;

            // download each of the files
            for (int file = 0; file < filenames.length; file += 2)
            {
                String currFile = filenames[file];

                // if the current filename is null, it means the browse
                // filename should have been looked up when the metadata file
                // was read, so use that name.
                if (currFile == null)
                {
                    int index = filenames[0].lastIndexOf('/');
                    currFile = filenames[0].substring(0, index + 1);
                    currFile += browseName; // ???? Where does this get set???
                }

                // if the filename is null, skip this file
                if (currFile == null)
                    continue;

                // get the extension on the file
                int index = currFile.lastIndexOf('.');
                String ext = currFile.substring(index);

                // name the destination file the scene id with the extension.
                // This is to get rid of the extra resolution part of the 
                // .meta file names.
                File destFile = new File(destinationDir, filenames[file + 1]);

                // download .meta files as text and others as binary
                if (ext.equals(".meta"))
                    browseName = downloadTextFile(currFile, destFile);
                else
                    downloadBinaryFile(currFile, destFile);
            }

            // if the user cancelled the download, stop the download
            if (pm.isCanceled())
                break;
        }

        // if the download wasn't cancelled, set the progress to complete
        if (!pm.isCanceled())
            currentSceneIndex = scenes.length;

        // help the garbage collector know which objects are no longer used
        destinationDir = null;
        currSensor = null;
    }

    // method to download text files from the server.  The newline character
    // is translated to the local OS newline character as part of the process.
    // The browse image filename found is returned (null if not found).
    //------------------------------------------------------------------------
    private String downloadTextFile(String sourceFile, File destFile)
    {
        String browseName = null;
        BufferedReader source = null;
        BufferedWriter dest = null;
        try
        {
            // build the URL for the source file
            URL sourceURL = new URL(applet.getCodeBase(), sourceFile);

            // open the source file and destination files
            source = new BufferedReader(
                        new InputStreamReader(sourceURL.openStream()));
            dest = new BufferedWriter(new FileWriter(destFile));

            // read each line from the source file and write it to the
            // destination file.  After each line in the destination file,
            // output a local OS newline character.
            String line;
            while ((line = source.readLine()) != null)
            {
                dest.write(line, 0, line.length());
                dest.newLine();

                // look for the browse name in case this sensor's browse 
                // filename can't be known ahead of time
                if (line.startsWith("LocalBrowseName"))
                {
                    int index = line.indexOf('=');
                    if (index != -1)
                    {
                        browseName = line.substring(index + 1).trim();
                    }

                }
            }

            // close the files
            source.close();
            source = null;
            dest.close();
            dest = null;
        }
        catch (Exception e)
        {
            // catch any exceptions and close any open files
            System.out.println("exception: " + e.toString());
            try
            {
                if (source != null)
                    source.close();
                if (dest != null)
                    dest.close();
            }
            catch (Exception e1) {}
        }

        return browseName;
    }

    // method to download binary files from the server
    //------------------------------------------------
    private void downloadBinaryFile(String sourceFile, File destFile)
    {
        DataInputStream source = null;
        FileOutputStream dest = null;
        try
        {
            // build the URL for the source file
            URL sourceURL = new URL(applet.getCodeBase(), sourceFile);

            // open the source file and destination files
            source = new DataInputStream(
                        new BufferedInputStream(sourceURL.openStream()));
            dest = new FileOutputStream(destFile);

            // allocate a buffer for the file data, making it large enough
            // that most files will be handled with a single read/write
            int bufsize = 60000;
            byte[] buf = new byte[bufsize];
            int numRead;

            // copy the data from the source file to the destination file
            while ((numRead = source.read(buf)) != -1)
            {
                dest.write(buf, 0, numRead);
            }

            // close the files
            source.close();
            source = null;
            dest.close();
            dest = null;
        }
        catch (Exception e)
        {
            // catch any exceptions and close open files
            System.out.println("exception: " + e.toString());
            try
            {
                if (source != null)
                    source.close();
                if (dest != null)
                    dest.close();
            }
            catch (Exception e1) {}
        }
    }
}
