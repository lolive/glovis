// GeographicProjection.java is a "dummy" projection transformation class.
// For datasets in the geographic projection, the geographic coordinates
// are simply multiplied by 100000 since the applet expects integer values
// for coordinates.
//------------------------------------------------------------------------
import java.awt.Point;

public class GeographicProjection implements ProjectionTransformation
{
    private final double SCALE_CONST = 100000.0;
                    // constant for scaling coordinates

    // Geographic projection constructor
    //----------------------------------
    GeographicProjection() { }

    // method to convert a projection coordinate in meters to lat/long in 
    // degrees
    //-------------------------------------------------------------------
    public LatLong projToLatLong(int xCoord, int yCoord)
    {
        double latitude =  (double)yCoord / SCALE_CONST;
        double longitude =  (double)xCoord / SCALE_CONST;

        return new LatLong(latitude, longitude);
    }

    // method to convert a lat/long in degrees to a projection coordinate
    // in meters
    //-------------------------------------------------------------------
    public Point latLongToProj(LatLong latLong)
    {
        int xCoord = (int)(latLong.longitude * SCALE_CONST);
        int yCoord = (int)(latLong.latitude * SCALE_CONST);

        return new Point(xCoord, yCoord);
    }
}
