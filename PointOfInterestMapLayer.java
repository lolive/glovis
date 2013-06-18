// PointOfInterestMapLayer.java implements a class stores and manipulates a 
// point of interest set by the user created by right-clicking a point on the 
// screen or lat/long entered from the dialog box.
//------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;

public class PointOfInterestMapLayer extends MapLayer 
{
    private imgViewer applet;        // pointer to applet
    private Image pushPinIcon;       // icon to use for point of interest
    private LatLong latLong;         // LatLong of the point
    public boolean showMapLayer;     // flag if point is set
    private Point savedUpperLeft;    // Saved Upperleft coords.
    private double savedPixelSize;   // Saved Display pixelSize
    private Dimension savedDispSize; // Saved Display dimensioms
    private ProjectionTransformation savedProj; // Saved Display projection
    
    // Constructor for the point of interest
    //--------------------------------------
    public PointOfInterestMapLayer(imgViewer applet, int menuShortcut)
    {
        super(applet.imgArea, "Point Of Interest", null, menuShortcut, true);
        
        this.applet = applet;

        // flag if maplayer should be shown
        showMapLayer = false;
       
        // get the icon to be placed on the point selected
        pushPinIcon = applet.getImage(applet.getCodeBase(), 
                                      "graphics/pushPin.gif");

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

    // method to clip the point of interest layer to 
    // the currently displayed area
    //----------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        // Save the values for to use in drawing the point of interest
        savedUpperLeft = upperLeft;
        savedPixelSize = applet.imgArea.md.actualPixelSize;
        savedDispSize = dispSize;
        savedProj = proj;
    }
   
    // method to find the screen coords from the lat/long coords
    //----------------------------------------------------------
    private Point getLatLongInScreenCoords()
    {
        if (savedUpperLeft == null)
            return null;
            
        Point displayPoint = savedProj.latLongToProj(latLong);
        
        if (displayPoint != null)
        {
            int x;
            int y;
            int xcoord;
            int ycoord;
            x = displayPoint.x;
            y = displayPoint.y;
            xcoord = (int)Math.round(((x-savedUpperLeft.x)/savedPixelSize)); 
            ycoord = (int)Math.round(((-y+savedUpperLeft.y)/savedPixelSize));
            displayPoint = new Point(xcoord,ycoord);
        }
        return displayPoint;
    }

    // method to clear the point
    //--------------------------
    public void clearPoint()
    {
        latLong = null;
        showMapLayer = false;
        applet.mapLayerMenu.setLayerState("Point Of Interest",false,
                                           isEnabled());

        // refresh screen
        applet.imgArea.repaint();
    }
    
    // method to draw the point of interest 
    //-------------------------------------
    public void draw(Graphics g)
    {
        Point displayPoint = getLatLongInScreenCoords();

        if (displayPoint != null)
        {
            int height = pushPinIcon.getHeight(null);
            int width = pushPinIcon.getWidth(null);
            
             // draw the push pin icon for the point of interest
             g.drawImage(pushPinIcon,displayPoint.x-height,
                         displayPoint.y-width,applet.imgArea);
        }
    }
    
    // method to see if the point of interest needs to be set
    //-------------------------------------------------------
    public boolean isEnabled()
    {
        return showMapLayer;
    }

    // method to get the current lat/long values of the set point of interest
    //-----------------------------------------------------------------------
    public LatLong getPointOfInterestLatLong()
    {
        return latLong;
    }
 
    // draw point to diplay entered
    //-----------------------------
    public void setPoint(LatLong setLatLong)
    {
        latLong = setLatLong;
        showMapLayer = true;
        applet.pointOfInterestDialog.setLatLong(latLong);
        applet.mapLayerMenu.setLayerState("Point Of Interest",true,
                                           isEnabled());
        applet.imgArea.repaint();
    }

}
