// LandsatETMSensor.java defines the Landsat ETM sensor details
//-------------------------------------------------------------
import java.awt.Color;

public class LandsatETMSensor extends LandsatSensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor
    LandsatETMSensor(imgViewer applet)
    {
        super(applet,"L7 SLC-on (1999-2003)", "l7", "LANDSAT_ETM",
              "showbrowse.cgi", "showmetadata.cgi", "USGS_logo.gif",
              "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/LETMP",
              "http://landsat.usgs.gov/technical_details/data_acquisition/"+
              "l7_acquisition_calendar.php",
              resolutions,borderX,borderY,Color.YELLOW);

        numQualityValues = 2;
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
