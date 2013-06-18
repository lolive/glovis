// LandsatSensor.java defines the Landsat sensor details that are shared 
// across the ETM, TM, and MSS sensors as well as GLS datasets
//----------------------------------------------------------------------
import java.text.DecimalFormat;
import java.awt.Color;
import java.awt.Dimension;

public abstract class LandsatSensor extends Sensor
{
    // Constructor
    LandsatSensor
    (
        imgViewer applet,       // I: applet reference
        String sensorName,      // I: name to use for the sensor
        String inventoryDir,    // I: name of directory for the data
        String datasetName,     // I: EE data set name for ordering
        String showbrowse,      // I: showBrowse.cgi script name
        String showmetadata,    // I: showMetadata.cgi script name
        String logo,            // I: logo for used for sensor/dataset
        String logoWebLink,     // I: logo web link address
        String productInfoURL,  // I: product info link for the sensor
        String acquisitionScheduleURL, // I: acquisition schedule link
        int[] resolutions,      // I: resolutions available
        int[] borderXIn,        // I: 4 corner highlight X border offsets
        int[] borderYIn,        // I: 4 corner highlight Y border offsets
        Color borderColor      // I: color to use for the highlight border
    )
    {
        super(applet,sensorName,inventoryDir,datasetName,showbrowse,
              showmetadata,logo,logoWebLink,productInfoURL,
              acquisitionScheduleURL,"Enter a 21 digit scene ID",resolutions,
              borderXIn,borderYIn,borderColor);

        // set the flags for the optional sensor features available
        hasJulianDateMetadata = true;
        isOrderable = true;
        hasColRowInSceneID = true;
        hasSwathMode = true;
        hasNdviLineGraph = true;
        hasLevel1 = true;
        hasCustomSceneInfoLine = true;
        warnWhenOrderingPoorQuality = true;
        qualityLimit = 8;
    }

    // method to return the custom scene info line
    //--------------------------------------------
    public String getCustomSceneInfo(Metadata scene)
    {
        if (scene == null)
            return "Qlty:    Sensor: ";

        String info = "Qlty: " + scene.getQuality();

        // Only add Level 1 Product info if there is anything to list
        if (!scene.level1.isEmpty())
        {
            info += "  Product: ";

            Sensor sensor = scene.getSensor();
            if (sensor instanceof Landsat8OLISensor)
                info += (scene.level1.isEmpty() ? "OLI" : "");
            else if (sensor instanceof LandsatETMSensor)
                info += "ETM+";
            else if (sensor instanceof LandsatETMSlcOffSensor)
                info += "ETM+";
            else if (sensor instanceof LandsatTMSensor)
                info += "TM";
            else if (sensor instanceof Landsat4_5MssSensor)
                info += "MSS";
            else if (sensor instanceof Landsat1_3MssSensor)
                info += "MSS";

            info += " " + scene.level1;
        }

        return info;
    }

    // method to return an image file name for a given metadata reference and
    // resolution
    //  Returns: full file name, including directory
    //-----------------------------------------------------------------------
    public String makeImageName
    (
        Metadata scene,     // I: metadata for scene
        int resolution      // I: resolution requested in meters
    )
    {
        StringBuffer imgName;

        // build the image name, starting with the TOC directory and adding
        // the items that make up the file name
        imgName = new StringBuffer(getCellDirectory(scene.gridCol,
                                                    scene.gridRow));
        imgName.append("/y");
        imgName.append(scene.year);
        imgName.append("/");
        imgName.append(scene.entityID);
        imgName.append("_");
        imgName.append("" + resolution);

        // load correct image based on full res or mosaic view
        if (resolution != 1000)
        {
            // full resolution uses jpeg files
            imgName.append(".jpg");
        }
        else
        {
            // mosaic area uses gif files
            imgName.append(".gif");
        }

        return imgName.toString();
    }

    // method to return the number of cells to display for a given resolution.
    // Sensor.SINGLE_SCENE is returned if only one scene should be displayed.
    //------------------------------------------------------------------------
    public int getNumCellsAtResolution
    (
        int resolution      // I: resolution in meters
    )
    {
        if (resolution == resolutions[0])
            return applet.md.getMosaicSize();
        else
            return SINGLE_SCENE;
    }

    // method to return a scene filter that is compatible with this sensor.
    // Returns a SceneFilter object.
    //---------------------------------------------------------------------
    public SceneFilter getSceneFilter
    (
        MosaicData md,      // I: the mosaic data object
        TOC[] tocs          // I: array of TOC's to build the scene filter for
    )
    {
        return new LandsatSceneFilter(md);
    }

    // method to return a nominal Landsat scene size in meters
    //--------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(180000,180000);
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
        files[0] = makeImageName(scene, resolutions[resolutions.length - 1]);
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

    // method to determine whether a given sensor name
    // is one of this sensor's aliases
    //------------------------------------------------
    public boolean validName(String sensorNameToCheck)
    {
        if (sensorNameToCheck.equals(sensorName))
        {
            return true;
        }
        // Some subclasses of LandsatSensor used to have spaces
        // so let's just allow some general flexibility on whitespace
        if (sensorNameToCheck.replaceAll("\\s","").equals(
            sensorName.replaceAll("\\s","")))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}

