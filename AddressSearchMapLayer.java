// AddressSearchMapLayer.java implements a map layer class for the results
// of address searches.
//------------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.net.URL;

public class AddressSearchMapLayer extends MapLayer 
{
    private imgViewer applet;        // pointer to applet
    private Image pushPinIcon;       // icon to use for point of interest
    private LatLong latLong;         // LatLong of the point
    private String addressName;      // name of the address
    public boolean enabled;          // allow the map layer to be selected
    private Point addressInScreenCoords; // address location converted to
                                         // screen coordinates

    // Constructor for the AddressSearchMapLayer
    //------------------------------------------
    public AddressSearchMapLayer(imgViewer applet, int menuShortcut)
    {
        super(applet.imgArea, "Address Search Result", null, menuShortcut,
              true);

        this.applet = applet;

        // disable the map layer until an address search is performed
        enabled = false;

        // determine if the address search is enabled on the web site
        try
        {
            // build the URL for the enable file
            URL directoryURL = new URL(applet.getCodeBase(), "searchenabled");
            BufferedInputStream file 
                    = new BufferedInputStream(directoryURL.openStream());
            file.close();
        }
        catch (Exception e)
        {
            // if the file isn't available, hide the search map layer
            hide();
        }

        // get the icon to be placed on the point selected
        pushPinIcon = applet.getImage(applet.getCodeBase(), 
                                      "graphics/pushPinYellow.gif");
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode)
    {
        // nothing to do for this class
        // return 0 since no files to load
        return 0;
    }

    // method to configure the map layer for the displayed area in
    // projection coordinates.
    //   Returns: number of files that will need to be loaded
    //------------------------------------------------------------
    public int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode)
    {
        // nothing to do for this class
        // return 0 since no files to load
        return 0;
    }

    // method to read the needed data for the north arrow
    //---------------------------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        // nothing to read
    }

    // method to clip the address to the currently displayed area
    //-----------------------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        double actualPixelSize = applet.imgArea.md.actualPixelSize;

        // convert the location to screen coordinates
        addressInScreenCoords = getLatLongInScreenCoords(upperLeft,
                actualPixelSize, proj);
    }

    // method to find the screen coords from the lat/long coords
    //----------------------------------------------------------
    private Point getLatLongInScreenCoords(Point upperLeft, double pixelSize,
                     ProjectionTransformation proj)
    {
        if (upperLeft == null)
            return null;

        Point displayPoint = proj.latLongToProj(latLong);

        if (displayPoint != null)
        {
            int x;
            int y;
            int xcoord;
            int ycoord;
            x = displayPoint.x;
            y = displayPoint.y;
            xcoord = (int)Math.round(((x-upperLeft.x)/pixelSize)); 
            ycoord = (int)Math.round(((-y+upperLeft.y)/pixelSize));
            displayPoint = new Point(xcoord,ycoord);
        }
        return displayPoint;
    }

    // method to determine if the feature is at the indicated screen
    // coordinates and return the name associated with a feature that contains
    // an X/Y coordinate.  It also returns the polygon with the smallest
    // bounding box that contains the point.
    //------------------------------------------------------------------------
    public MapLayerFeatureInfo findFeatureName(int x, int y)
    {
        if (addressInScreenCoords == null)
            return null;

        // determine if the location is over the icon
        int x1 = addressInScreenCoords.x;
        int y1 = addressInScreenCoords.y;
        int height = pushPinIcon.getHeight(null);
        int width = pushPinIcon.getWidth(null);
        if ((x >= x1 - width) && (x < x1 + 5) && (y >= y1 - height)
            && (y < y1))
        {
            MapLayerFeatureInfo info = new MapLayerFeatureInfo();
            info.name = addressName;
            info.area = 10;
            return info;
        }
        else
            return null;
    }

    // method to draw the point of interest 
    //-------------------------------------
    public void draw(Graphics g)
    {
        Point displayPoint = addressInScreenCoords;

        if (displayPoint != null)
        {
            int height = pushPinIcon.getHeight(null);
            int width = pushPinIcon.getWidth(null);

             // draw the push pin icon for the point of interest
             g.drawImage(pushPinIcon,displayPoint.x-width,
                         displayPoint.y-height,applet.imgArea);
        }
    }

    // method to see if the point of interest needs to be set
    //-------------------------------------------------------
    public boolean isEnabled()
    {
        return enabled;
    }

    // draw point to diplay entered
    //-----------------------------
    public void setPoint(LatLong setLatLong, String address)
    {
        latLong = setLatLong;
        addressName = address;
        enabled = true;
        applet.mapLayerMenu.setLayerState(getName(), true, isEnabled());
        applet.md.gotoLatLong(latLong.latitude,latLong.longitude);

        // refresh screen
        applet.imgArea.repaint();
    }

    // method to clear the point
    //--------------------------
    public void clearPoint()
    {
        latLong = null;
        addressInScreenCoords = null;
        enabled = false;
        applet.mapLayerMenu.setLayerState(getName(), false, isEnabled());

        // refresh screen
        applet.imgArea.repaint();
    }
}
