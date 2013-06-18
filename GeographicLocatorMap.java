// GeographicLocatorMap.java provides the implementation details specific
// to a locator map in the geographic projection.
//-----------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Point;

public class GeographicLocatorMap extends LocatorMapImpl
{
    private imgViewer applet;   // reference to the applet
    private GeographicLocatorMapConfig config; // configuration for this map

    // Constructor for the geographic locator map implementation
    //----------------------------------------------------------
    GeographicLocatorMap(imgViewer applet, GeographicLocatorMapConfig config)
    {
        this.applet = applet;
        this.config = config;

        // load the info for the geographic locator map
        mapImage = applet.getImage(applet.getCodeBase(), config.mapImage);
        if (config.useBoundaryImage)
        {
            worldBoundaries = applet.getImage(applet.getCodeBase(), 
                config.boundaryImage);
        }
        useBoundaryImage = config.useBoundaryImage;

        // set the image size
        imageSize = new Dimension(config.imageWidth, config.imageHeight);
    }

    // method to return the geographic locator map configuration object
    //-----------------------------------------------------------------
    public GeographicLocatorMapConfig getMapConfig()
    {
        return config;
    }

    // Method to calculate the location in the locator map for the lat/long
    // passed in.  Returns a Point with the pixel location to set.
    //---------------------------------------------------------------------
    public Point latLongToPixel(LatLong latLong)
    {
        // handle cases where the locator map image crosses the
        // international date line
        if (config.crossesDateLine)
        {
            if (latLong.longitude > 0.0)
                latLong.longitude -= 360.0;
        }

        // convert the lat/long into a pixel coordinate
        int xLoc = (int)((latLong.longitude - config.leftLon) /
                config.degreesPerPixelLon);
        int yLoc = (int)((config.topLat - latLong.latitude) /
                config.degreesPerPixelLat);

        return new Point(xLoc, yLoc);
    }

    // Method to calculate the location in the locator map for the gridCol
    // and gridRow passed in.  Returns a Point with the pixel location to set.
    //------------------------------------------------------------------------
    public Point gridToPixel(int gridCol, int gridRow)
    {
        NavigationModel nm = applet.sensorMenu.getCurrentSensor().navModel;
        LatLong latLong = nm.gridToLatLong(gridCol, gridRow);
        return latLongToPixel(latLong);
    }

    // method to convert an x,y pixel location to a lat/long
    //------------------------------------------------------
    public LatLong pixelToLatLong(int x, int y)
    {
        double latitude = config.topLat - (y * config.degreesPerPixelLat);
        double longitude = config.leftLon + (x * config.degreesPerPixelLon);

        return new LatLong(latitude, longitude);
    }

    // method to convert an x,y pixel location to a gridCol/gridCol
    //-------------------------------------------------------------
    public Point pixelToGrid(int x, int y)
    {
        LatLong latLong = pixelToLatLong(x, y);
        NavigationModel nm = applet.sensorMenu.getCurrentSensor().navModel;
        return nm.latLongToGrid(latLong.latitude, latLong.longitude);
    }

    // Method to move the displayed location to a new area as a result of
    // a mouse click in the locator map.  Passed in x, y are the pixel
    // location clicked in the locator map.
    //-------------------------------------------------------------------
    public void moveTo(int x, int y)
    {
        // convert the pixel to a lat/long, then go to that lat/long
        LatLong latLong = pixelToLatLong(x, y);
        applet.imgArea.md.gotoLatLong(latLong.latitude, latLong.longitude);
    }
}
