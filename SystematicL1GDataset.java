// SystematicL1GDataset.java defines the Systematic L1G dataset details
//----------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;

public class SystematicL1GDataset extends LandsatSensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor
    SystematicL1GDataset(imgViewer applet)
    {
        super(applet,"Systematic L1G", "lsat_sys", "SYS_ETM",
              "showLandsatL1Browse.cgi", "showLandsatL1Metadata.cgi",
              "USGS_logo.gif", "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/Tri_Dec_GLOO",
              null, resolutions, borderX, borderY, Color.YELLOW);

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // reverse some options set by the base class
        hasJulianDateMetadata = false;
        hasSwathMode = false;
        warnWhenOrderingPoorQuality = false;
        qualityLimit = 0;

        // set the flags for the optional sensor features available
        hasNdviLineGraph = true;
        isOrderable = false;
        isDownloadable = true;
        hasCloudCover = false;
        useCloudCoverForDefaultScenes = false;
        dataHasGaps = false;
        downloadFileFormat = "GeoTIFF";
        slowDownloadStart = true;

        // override the scene id hint since it is a different length than the
        // normal Landsat scene id
        sceneIdHint = "Enter a 21 character scene ID";

        // set the dataset name for the CGI scripts since our name
        // is different than EE's name for the dataset
        cgiDatasetName = "SYSTEMATIC_L1G";
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
        return 2003;
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
