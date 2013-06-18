// LandsatCombined.java implements a dataset class that is composed of
// other Landsat datasets
//--------------------------------------------------------------------
import java.util.Vector;
import java.util.HashMap;
import java.awt.Color;

public class LandsatCombined extends LandsatSensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    private Sensor[] sensors;     // other sensors contained in this dataset
    private int startYear;        // starting year of the combined datasets

    // Constructor
    //------------
    LandsatCombined(imgViewer applet, Sensor[] datasets)
    {
        super(applet,"Landsat 4 - Present","","LANDSAT_COMBINED",
              "showbrowse.cgi",
              "showmetadata.cgi","USGS_logo.gif","http://www.usgs.gov",
              "https://lta.cr.usgs.gov/LETMP",
              null,resolutions,borderX,borderY,Color.GRAY);

        numQualityValues = 2;
        hasMultipleDatasets = true;
        isDownloadable = true;
        mightBeDownloadable = true;
        maxScenesPerOrder = 100;

        sensors = new Sensor[datasets.length];

        // find the starting year for the combined dataset
        startYear = 10000;
        for (int i = 0; i < datasets.length; i++)
        {
            sensors[i] = datasets[i];

            // find the earliest starting year
            if (sensors[i].getStartingYear() < startYear)
                startYear = sensors[i].getStartingYear();
        }

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // make the scene lists for the combined dataset CombinedSceneLists
        SceneList[] lists = new SceneList[datasets.length];
        SceneList[] hiddenLists = new SceneList[datasets.length];
        for (int i = 0; i < datasets.length; i++)
        {
            lists[i] = datasets[i].sceneList;
            hiddenLists[i] = datasets[i].hiddenSceneList;
        }
        sceneList = new CombinedSceneList(applet, this, lists);
        hiddenSceneList = new CombinedSceneList(applet, this, hiddenLists);
    }

    // method to read the multiple datasets into the TOC
    //--------------------------------------------------
    public void readTOC(TOC cell)
    {
        cell.read(sensors[0]);
        TOC partialCell = new TOC(applet.getCodeBase(), cell.gridCol, 
                                  cell.gridRow);
        for (int i = 1; i < sensors.length; i++)
        {
            if (cell.valid)
            {
                // the cell is valid, so read the next sensor's cell and add
                // it to the current one
                partialCell.read(sensors[i]);
                cell.add(partialCell);
            }
            else
            {
                // the cell is not valid, so read the new sensor directly into
                // the main cell
                cell.read(sensors[i]);
            }
        }
    }

    // method to determin the different color of border based on which dataset
    // the scene comes from 
    //------------------------------------------------------------------------
    public Color getBorderColor(Metadata scene)
    {
        Sensor sensor = scene.getSensor();

        if (sensor instanceof Landsat8OLISensor)
            return Color.orange;
        else if (sensor instanceof LandsatETMSensor)
            return Color.blue;
        else if (sensor instanceof LandsatETMSlcOffSensor)
            return Color.green;
        else if (sensor instanceof LandsatTMSensor)
            return Color.yellow;
        else if (sensor instanceof Landsat4_5MssSensor)
            return Color.red;
        else
            return Color.lightGray;
    }

    // method to return the custom scene info line
    // Adds Product if set, otherwise always sets Sensor
    //--------------------------------------------
    public String getCustomSceneInfo(Metadata scene)
    {
        if (scene == null)
            return "Qlty:    Sensor: ";

        String info = "Qlty: " + scene.getQuality();

        if (scene.level1.isEmpty())
            info += "  Sensor: ";
        else
            info += "  Product: ";

        Sensor sensor = scene.getSensor();
        if (sensor instanceof Landsat8OLISensor)
            info += (scene.level1.isEmpty() ? "OLI" : "");
        else if (sensor instanceof LandsatETMSensor)
            info += "ETM+";
        else if (sensor instanceof LandsatETMSlcOffSensor)
            info += (scene.level1.isEmpty() ? "ETM+ SLC-off" : "ETM+");
        else if (sensor instanceof LandsatTMSensor)
            info += "TM";
        else if (sensor instanceof Landsat4_5MssSensor)
            info += "MSS";

        if (!scene.level1.isEmpty())
            info += " " + scene.level1;

        return info;
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return startYear;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return -1;
    }

    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public int getImageFileSize(int resolution)
    {
        if (resolution == 1000)
            return 35000;
        else
            return 120000;
    }

    // method to build the URL for ordering a list of scenes. This overloads
    // the default method so we can order from multiple datasets.
    //---------------------------------------------------------------------
    public String buildOrderURL
    (
        Metadata[] scenes    // I: array of scenes to order
    )
    {
        HashMap idMap = new HashMap(); // dataset name mapped to list of IDs
        Vector idList;                 // list of IDs
        String ShoppingCartUrl = new String("");

        for (int i = 0; i < scenes.length; i++) 
        {
            Metadata scene = scenes[i];
            Sensor sensor = scene.getSensor();
            if (idMap.containsKey(sensor.datasetName))
            {
                idList = (Vector) idMap.get(sensor.datasetName);
            }
            else
            {
                idList = new Vector();
            }
            idList.add(scene.entityID);
            idMap.put(sensor.datasetName, idList);
        }

        ShoppingCartUrl = EarthExplorer.buildShoppingCartMultiDatasetUrl(idMap);

        return ShoppingCartUrl;
    }

    // method to return the files to download for a given scene when the
    // user downloads metadata and browse.  Returns an array of filenames where
    // the source and destination file names are paired up.  If the source
    // browse name is not known, null is returned in that array entry.
    //-------------------------------------------------------------------------
    public String[] getFilesForScene(Metadata scene)
    {
        String[] files = new String[4];

        // set the metadata source and destination names
        files[0] = scene.getSensor().makeImageName(scene,
                                        resolutions[resolutions.length - 1]);
        int index = files[0].lastIndexOf('/');
        files[0] = files[0].substring(0, index + 1);
        files[0] += scene.entityID;
        files[0] += ".meta";
        files[1] = scene.entityID + ".meta";

        // set the browse image source and destination names.  Note that
        // the source is set to null since the filename varies and the null
        // tells the file downloader to get the browse name from the metadata
        // file.
        files[2] = null;
        files[3] = scene.entityID + ".jpg";

        return files;
    }

    // method to get the image name for the scene.  Because this sensor
    // is composed of multiple datasets, we need to use scene's sensor
    // to determine how to make the image name
    //------------------------------------------------------------------------
    public String makeImageName(Metadata scene, int pixelSize)
    {
        return scene.getSensor().makeImageName(scene,pixelSize);
    }

    // method to create the metadata for the results of a search for scene
    // operation
    //--------------------------------------------------------------------
    public Metadata createMetadata(String dataLine, int gridCol, int gridRow)
    {
        // find the correct dataset
        for (int i = 0; i < sensors.length; i++)
        {
            if (dataLine.startsWith(sensors[i].datasetName))
            {
                // this is the correct dataset, so create the metadata using
                // this sensor (after removing the datasetName)
                int index = dataLine.indexOf(',');
                String line = dataLine.substring(index + 1);
                return new Metadata(line, sensors[i], gridCol, gridRow);
            }
        }
        return null;
    }

    // method to return the list of sensors in the Landsat combined dataset.
    //----------------------------------------------------------------------
    public Sensor[] getSensorList()
    {
        return sensors;
    }

    // method to build the URL for downloading a scene
    //------------------------------------------------
    public String buildDownloadURL(Metadata scene)
    {
        return scene.getSensor().buildDownloadURL(scene);
    }

    // method to determine whether a given sensor name
    // is one of this sensor's aliases
    //------------------------------------------------
    public boolean validName(String sensorNameToCheck)
    {
        if (sensorNameToCheck.equals(sensorName))
        {
            return true;
        }
        // This dataset used to be called L4-7 Combined
        // (before Landsat 8 was added)
        if (sensorNameToCheck.equals("L4-7 Combined"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}

