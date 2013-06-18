// USLocatorMapConfig.java defines a locator map for the lower 48 states of
// the US.
//---------------------------------------------------------------------------

public class USLocatorMapConfig extends GeographicLocatorMapConfig
{
    private static final int IMAGE_WIDTH = 750;
    private static final int IMAGE_HEIGHT = 309;
    private static final double LEFT_LON = -128.04;
    private static final double RIGHT_LON = -60.74;
    private static final double TOP_LAT = 50.85;
    private static final double BOTTOM_LAT = 23.04;
    private static final String MAP_IMAGE = "graphics/USMap.jpg";
    private static final String BOUNDARY_IMAGE 
            = "graphics/USBoundariesBlack.gif";

    // set this to true if you want to limit the arrow buttons to the 
    // geographic bumper.
    private static final boolean ENFORCE_GEOGRAPHIC_BUMPER = true; 

    // set this to true if the locator map crosses the international date line 
    // TBD - could just calculate this from the left/right longitude values
    private static final boolean CROSSES_DATELINE = false;


    // constructor
    //------------
    public USLocatorMapConfig()
    {
        super(IMAGE_WIDTH, IMAGE_HEIGHT, LEFT_LON, RIGHT_LON, TOP_LAT,
              BOTTOM_LAT, MAP_IMAGE, BOUNDARY_IMAGE, ENFORCE_GEOGRAPHIC_BUMPER,
              CROSSES_DATELINE);
    }
}
