// CreateProjection.java uses the Factory pattern to convert a projection
// number into a ProjectionTransformation object.
//-----------------------------------------------------------------------
import java.awt.Point;

public class CreateProjection
{
    // keep a private WRS2Model reference
    private static WRS2Model wrs2 = new WRS2Model();

    // definitions for the projection codes used
    private static final int SOUTH_POLE    = 1001;
    public static final int NORTH_AMERICA  = 1002;
    private static final int SOUTH_AMERICA = 1003;
    private static final int EUROPE        = 1004;
    private static final int AFRICA        = 1005;
    private static final int AUSTRALIA     = 1006;
    private static final int ASIA          = 1007;
    private static final int SOUTH_PACIFIC = 1008;
    public static final int SINUSOIDAL     = 1010;
    public static final int FAKE_GEOGRAPHIC = 1011;
    public static final int POLAR_STEREOGRAPHIC = 1012;
    public static final int OUTTA_RANGE    = 1100;

    // method to return a projection transformation object given a projection
    // code.
    //-----------------------------------------------------------------------
    public static ProjectionTransformation fromProjectionNumber(int projCode)
    {
        ProjectionTransformation pt;

        switch (projCode)
        {
            case CreateProjection.SOUTH_POLE:
                // South Pole Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, 0.0, -90.0);
                break;
            case CreateProjection.NORTH_AMERICA:
                // North America Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, -100.0, 50.0);
                break;
            case CreateProjection.SOUTH_AMERICA:
                // South America Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, -60.0, -15.0);
                break;
            case CreateProjection.EUROPE:
                // Europe Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, 20.0, 55.0);
                break;
            case CreateProjection.AFRICA:
                // Africa Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, 20.0, 5.0);
                break;
            case CreateProjection.AUSTRALIA:
                // Australia Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, 135.0, -15.0);
                break;
            case CreateProjection.SINUSOIDAL:
                // Sinusoidal projection for MODIS data
                pt = new SinusoidalProjection(6371007.181, 0.0);
                break;
            case CreateProjection.SOUTH_PACIFIC:
                // South Pacific Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, -140.0, -15.0);
                break;
            case CreateProjection.FAKE_GEOGRAPHIC:
                // fake geographic projection
                pt = new GeographicProjection();
                break;
            case CreateProjection.POLAR_STEREOGRAPHIC:
                // polar stereographic projection for the Antarctica dataset
                pt = new PolarStereographicProjection(6378137.0, 6356752.3142,
                                                      0.0, -71.0, 0, 0);
                break;
            case CreateProjection.ASIA:
                // let the 1007 case be the default to allow the app to start
                // if an area with no scenes is selected
            default:
                // Asia Lambert Azimuthal Equal Area
                pt = new LamAzProjection(6370997.0, 100.0, 45.0);
                break;
        }

        return pt;
    }

    // method to return the default projection code for a given lat/long
    // NOTE: This code must agree with the implementation of findRegion.c
    //       that is used to ingest the data in support/findRegion in the
    //       GloVis source code.
    // NOTE: this routine only supports descending satellite passes
    //-------------------------------------------------------------------
    public static int getDefaultProjectionCode(Sensor sensor, LatLong latLong)
    {
        // if the sensor has a default projection code, return it.  Otherwise,
        // select the default based on the lat/long
        if (sensor.defaultProjectionCode > 0)
            return sensor.defaultProjectionCode;

        // convert the lat/long into a path and row
        Point pr = wrs2.latLongToGrid(latLong.latitude,latLong.longitude);
        int path = pr.x;
        int row = pr.y;

        // convert the path/row to a projection code.  
        int code = CreateProjection.OUTTA_RANGE;
        if (row < 1) return code;
        if (row > 124) return code;

        if (path < 1) return code;
        if (path > 233) return code;

        // South Pole
        if ((row > 101) && (row < 144)) return CreateProjection.SOUTH_POLE;

        // North America, Descending/Day
        code = CreateProjection.NORTH_AMERICA;
        if ((row < 30) && (path <  85)) return code;
        if ((row < 20) && (path > 223)) return code;
        if ((row < 10) && (path <  88)) return code;
        if ((row < 30) && (path > 223)) return code;
        if ((row < 45) && (path <  81)) return code;
        if ((row < 35) && (path > 223)) return code;
        if ((row < 50) && (path > 223)) return code;
        if ((row < 51) && (path <  76)) return code;
        if ((row < 58) && ((path > 12) && (path < 76))) return code;

        // Europe, Descending/Day
        if ((path < 224) && (path > 174))
        {
            code = CreateProjection.EUROPE;
            if (row < 34) return code;
            if ((row < 35) && (path > 196)) return code;
            if ((row < 36) && (path > 198)) return code;
            if ((row < 36) && (path < 191)) return code;
            if ((row < 37) && (path < 186)) return code;
        }

        // South America, Descending/Day
        code = CreateProjection.SOUTH_AMERICA;
        if ((row <  58) && (path <  12)) return code;
        if ((row <  58) && (path > 210)) return code;
        if ((row <  61) && (path > 210)) return code;
        if ((row >  60) && (path > 200)) return code;
        if ((row < 102) && (path <  30)) return code;

        // Africa, Descending/Day
        code = CreateProjection.AFRICA;
        if ((row > 57) && (path < 201) && (path > 149)) return code;
        if ((row > 36) && (path < 211) && (path > 154)) return code;
        if ((row > 35) && (path < 211) && (path > 184)) return code;
        if ((row > 34) && (path < 200) && (path > 189)) return code;
        if ((row > 33) && (path < 198) && (path > 189)) return code;

        // Australia, Descending/Day
        code = CreateProjection.AUSTRALIA;
        if ((row > 57) && (row < 123) && (path > 65)) return code;
        if ((row > 55) && (path >  74) && (path < 136)) return code;
        if ((row > 53) && (path > 114) && (path < 136)) return code;

        // South Pacific, Descending/Day
        code = CreateProjection.SOUTH_PACIFIC;
        if ((row > 57) && (path < 66) && (path > 29)) return code;

        // else Asia
        return CreateProjection.ASIA;
    }
}
