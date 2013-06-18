// CloudCoverChoice.java implements a choice component for choosing a cloud
// cover limit.
//-------------------------------------------------------------------------
import javax.swing.JComboBox;

public class CloudCoverChoice extends JComboBox
{
    // Constructor for the CloudCoverLimit widget
    //-------------------------------------------
    public CloudCoverChoice()
    {
        // do not allow editing
        setEditable(false);

        // populate the dropdown list with percent increments every 10%
        addItem("100%");
        addItem("90%");
        addItem("80%");
        addItem("70%");
        addItem("60%");
        addItem("50%");
        addItem("40%");
        addItem("30%");
        addItem("20%");
        addItem("10%");
        addItem("0%");
        setCloudCover(100);
    }

    // method to return the selected cloud cover as an integer (0-100)
    //----------------------------------------------------------------
    public int getCloudCover()
    {
        // calculate the maximum cloud cover based on the selected index
        // of the choice widget
        int index = getSelectedIndex();
        // convert the index into a percentage cloud cover.  The indices 
        // are 0-10, but the first entry is 100 and the last 0, so the 
        // following transforms it correctly.
        int cc = 100 - index * 10;
        return cc;
    }

    // method to set the selected cloud cover to the integer passed in (0-100)
    //------------------------------------------------------------------------
    public void setCloudCover(int cc)
    {
        // set the selected index by converting the percent passed in into 
        // the correct index
        int index = (100 - cc)/10;
        setSelectedIndex(index);
    }
}
