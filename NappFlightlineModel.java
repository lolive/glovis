// NappFlightlineModel.java implements a navigation model for the NAPP aerial
// data flight line.
//
// Notes:
//  - The "row" for this model is the station number.  The station number
//    is related to the scene center latitude by the equation:
//          station = 32 * latitude - 767
//    So, for the entire -90 to 90 degree latitude range, there are 5760
//    stations.
//  - The "column" for this model is the flight line.  The flight line is
//    related to the scene center in the following way (using an example):
//      0998W
//          099 = Longitude (in degrees)
//            8 = Location of the quad in relation to the positions of the
//                eight quads in a degree of longitude
//          W/E = Indicates whether the flight line is to the east or west
//                of the center of the quad.
//    The pattern running from 98 degrees to 99 degrees west longitude is:
//      0981E, 0981W, 0982E, 0982W, 0983E, 0983W, 0984E, 0984W
//      0985E, 0985W, 0986E, 0986W, 0987E, 0987W, 0988E, 0988W
//      0991E, 0991W, 0992E, 0992W, etc
//    So, there are 16 flight lines per degree of longitude.
//  - Note that the flight line is apparently only valid for west longitudes.
//    To cover the whole world, we'll consider the range to be from 0-360.
//      
//-----------------------------------------------------------------------------
import java.awt.Point;
import java.text.DecimalFormat;

public class NappFlightlineModel extends NavigationModel
{
    private String modelName = "Flight";
    private String rowName = "Stn";     // abbreviation for station
    private String colName = "Line";
    private DecimalFormat threeDigitFormat;

    // constructor for the NAPP flight line navigation model
    //------------------------------------------------------
    public NappFlightlineModel()
    {
        threeDigitFormat = new DecimalFormat("000");
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
        // limit the maximum column to -125 degrees of longitude (16 flight
        // lines per degree of longitude)
        return 125 * 16;
    }

    // method to return the minimum grid column
    //-----------------------------------------
    public int getMinimumColumn()
    {
        // limit the maximum column to -65 degrees of longitude (16 flight
        // lines per degree of longitude)
        return 65 * 16;
    }

    // method to return the maximum grid row
    //--------------------------------------
    public int getMaximumRow()
    {
        // 32 stations per degree of latitude limited to 50 degrees lat
        return 32 * 50 - 767;
    }

    // method to return the minimum grid row
    //--------------------------------------
    public int getMinimumRow()
    {
        // 32 stations per degree of latitude limited to 24 degrees lat
        return 32 * 24 - 767;
    }

    // method to convert the column number to a string
    //------------------------------------------------
    public String getColumnString(int col)
    {
        int lon = col / 16;
        int quad = col - lon * 16;
        char w_e = 'E';
        if ((quad & 0x01) == 0x01)
            w_e = 'W';
        quad /= 2;
        quad++;

        return "" + threeDigitFormat.format(lon) + quad + w_e;
    }

    // method to convert the row number to a string
    //---------------------------------------------
    public String getRowString(int row)
    {
        return "" + row;
    }

    // method to convert a column string to a column number.  Returns a value
    // outside the valid range if an error occurs in the conversion.
    //-----------------------------------------------------------------------
    public int getColumnNumberFromString(String colString)
    {
        int errorValue = getMinimumColumn() - 1;

        // if the string is not the correct size, return an error
        if (colString.length() != 5)
            return errorValue;

        String w_eString = colString.substring(4,5);
        String quadString = colString.substring(3,4);
        String lonString = colString.substring(0,3);

        // if the W/E indication is not a legal value, return an error
        int w_e = 0;
        if (w_eString.equalsIgnoreCase("W"))
            w_e = 1;
        else if (!w_eString.equalsIgnoreCase("E"))
            return errorValue;

        int quad;
        int lon;
        try
        {
            quad = Integer.parseInt(quadString);
            lon = Integer.parseInt(lonString);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        int col = lon * 16 + (quad - 1) * 2 + w_e;

        return col;
    }

    // method to convert a row string to a row number.  Returns a value outside
    // the valid range if an error occurs in the conversion.
    //-------------------------------------------------------------------------
    public int getRowNumberFromString(String rowString)
    {
        int errorValue = getMinimumRow() - 1;
        int row;
        try
        {
            row = Integer.parseInt(rowString);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        return row;
    }

    // method to return the number of digits in the column number
    //-----------------------------------------------------------
    public int getColumnDigits()
    {
        return 5;
    }

    // method to convert a latitude/longitude to a integer grid column/row
    //--------------------------------------------------------------------
    public Point latLongToGrid(double latitude, double longitude)
    {
        int station = (int)Math.round(latitude * 32 - 767);
        int flightLine = (int)Math.round((-longitude - 1.0/32.0) * 16);
        Point grid = new Point(flightLine, station);
        return grid;
    }

    // method to convert a projection coordinate to a grid column/row that has
    // not been rounded to an integer
    //------------------------------------------------------------------------
    public DoublePoint projToDoubleGrid(Point xy, ProjectionTransformation proj)
    {
        LatLong latLong = proj.projToLatLong(xy.x, xy.y);
        double station = latLong.latitude * 32 - 767;
        double flightLine = (-latLong.longitude - 1.0/32.0) * 16;
        DoublePoint grid = new DoublePoint(flightLine, station);
        return grid;
    }

    // method to convert a grid column/row to a latitude and longitude
    //----------------------------------------------------------------
    public LatLong gridToLatLong(int gridCol, int gridRow)
    {
        double latitude = (gridRow + 767.0) / 32.0;
        double longitude = -(gridCol / 16.0 + 1.0/32.0);
        if (longitude < -180.0)
            longitude += 360.0;
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
