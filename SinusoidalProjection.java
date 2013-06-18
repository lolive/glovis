// SinusoidalProjection.java is a class to perform Sinusoidal projection
// transformations.
//----------------------------------------------------------------------
import java.awt.Point;

public class SinusoidalProjection implements ProjectionTransformation
{
    private double HALF_PI;     // PI / 2.0
    private double TWO_PI;      // PI * 2.0
    private double EPSLN;       // Test for convergence -- practical "zero"
    private double R2D;         // Radians to Degrees conversion factor
    private double D2R;         // Degrees to Radians conversion factor
    private double lon_center;  // Center longitude (projection center)
    private double radius;      // Radius of the earth (sphere)

    // Sinusoidal projection constructor
    //----------------------------------
    SinusoidalProjection(double radius, double center_long)
    {
        HALF_PI = Math.PI / 2.0;
        TWO_PI = Math.PI * 2.0;
        EPSLN = 1.0e-10;
        R2D     = 57.2957795131;
        D2R     = 0.01745329252;
        this.radius = radius;
        lon_center = center_long * D2R;
    }

    // method to convert a projection coordinate in meters to lat/long in 
    // degrees
    //-------------------------------------------------------------------
    public LatLong projToLatLong(int xCoord, int yCoord)
    {
        double temp;     // Re-used temporary variable
        double latitude;
        double longitude;

        double x = xCoord;
        double y = yCoord;

        // Inverse equations
        latitude = y/radius;
        temp = Math.abs(latitude);
        if (temp > HALF_PI)
        {
            // y coordinate is outside range that can map to a lat/long
            return null;
        }
        temp -= HALF_PI;
        if (Math.abs(temp) > EPSLN)
        {
            longitude = lon_center + x / (radius * Math.cos(latitude));

            // Note: no check to see if the longitude goes off the edge of
            // the world since that is normal for this projection at the 
            // edges.
        }
        else
            longitude = lon_center;

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
        double delta_lon = longitude - lon_center;
        double cos_lat = Math.cos(latitude);
        double x = radius * delta_lon * cos_lat;
        double y = radius * latitude;
        int int_x = (int)Math.floor(x + 0.5);
        int int_y = (int)Math.floor(y + 0.5);
        return new Point(int_x,int_y);
    }
}
