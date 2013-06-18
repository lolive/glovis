// PolarNavModel.java implements a navigation model for datasets in a polar
// stereographic projection at the south pole.
//
// Notes:
//  - The polar region is split into 120km cells in the polar stereographic
//  projection used for the Antarctica data
//-----------------------------------------------------------------------------
import java.awt.Point;
import java.text.DecimalFormat;

public class PolarNavModel extends NavigationModel
{
    // these names don't matter since they won't be displayed 
    private String modelName = "Polar";
    private String rowName = "Row";
    private String colName = "Col";

    private int min_col;
    private int max_col;
    private int min_row;
    private int max_row;
    private int columns;
    private int rows;

    // projection extents (in km)
    private final int CELL_SIZE = 120;
    private final int MIN_X = -2880;
    private final int MAX_X = 2880;
    private final int MIN_Y = -2880;
    private final int MAX_Y = 2880;

    private ProjectionTransformation proj;

    // constructor for the polar navigation model
    //-------------------------------------------
    public PolarNavModel()
    {
        min_col = MIN_X / CELL_SIZE;
        max_col = MAX_X / CELL_SIZE;
        min_row = MIN_Y / CELL_SIZE;
        max_row = MAX_Y / CELL_SIZE;
        columns = max_col - min_col + 1;
        rows = max_row - min_row + 1;

        proj = CreateProjection.fromProjectionNumber(
                    CreateProjection.POLAR_STEREOGRAPHIC);
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
        return max_col;
    }

    // method to return the minimum grid column
    //-----------------------------------------
    public int getMinimumColumn()
    {
        return min_col;
    }

    // method to return the maximum grid row
    //--------------------------------------
    public int getMaximumRow()
    {
        return max_row;
    }

    // method to return the minimum grid row
    //--------------------------------------
    public int getMinimumRow()
    {
        return min_row;
    }

    // method to convert the column number to a string
    //------------------------------------------------
    public String getColumnString(int col)
    {
        String dir;

        if (col < 0)
        {
            dir = "W";
            col = -col;
        }
        else
            dir = "E";

        return dir + col;
    }

    // method to convert the row number to a string
    //---------------------------------------------
    public String getRowString(int row)
    {
        String dir;

        if (row < 0)
        {
            dir = "S";
            row = -row;
        }
        else
            dir = "N";

        return dir + row;
    }

    // method to convert a column string to a column number.  Returns a value
    // outside the valid range if an error occurs in the conversion.
    //-----------------------------------------------------------------------
    public int getColumnNumberFromString(String colString)
    {
        int errorValue = getMinimumColumn() - 1;

        // if the string is not the correct size, return an error
        if (colString.length() < 2)
            return errorValue;

        String dir = colString.substring(0,1);
        String tile = colString.substring(1);

        int col;
        try
        {
            col = Integer.parseInt(tile);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        if (dir.equals("W"))
            col = -col;

        return col;
    }

    // method to convert a row string to a row number.  Returns a value outside
    // the valid range if an error occurs in the conversion.
    //-------------------------------------------------------------------------
    public int getRowNumberFromString(String rowString)
    {
        int errorValue = getMinimumRow() - 1;

        // if the string is not the correct size, return an error
        if (rowString.length() < 2)
            return errorValue;

        String dir = rowString.substring(0,1);
        String tile = rowString.substring(1);

        int row;
        try
        {
            row = Integer.parseInt(tile);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        if (dir.equals("S"))
            row = -row;

        return row;
    }

    // method to return the number of digits in the column number
    //-----------------------------------------------------------
    public int getColumnDigits()
    {
        return 3;
    }

    // method to convert a latitude/longitude to a integer grid column/row
    //--------------------------------------------------------------------
    public Point latLongToGrid(double latitude, double longitude)
    {
        // convert from lat/long to projection coordinates, then to the grid
        LatLong latLong = new LatLong(latitude, longitude);
        Point coords = proj.latLongToProj(latLong);

        int row = (int)Math.round(coords.y / (CELL_SIZE * 1000));
        int col = (int)Math.round(coords.x / (CELL_SIZE * 1000));
        Point grid = new Point(col, row);
        return grid;
    }

    // method to convert a projection coordinate to a grid column/row that has
    // not been rounded to an integer
    //------------------------------------------------------------------------
    public DoublePoint projToDoubleGrid(Point xy, ProjectionTransformation proj)
    {
        // no proj needed as a parameter so just ignore it
        double row = xy.y / (CELL_SIZE * 1000);
        double col = xy.x / (CELL_SIZE * 1000);
        DoublePoint grid = new DoublePoint(col, row);
        return grid;
    }

    // method to convert a grid column/row to a latitude and longitude
    //----------------------------------------------------------------
    public LatLong gridToLatLong(int gridCol, int gridRow)
    {
        // convert from the grid to a projection, then lat/long
        int x = gridCol * CELL_SIZE * 1000;
        int y = gridRow * CELL_SIZE * 1000;
        LatLong latLong = proj.projToLatLong(x, y);
        return latLong;
    }

    // method to convert a grid column/row to a projection coordinate for 
    // a given projection transformation
    //-------------------------------------------------------------------
    public Point gridToProjCoords(int gridCol, int gridRow, 
            ProjectionTransformation proj)
    {
        // ignoring the projection parameter since it should be the same as
        // the internal projection
        int x = gridCol * CELL_SIZE * 1000;
        int y = gridRow * CELL_SIZE * 1000;
        return new Point(x, y);
    }

    // method to limit the grid columns to the defined bounds for flight line.
    // Returns the value after limiting.
    //------------------------------------------------------------------------
    public int checkColumnBounds(int col)
    {
        int max = getMaximumColumn();
        int min = getMinimumColumn();

        if (col > max)
            col = max;
        else if (col < min)
            col = min;

        return col;
    }

    // method to limit the grid rows to the defined bounds for the station.
    // Returns the value after limiting.
    //---------------------------------------------------------------------
    public int checkRowBounds(int row)
    {
        int max = getMaximumRow();
        int min = getMinimumRow();

        if (row > max)
            row = max;
        else if (row < min)
            row = min;

        return row;
    }

    // method to return that the correct down direction
    //-------------------------------------------------
    public int getRowDownDirection()
    {
        return -1;
    }

    // method to define the "right" direction for the column numbers
    //--------------------------------------------------------------
    public int getColumnRightDirection()
    {
        // column numbers increase to the right
        return 1;
    }
}
