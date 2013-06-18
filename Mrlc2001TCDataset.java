// Mrlc2001TCDataset.java defines the 2001 Terrain Corrected (Radiance)
// MRLC Dataset details
//--------------------------------------------------------------------
import java.awt.Color;

public class Mrlc2001TCDataset extends MrlcDataset
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor
    Mrlc2001TCDataset(imgViewer applet)
    {
        super(applet, "MRLC/MTBS Radiance", "mrlc_2001_tc", "MRLC2K_ARCH",
              "https://lta.cr.usgs.gov/MRLC2001",
              resolutions, borderX, borderY, Color.YELLOW);

        numQualityValues = 0;

        // override the default scene id hint
        sceneIdHint = new String("Enter a 21 character scene ID");

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // enable downloading for the data
        isOrderable = false;
        downloadFileFormat = "tarred NDF";
        isDownloadable = true;
        slowDownloadStart = true;
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
