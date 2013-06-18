// ModisTileModel.java implements the MODIS V/H Tile navigation model.
//-----------------------------------------------------------------------------
import java.awt.Point;

public class ModisTileModel extends NavigationModel
{
    private String modelName = "MODIS";
    private String rowName = "V";
    private String colName = "H";
    private LatLongToModisTile convToTile;

    // constructor for the WRS2 Navigation Model.  Defaults to descending
    // row numbers.
    //-------------------------------------------------------------------
    ModisTileModel()
    {
        convToTile = new LatLongToModisTile();
    }

    // method to return a model name for display
    //------------------------------------------
    public String getModelName()
    {
        return modelName;
    }

    // method to return the name of the columns in the grid
    //-----------------------------------------------------
    public String getColName()
    {
        return colName;
    }

    // method to return the name of the rows in the grid
    //--------------------------------------------------
    public String getRowName()
    {
        return rowName;
    }

    // method to return the maximum grid column
    //-----------------------------------------
    public int getMaximumColumn()
    {
        return 35;
    }

    // method to return the minimum grid column
    //-----------------------------------------
    public int getMinimumColumn()
    {
        return 0;
    }

    // method to return the maximum grid row
    //--------------------------------------
    public int getMaximumRow()
    {
        return 17;
    }

    // method to return the minimum grid row
    //--------------------------------------
    public int getMinimumRow()
    {
        return 0;
    }

    // method to convert a latitude/longitude to a integer grid column/row
    //--------------------------------------------------------------------
    public Point latLongToGrid(double latitude, double longitude)
    {
        LatLong latLong = new LatLong(latitude,longitude);
        ModisTile tile = convToTile.latLongToTile(latLong);
        Point gridLoc = new Point(tile.h, tile.v);

        return gridLoc;
    }

    // method to convert a projection coordinate to a grid column/row that has
    // not been rounded to an integer
    //------------------------------------------------------------------------
    public DoublePoint projToDoubleGrid(Point xy, ProjectionTransformation proj)
    {
        DoublePoint gridLoc = convToTile.coordinateToDoubleGrid(xy.x, xy.y);
        return gridLoc;
    }

    // method to convert a grid column/row to a latitude and longitude
    //----------------------------------------------------------------
    public LatLong gridToLatLong(int gridCol, int gridRow)
    {
        ModisTile tile = new ModisTile(gridCol,gridRow);
        return convToTile.tileToLatLong(tile);
    }

    // method to convert a grid column/row to a projection coordinate for 
    // a given projection transformation
    //-------------------------------------------------------------------
    public Point gridToProjCoords(int gridCol, int gridRow, 
            ProjectionTransformation proj)
    {
        ModisTile tile = new ModisTile(gridCol, gridRow);
        Point coord = convToTile.tileToCoordinate(tile,true);
        return coord;
    }

    // method to limit the grid columns to the defined bounds for the MODIS
    // tile grid.  Returns the value after limiting.
    //---------------------------------------------------------------------
    public int checkColumnBounds(int col)
    {
        while (col > 35)
            col -= 36;
        while (col < 0)
            col += 36;
        return col;
    }

    // method to limit the grid rows to the defined bounds for the MODIS
    // tile grid.  Returns the value after limiting.
    //------------------------------------------------------------------
    public int checkRowBounds(int row)
    {
        while (row > 17)
            row -= 18;
        while (row < 0)
            row += 18;
        return row;
    }

    // method to define the "to the right" direction for the column numbers.
    // Implementation is provided since it deviates from the default.
    //----------------------------------------------------------------------
    public int getColumnRightDirection()
    {
        // column numbers increase to the right
        return 1;
    }

    // method to check if a grid cell is a valid cell.  Returns true if the 
    // cell is a valid cell, or false if it is not.
    //---------------------------------------------------------------------
    public boolean isValidGridCell(int gridCol, int gridRow)
    {
        ModisTile tile = new ModisTile(gridCol, gridRow);
        return convToTile.isValidTile(tile);
    }
}
