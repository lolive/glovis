// Gls1975Mss1_3Dataset.java defines the GLS1975Mss1_3 Landsat dataset details
//----------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;

public class Gls1975Mss1_3Dataset extends LandsatSensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor with no parameters (plain GLS)
    Gls1975Mss1_3Dataset(imgViewer applet)
    {
        this(applet, "GLS1975 (Landsat 1-3)", // sensorName (menu, Scene List title)
                     "GLS1975", // datasetName (Shopping Cart, searchForScene)
                     "https://lta.cr.usgs.gov/GLS",
                     false, // isOrderable
                     true   // isDownloadable
        );
    }

    // Constructor
    Gls1975Mss1_3Dataset(imgViewer applet, String sensorName, String datasetName,
        String productInfoUrl, boolean isOrderable, boolean isDownloadable)
    {
        super(applet, sensorName, "gls/gls1975_mss1_3", datasetName,
              "showLandsatL1Browse.cgi", "showLandsatL1Metadata.cgi",
              "USGS_logo.gif", "http://www.usgs.gov",
              productInfoUrl,
              null, resolutions, borderX, borderY, Color.YELLOW);

        // set the navigation model to the WRS-1 descending model
        navModel = new WRS1Model();

        // reverse some options set by the base class
        hasJulianDateMetadata = false;
        hasSwathMode = false;
        warnWhenOrderingPoorQuality = false;
        qualityLimit = 0;
        hasCustomSceneInfoLine = false;

        // set the flags for the optional sensor features available
        this.isOrderable = isOrderable;
        this.isDownloadable = isDownloadable;
        hasCloudCover = false;
        useCloudCoverForDefaultScenes = false;
        dataHasGaps = false;
        downloadFileFormat = "GeoTIFF";
        slowDownloadStart = true;

        // override the scene id hint since it is a different length than the
        // normal Landsat scene id
        sceneIdHint = "Enter a 19 digit scene ID";

        // set the dataset name for the CGI scripts since our name
        // is different than EE's name for the dataset
        cgiDatasetName = "GLS1975_1_3MSS";
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 1972;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return 1983;
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
}
