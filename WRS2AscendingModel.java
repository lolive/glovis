// WRS2Model.java implements the World Reference System version 2 (WRS-2)
// navigation model
//-----------------------------------------------------------------------------
import java.awt.Point;

public class WRS2AscendingModel extends WRS2Model
{
    WRS2AscendingModel()
    {
        super(true);
    }

    // method to limit the grid rows to the defined bounds for WRS-2.
    // Returns the value after limiting.
    //---------------------------------------------------------------
    public int checkRowBounds(int row)
    {
        while (row > 248)
            row -= 124;
        while (row < 125)
            row += 124;
        return row;
    }

    // method to define the "down" direction for the row numbers
    //----------------------------------------------------------
    public int getRowDownDirection()
    {
        return -1;
    }

    // method to return the maximum grid row
    //--------------------------------------
    public int getMaximumRow()
    {
        return 248;
    }
                                                                               // method to return the minimum grid row
    //--------------------------------------
    public int getMinimumRow()
    {
        return 125;
    }
}
