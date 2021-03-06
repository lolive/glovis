// SinusoidalLocatorMapConfig.java is a simple class to allow configuring the
// locator map that is in the Sinusoidal projection.
//
// Note: this config file is written using static finals.  So, it is a 
//       compile time configuration.
//---------------------------------------------------------------------------

public class SinusoidalLocatorMapConfig
{
    public static final int IMAGE_WIDTH = 503;
    public static final int IMAGE_HEIGHT = 251;
    public static final double LEFT_X = -20015109.354;
    public static final double RIGHT_X = (LEFT_X + 926.625433 * 1200 * 36);
    public static final double TOP_Y = 10007554.677;
    public static final double BOTTOM_Y = (TOP_Y - 926.625433 * 1200 * 18);
    public static final String MAP_IMAGE = "graphics/modistiles.jpg";
    public static final String BOUNDARY_IMAGE  = "";

    // set this to false if there is no boundaries image to overlay on the
    // locator map
    public static final boolean USE_BOUNDARY_IMAGE = false;
	
    public static final double METERS_PER_PIXEL_HORIZONTAL = 
                                    (RIGHT_X - LEFT_X) / IMAGE_WIDTH;
    public static final double METERS_PER_PIXEL_VERTICAL = 
                                    (TOP_Y - BOTTOM_Y) / IMAGE_HEIGHT;
}
