//  NavigateDate.java implements panel to allow easily going to a date
//--------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class NavigateDate extends JPanel implements ActionListener, Observer
{
    private JButton goButton;   // button to go to the selected date
    private JComboBox year;     // year selection widget
    private MonthChoice month;  // month selection widget
    private int startYear;      // first year in the selection widget
    private int endYear;        // last year in the selection widget
    private MosaicData md;      // mosaic data object
    private imgViewer applet;   // applet object

    // Constructor for the SceneList
    // -----------------------------
    NavigateDate(imgViewer parent, MosaicData mdIn)
    {
        md = mdIn;
        applet = parent;

        // use the box layout
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // create a month dropdown list
        month = new MonthChoice();
        month.setFont(parent.normalFont);
        month.setToolTipText("Set target month");
        add(month);

        // create a year dropdown list
        year = new JComboBox();
        year.setFont(parent.normalFont);
        year.setEditable(false);
        year.setToolTipText("Set target year");
        // add contents to the dropdown to get it sized properly.  The 
        // real contents will be set when the scene info is updated.
        year.addItem("1999");
        add(year);

        // make a button to perform the actual date jump
        goButton = new JButton("Go");
        goButton.setToolTipText("Go to target date");
        goButton.addActionListener(this);
        add(goButton);

        // default the date (will be wiped out anyway when the scene info
        // is updated)
        startYear = 1999;
        endYear = 1999;

        // set the size so that it won't grow in height (otherwise the box
        // layout will let it grow too tall)
        Dimension size = getPreferredSize();
        size.width = 100;
        setMinimumSize(size);
        size.width = 240;
        setMaximumSize(size);
    }

    // Method for the Observer interface.  Make sure the date navigation
    // widget tracks the current scene selected
    //------------------------------------------------------------------
    public void update(Observable ob, Object arg)
    {
        Metadata currentScene = md.getCurrentScene();

        if (currentScene != null)
        {
            int year = currentScene.date / 10000;
            int month = (currentScene.date - (year * 10000)) / 100; 

            // get the current scene filter
            SceneFilter sf = md.sceneFilter;

            // filter the year to match the date limits
            SearchLimitDialog dl = applet.searchLimitDialog;
            int startYear = dl.getStartYear();
            int endYear = dl.getEndYear();
            if (sf.getFirstYear() != 0 && sf.getFirstYear() > startYear)
                startYear = sf.getFirstYear();
            if (sf.getLastYear() != 0 && sf.getFirstYear() < endYear)
                endYear = sf.getLastYear();

            // make sure the month matches the allowed date range (can be
            // temporarily out of agreement during search limit application)
            if (month < dl.getStartMonth() + 1)
                month = dl.getStartMonth() + 1;
            else if (month > dl.getEndMonth() + 1)
                month = dl.getEndMonth() + 1;

            // update the widget for the new date parameters if the years are
            // valid (might not be valid if no scenes left in the selected
            // range of years in search limits)
            if (startYear <= endYear)
            {
                trackSelectedScene(startYear,endYear,dl.getStartMonth()+1,
                                   dl.getEndMonth()+1,month,year);
            }
        }
    }

    // method to load the years for the current scene
    //-----------------------------------------------
    public void trackSelectedScene
    (   
        int begin,          // first year for scene
        int end,            // last year for scene
        int startMonth,     // starting month for month range
        int endMonth,       // ending month for month range
        int sceneMonth,     // current scene's month
        int sceneYear       // current scene's year
    )
    {
        // reload the year range if it changes
        if ((begin != startYear) || (end != endYear))
        {
            // remember the year that was selected
            int oldYear = year.getSelectedIndex() + startYear;

            // clean out and reload the year list
            year.removeAllItems();
            for (int i = begin; i <= end; i++)
            {
                year.addItem(Integer.toString(i));
            }

            // save the year range
            startYear = begin;
            endYear = end;

            // maintain the previously selected year if possible
            int index;
            if (oldYear < startYear)
                index = 0;
            else if (oldYear > end)
                index = end - begin;
            else
                index = oldYear - begin;
            year.setSelectedIndex(index);
        }

        // select the current scene's year (if it is legal since there are
        // transient conditions when the scene year might be out of range) and
        // month
        if ((sceneYear >= startYear) && (sceneYear <= endYear))
            year.setSelectedIndex(sceneYear - startYear);
        month.configure(startMonth,endMonth,sceneMonth);
    }

    // method to handle the go button
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();
        if (command.equals("Go"))
        {
            // jump to the date
            int currentMonth = month.getSelectedMonth();
            int currentYear = year.getSelectedIndex() + startYear;
            md.sceneFilter.gotoDate(currentYear,currentMonth);
        }
    }
}
