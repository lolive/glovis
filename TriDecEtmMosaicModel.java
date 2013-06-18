// TriDecEtmMosaicModel.java implements a navigation model for the Tri-Decadal
// ETM+ Mosaics.
//
// Notes:
//  - The "row" for this model is a N/S indication, the latitude truncated to
//    the next lower 5 degrees, and a U/L (upper/lower) indication
//  - The "column" for this model is the UTM zone followed by a L/R
//    (left/right) indication
//      
//-----------------------------------------------------------------------------
import java.awt.Point;
import java.text.DecimalFormat;

public class TriDecEtmMosaicModel extends NavigationModel
{
    private String modelName = "Grid";
    private String rowName = "Row";     // abbreviation for station
    private String colName = "Col";
    private DecimalFormat twoDigitFormat;

    // constructor for the Tri-Decadal ETM Mosaic navigation model
    //------------------------------------------------------------
    public TriDecEtmMosaicModel()
    {
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
        // 2 columns per UTM zone
        return 2 * 60 - 1;
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
        // 2 rows for every 5 degrees of latitude
        return 180/5 * 2 - 1;
    }

    // method to return the minimum grid row
    //--------------------------------------
    public int getMinimumRow()
    {
        return 0;
    }

    // method to convert the column number to a string
    //------------------------------------------------
    public String getColumnString(int col)
    {
        int zone = col/2 + 1;
        String colString = twoDigitFormat.format(zone);
        if ((col & 0x01) == 1)
            colString += "R";
        else
            colString += "L";

        return colString;
    }

    // method to convert the row number to a string
    //---------------------------------------------
    public String getRowString(int row)
    {
        String rowString;
        int shiftedRow = row - (90/5 * 2);
        if (shiftedRow < 0)
        {
            rowString = "S";
            shiftedRow = -shiftedRow - 1;
        }
        else
            rowString = "N";
        rowString += twoDigitFormat.format(shiftedRow/2 * 5);
        if ((row & 0x01) == 1)
            rowString += "U";
        else
            rowString += "L";

        return rowString;
    }

    // method to convert a column string to a column number.  Returns a value
    // outside the valid range if an error occurs in the conversion.
    //-----------------------------------------------------------------------
    public int getColumnNumberFromString(String colString)
    {
        int errorValue = getMinimumColumn() - 1;

        // if the string is not the correct size, return an error
        if (colString.length() != 3)
            return errorValue;

        String zoneString = colString.substring(0,2);
        String l_rString = colString.substring(2,3);

        // if the L/R indication is not a legal value, return an error
        int l_r = 0;
        if (l_rString.equalsIgnoreCase("R"))
            l_r = 1;
        else if (!l_rString.equalsIgnoreCase("L"))
            return errorValue;

        int zone;
        try
        {
            zone = Integer.parseInt(zoneString);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        int col = (zone - 1) * 2 + l_r;

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

        String n_sString = rowString.substring(0,1);
        String latString = rowString.substring(1,3);
        String u_lString = rowString.substring(3,4);

        // if the N/S indication is not a legal value, return an error
        int n_s = 0;
        if (n_sString.equalsIgnoreCase("N"))
            n_s = 1;
        else if (!n_sString.equalsIgnoreCase("S"))
            return errorValue;

        int lat;
        try
        {
            lat = Integer.parseInt(latString);
        }
        catch (NumberFormatException e)
        {
            return errorValue;
        }

        // if the U/L indication is not a legal value, return an error
        int u_l = 0;
        if (u_lString.equalsIgnoreCase("U"))
            u_l = 1;
        else if (!u_lString.equalsIgnoreCase("L"))
            return errorValue;

        // invert the latitude sign if in the southern hemisphere
        if (n_s == 0)
        {
            lat = -lat;
            if (u_l == 0)
                u_l = -2;
            else
                u_l = -1;
        }
        int row = (lat + 90) /5 * 2 + u_l;

        return row;
    }

    // method to return the number of digits in the column number
    //-----------------------------------------------------------
    public int getColumnDigits()
    {
        return 3;
    }

   // method to return the number of digits in the row number
   //--------------------------------------------------------
    public int getRowDigits()
    {
        return 4;
    }

    private int getUtmZoneFromLongitude(double longitude)
    {
        int zone = (int)(Math.abs(-180.0 - longitude) / 6.0) + 1;

        return zone;
    }

    private int getLongitudeFromUtmZone(int zone)
    {
        int lon = (zone - 1) * 6 - 180 + 3;

        return lon;
    }

    // method to convert a latitude/longitude to a integer grid column/row
    //--------------------------------------------------------------------
    public Point latLongToGrid(double latitude, double longitude)
    {
        // calculate the zone from the longitude
        int zone = getUtmZoneFromLongitude(longitude);

        // calculate the longitude of the center of the UTM zone
        int center_lon = getLongitudeFromUtmZone(zone);

        int col = (zone - 1) * 2;

        // convert the longitude to the left/right indication
        if (longitude >= center_lon)
            col++;

        latitude += 90.0;
        int row = (int)(latitude / 5);
        int lat_to_5_degrees = row * 5;
        row *= 2;
        if (latitude >= (lat_to_5_degrees + 2.5))
            row++;

        Point grid = new Point(col, row);
        return grid;
    }

    // method to convert a projection coordinate to a grid column/row that has
    // not been rounded to an integer
    //------------------------------------------------------------------------
    public DoublePoint projToDoubleGrid(Point xy, ProjectionTransformation proj)
    {
        LatLong latLong = proj.projToLatLong(xy.x, xy.y);

        // calculate the zone from the longitude
        int zone = getUtmZoneFromLongitude(latLong.longitude);

        // calculate the longitude of the center of the UTM zone
        int center_lon = getLongitudeFromUtmZone(zone);

        double col = (zone - 1) * 2;

        // convert the longitude to the left/right indication
        if (latLong.longitude >= center_lon)
        {
            col++;
            col += (latLong.longitude - center_lon)/3.0;
        }
        else
        {
            col += (latLong.longitude - (center_lon - 3.0))/3.0;
        }

        latLong.latitude += 90.0;
        double row = (int)(latLong.latitude / 5);
        int lat_to_5_degrees = (int)row * 5;
        row *= 2;
        if (latLong.latitude >= (lat_to_5_degrees + 2.5))
        {
            row++;
            row += (latLong.latitude - (lat_to_5_degrees + 2.5))/2.5;
        }
        else
        {
            row += (latLong.latitude - lat_to_5_degrees)/2.5;
        }

        DoublePoint grid = new DoublePoint(col, row);
        return grid;
    }

    // method to convert a grid column/row to a latitude and longitude
    //----------------------------------------------------------------
    public LatLong gridToLatLong(int gridCol, int gridRow)
    {
        double latitude = (gridRow * 2.5) - 90.0 + 1.25;
        double longitude = gridCol * 3.0 + 1.5 - 180.0;
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
            col -= (max + 1);
        else if (col < min)
            col += max + 1;

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

    // method to define the "right" direction for the column numbers.  Returns
    // 1 if increasing column numbers are "to the right".  Returns -1 if
    // decreasing column numbers are "to the right".  A default implementation
    // is provided.
    //------------------------------------------------------------------------
    public int getColumnRightDirection()
    {
        // column numbers increase to the right
        return 1;
    }

    // method to indicate whether the grid should be allowed to wrap around
    // in the column direction.
    //---------------------------------------------------------------------
    public boolean allowColumnWrapAround()
    {
        return true;
    }
}
