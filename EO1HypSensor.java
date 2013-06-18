// EO1HypSensor.java defines the EO-1 Hyperion sensor details
//-----------------------------------------------------------
import java.awt.Dimension;

public class EO1HypSensor extends EO1Sensor
{
    private static int[] resolutions = {1000,120};

    // Constructor
    EO1HypSensor
    (
        imgViewer applet        // I: applet reference
    )
    {
        super(applet,"EO-1 Hyperion", "eo1/hyp", "EO1_HYP_PUB", resolutions);
    }

    // method to return a nominal EO-1 Hyperion scene size in meters
    //--------------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(7700,42000);
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
