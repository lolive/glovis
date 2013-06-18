// SinusoidalLocatorMap.java provides the implementation details specific
// to a locator map in the sinusoidal projection for the MODIS data.
//
// Note: the image to use and its extents are configured by the 
//       SinusoidalLocatorMapConfig class.
//-----------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Point;

public class SinusoidalLocatorMap extends LocatorMapImpl
{
    private imgViewer applet;
    private LatLongToModisTile modisTileConversion;
    private ProjectionTransformation projTran; // projection transformation

    SinusoidalLocatorMap(imgViewer applet)
    {
        this.applet = applet;

        // create the transformation object needed
        modisTileConversion = new LatLongToModisTile();
        projTran = CreateProjection.fromProjectionNumber(
                    CreateProjection.SINUSOIDAL);

        // load the info for the sinusoidal locator map
        mapImage = applet.getImage(applet.getCodeBase(), 
                SinusoidalLocatorMapConfig.MAP_IMAGE);
        if (SinusoidalLocatorMapConfig.USE_BOUNDARY_IMAGE)
        {
            worldBoundaries = applet.getImage(applet.getCodeBase(), 
                    SinusoidalLocatorMapConfig.BOUNDARY_IMAGE);
        }
        useBoundaryImage = SinusoidalLocatorMapConfig.USE_BOUNDARY_IMAGE;

        // set the image size
        imageSize = new Dimension(SinusoidalLocatorMapConfig.IMAGE_WIDTH, 
                                  SinusoidalLocatorMapConfig.IMAGE_HEIGHT);
    }
    
    // Method to calculate the location in the locator map for the lat/long
    // passed in.  Returns a Point with the pixel location to set.
    //---------------------------------------------------------------------
    public Point latLongToPixel(LatLong latLong)
    {
        ModisTile tile = modisTileConversion.latLongToTile(latLong);
        return gridToPixel(tile.h, tile.v);
    }

    // Method to calculate the location in the locator map for the gridCol
    // and gridRow passed in.  Returns a Point with the pixel location to set.
    //------------------------------------------------------------------------
    public Point gridToPixel(int gridCol, int gridRow)
    {
        ModisTile tile = new ModisTile(gridCol,gridRow);

        Point loc = modisTileConversion.tileToCoordinate(tile,true);

        int xLoc = (int)((loc.x - SinusoidalLocatorMapConfig.LEFT_X) / 
                SinusoidalLocatorMapConfig.METERS_PER_PIXEL_HORIZONTAL);
        int yLoc = (int)((SinusoidalLocatorMapConfig.TOP_Y - loc.y) / 
                SinusoidalLocatorMapConfig.METERS_PER_PIXEL_VERTICAL);

        return new Point(xLoc, yLoc);
    }

    // helper method to convert an x,y pixel location to a projection
    // coordinate
    //---------------------------------------------------------------
    private Point pixelToProj(int x, int y)
    {
        int projY = (int)(SinusoidalLocatorMapConfig.TOP_Y 
                - y * SinusoidalLocatorMapConfig.METERS_PER_PIXEL_VERTICAL);
        int projX = (int)(SinusoidalLocatorMapConfig.LEFT_X 
                + x * SinusoidalLocatorMapConfig.METERS_PER_PIXEL_HORIZONTAL);
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
        ModisTile tile = modisTileConversion.coordinateToTile(proj.x, proj.y);
        return new Point(tile.h, tile.v);
    }

    // Method to move the displayed location to a new area as a result of
    // a mouse click in the locator map.  Passed in x, y are the pixel
    // location clicked in the locator map.
    //-------------------------------------------------------------------
    public void moveTo(int x, int y)
    {
        Point proj = pixelToProj(x, y);

        ModisTile tile = modisTileConversion.coordinateToTile(proj.x,proj.y);
        if (modisTileConversion.isValidTile(tile))
            applet.imgArea.md.scrollData(tile.h,tile.v,0,0,false,true,false);
        else
        {
            // the grid cell isn't a legal one
            applet.statusBar.showStatus("Selected tile does not contain data!");
        }
    }
}
