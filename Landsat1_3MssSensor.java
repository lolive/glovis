// Landsat1_3MssSensor.java defines the Landsat 1-3 MSS sensor details
//--------------------------------------------------------------------
import java.awt.Color;

public class Landsat1_3MssSensor extends LandsatSensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor
    Landsat1_3MssSensor(imgViewer applet)
    {
        super(applet,"Landsat 1-3 MSS","l1_3mss","LANDSAT_MSS",
              "showbrowse.cgi","showmetadata.cgi","USGS_logo.gif",
              "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/MSS",
              null,
              resolutions,borderX,borderY,Color.YELLOW);

        numQualityValues = 1;
        hasNdviLineGraph = false;
        
        // the Landsat L1T scenes are downloadable
        isDownloadable = true;
        mightBeDownloadable = true;
        maxScenesPerOrder = 100;

        // set the navigation model to the WRS-1 descending model
        navModel = new WRS1Model();
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
