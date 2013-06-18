// EO1Sensor.java defines the EO-1 sensor details common to both the ALI and
// Hyperion instruments.
//--------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;

public abstract class EO1Sensor extends Sensor
{
    private DecimalFormat fourDigitFormat;  // four digit number formatter
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets


    // Constructor with basic parameters (plain EO-1 ALI & Hyperion)
    EO1Sensor
    (
        imgViewer applet,       // I: applet reference
        String sensorName,      // I: name to use for the sensor
        String inventoryDir,    // I: name of directory for the data
        String datasetName,     // I: EE data set name for ordering
        int[] resolutions       // I: resolutions for this sensor
    )
    {
        super(applet,sensorName,inventoryDir,datasetName, "showEO1Browse.cgi",
              "showEO1Metadata.cgi", "USGS_logo.gif", "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/ALI",  // covers ALI & Hyperion
              "http://eo1.usgs.gov/acquisition",
              "Enter at least the first 22 characters of a scene ID",
              resolutions, borderX,borderY,Color.YELLOW);

        // enable the feature to do a full mosaic
        isFullMosaic = true;

        // Set up formatter for creating file names
        fourDigitFormat = new DecimalFormat ("0000");

        // set the flags for the optional sensor features available
        hasJulianDateMetadata = true;
        isOrderable = false;
        isDownloadable = true;
        hasColRowInSceneID = true;
        hasCustomSceneInfoLine = true;
        allowAddAll = false;      
        hasLookAngle = true;
        hasNdviLineGraph = true;

        // set the data acquisition request URL
        dataAcqRequestURL = new String("http://eo1.usgs.gov/dar");

        // don't use the transparent pixel fix so that the SCA misalignment is
        // completely highlighted
        useHighlightTransparentPixelFix = false;

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // enable user defined area
        hasUserDefinedArea = true;
    }

    // Constructor with extended parameters (GLS2005 Islands - EO-1)
    EO1Sensor
    (
        imgViewer applet,       // I: applet reference
        String sensorName,      // I: name to use for the sensor
        String inventoryDir,    // I: name of directory for the data
        String datasetName,     // I: EE data set name for ordering
        String showBrowseCGI,   // I: CGI script to use to show browse
        String showMetadataCGI, // I: CGI script to use to show metadata
        String productInfoURL,  // I: URL that describes this product
        String acquisitionScheduleURL, // I: acquisition schedule link
        String dataAcqRequestURL,      // I: acquisition request link
        int[] resolutions       // I: resolutions for this sensor
    )
    {
        super(applet,sensorName,inventoryDir,datasetName, showBrowseCGI,
              showMetadataCGI, "USGS_logo.gif", "http://www.usgs.gov",
              productInfoURL, acquisitionScheduleURL,
              "Enter at least the first 22 characters of a scene ID",
              resolutions, borderX,borderY,Color.YELLOW);

        // enable the feature to do a full mosaic
        isFullMosaic = true;

        // Set up formatter for creating file names
        fourDigitFormat = new DecimalFormat ("0000");

        // set the flags for the optional sensor features available
        hasJulianDateMetadata = true;
        isOrderable = false;
        isDownloadable = true;
        hasColRowInSceneID = true;
        hasCustomSceneInfoLine = true;
        allowAddAll = false;      
        hasLookAngle = true;
        hasNdviLineGraph = true;

        // set the data acquisition request URL
        dataAcqRequestURL = dataAcqRequestURL;

        // don't use the transparent pixel fix so that the SCA misalignment is
        // completely highlighted
        useHighlightTransparentPixelFix = false;

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // enable user defined area
        hasUserDefinedArea = true;
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
        imgName.append(fourDigitFormat.format(resolution));

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
        return new SwathSceneFilter(md, tocs, this, applet, true);
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 1999;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return -1;
    }

    // method to convert an entity ID from a scene's metadata to the value
    // short enough to display.  The first 22 characters are displayed.
    //--------------------------------------------------------------------
    String buildShortEntityID(Metadata scene)
    {
        return (scene.entityID.substring(0,22));
    }

    // method to return the custom scene info line
    //--------------------------------------------
    public String getCustomSceneInfo(Metadata scene)
    {
        if (scene == null)
            return "Look Angle: ";

        return "Look Angle: " + scene.lookAngle;
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
        int index = files[0].lastIndexOf('.');
        files[0] = files[0].substring(0, index);
        files[0] += ".meta";
        files[1] = scene.entityID + ".meta";

        // set the browse image source and destination names
        files[2] = getCellDirectory(scene.gridCol, scene.gridRow) 
                 + "/y" + scene.year + "/" + scene.entityID.substring(0,3)
                 + scene.entityID.substring(4) + ".jpeg";
        files[3] = scene.entityID + ".jpeg";

        return files;
    }
}
