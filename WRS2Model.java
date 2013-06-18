// WRS2Model.java implements the World Reference System version 2 (WRS-2)
// navigation model
//-----------------------------------------------------------------------------
import java.awt.Point;

public class WRS2Model extends NavigationModel
{
    private String modelName = "WRS-2";
    private String rowName = "Row";
    private String pathName = "Path";
    private ll2PathRow convToWRS2;

    // constructor for the WRS2 Navigation Model.  Defaults to descending
    // row numbers.
    //-------------------------------------------------------------------
    WRS2Model()
    {
        convToWRS2 = new ll2PathRow(ll2PathRow.WRS2_REFERENCE_SYSTEM,false);
    }

    // constructor for the WRS2 Navigation Model that allows overriding the
    // descending rows if needed (intended to be used from derived classes 
    // that need ascending row support instead)
    //---------------------------------------------------------------------
    protected WRS2Model(boolean ascending)
    {
        convToWRS2 = new ll2PathRow(ll2PathRow.WRS2_REFERENCE_SYSTEM,ascending);
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
        return pathName;
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
        return 233;
    }

    // method to return the minimum grid column
    //-----------------------------------------
    public int getMinimumColumn()
    {
        return 1;
    }

    // method to return the maximum grid row
    //--------------------------------------
    public int getMaximumRow()
    {
        return 124;
    }

    // method to return the minimum grid row
    //--------------------------------------
    public int getMinimumRow()
    {
        return 1;
    }

    // method to convert a latitude/longitude to a integer grid column/row
    //--------------------------------------------------------------------
    public Point latLongToGrid(double latitude, double longitude)
    {
        convToWRS2.toPathRow(latitude,longitude);
        Point pr = new Point(convToWRS2.path,convToWRS2.row);

        return pr;
    }

    // method to convert a projection coordinate to a grid column/row that has
    // not been rounded to an integer
    //------------------------------------------------------------------------
    public DoublePoint projToDoubleGrid(Point xy, ProjectionTransformation proj)
    {
        LatLong latLong = proj.projToLatLong(xy.x, xy.y);
        convToWRS2.toPathRow(latLong.latitude, latLong.longitude);
        DoublePoint pr = new DoublePoint(convToWRS2.doublePath,
                                         convToWRS2.doubleRow);
        return pr;
    }

    // method to convert a grid column/row to a latitude and longitude
    //----------------------------------------------------------------
    public LatLong gridToLatLong(int gridCol, int gridRow)
    {
        convToWRS2.toLatLong(gridCol, gridRow);
        return new LatLong(convToWRS2.latitude, convToWRS2.longitude);
    }

    // method to convert a grid column/row to a projection coordinate for 
    // a given projection transformation
    //-------------------------------------------------------------------
    public Point gridToProjCoords(int gridCol, int gridRow, 
            ProjectionTransformation proj)
    {
        LatLong latLong = gridToLatLong(gridCol, gridRow);
        Point coord = proj.latLongToProj(latLong);
        return coord;
    }

    // method to limit the grid columns to the defined bounds for WRS-2.
    // Returns the value after limiting.
    //------------------------------------------------------------------
    public int checkColumnBounds(int col)
    {
        while (col > 233)
            col -= 233;
        while (col < 1)
            col += 233;
        return col;
    }

    // method to limit the grid rows to the defined bounds for WRS-2.
    // Returns the value after limiting.
    //---------------------------------------------------------------
    public int checkRowBounds(int row)
    {
        while (row > 124)
            row -= 124;
        while (row < 1)
            row += 124;
        return row;
    }

    // method to indicate whether the grid should be allowed to wrap around
    // in the column direction.  WRS models allow wrap around in the column
    // direction.
    //---------------------------------------------------------------------
    public boolean allowColumnWrapAround()
    {
        return true;
    }
}
