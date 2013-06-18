// ProjectionTransformation.java defines the interface for a class that can
// perform projection transformations
//-------------------------------------------------------------------------
import java.awt.Point;

interface ProjectionTransformation
{
    // method to convert an X/Y projection coordinate in meters to a 
    // latitude/longitude in degrees
    public LatLong projToLatLong(int xCoord, int yCoord);
    // method to convert a latitude/longitude in degrees to a X/Y projection 
    // coordinate in meters
    public Point latLongToProj(LatLong latLong);
}
