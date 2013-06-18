// LamAzProjection.java is a class to perform Lambert Azimuthal Equal Area
// projection transformations.
//------------------------------------------------------------------------
import java.awt.Point;

public class LamAzProjection implements ProjectionTransformation
{
    private double HALF_PI;     // PI / 2.0
    private double TWO_PI;      // PI * 2.0
    private double EPSLN;       // Test for convergence -- practical "zero"
    private double R2D;         // Radians to Degrees conversion factor
    private double D2R;         // Degrees to Radians conversion factor
    private double lon_center;  // Center longitude (projection center)
    private double lat_center;  // Center latitude (projection center)
    private double R;           // Radius of the earth (sphere)
    private double sin_lat_o;   // Sine of the center latitude
    private double cos_lat_o;   // Cosine of the center latitude

    // Lambert Azimuthal Equal Area projection constructor
    //----------------------------------------------------
    LamAzProjection(double radius, double center_long, double center_lat)
    {
        HALF_PI = Math.PI / 2.0;
        TWO_PI = Math.PI * 2.0;
        EPSLN = 1.0e-10;
        R2D     = 57.2957795131;
        D2R     = 0.01745329252;
        R = radius;
        lon_center = center_long * D2R;
        lat_center = center_lat * D2R;
        sin_lat_o = Math.sin(lat_center);
        cos_lat_o = Math.cos(lat_center);
    }

    // method to convert a projection coordinate in meters to lat/long in 
    // degrees
    //-------------------------------------------------------------------
    public LatLong projToLatLong(int xCoord, int yCoord)
    {
        double Rh;
        double z;        // Great circle dist from proj center to given point
        double sin_z;    // Sine of z 
        double cos_z;    // Cosine of z
        double temp;     // Re-used temporary variable
        double latitude;
        double longitude;

        double x = xCoord;
        double y = yCoord;

        // Inverse equations
        // -----------------
        Rh = Math.sqrt(x * x + y * y);
        temp = Rh / (2.0 * R);
        if (temp > 1) 
        {
            System.out.println("Error in LamAz projToLatLong");
            return null;
        }
        z = 2.0 * Math.asin(temp);
        sin_z = Math.sin(z);
        cos_z = Math.cos(z);
        longitude = lon_center;
        if (Math.abs(Rh) > EPSLN)
        {
            latitude = Math.asin(sin_lat_o*cos_z + cos_lat_o * sin_z * y / Rh);
            temp = Math.abs(lat_center) - HALF_PI;
            if (Math.abs(temp) > EPSLN)
            {
                temp = cos_z - sin_lat_o * Math.sin(latitude);
                if(temp!=0.0) 
                    longitude = adjust_lon(lon_center +
                                    Math.atan2(x*sin_z*cos_lat_o,temp*Rh));
            }
            else if (lat_center < 0.0) 
                longitude = adjust_lon(lon_center - Math.atan2(-x, y));
            else 
                longitude = adjust_lon(lon_center + Math.atan2(x, -y));
        }
        else 
            latitude = lat_center;

        // convert the result to degrees
        latitude *= R2D;
        longitude *= R2D;

        return new LatLong(latitude, longitude);
    }

    // method to convert a lat/long in degrees to a projection coordinate
    // in meters
    // NOTE: this code is a direct translation of the code in gctp, with
    //       the false easting/northing option being omitted.
    //-------------------------------------------------------------------
    public Point latLongToProj(LatLong latLong)
    {
        double latitude = latLong.latitude * D2R;
        double longitude = latLong.longitude * D2R;
        double sin_lat = Math.sin(latitude);
        double cos_lat = Math.cos(latitude);
        double delta_lon = adjust_lon(longitude - lon_center);
        double sin_delta_lon = Math.sin(delta_lon);
        double cos_delta_lon = Math.cos(delta_lon);
        double g = sin_lat_o * sin_lat + cos_lat_o * cos_lat * cos_delta_lon;
        if (g == -1.0)
        {
            System.out.println("Error in LamAz latLongToProj");
            return null;
        }
        double ksp = R * Math.sqrt(2.0 / (1.0 + g));
        double x = ksp * cos_lat * sin_delta_lon;
        double y = ksp * (cos_lat_o * sin_lat - sin_lat_o * cos_lat 
                   * cos_delta_lon);
        int int_x = (int)Math.floor(x + 0.5);
        int int_y = (int)Math.floor(y + 0.5);
        return new Point(int_x,int_y);
    }

    // Function to return the sign of an argument
    //-------------------------------------------
    private int sign(double x) 
    { 
        if (x < 0.0) 
            return(-1); 
        else 
            return(1);
    }

    // Function to adjust longitude to -180 to 180
    //--------------------------------------------
    private double adjust_lon(double x) 
    {
        x = (Math.abs(x) < Math.PI) ? x : (x - (sign(x) * TWO_PI));
        return(x);
    }

}
