// TiledMapLayer.java implements a helper class for map layer classes that
// have their data files divided into tiles.
//------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;


public abstract class TiledMapLayer extends MapLayer
{
    private String baseFilename;    // base part of file name for this type
    protected MapLayerFileCache fileCache; // cache of files for the map layer

    // for brevity in sinusoidal linework tile names,
    // we represent meters in millions
    private final int projCoordGridDivisor = 1000000;
    // to create reasonably-sized tiles, they're in increments of 5 million:
    private final int projCoordGridIncrement = 5;


    // constructor for the TiledMapLayer class
    //----------------------------------------
    TiledMapLayer(Component parent, String layerName, String baseFilename,
                  Color layerColor, int menuShortcut, boolean lowResAvailable)
    {
        super(parent, layerName, layerColor, menuShortcut, lowResAvailable);

        this.baseFilename = baseFilename;
        fileCache = new MapLayerFileCache();
    }

    // method to find the 15 degree tile a latitude or longitude value belongs
    // in.
    //   Returns: 15 degree tile this belongs to
    //------------------------------------------------------------------------
    protected int to15deg(double value, boolean minFlag)
    {
        // 15 degree tiles are named by the upper left corner lat/long of the 
        // tile
        int intValue = (int)value;

        int tileIndex;
        int rem;

        tileIndex = intValue/15;
        rem = intValue % 15;
        
        if ((rem < 0) || ((rem == 0) && (minFlag)))
            tileIndex--;

        // convert the tile index back into a multiple of 15
        tileIndex *= 15;

        return tileIndex;
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode)
    {
        int numFiles = 0;   // number of files found to load

        // age the cache contents
        fileCache.age();

        // if the bounds are not defined, nothing to do
        if ((minboxULdeg == null) || (minboxLRdeg == null))
            return 0;

        // calculate the 15 degree intervals needed to cover the bounding box
        int leftX = to15deg(minboxULdeg.longitude,true);
        int rightX = to15deg(minboxLRdeg.longitude,false);
        int topY = to15deg(minboxULdeg.latitude,false);
        int bottomY = to15deg(minboxLRdeg.latitude,true);
        
        // shift the latitude values from the lower-left corner to the
        // upper-left corner to let us build the filenames we want by using the
        // upper-left corner of the tile
        bottomY = bottomY + 15;
        topY = topY + 15;

        // detect wraparound on longitude.  To handle it, just set the left
        // and right X values to a fixed value (only happens over New Zealand).
        // This is a little kludgy, but it works for now.
        if (((rightX - leftX) > 180) || ((leftX - rightX) > 180))
        {
            leftX = 165;
            rightX = 180;
        }

        // make sure the left value is really to the left
        if (leftX > rightX)
        {
            int temp = rightX;
            rightX = leftX;
            leftX = temp;
        }

        for (int x = leftX; x <= rightX; x += 15)
        {
            for (int y = topY; y >= bottomY; y -= 15)
            {
                // build the path name of the linework file
                StringBuffer name = new StringBuffer("linework/");

                // add the South/North part of the file name
                if (y < 0)
                    name.append("S");
                else
                    name.append("N");

                // append the latitude value
                name.append(Integer.toString(Math.abs(y)));

                // add the West/East part of the file name
                if (x < 0)
                    name.append("W");
                else
                    name.append("E");

                // append the longitude value
                name.append(Integer.toString(Math.abs(x)));

                // Add the linework type and projection code
                StringBuffer temp = new StringBuffer(name.toString());
                temp.append(baseFilename);
                temp.append(".");
                temp.append(Integer.toString(projCode));

                // add the filenames to the fileCache and add up how many to
                // load
                if (fileCache.addFile(temp.toString()))
                    numFiles++;
            }
        }
        return numFiles;
    }

    // method to find the 5-million-meter tile that encloses the
    // given point - the 5-million-meter point above and left of the point.
    //   returns: a Point object whose x and y are the values of the
    //            upper left corner of the tile containing the given point.
    //---------------------------------------------------------------------
    private Point findProjCoordGridTile(Point inputCoord)
    {
        Point outputGridCoord = new Point(0,0);

        int divisor = projCoordGridDivisor * projCoordGridIncrement;
        int xTileNo = inputCoord.x / divisor;
        int xRem    = inputCoord.x % divisor;
        int yTileNo = inputCoord.y / divisor;
        int yRem    = inputCoord.y % divisor;

        if (xTileNo <= 0 && xRem < 0)
        {
            // we're in the western hemisphere, not on a boundary line, so
            // the actual index we want is one to the west
            xTileNo--;
        }
        if (yTileNo >= 0 && yRem > 0)
        {
            // we're in the northern hemisphere, not on a boundary line, so
            // the actual index we want is one to the north
            yTileNo++;
        }

        // convert the indices back to the grid corner values
        outputGridCoord.x = xTileNo * projCoordGridIncrement;
        outputGridCoord.y = yTileNo * projCoordGridIncrement;

        return outputGridCoord;
    }

    // method to configure the map layer for the displayed area in projection
    // coordinates.
    //   Returns: number of files that will need to be loaded
    //---------------------------------------------------------------------
    public int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode)
    {
        int numFiles = 0;    // number of files found to load

        // age the cache contents
        fileCache.age();

        // if the bounds are not defined, nothing to do
        if ((ulCoord == null) || (lrCoord == null))
            return 0;

        // find the coordinates for the tiles that cover the given points
        Point ulTileCorner = findProjCoordGridTile(ulCoord);
        Point lrTileCorner = findProjCoordGridTile(lrCoord);

        // build the file names of all of the map layer files to load
        // and add them to fileCache
        for (int x = ulTileCorner.x;
                 x <= lrTileCorner.x;
                 x+= projCoordGridIncrement)
        {
            for (int y = ulTileCorner.y;
                     y >= lrTileCorner.y;
                     y-= projCoordGridIncrement)
            {
                StringBuffer filename = new StringBuffer("linework/");

                // add the South/North part of the file name
                if (y < 0)
                    filename.append("S");
                else
                    filename.append("N"); // 0 is N

                // append the upper coordinate value
                filename.append(Integer.toString(Math.abs(y)));

                // add the West/East part of the file name
                if (x < 0)
                    filename.append("W");
                else
                    filename.append("E"); // 0 is E

                // append the left coordinate value
                filename.append(Integer.toString(Math.abs(x)));

                // append the linework type and projection code
                filename.append(baseFilename);
                filename.append(".");
                filename.append(Integer.toString(projCode));

                // add the file name to the fileCache and increment counter
                if (fileCache.addFile(filename.toString()))
                    numFiles++;
            }
        }

        return numFiles;
    }
}
