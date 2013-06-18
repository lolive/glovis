// NavigationModel.java is an abstract base class for sensor navigation models.
//
//-----------------------------------------------------------------------------
import java.awt.Point;

public abstract class NavigationModel
{
    // method to return a model name for display
    public abstract String getModelName();
    // method to return the name of the columns in the grid
    public abstract String getColName();
    // method to return the name of the rows in the grid
    public abstract String getRowName();
    // method to convert a latitude/longitude to an integer grid column/row
    public abstract Point latLongToGrid(double latitude, double longitude);
    // method to convert a projection coordinate to a grid column/row that has
    // not been rounded to an integer
    public abstract DoublePoint projToDoubleGrid(Point xy, 
                                                 ProjectionTransformation proj);
    // method to convert a grid column/row to a latitude and longitude
    public abstract LatLong gridToLatLong(int gridCol, int gridRow);
    // method to convert a grid column/row to a projection coordinate for 
    // a given projection transformation
    public abstract Point gridToProjCoords(int gridCol, int gridRow, 
            ProjectionTransformation proj);
    // method to limit the grid columns.  Returns the value after limiting.
    public abstract int checkColumnBounds(int col);
    // method to limit the grid rows.  Returns the value after limiting.
    public abstract int checkRowBounds(int row);
    // method to return the minimum grid column
    public abstract int getMinimumColumn();
    // method to return the maximum grid column
    public abstract int getMaximumColumn();
    // method to return the minimum grid row
    public abstract int getMinimumRow();
    // method to return the maximum grid row
    public abstract int getMaximumRow();

    //method to convert a column number to a string
    public String getColumnString(int col)
    {
        return "" + col;
    }

    //method to convert a row number to a string
    public String getRowString(int row)
    {
        return "" + row;
    }

    // method to convert a column string to a column number.  Returns a value
    // outside the valid range if an error occurs in the conversion.
    public int getColumnNumberFromString(String colString)
    {
        return Integer.parseInt(colString);
    }

    // method to convert a row string to a row number.  Returns a value outside
    // the valid range if an error occurs in the conversion.
    public int getRowNumberFromString(String rowString)
    {
        return Integer.parseInt(rowString);
    }

    // method to return the number of digits in the column number
    public int getColumnDigits()
    {
        return 3;
    }
    // method to return the number of digits in the row number
    public int getRowDigits()
    {
        return 3;
    }
    
    // method to define the "down" direction for the row numbers.  Returns 1
    // if increasing row numbers are "down".  Returns -1 if decreasing row
    // numbers are "down".  A default implementation is provided.
    public int getRowDownDirection()
    {
        return 1;
    }
    // method to define the "right" direction for the column numbers.  Returns 
    // 1 if increasing column numbers are "to the right".  Returns -1 if 
    // decreasing column numbers are "to the right".  A default implementation
    // is provided.
    public int getColumnRightDirection()
    {
        // column numbers increase to the left (normal for WRS model)
        return -1;
    }
    // method to check if a grid cell is a valid cell.  Returns true if the 
    // cell is a valid cell, or false if it is not.  Note this is to support
    // datasets like MODIS where some grid cells are "off the edge of the 
    // world".  The default implementation assumes all cells are valid.
    public boolean isValidGridCell(int gridCol, int gridRow)
    {
        return true;
    }
    // method to indicate whether the grid should be allowed to wrap around
    // in the column direction.  The default is to not allow wrap around.
    public boolean allowColumnWrapAround()
    {
        return false;
    }
    // method to indicate whether the grid should be allowed to wrap around
    // in the row direction.  The default is to not allow wrap around.
    public boolean allowRowWrapAround()
    {
        return false;
    }
}
