// Gls1990Dataset.java defines the GLS1990 Landsat dataset details
//----------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;

public class Gls1990Dataset extends LandsatSensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor with no parameters (plain GLS)
    Gls1990Dataset(imgViewer applet)
    {
        this(applet, "GLS1990", // sensorName (menu, Scene List title)
                     "GLS1990", // datasetName (Shopping Cart, searchForScene)
                     "https://lta.cr.usgs.gov/GLS",
                     false, // isOrderable
                     true   // isDownloadable
        );
    }

    // Constructor
    Gls1990Dataset(imgViewer applet, String sensorName, String datasetName,
        String productInfoUrl, boolean isOrderable, boolean isDownloadable)
    {
        super(applet, sensorName, "gls/gls1990", datasetName,
              "showLandsatL1Browse.cgi", "showLandsatL1Metadata.cgi",
              "USGS_logo.gif", "http://www.usgs.gov",
              productInfoUrl,
              null, resolutions, borderX, borderY, Color.YELLOW);

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // reverse some options set by the base class
        hasJulianDateMetadata = false;
        hasSwathMode = false;
        warnWhenOrderingPoorQuality = false;
        qualityLimit = 0;
        hasCustomSceneInfoLine = false;

        // set the flags for the optional sensor features available
        hasNdviLineGraph = true;
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

        // set the dataset name for the CGI scripts since for L1T
        // we have to pass a dataset name
        cgiDatasetName = "GLS1990";
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 1984;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return 1997;
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
