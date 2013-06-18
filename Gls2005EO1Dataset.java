// Gls2005EO1Dataset.java defines the GLS2005 EO-1 Islands sensor details
// This dataset uses the EO-1 ALI browse but GLS metadata.
//------------------------------------------------------------------------
import java.awt.Dimension;

public class Gls2005EO1Dataset extends EO1Sensor
{
    private static int[] resolutions = {1000,240};

    // Constructor
    Gls2005EO1Dataset
    (
        imgViewer applet        // I: applet reference
    )
    {
        super(applet,"GLS2005 Islands (EO-1)", "gls/gls2005_eo1",
              "GLS2005_EO1_ISLANDS", // name to pass to Shopping Cart
              "showEO1Browse.cgi", "showLandsatL1Metadata.cgi",
              "https://lta.cr.usgs.gov/GLS",
              "acqSchedule.html", "",
              resolutions);

        // reverse some options set by the parent class
        hasJulianDateMetadata = false;
        hasSwathMode = false;
        warnWhenOrderingPoorQuality = false;
        qualityLimit = 0;
        hasCustomSceneInfoLine = false;
        hasLookAngle = false;

        // set the flags for the optional sensor features available
        hasNdviLineGraph = true;
        hasCloudCover = false;
        useCloudCoverForDefaultScenes = false;
        dataHasGaps = false;
        downloadFileFormat = "GeoTIFF";
        slowDownloadStart = true;

        // set the dataset name for the CGI scripts since for L1T
        // we have to pass a dataset name
        cgiDatasetName = "GLS2005_EO1";

    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 2004;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return 2008;
    }

    // method to return a nominal EO-1 ALI scene size in meters
    //---------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        // Same as EO-1 ALI
        return new Dimension(37000,42000);
    }

    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public int getImageFileSize(int resolution)
    {
        // Same as EO-1 ALI
        if (resolution == 1000)
            return 10000;
        else
            return 60000;
    }
}
