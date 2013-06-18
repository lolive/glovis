// NhapNavModel.java implements a navigation model for the NHAP aerial data.
//
// Notes:
//  - The NHAP navigation model splits each degree of longitude into 8 columns
//    and each degree of latitude into "referencePoints" rows.  The latitude
//    is configurable since the B&W and color datasets have a different number
//    of reference points in the latitude direction.
//-----------------------------------------------------------------------------
import java.awt.Point;
import java.text.DecimalFormat;

public class NhapNavModel extends NavigationModel
{
    // these names don't matter since they won't be displayed 
    private String modelName = "Nhap";
    private String rowName = "Row";
    private String colName = "Col";
    private DecimalFormat threeDigitFormat;
    private DecimalFormat twoDigitFormat;
    private int referencePoints;

    // constructor for the NAPP flight line navigation model
    //------------------------------------------------------
    public NhapNavModel
    (
        int referencePoints     // I: reference points in a degree of latitude
    )
    {
        this.referencePoints = referencePoints;
        threeDigitFormat = new DecimalFormat("000");
        twoDigitFormat = new DecimalFormat("00");
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
        // limit the maximum column to -126 degrees of longitude (8 flight
        // lines per degree of longitude)
        return 125 * 8;
    }

    // method to return the minimum grid column
    //-----------------------------------------
    public int getMinimumColumn()
    {
        // limit the minimum column to -65 degrees of longitude (8 flight lines
        // per degree of longitude)
        return 65 * 8;
    }

    // method to return the maximum grid row
    //--------------------------------------
    public int getMaximumRow()
    {
        // maximum is the reference points in the northern hemisphere
        return referencePoints * 50 - 1;
    }

    // method to return the minimum grid row
    //--------------------------------------
    public int getMinimumRow()
    {
        // for simplicity assume 24 degrees latitude is the minimum
        return referencePoints * 24;
    }

    // method to convert the column number to a string
    //------------------------------------------------
    public String getColumnString(int col)
    {
        int lon = col / 8;
        int flight = col - lon * 8 + 1;

        return "" + threeDigitFormat.format(lon) + flight;
    }

    // method to convert the row number to a string
    //---------------------------------------------
    public String getRowString(int row)
    {
        int lat = row / referencePoints;
        int sublat = row - (int)(lat * referencePoints
                            + 1.0/(2.0 * referencePoints)) + 1;

        return "" + twoDigitFormat.format(lat) + twoDigitFormat.format(sublat);
    }

    // method to convert a column string to a column number.  Returns a value
    // outside the valid range if an error occurs in the conversion.
    //-----------------------------------------------------------------------
    public int getColumnNumberFromString(String colString)
    {
        int errorValue = getMinimumColumn() - 1;

        // if the string is not the correct size, return an error
        if (colString.length() != 4)
            return errorValue;

        String lonString = colString.substring(0,3);
        String flightString = colString.substring(3,4);

        int flight;
        int lon;
        try
        {
            flight = Integer.parseInt(flightString);
            lon = Integer.parseInt(lonString);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        int col = lon * 8 + (flight - 1);

        return col;
    }

    // method to convert a row string to a row number.  Returns a value outside
    // the valid range if an error occurs in the conversion.
    //-------------------------------------------------------------------------
    public int getRowNumberFromString(String rowString)
    {
        int errorValue = getMinimumRow() - 1;

        // if the string is not the correct size, return an error
        if (rowString.length() != 4)
            return errorValue;

        String latString = rowString.substring(0,2);
        String sublatString = rowString.substring(2,4);

        int sublat;
        int lat;
        try
        {
            sublat = Integer.parseInt(sublatString);
            lat = Integer.parseInt(latString);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        int row = lat * referencePoints + (sublat - 1);

        return row;
    }

    // method to return the number of digits in the column number
    //-----------------------------------------------------------
    public int getColumnDigits()
    {
        return 4;
    }

    // method to convert a latitude/longitude to a integer grid column/row
    //--------------------------------------------------------------------
    public Point latLongToGrid(double latitude, double longitude)
    {
        int row = (int)Math.round(latitude * referencePoints);
        int col = (int)Math.round((-longitude - 0.0625) * 8);
        Point grid = new Point(col, row);
        return grid;
    }

    // method to convert a projection coordinate to a grid column/row that has
    // not been rounded to an integer
    //------------------------------------------------------------------------
    public DoublePoint projToDoubleGrid(Point xy, ProjectionTransformation proj)
    {
        LatLong latLong = proj.projToLatLong(xy.x, xy.y);
        double row = latLong.latitude * referencePoints;
        double col = (-latLong.longitude + 0.0625) * 8;
        DoublePoint grid = new DoublePoint(col, row);
        return grid;
    }

    // method to convert a grid column/row to a latitude and longitude
    //----------------------------------------------------------------
    public LatLong gridToLatLong(int gridCol, int gridRow)
    {
        double latitude = gridRow / (double)referencePoints;
        double longitude = -gridCol / 8.0 - 0.0625;
        return new LatLong(latitude, longitude);
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

    // method to indicate whether the grid should be allowed to wrap around
    // in the column direction.
    //---------------------------------------------------------------------
    public boolean allowColumnWrapAround()
    {
        return false;
    }
}
