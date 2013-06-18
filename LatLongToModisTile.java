// LatLongToModisTile.java provides methods to convert between lat/long and
// MODIS Tile numbers (V/H).
//-------------------------------------------------------------------------
import java.awt.*;
import java.applet.*;

public class LatLongToModisTile
{
    private ProjectionTransformation projection;

    // MODIS grid constantants
    private double ulGridX = -20015109.354; //tile (0,0) upper left X coordinate
    private double ulGridY = 10007554.677;  //tile (0,0) upper left Y coordinate
    private double gridCellSizeMeters = 926.625433 * 1200; 
                // size of a grid cell in meters (same in both dimensions)

    // Constructor for the ll2PathRow object
    //--------------------------------------
    LatLongToModisTile()
    {
        projection = 
            CreateProjection.fromProjectionNumber(CreateProjection.SINUSOIDAL);
    }

    // method to convert a Sinusoidal projection coordinate to a MODIS tile
    // number, keeping the fractional value
    //---------------------------------------------------------------------
    public DoublePoint coordinateToDoubleGrid(int xCoordinate, int yCoordinate)
    {
        double h = (xCoordinate - ulGridX)/gridCellSizeMeters;
        double v = (ulGridY - yCoordinate)/gridCellSizeMeters;

        if (h < 0.0)
            h = 0.0;
        else if (h >= 36)
            h = 35.9;
        if (v < 0.0)
            v = 0.0;
        else if (v >= 18)
            v = 17.9;

        return new DoublePoint(h,v);
    }

    // method to convert a Sinusoidal projection coordinate to a MODIS tile
    // number. Note that the calculated tile numbers are not verified to
    // be in the expected range since there are situations where invalid
    // tile numbers are used (i.e. edges of the MODIS grid).
    //---------------------------------------------------------------------
    public ModisTile coordinateToTile(int xCoordinate, int yCoordinate)
    {
        int h = (int)((xCoordinate - ulGridX)/gridCellSizeMeters);
        int v = (int)((ulGridY - yCoordinate)/gridCellSizeMeters);

        return new ModisTile(h,v);
    }

    // method to convert a lat/long to a MODIS tile number
    //----------------------------------------------------
    public ModisTile latLongToTile(LatLong latLong)
    {
        Point coordinate = projection.latLongToProj(latLong);
        return coordinateToTile(coordinate.x, coordinate.y);
    }

    // method to convert a MODIS tile number to a Sinusoidal coordinate.
    // The coordinate returned is in the center of the tile.
    //------------------------------------------------------------------
    public Point tileToCoordinate(ModisTile tile, boolean useTileCenter)
    {
        double centerAdjust = 0.0;
        if (useTileCenter)
            centerAdjust = 0.5;
        int x = (int)Math.round(ulGridX 
              + (((double)tile.h + centerAdjust) * gridCellSizeMeters));
        int y = (int)Math.round(ulGridY 
              - (((double)tile.v + centerAdjust) * gridCellSizeMeters));

        return new Point(x,y);
    }

    // method to convert a MODIS tile number to a lat/long.  The lat/long
    // returned is in the center of the tile.
    //-------------------------------------------------------------------
    public LatLong tileToLatLong(ModisTile tile)
    {
        Point coordinate = tileToCoordinate(tile, true);
        LatLong latLong = projection.projToLatLong(coordinate.x, coordinate.y);
        return latLong;
    }

    // method to verify that a given tile maps to a point on the earth
    // (i.e. isn't a tile that is off the edge of the world)
    //----------------------------------------------------------------
    public boolean isValidTile(ModisTile tile)
    {
        // copy the tile to make modifications to it
        ModisTile testTile = new ModisTile(tile.h, tile.v);

        // pick the corner of the tile that is nearest to the center of 
        // the projection
        if (testTile.v <= 8)
            testTile.v++;
        if (testTile.h <= 17)
            testTile.h++;

        Point coordinate = tileToCoordinate(testTile, false);
        LatLong latLong = projection.projToLatLong(coordinate.x, coordinate.y);
        // special test cases for tiles that fall right on the edge and the
        // rounding of coordinates to integers lets the tiles slip by
        if (((tile.h == 8) && (tile.v == 2))
            || ((tile.h == 8) && (tile.v == 15))
            || ((tile.h == 27) && (tile.v == 2))
            || ((tile.h == 27) && (tile.v == 15)))
        {
            latLong = null;
        }

        // if no lat/long was found, the tile does not contain data
        if (latLong == null)
            return false;
        else
            return true;
    }
/*
    public static void main(String args[])
    {
        LatLongToModisTile t = new LatLongToModisTile();
        
        ModisTile tile = t.coordinateToTile(20000000,-10000000);
        if (tile != null)
            System.out.println("" + tile.h + " " + tile.v);
        
        LatLong latLong = new LatLong(0.0, 0.0);
        tile = t.latLongToTile(latLong);
        if (tile != null)
            System.out.println("" + tile.h + " " + tile.v);

        Point p = t.tileToCoordinate(tile);
        System.out.println("" + p.x + " " + p.y);

        tile.h = 24;
        tile.v = 16;
        latLong = t.tileToLatLong(tile);
        if (latLong != null)
            System.out.println("" + latLong.latitude + " " + latLong.longitude);
        else
            System.out.println("does not map to lat/long");
    }
*/
}
