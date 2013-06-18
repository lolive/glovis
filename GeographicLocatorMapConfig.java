// GeographicLocatorMapConfig.java is a simple class to allow defining the
// configuration for different geographic locator maps.
//---------------------------------------------------------------------------

public class GeographicLocatorMapConfig
{
    public int imageWidth = 1007;
    public int imageHeight = 503;
    public double leftLon = -180.0;
    public double rightLon = 180.0;
    public double topLat = 90.0;
    public double bottomLat = -90.0;
    public double degreesPerPixelLon;
    public double degreesPerPixelLat;
    public String mapImage;
    public String boundaryImage;

    // set this to true if you want to limit the arrow buttons to the 
    // geographic bumper.
    public boolean enforceGeographicBumper;

    // set this to true if the locator map crosses the international date line 
    public boolean crossesDateLine;

    // set this to false if there is no boundaries image to overlay on the
    // locator map
    public boolean useBoundaryImage;
	

    // constructor
    //------------
    public GeographicLocatorMapConfig(
        int imageWidth, int imageHeight, double leftLon, double rightLon,
        double topLat, double bottomLat, String mapImage, String boundaryImage,
        boolean enforceGeographicBumper, boolean crossesDateLine)
    {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.leftLon = leftLon;
        this.rightLon = rightLon;
        this.topLat = topLat;
        this.bottomLat = bottomLat;
        this.mapImage = mapImage;
        this.boundaryImage = boundaryImage;
        this.enforceGeographicBumper = enforceGeographicBumper;
        this.crossesDateLine = crossesDateLine;

        useBoundaryImage = (boundaryImage != null);
        degreesPerPixelLon = (rightLon - leftLon) / imageWidth;
        degreesPerPixelLat = (topLat - bottomLat) / imageHeight;
    }
}

