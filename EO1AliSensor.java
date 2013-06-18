// EO1AliSensor.java defines the EO-1 ALI sensor details
//------------------------------------------------------
import java.awt.Dimension;

public class EO1AliSensor extends EO1Sensor
{
    private static int[] resolutions = {1000,240};

    // Constructor
    EO1AliSensor
    (
        imgViewer applet        // I: applet reference
    )
    {
        super(applet,"EO-1 ALI", "eo1/ali", "EO1_ALI_PUB", resolutions);
    }

    // method to return a nominal EO-1 ALI scene size in meters
    //---------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(37000,42000);
    }

    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public int getImageFileSize(int resolution)
    {
        if (resolution == 1000)
            return 10000;
        else
            return 60000;
    }
}
