// UtmZoneMapLayer.java implements the class for displaying a grid of points
// for the UTM 6 degree x 5 degree areas.
//-----------------------------------------------------------------------
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;


public class UtmZoneMapLayer extends MapLayer
{
    private imgViewer applet;
    private LatLong ULcorner;    // upper left corner to display
    private LatLong LRcorner;    // lower right corner to display
    private Point[] gridOnScreen;// screen coords of grid centers

    // constructor for the MapLayer class
    //-----------------------------------
    public UtmZoneMapLayer(imgViewer applet, Color layerColor,
                           int menuShortcut)
    {
        super(applet.imgArea, "UTM Zone Centers", layerColor, menuShortcut,
              true);

        this.applet = applet;

        ULcorner = new LatLong(0.0, 0.0);
        LRcorner = new LatLong(0.0, 0.0);
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode)
    {
        // using the bounding box, find the range of latitudes and longitudes
        // that needs to be covered
        double maxLat = (int)(minboxULdeg.latitude + 10);
        maxLat = (int)(maxLat/5) * 5;
        if (maxLat > 90.0)
            maxLat = 87.5;
        double minLat = (int)(minboxLRdeg.latitude - 10);
        minLat = (int)(minLat/5) * 5;
        if (minLat < -90.0)
            minLat = -90.0;
        int minLon = (int)(minboxULdeg.longitude - 12);
        minLon = ((minLon + 180)/6) * 6 - 180;
        int maxLon = (int)(minboxLRdeg.longitude + 12);
        maxLon = ((maxLon + 180)/6) * 6 - 180;

        ULcorner.latitude = maxLat;
        ULcorner.longitude = minLon;
        LRcorner.latitude = minLat;
        LRcorner.longitude = maxLon;

        // return 0 since no files to load
        return 0;
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode)
    {
        // nothing to do for this class, but required to implement

        // return 0 since no files to load
        return 0;
    }

    // method to read the map layer file
    //----------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        // nothing to read
    }

    // method to clip the map layer components to the display
    //-------------------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        // get the navigation model
        NavigationModel nm = applet.sensorMenu.getCurrentSensor().navModel;
        double lat_spacing = 180.0 / (nm.getMaximumRow() + 1);
        double lon_spacing = 360.0 / (nm.getMaximumColumn() + 1);

        int numPoints = ((int)((ULcorner.latitude - LRcorner.latitude)
                               /lat_spacing) + 1)
                    * ((int)((LRcorner.longitude - ULcorner.longitude)
                        /lon_spacing) + 1);
        gridOnScreen = new Point[numPoints];

        int index = 0;
        LatLong latLong = new LatLong(0.0,0.0);
        for (double lat = LRcorner.latitude + lat_spacing/2.0;
             lat <= ULcorner.latitude;
             lat += lat_spacing)
        {
            latLong.latitude = lat;
            for (double lon = ULcorner.longitude + lon_spacing/2.0;
                 lon <= LRcorner.longitude; lon += lon_spacing)
            {
                latLong.longitude = lon;
                Point xy = proj.latLongToProj(latLong);
                int x = (xy.x - upperLeft.x)/pixelSize;
                int y = (upperLeft.y - xy.y)/pixelSize;
                gridOnScreen[index++] = new Point(x,y);
            }
        }
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        if (gridOnScreen == null)
            return;

        // draw grid centers if they are defined
        for (int i = 0; i < gridOnScreen.length; i++)
        {
            if (gridOnScreen[i] != null)
            {
                g.setColor(Color.BLACK);
                g.fillRect(gridOnScreen[i].x - 6, gridOnScreen[i].y - 6, 12,12);
                g.setColor(color);
                g.fillRect(gridOnScreen[i].x - 5, gridOnScreen[i].y - 5, 10,10);
            }
        }
    }
}
