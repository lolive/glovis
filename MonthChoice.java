// MonthChoice.java encapsulates a Choice component for month names
//
//-----------------------------------------------------------------
import javax.swing.JComboBox;

public class MonthChoice extends JComboBox
{
    private String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private int firstMonth; // first available month in the choice widget
    private int lastMonth;  // last available month in the choice widget

    // constructor for building a month choice widget with a range of months
    //----------------------------------------------------------------------
    MonthChoice(int firstMonth, int lastMonth)
    {
        setEditable(false);

        if ((firstMonth < 1) || (lastMonth > 12) || (firstMonth > lastMonth))
        {
            throw new IllegalArgumentException(
                "Invalid Month to MonthChoice constructor");
        }

        configure(firstMonth,lastMonth,firstMonth);
    }

    // constructor for build a month choice with all the months
    //---------------------------------------------------------
    MonthChoice()
    {
        this(1,12);
    }

    // method to modify the range of months shown and set the selected month.
    // Note: all months are in range of 1-12
    //-----------------------------------------------------------------------
    void configure(int startMonth, int endMonth, int displayedMonth)
    {
        // update the months displayed if different than the current list
        // (only when it actually changes to reduce flicker to only happen
        // when absolutely necessary)
        if ((startMonth != firstMonth) || (endMonth != lastMonth))
        {
            firstMonth = startMonth;
            lastMonth = endMonth;

            removeAllItems();
            if (startMonth <= endMonth)
            {
                // handle when the start month comes before the end month
                for (int i = startMonth - 1; i < endMonth; i++)
                    addItem(monthNames[i]);
            }
            else
            {
                // handle the case when the start month is greater then the
                // end month (i.e. range of months crosses the Dec/Jan
                // boundary)
                for (int i = startMonth - 1; i < 12; i++)
                    addItem(monthNames[i]);
                for (int i = 0; i < endMonth; i++)
                    addItem(monthNames[i]);
            }
        }
        // select the indicated month, handling the case where the month 
        // range crosses the Dec/Jan border
        int selectMonth = displayedMonth - startMonth;
        if (selectMonth < 0)
            selectMonth += 12;
        setSelectedIndex(selectMonth);
    }

    // method to return the selected month (1-12)
    //-------------------------------------------
    int getSelectedMonth()
    {
        int month = getSelectedIndex() + firstMonth;

        // if the month is greater than 12, it has wrapped past the Dec/Jan
        // border, so subtract 12 to get the real month
        if (month > 12)
            month -= 12;
        return month;
    }
}
