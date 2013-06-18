// LocatorMapImpl.java is an abstract class that defines the customizable
// behavior of the locator map.
//-----------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;

public abstract class LocatorMapImpl
{
    public Image mapImage;          // image for the locator map
    public Image worldBoundaries;   // image for the world boundaries
    public boolean useBoundaryImage;// flag to use the world boundaries
    public Dimension imageSize;     // size of the image
    
    // Method to calculate the location in the locator map for the lat/long
    // passed in.  Returns a Point with the pixel location to set.
    //---------------------------------------------------------------------
    public abstract Point latLongToPixel(LatLong latLong);

    // Method to calculate the location in the locator map for the gridCol
    // and gridRow passed in.  Returns a Point with the pixel location to set.
    //------------------------------------------------------------------------
    public abstract Point gridToPixel(int gridCol, int gridRow);

    // method to convert an x,y pixel location to a lat/long
    //------------------------------------------------------
    public abstract LatLong pixelToLatLong(int x, int y);

    // method to convert an x,y pixel location to a gridCol/gridCol
    //-------------------------------------------------------------
    public abstract Point pixelToGrid(int x, int y);

    // Method to move the displayed location to a new area as a result of
    // a mouse click in the locator map.  Passed in x, y are the pixel
    // location clicked in the locator map.
    //-------------------------------------------------------------------
    public abstract void moveTo(int x, int y);

    // method to return the geographic locator map configuration object.
    // The default method provided returns null to indicate there isn't one.
    //----------------------------------------------------------------------
    public GeographicLocatorMapConfig getMapConfig()
    {
        return null;
    }

    // method to cleanup any resources when the applet is stopped
    //-----------------------------------------------------------
    public void cleanup()
    {
        if (mapImage != null)
            mapImage.flush();
        mapImage = null;
        if (worldBoundaries != null)
            worldBoundaries.flush();
        worldBoundaries = null;
    }
}
