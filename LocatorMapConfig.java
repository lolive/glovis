// LocatorMapConfig.java defines the configuration of the default locator
// map to use.
//---------------------------------------------------------------------------

public class LocatorMapConfig extends GeographicLocatorMapConfig
{
    private static final int IMAGE_WIDTH = 1007;
    private static final int IMAGE_HEIGHT = 503;
    private static final double LEFT_LON = -180.0;
    private static final double RIGHT_LON = 180.0;
    private static final double TOP_LAT = 90.0;
    private static final double BOTTOM_LAT = -90.0;
    private static final String MAP_IMAGE = "graphics/World5Minute.jpg";
    private static final String BOUNDARY_IMAGE 
            = "graphics/WorldBoundariesBlack.gif";

    // set this to true if you want to limit the arrow buttons to the 
    // geographic bumper.
    private static final boolean ENFORCE_GEOGRAPHIC_BUMPER = false; 

    // set this to true if the locator map crosses the international date line 
    // TBD - could just calculate this from the left/right longitude values
    private static final boolean CROSSES_DATELINE = false;

    public LocatorMapConfig()
    {
        super(IMAGE_WIDTH, IMAGE_HEIGHT, LEFT_LON, RIGHT_LON, TOP_LAT,
              BOTTOM_LAT, MAP_IMAGE, BOUNDARY_IMAGE, ENFORCE_GEOGRAPHIC_BUMPER,
              CROSSES_DATELINE);
    }
}

