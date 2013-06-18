// Landsat8OLISensor.java defines the Landsat OLI sensor details
//-------------------------------------------------------------
import java.awt.Color;

public class Landsat8OLISensor extends LandsatSensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor
    Landsat8OLISensor(imgViewer applet)
    {
        super(applet,"Landsat 8 OLI", "l8oli", "LANDSAT_8",
              "showbrowse.cgi", "showmetadata.cgi", "USGS_logo.gif",
              "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/L8",
              "http://landsat.usgs.gov/tools_acq.php",
              resolutions,borderX,borderY,Color.YELLOW);

        numQualityValues = 1; // Yes, L8 only has 1
        qualityLimit = 9;

        // the Landsat L1T scenes are downloadable
        isDownloadable = true;
        mightBeDownloadable = true;
        maxScenesPerOrder = 100;

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 2013;
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
}

