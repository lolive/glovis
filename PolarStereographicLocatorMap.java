// PolarStereographicLocatorMap.java provides the implementation details
// specific to a locator map in the polar stereographic projection for
// Antarctica data.
//
// Note: the image to use and its extents are configured by the 
//       PolarStereographicLocatorMapConfig class.
//-----------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Point;

public class PolarStereographicLocatorMap extends LocatorMapImpl
{
    private imgViewer applet;
    private ProjectionTransformation projTran; // projection transformation

    // grid cells are 120,000m square
    private final int GRID_CELL_SIZE = 120000; 

    PolarStereographicLocatorMap(imgViewer applet)
    {
        this.applet = applet;

        // create the transformation object needed
        projTran = CreateProjection.fromProjectionNumber(
                    CreateProjection.POLAR_STEREOGRAPHIC);

        // load the info for the polar stereographic locator map
        mapImage = applet.getImage(applet.getCodeBase(), 
                PolarStereographicLocatorMapConfig.MAP_IMAGE);
        if (PolarStereographicLocatorMapConfig.USE_BOUNDARY_IMAGE)
        {
            worldBoundaries = applet.getImage(applet.getCodeBase(), 
                    PolarStereographicLocatorMapConfig.BOUNDARY_IMAGE);
        }
        useBoundaryImage 
            = PolarStereographicLocatorMapConfig.USE_BOUNDARY_IMAGE;

        // set the image size
        imageSize = new Dimension(
                            PolarStereographicLocatorMapConfig.IMAGE_WIDTH, 
                            PolarStereographicLocatorMapConfig.IMAGE_HEIGHT);
    }
    
    // Method to calculate the location in the locator map for the lat/long
    // passed in.  Returns a Point with the pixel location to set.
    //---------------------------------------------------------------------
    public Point latLongToPixel(LatLong latLong)
    {
        Point xy = projTran.latLongToProj(latLong);
        return projToPixel(xy);
    }

    // Method to calculate the location in the locator map for the gridCol
    // and gridRow passed in.  Returns a Point with the pixel location to set.
    //------------------------------------------------------------------------
    public Point gridToPixel(int gridCol, int gridRow)
    {
        Point xy = new Point(gridCol * GRID_CELL_SIZE,
                             gridRow * GRID_CELL_SIZE);
        return projToPixel(xy);
    }

    // helper method to convert an projection coordinate to an x,y pixel
    // location
    //---------------------------------------------------------------
    private Point projToPixel(Point xy)
    {
        int xLoc = (int)((xy.x - PolarStereographicLocatorMapConfig.LEFT_X) / 
                PolarStereographicLocatorMapConfig.METERS_PER_PIXEL_HORIZONTAL);
        int yLoc = (int)((PolarStereographicLocatorMapConfig.TOP_Y - xy.y) / 
                PolarStereographicLocatorMapConfig.METERS_PER_PIXEL_VERTICAL);
        return new Point(xLoc, yLoc);
    }

    // helper method to convert an x,y pixel location to a projection
    // coordinate
    //---------------------------------------------------------------
    private Point pixelToProj(int x, int y)
    {
        int projY = (int)(PolarStereographicLocatorMapConfig.TOP_Y 
            - y * PolarStereographicLocatorMapConfig.METERS_PER_PIXEL_VERTICAL);
        int projX = (int)(PolarStereographicLocatorMapConfig.LEFT_X 
          + x * PolarStereographicLocatorMapConfig.METERS_PER_PIXEL_HORIZONTAL);
        return new Point(projX, projY);
    }

    // method to convert an x,y pixel location to a lat/long
    //------------------------------------------------------
    public LatLong pixelToLatLong(int x, int y)
    {
        Point proj = pixelToProj(x, y);
        return projTran.projToLatLong(proj.x, proj.y);
    }

    // method to convert an x,y pixel location to a gridCol/gridCol
    //-------------------------------------------------------------
    public Point pixelToGrid(int x, int y)
    {
        Point proj = pixelToProj(x, y);
        int col = Math.round(proj.x / GRID_CELL_SIZE);
        int row = Math.round(proj.y / GRID_CELL_SIZE);
        return new Point(col, row);
    }

    // Method to move the displayed location to a new area as a result of
    // a mouse click in the locator map.  Passed in x, y are the pixel
    // location clicked in the locator map.
    //-------------------------------------------------------------------
    public void moveTo(int x, int y)
    {
        Point grid = pixelToGrid(x, y);

        applet.imgArea.md.scrollData(grid.x, grid.y, 0, 0, false, true, false);
    }
}
