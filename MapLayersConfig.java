// MapLayersConfig.java is a simple class to allow configuring the map layers
// to display upon start up, as well as enable or disable optional map layer
// features.  Different AmericaView Consortium members may want different
// default layers upon start-up, which can be set by simply editing this file.
// The EROS default is for no map layers to be visible upon start-up.
//
//
// Note: This config file is written using static finals.  So, it is a 
//       compile time configuration.
//---------------------------------------------------------------------------

public class MapLayersConfig
{
    // These values control whether the different map layers are turned on
    // when the user first starts the applet
    public static final boolean DISPLAY_ADMIN_BOUNDARIES = false;
    public static final boolean DISPLAY_COLLECTION_GRID = false;
    public static final boolean DISPLAY_COUNTRY_BOUNDARIES = false;
    public static final boolean DISPLAY_NORTH_ARROW = false;
    public static final boolean DISPLAY_POINTS_OF_INTEREST = false;
    public static final boolean DISPLAY_PROTECTED_AREA_POINTS = false;
    public static final boolean DISPLAY_PROTECTED_AREA_POLYGONS = false;
    public static final boolean DISPLAY_RAILROADS = false;
    public static final boolean DISPLAY_ROADS = false;
    public static final boolean DISPLAY_SCENE_LIST_OVERLAY = true;
    public static final boolean DISPLAY_WATER = false;
    public static final boolean DISPLAY_WORLD_CITIES = false;
}
