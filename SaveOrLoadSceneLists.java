// Name: SaveOrLoadSceneLists.java
//
// Description: provides a helper class for saving/loading the contents of
// the scene lists to/from a file on the user's hard drive.
//
// Notes:
//  - The main goal of this class is to implement the save and load methods.
//  - Due to the need to validate a loaded list of scenes against the scene
//    inventory on the server, a separate thread is used to validate the 
//    data since it can take a while on a slow connection (during which 
//    time we don't want the applet to appear hung).
//  - After the scenes are successfully validated, the actual loading of the
//    scenes into the applet scene lists needs to happen in the main thread,
//    so the invokeLater method is used to invoke a routine in the main
//    applet thread.
//  - The inner classes declared in this class are all declared as "static
//    classes".  This is needed since they are called from a static method
//    of the class.  All it means is that the inner classes do not have a
//    reference to the outer class and cannot access members of the outer
//    class.
//--------------------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;  // invokeLater
import javax.swing.Timer;

public class SaveOrLoadSceneLists
{
    static File savedDirectory = null;  // directory previously chosen

    // method to save the contents of the scene lists to a file. Note that the
    // method is "static" since it is really just a helper function that does
    // not require an object to be created.
    //------------------------------------------------------------------------
    static void save(imgViewer applet, boolean useHiddenList)
    {
        // create a swing file chooser to allow the user to select a file
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Select scene list file name");

        // if there is a previously saved directory and that directory
        // exists, start the file chooser there
        if ((savedDirectory != null) && savedDirectory.exists())
            fc.setCurrentDirectory(savedDirectory);

        // show the file chooser dialog
        int result = fc.showSaveDialog(applet.getDialogContainer());

        if (result == JFileChooser.APPROVE_OPTION)
        {
            // get the file name chosen
            File destFile = fc.getSelectedFile();

            // save the directory name so it can be defaulted next time
            savedDirectory = fc.getCurrentDirectory();

            // declare the output stream for the saved scenes file (here so
            // that the catch statement can detect whether it needs closing)
            PrintWriter dest = null;

            try
            {
                // open the file for writing
                dest = new PrintWriter(new BufferedWriter(
                        new FileWriter(destFile)));

                // write the header to the file
                dest.println("GloVis Scene List");

                // get the full list of datasets (sensors)
                Sensor[] sensors = applet.getSensors();

                // loop over the available sensors
                for (int sensor = 0; sensor < sensors.length; sensor++)
                {
                    Sensor currSensor = sensors[sensor];

                    // do not save the scene lists for datasets that are 
                    // actually composed of other datasets since the base
                    // datasets will save the scene list contents
                    if (!currSensor.hasMultipleDatasets)
                    {
                        // pick which scene list is being saved
                        SceneList sceneList;
                        if (useHiddenList)
                            sceneList = currSensor.hiddenSceneList;
                        else
                            sceneList = currSensor.sceneList;

                        // get the number of scenes in the current scene list
                        int sceneCount = sceneList.getSceneCount();

                        // if there are scenes in the list, write them to the
                        // file
                        if (sceneCount > 0)
                        {
                            // add the sensor name to the file for the scenes
                            // that follow
                            dest.println("sensor=" + currSensor.sensorName);

                            // add the EE dataset_name in case the user wants
                            // to load this list into EE (probably for bulk)
                            dest.println("ee_dataset_name="
                                + currSensor.datasetName);

                            // write the entity id for each scene in the list
                            for (int i = 0; i < sceneCount; i++)
                            {
                                Metadata scene = sceneList.getSceneAt(i);

                                dest.println(currSensor.buildEntityID(scene));
                            }
                        }
                    }
                }

                // close the output file
                dest.close();
                dest = null;
            }
            catch (Exception e)
            {
                // popup a message box with an error message
                JOptionPane.showMessageDialog(applet.getDialogContainer(),
                        "Error writing " + destFile,
                        "Error Writing Scene List File", 
                        JOptionPane.ERROR_MESSAGE);

                // catch any exceptions and close any open files
                System.out.println("exception: " + e.toString());
                try
                {
                    if (dest != null)
                        dest.close();
                }
                catch (Exception e1) {}
            }
        }
    }

    // method to load the contents of the scene lists from a file.  Note that
    // the method is "static" since it is really just a helper function that
    // does not require an object to be created.
    //-------------------------------------------------------------------------
    static void load(imgViewer applet, boolean useHiddenList)
    {
        // create a swing file chooser to allow the user to select the file
        // to load
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Select scene list file name");

        // if there is a previously saved directory and that directory
        // exists, start the file chooser there
        if ((savedDirectory != null) && savedDirectory.exists())
            fc.setCurrentDirectory(savedDirectory);

        // show the file chooser dialog
        int result = fc.showOpenDialog(applet.getDialogContainer());
        if (result == JFileChooser.APPROVE_OPTION)
        {
            // a file was chosen, so set it
            File sourceFile = fc.getSelectedFile();

            // save the directory too so it can be defaulted next time
            savedDirectory = fc.getCurrentDirectory();

            BufferedReader source = null;
            int lineNumber = 0;

            try
            {
                // open the file for reading
                source = new BufferedReader(new FileReader(sourceFile));

                // verify the first line has the scene list tag
                String line = source.readLine();
                lineNumber++;
                if ((line == null) || !line.contains("GloVis Scene List"))
                {
                    // popup a message box with an error message
                    JOptionPane.showMessageDialog(applet.getDialogContainer(),
                      "The file does not appear to be a GloVis Scene List File",
                      "Error Loading Scene List File", 
                      JOptionPane.ERROR_MESSAGE);
                    
                    source.close();
                    source = null;
                    return;
                }

                // declare a vector to hold the list of scenes read
                Vector loadedScenes = new Vector();

                // get the full list of datasets (sensors)
                Sensor[] sensors = applet.getSensors();

                boolean errorFound = false;
                boolean sensorSet = false;
                Sensor sensor = null;

                // read all the lines from the input file
                while ((line = source.readLine()) != null)
                {
                    lineNumber++;
                    line = line.trim();
                    if (line.isEmpty())
                    {
                        continue; // ignore blank lines
                    }
                    else if (line.startsWith("sensor=")
                          || line.startsWith("dataset="))
                    {
                        // determine if this line is defining the current sensor
                        // clear the sensor set flag in case this line
                        // doesn't match a sensor name
                        sensorSet = false;

                        // isolate the sensor name
                        int index = line.indexOf('=');
                        String sensorName = line.substring(index + 1);

                        // match the sensor name to a sensor reference
                        for (int i = 0; i < sensors.length; i++)
                        {
                            Sensor testSensor = sensors[i];

                            // over the years, some sensors have changed names
                            // so we check for the current as well as past or
                            // other (unique) alternate names for each sensor
                            // so we don't break any saved scene lists
                            if (testSensor.validName(sensorName))
                            {
                                // found a matching sensor
                                sensor = testSensor;
                                sensorSet = true;
                                break;
                            }
                        }
                        if (!sensorSet)
                        {
                            JOptionPane.showMessageDialog(
                                applet.getDialogContainer(),
                                "Invalid sensor/dataset at line " + lineNumber,
                                "Error Loading Scene List File",
                                JOptionPane.ERROR_MESSAGE);
                            errorFound = true;
                            break;
                        }
                    }
                    else if (line.contains("="))
                    {
                        // ignore key=value pairs except sensor= or dataset=
                        // such as ee_dataset_name= (which only EE cares about)
                        continue;
                    }
                    else if (sensorSet)
                    {
                        // must be an Entity ID: A-Z a-z 0-9 _ . : -
                        if (line.matches("^[\\w.:-]+$"))
                        {
                            // add an entry for this scene to the list
                            LoadedScene loadedScene
                                = new SaveOrLoadSceneLists.LoadedScene();
                            loadedScene.sensor = sensor;
                            loadedScene.entityID = line;
                            loadedScenes.add(loadedScene);
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(
                                applet.getDialogContainer(),
                                "Invalid scene ID at line " + lineNumber,
                                "Error Loading Scene List File",
                                JOptionPane.ERROR_MESSAGE);
                            errorFound = true;
                            break;
                        }
                    }
                    else
                    {
                        // popup a message box with an error message
                        JOptionPane.showMessageDialog(
                            applet.getDialogContainer(),
                            "The file does not appear to be a GloVis Scene "
                            + "List File", "Error Loading Scene List File", 
                            JOptionPane.ERROR_MESSAGE);
                        errorFound = true;
                        break;
                    }
                }

                // close the output file
                source.close();
                source = null;

                if (!errorFound)
                {
                    // the file successfully loaded, so create a thread to 
                    // validate the contents of the list.  A separate thread
                    // is used since this process can take a long time on a
                    // slow connection.
                    SceneValidator sv
                        = new SaveOrLoadSceneLists.SceneValidator(applet,
                            loadedScenes, useHiddenList);
                    sv.startValidation();
                }
            }
            catch (Exception e)
            {
                // popup a message box on error
                JOptionPane.showMessageDialog(applet.getDialogContainer(),
                        "Error loading " + sourceFile,
                        "Error Loading Scene List File", 
                        JOptionPane.ERROR_MESSAGE);

                // catch any exceptions and close any open files
                System.out.println("exception: " + e.toString());
                try
                {
                    if (source != null)
                        source.close();
                }
                catch (Exception e1) {}
            }
        }
    }

    // inner class to allow storing a reference to a sensor and an entity id
    // together in a container.  This is used to pass the list of scenes 
    // loaded from a file to the scene validator.
    //----------------------------------------------------------------------
    private static class LoadedScene
    {
        Sensor sensor;
        String entityID;
    };

    // inner class to allow storing a reference to a sensor and a metadata
    // reference together in a container.  This is used to pass the list
    // of validated scene metadata to the object to update the scene lists
    // in the main thread.
    //--------------------------------------------------------------------
    private static class ValidatedScene
    {
        Sensor sensor;
        Metadata scene;
    };

    // Inner class to validate a list of scenes loaded from a file against
    // the current inventory to make sure they are valid.  Also loads the 
    // enough metadata for the scene to be able to add it to a sensor scene
    // list.  This class does the validation using a separate thread.
    //---------------------------------------------------------------------
    private static class SceneValidator implements Runnable, ActionListener
    {
        private imgViewer applet;       // applet reference
        private Vector loadedScenes;    // list of scenes from the file
        private boolean useHiddenList;  // flag to indicate whether the 
                                        // ordering or hidden scene list is
                                        // being loaded
        ProgressMonitor pm;             // progress monitor for showing progress
        String currentSceneID;          // current scene ID for displaying in
                                        // progress monitor
        int currentSceneIndex;          // current scene index for monitor
        Timer refreshTimer;             // timer for updating progress

        // constructor
        //------------
        SceneValidator(imgViewer applet, Vector loadedScenes, 
                       boolean useHiddenList)
        {
            this.applet = applet;
            this.loadedScenes = loadedScenes;
            this.useHiddenList = useHiddenList;

            // create a progress monitor to keep the user informed of the
            // progress and pop it up if the download takes longer than 500
            // milliseconds
            pm = new ProgressMonitor(applet.getDialogContainer(),
                        "Validating Scene List", null, 0, loadedScenes.size());
            pm.setMillisToPopup(500);

            // create the refresh timer for updating the progress monitor
            refreshTimer = new Timer(500, this);
            refreshTimer.start();
        }

        // method to create the thread to do the validation and start it
        //--------------------------------------------------------------
        public void startValidation()
        {
            Thread validateThread = new Thread(this, "Validate Thread");
            validateThread.start();
        }

        // handle timer events for updating the progress monitor (needs to be
        // done in a timer so the updates happen in the GUI thread)
        //-------------------------------------------------------------------
        public void actionPerformed(ActionEvent event)
        {
            // indicate which scene id is being downloaded in the progress
            // monitor and update the progress
            pm.setNote("Validating " + currentSceneID);
            pm.setProgress(currentSceneIndex);

            // stop the timer when the download is cancelled or the work is
            // complete
            // if the download wasn't cancelled, set the progress to complete
            if (pm.isCanceled() || (currentSceneIndex >= loadedScenes.size()))
            {
                // complete the progress monitor
                pm.setProgress(loadedScenes.size());

                // make sure the progress monitor is closed
                pm.close();

                // stop the timer
                refreshTimer.stop();
                refreshTimer = null;
            }
        }

        // run method for the validation thread
        //-------------------------------------
        public void run()
        {
            int numScenes = loadedScenes.size();

            // create a vector for the validated scenes
            Vector validatedScenes = new Vector();

            // track any errors encountered when validating the scenes
            String[] errors = new String[10];
            errors[0] = "The following scenes are not in the inventory:";
            int errorCount = 0;

            // process the scenes in the loaded scene list
            for (int index = 0; index < numScenes; index++)
            {
                // get the current scene from the list
                LoadedScene loadedScene 
                            = (LoadedScene)loadedScenes.elementAt(index);

                // cache a reference to the sensor
                Sensor sensor = loadedScene.sensor;

                // update the variables used to update the progress monitor
                currentSceneID = loadedScene.entityID;
                currentSceneIndex = index;

                // validate the scene by using the search for a scene feature
                Metadata scene = applet.searchForSceneDialog.searchForScene(
                        sensor, loadedScene.entityID);
                if (scene != null)
                {
                    // the scene was found in the inventory, so save its
                    // metadata in the valided scene list
                    ValidatedScene validatedScene = new SaveOrLoadSceneLists.ValidatedScene();
                    validatedScene.sensor = sensor;
                    validatedScene.scene = scene;
                    validatedScenes.add(validatedScene);
                }
                else
                {
                    // the scene wasn't found, so add it to the error message
                    // of the scenes that couldn't be found (limiting the 
                    // number of errors reported)
                    if (errorCount < 8)
                    {
                        errorCount++;
                        errors[errorCount] = sensor.sensorName + ": " 
                            + loadedScene.entityID;
                    }
                    else if (errorCount == 8)
                    {
                        errorCount++;
                        errors[errorCount] = "  and more...";
                    }
                }

                // if the user cancelled the download, stop the download
                if (pm.isCanceled())
                    break;
            }

            // if the download wasn't cancelled, set the progress to complete
            if (!pm.isCanceled())
            {
                // update the index to the last scene
                currentSceneIndex = numScenes;

                // if there were errors detected, copy the errors to a list
                // that is sized properly for the number of errors reported
                // (to make it easy to pass to a message box)
                String[] errorList = null;
                if (errorCount > 0)
                {
                    errorList = new String[errorCount + 1];
                    for (int i = 0; i < errorCount + 1; i++)
                        errorList[i] = errors[i];
                }

                // create the object to update the applet scene lists
                // and call invokeLater to make sure the applet scene lists
                // are updated from the main thread
                UpdateSceneLists updateSceneLists = new SaveOrLoadSceneLists.UpdateSceneLists(
                            applet, validatedScenes, useHiddenList, errorList);
                SwingUtilities.invokeLater(updateSceneLists);
            }
        }
    }

    // Inner class to update the applet scene lists with the contents of
    // a validated scene list.  This class exists to allow invokeLater to
    // be used to call this class' run method to load the applet scene lists
    // from the main thread.
    //---------------------------------------------------------------------
    private static class UpdateSceneLists implements Runnable
    {
        private imgViewer applet;       // applet reference
        private Vector validatedScenes; // list of validated scenes
        private String[] errorList;     // list of vaildation errors
        private boolean useHiddenList;  // flag to indicate whether the 
                                        // ordering or hidden scene list is
                                        // being loaded

        // constructor
        //------------
        UpdateSceneLists(imgViewer applet, Vector validatedScenes, 
                         boolean useHiddenList, String[] errorList)
        {
            this.applet = applet;
            this.validatedScenes = validatedScenes;
            this.useHiddenList = useHiddenList;
            this.errorList = errorList;
        }

        // method invoked by the invokeLater call.  It updates the applet
        // scene lists with the validated scenes and pops up a message box
        // with any errors.
        //----------------------------------------------------------------
        public void run()
        {
            // if any errors were encountered, pop up a message box with
            // the errors
            if (errorList != null)
            {
                JOptionPane.showMessageDialog(applet.getDialogContainer(),
                        errorList, "Error Loading Scene List File",
                        JOptionPane.ERROR_MESSAGE);
            }

            // load the validated scenes in to the correct scene list
            int numScenes = validatedScenes.size();
            for (int index = 0; index < numScenes; index++)
            {
                ValidatedScene validatedScene 
                    = (ValidatedScene)validatedScenes.elementAt(index);

                if (!useHiddenList)
                    validatedScene.sensor.sceneList.add(validatedScene.scene);
                else
                {
                    validatedScene.sensor.hiddenSceneList.add(
                            validatedScene.scene);
                }
            }
        }
    }
}
