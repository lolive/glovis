//  ll2PathRow.java provides methods to convert between WRS-2 path/row and
//  lat/long.
//
//---------------------------------------------------------------

public class ll2PathRow
{
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double R2D = 57.2957795131; // Radians to Degrees 
    private static final double D2R = 0.01745329252; // Degrees to Radians
    private static final double SEC_PER_DAY = 86400.0; // Seconds in a day
    private static final double semi_major = 6378137.0; // Semi-Major axis
    private static final double semi_minor = 6356752.3; // Semi-Minor axis
    private static final double scenes_per_orbit = 248;
                                // Scenes per orbit of spacecraft
    private static final double descending_node_row = 60;
                                // equator row for descending paths
    private static final double ascending_node_row = 184;
                                // equator row for ascending paths
    private static final double earth_spin_rate = TWO_PI / SEC_PER_DAY;
                                            // Earth solar rotation rate

    private double inclination;       // Inclination angle in radians 
    private double long_path1_row60;  // Longitude of path 1, row 60
    private double long_path1_row184; // Longitude of path 1, row 184
    private double days_per_cycle;    // Days per WRS cycle
    private double orbits_per_cycle;  // Orbits per WRS cycle
    private double sc_ang_rate;       // Angular velocity of spacecraft

    public int path;            // WRS-2 path calculated by last toPathRow call
    public int row;             // WRS-2 row calculated by last toPathRow call
    public double longitude;    // longitude calculated by last toLatLong call
    public double latitude;     // latitude calculated by last toLatLong call
    public double doublePath;   // WRS-2 floating point path calculated by last
                                // toPathRow call
    public double doubleRow;    // WRS-2 floating point row calculated by last
                                // toPathRow call

    // define the supported reference systems
    public static final int WRS1_REFERENCE_SYSTEM = 1;
    public static final int WRS2_REFERENCE_SYSTEM = 2;

    // the following are set to the values for either descending or ascending
    // rows by the constructor.
    private double equator_row; // row where a path crosses the equator
    private double long_path1_at_equator; // longitude of path 1 at the equator
    private double direction_sign;

    // Constructor for the ll2PathRow object
    //--------------------------------------
    ll2PathRow(int reference_system, boolean ascending)
    {
        // set the constants for the selected reference system
        if (reference_system == WRS1_REFERENCE_SYSTEM)
        {
            inclination = 99.092 * D2R;
            long_path1_row60 = -65.48 * D2R;
            long_path1_row184 = 101.62 * D2R;
            days_per_cycle = 18;
            orbits_per_cycle = 251;
        }
        else if (reference_system == WRS2_REFERENCE_SYSTEM)
        {
            inclination = 98.2096 * D2R;
            long_path1_row60 = -64.6 * D2R;
            long_path1_row184 = 103.03948 * D2R;
            days_per_cycle = 16;
            orbits_per_cycle = 233;
        }
        else
        {
            throw(new IllegalArgumentException("Unknown reference system"));
        }

        sc_ang_rate = (TWO_PI * orbits_per_cycle) / 
                      (days_per_cycle * SEC_PER_DAY);
        if (ascending)
        {
            equator_row = ascending_node_row;
            long_path1_at_equator = long_path1_row184;
            direction_sign = -1.0;
        }
        else
        {
            equator_row = descending_node_row;
            long_path1_at_equator = long_path1_row60;
            direction_sign = 1.0;
        }
    }

    // method to convert lat/long to path/row.  The path/row values
    // calculated are stored in the public path and row variables
    //-------------------------------------------------------------
    public void toPathRow(double latitude, double longitude) 
    {
        double wrs_gclat;       // WRS scene center geocentric latitude
        double central_angle;   // Central travel angle for this row
        double tmp;
        double delta_long;      // Longitude offset due to central ang.
        double long_origin;     // Longitude of row 60 for given path

        // Different jvm runtime environments are producing different trig 
        // results
        // Quick fix for the poles
        //------------------------
        if (latitude >  81.08) 
            latitude =  81.08;
        if (latitude < -81.84) 
            latitude = -81.84;
        // end of Quick fix for the poles

        latitude *= D2R;
        longitude *= D2R;

        while( longitude > Math.PI )
            longitude = longitude - TWO_PI;
        while( longitude < -(Math.PI) )
            longitude = longitude + TWO_PI;

        // Convert the WRS geodetic latitude to geocentric latitude
        wrs_gclat = Math.atan(Math.tan(latitude) * (semi_minor / semi_major) 
                              * (semi_minor / semi_major));
        central_angle = direction_sign * Math.asin(-1.0 * (Math.sin(wrs_gclat) 
                                  / Math.sin(inclination)));

        // Calculate floating point row
        tmp = (scenes_per_orbit * central_angle) / TWO_PI + equator_row;

        // Round to nearest row
        doubleRow = tmp;
        row = (int)Math.floor(tmp + 0.5);

        delta_long = direction_sign * Math.atan2(
                Math.tan(wrs_gclat) / Math.tan(inclination), 
                Math.cos(central_angle) / Math.cos(wrs_gclat));
        long_origin = longitude + delta_long + central_angle 
                * (earth_spin_rate / sc_ang_rate);

        // Calculate floating point path
        tmp = (( -long_origin + long_path1_at_equator ) / TWO_PI) 
                * orbits_per_cycle + 1;

        // See if the row has wrapped out of range
        if (row < 1 )
        {
            tmp -= 16.0;
            row += scenes_per_orbit;
        }
        if (row > scenes_per_orbit)
        {
            tmp += 16.0;
            row -= scenes_per_orbit;
        }

        // Round to nearest path
        doublePath = tmp;
        path = (int)Math.floor(tmp + 0.5);

        // Make sure the path is in range
        if (path < 1 ) 
            path += orbits_per_cycle;
        if (path > orbits_per_cycle ) 
            path -= orbits_per_cycle;
    }

    // method to convert path/row to a lat/long.  The lat/long values
    // calculated are stored in the public latitude and longitude variables
    //---------------------------------------------------------------------
    public void toLatLong(int path, int row)
    {
        double wrs_gclat;       // WRS scene center geocentric latitude
        double central_angle;   // Central travel angle for this row
        double tmp;
        double delta_long;      // Longitude offset due to central ang.
        double long_origin;     // Longitude of row 60 for given path

        central_angle = direction_sign * (row - equator_row) * TWO_PI / 
                        scenes_per_orbit;
        wrs_gclat = Math.asin(-1.0 * Math.sin(central_angle) * 
                              Math.sin(inclination));
        latitude = Math.atan(Math.tan(wrs_gclat) * (semi_major/semi_minor) *
                             (semi_major/semi_minor));
        
        latitude *= R2D;

        long_origin = long_path1_at_equator - (((path - 1)/orbits_per_cycle)
                    * TWO_PI);
        delta_long = direction_sign * Math.atan2(
                    Math.tan(wrs_gclat) / Math.tan(inclination), 
                    Math.cos(central_angle) / Math.cos(wrs_gclat));
        
        longitude = long_origin - delta_long - (direction_sign * central_angle)
                  * (earth_spin_rate / sc_ang_rate);
        
        while( longitude > Math.PI )
            longitude = longitude - TWO_PI;
        while( longitude < -(Math.PI) )
            longitude = longitude + TWO_PI;

        longitude *= R2D;
    }
}
