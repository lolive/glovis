// SearchLimitDialog.java implements a dialog for setting search limits for the
// scenes to display
//
//-----------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;

public class SearchLimitDialog extends JDialog implements WindowListener,
    ActionListener, Observer, ComponentListener
{
    private JPanel cloudCoverPanel;  // panel for the cloud cover dropdown
    private JPanel qualityPanel;     // panel for the quality dropdown
    private CloudCoverChoice ccPercent;// dropdown for the cloud cover dropdown
    private JPanel dataVersionPanel; // panel for the data version selection
    private JComboBox dataVersionChoice; // dropdown for the data version
    private JPanel gridColRowPanel;  // panel for the grid col/row selection
    private JComboBox startGridColChoice; // dropdown for start grid column 
    private JComboBox endGridColChoice; // dropdown for end grid column 
    private JComboBox startGridRowChoice; // dropdown for start grid row
    private JComboBox endGridRowChoice; // dropdown for end grid row
    private JPanel mainPanel;        // main panel for limit controls
    private JPanel datePanel;        // panel for date selection widgets
    private JPanel buttonPanel;      // panel for action buttons
    private JComboBox startYearChoice;// dropdown for selecting starting year
    private JComboBox endYearChoice;   // dropdown for selecting ending year
    private MonthChoice startMonthChoice;// dropdown for the starting month
    private MonthChoice endMonthChoice;  // dropdown for the ending month
    private JButton okButton;        // button to accept the changes
    private JButton cancelButton;    // button to cancel the changes
    private JButton clearButton;     // button to clear any date limits
    private JButton applyButton;     // button to apply changes without closing
    private JCheckBox filterToSceneList;// checkbox to display selected scenes
    private JCheckBox filterToDownloadable;// checkbox to display downloadable
    private JCheckBox filterToUserArea; // checkbox to show only user area
    private JPanel downloadablePanel;// panel for the downloadable checkbox
    private JPanel userAreaPanel;    // panel for user area checkbox
    private JComboBox qualityChoice; // dropdown for selecting the quality
    private JLabel availScenes;      // label for displaying the number of 
                                     // available scenes

    // TBD - use the observer pattern instead?
    private MosaicData md;          // reference to the MosaicData object
    private imgViewer applet;       // reference to the main applet
    private NavigationModel nm;     // reference to the NavigationModel object
    
    private int rangeStartYear;     // starting year of current sensor range
    private int rangeEndYear;       // ending year of current sensor range
    private int sensorStartYear;    // starting year of the current sensor
    private int sensorEndYear;      // ending year of the current sensor range
    private int sensorMaximumColumn; // starting grid column of current sensor
    private int sensorMinimumColumn; // ending grid column of current sensor
    private int sensorMaximumRow;    // starting grid row of current sensor
    private int sensorMinimumRow;    // ending grid column of current sensor
    
    // variables for remembering the last accepted date range
    private int startYear;          // last accepted starting year limit
    private int endYear;            // last accepted ending year limit    
    private int startMonth;         // last accepted starting month limit (0-11)
    private int endMonth;           // last accepted ending month limit (0-11)    
    private int startGridCol;       // last accepted start grid Col
    private int endGridCol;         // last accepted end grid Col
    private int startGridRow;       // last accepted start grid Row
    private int endGridRow;         // last accepted end grid Row
    private boolean gridColRowFilterSet;// flag to indicate the row/column
                                    // filter is set
    private int minQuality;         // last accepted minimum quality limit
    private String dataVersion;     // last accepted data version (or All)
    private int cloudCoverValue;    // last accepted cloud cover value

    // variables for detecting when the displayed data has changed so the 
    // available scene count can be updated 
    private int savedResolution;    // last saved resolution
    private int savedGridCol;       // last saved grid column of center cell
    private int savedGridRow;       // last saved grid Row of center cell 
    private int savedSubCol;        // last saved fractional column step 
                                    // when displaying 1 cell
    private int savedSubRow;        // last saved fractional row step 
                                    // when displaying  1 cell       
    private String savedSensorName; // last saved sensor name

    private int numOfAvailScenes;   // number of available scenes
    private boolean sceneListFilterStatus;  // stores last accepted status of
                                            // scene list filter checkbox
    private boolean downloadableFilterStatus;// stores last accepted status of
                                            // downloadable filter checkbox
    private boolean userAreaFilterStatus;   // stores last accepted status of
                                            // user area filter
    private boolean limitSet; // this flag tracks when a search limit has 
                              // been set.  This allows preserving the limits
                              // when a user changes to a new sensor. 
    private boolean limitCleared; // this flag tracks when the user has 
                              // pressed the clear button and not set any
                              // limits afterwards.  This needs to be tracked
                              // since the limits are not really cleared until 
                              // the user presses "accept".
    private TOC[] copyOfMosaicCells;  // a copy of TOC's for the mosiac area
    private String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    // define the range of quality values
    private final static int QUAL_RANGE = Metadata.IMAGE_QUALITY_MAX;

    // accessor methods for the date limit range values.  Note that months are
    // in the range of 0-11.
    public int getStartYear() { return startYear; }
    public int getEndYear() { return endYear; }
    public int getStartMonth() { return startMonth; }
    public int getEndMonth() { return endMonth; }

    // accessor method for the start and end grid col and row
    public int getStartGridCol(){ return startGridCol; }
    public int getEndGridCol(){ return endGridCol; }
    public int getStartGridRow(){ return startGridRow; }
    public int getEndGridRow(){ return endGridRow; }

    // accessor method for getting the current minquality value
    public int getMinQuality() { return minQuality; }

    // accessor method for getting scene list filter state for displaying scene
    // list scenes only
    public boolean isSceneListFilterEnabled() 
    { 
        return sceneListFilterStatus; 
    }

    // accessor method for getting downloadable filter state for displaying
    // scenes that are currently downloadable only
    public boolean isDownloadableFilterEnabled() 
    { 
        return downloadableFilterStatus; 
    }

    // accessor method for getting the state of the user defined area 
    // checkbox
    public boolean isUserDefinedAreaEnabled()
    {
        return userAreaFilterStatus;
    }

    // accessor method for the data version to display (or All)
    public String getDataVersion() { return dataVersion; }

    // Constructor for the search limit dialog
    //----------------------------------------
    public SearchLimitDialog(JFrame parent, imgViewer applet, MosaicData md)
    {
        super(parent,"Set Search Limits",false);
        this.applet = applet;
        this.md = md;

        getContentPane().setLayout(new BorderLayout());

        mainPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.setLayout(gridbag);

        // set up the year filter entry
        datePanel = new JPanel();
        datePanel.setLayout(new GridBagLayout());

        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new GridLayout(2,2));

        JLabel label = new JLabel("Start Year:");
        tempPanel.add(label);
        startYearChoice = new JComboBox();
        startYearChoice.setToolTipText("Set start year limit");
        startYearChoice.addActionListener(this);
        tempPanel.add(startYearChoice);

        label = new JLabel("End Year:");
        tempPanel.add(label);
        endYearChoice = new JComboBox();
        endYearChoice.setToolTipText("Set end year limit");
        endYearChoice.addActionListener(this);
        tempPanel.add(endYearChoice);

        datePanel.add(tempPanel, gbc);

        // set up the month filter entry
        tempPanel = new JPanel();
        tempPanel.setLayout(new GridLayout(2,2));

        label = new JLabel("Start Month:");
        tempPanel.add(label);
        startMonthChoice = new MonthChoice();
        startMonthChoice.setToolTipText("Set start month limit");
        startMonthChoice.addActionListener(this);
        tempPanel.add(startMonthChoice);

        label = new JLabel("End Month:");
        tempPanel.add(label);
        endMonthChoice = new MonthChoice();
        endMonthChoice.setSelectedIndex(11);
        endMonthChoice.setToolTipText("Set end month limit");
        endMonthChoice.addActionListener(this);
        tempPanel.add(endMonthChoice);
        datePanel.add(tempPanel, gbc);

        mainPanel.add(datePanel, gbc);

        // create a the max cloud cover selection in its own panel so it can be
        // made invisible for sensors that don't have cloud cover values
        cloudCoverPanel = new JPanel();
        cloudCoverPanel.setLayout(new GridLayout(1,2));
        label = new JLabel("Max Cloud:");
        cloudCoverPanel.add(label);
        ccPercent = new CloudCoverChoice();
        ccPercent.setToolTipText("Set max cloud cover limit");
        ccPercent.addActionListener(this);
        cloudCoverPanel.add(ccPercent);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        mainPanel.add(cloudCoverPanel, gbc);

        // create the data version selection in its own panel so it can be made
        // invisible for sensors that don't have data versions
        dataVersionPanel = new JPanel();
        dataVersionPanel.setLayout(new GridLayout(1,2));
        label = new JLabel("Data Version:");
        dataVersionPanel.add(label);
        dataVersionChoice = new JComboBox();
        dataVersionChoice.addActionListener(this);
        dataVersionChoice.setToolTipText("Set data version limit");
        dataVersionPanel.add(dataVersionChoice);
        mainPanel.add(dataVersionPanel, gbc);
        
        // create the grid Column and row range selection in its own panel 
        // so it can be made invisible for sensors that don't need to search 
        // on column and row.
        gridColRowPanel = new JPanel();
        gridColRowPanel.setLayout(new GridLayout(4,2));
        
        label = new JLabel("Start Path:");
        gridColRowPanel.add(label);
        startGridColChoice = new JComboBox();
        startGridColChoice.addActionListener(this);
        gridColRowPanel.add(startGridColChoice);
        
        label = new JLabel("End Path:");
        gridColRowPanel.add(label);
        endGridColChoice = new JComboBox();
        endGridColChoice.addActionListener(this);
        gridColRowPanel.add(endGridColChoice);

        label = new JLabel("Start Row:");
        gridColRowPanel.add(label);
        startGridRowChoice = new JComboBox();
        startGridRowChoice.addActionListener(this);
        gridColRowPanel.add(startGridRowChoice);
        
        label = new JLabel("End Row:");
        gridColRowPanel.add(label);
        endGridRowChoice = new JComboBox();
        endGridRowChoice.addActionListener(this);
        gridColRowPanel.add(endGridRowChoice);
        mainPanel.add(gridColRowPanel, gbc);

        // create the minimum quality drop-down in its own panel so it can be
        // made invisible for sensors that don't have data versions
        qualityPanel = new JPanel();
        qualityPanel.setLayout(new GridLayout(1,2));
        label = new JLabel("Min Quality:");
        qualityPanel.add(label);
        qualityChoice = new JComboBox();
        for (int i = QUAL_RANGE; i >= 0; i--)
            qualityChoice.addItem("" + i);
        qualityChoice.setToolTipText("Set min quality limit");
        qualityChoice.addActionListener(this);
        qualityPanel.add(qualityChoice);
        mainPanel.add(qualityPanel, gbc);
        
        // add checkbox to only display selected scenes
        tempPanel = new JPanel();
        tempPanel.setLayout(new GridLayout(1,1));
        filterToSceneList = new JCheckBox("Show only scenes in the scene list",
            false);
        filterToSceneList.setToolTipText("Show only scenes in scene list");
        filterToSceneList.addActionListener(this);
        sceneListFilterStatus = false;
        tempPanel.add(filterToSceneList);
        mainPanel.add(tempPanel, gbc);

        // create the downloadable checkbox in its own panel so it can be made
        // invisible for sensors that are not mightBeDownloadable
        downloadablePanel = new JPanel();
        downloadablePanel.setLayout(new GridLayout(1,1));
        filterToDownloadable = new JCheckBox("Show only downloadable scenes",
            false);
        filterToDownloadable.setToolTipText("Show only downloadable scenes");
        filterToDownloadable.addActionListener(this);
        downloadableFilterStatus = false;
        downloadablePanel.add(filterToDownloadable);
        mainPanel.add(downloadablePanel, gbc);

        //add checkbox for user defined area
        userAreaPanel = new JPanel();
        userAreaPanel.setLayout(new GridLayout(1,1));
        filterToUserArea = new JCheckBox(
                    "Show only scenes in user defined area", false);
        filterToUserArea.setToolTipText(
                    "Show only scenes in the user defined area");
        filterToUserArea.addActionListener(this);
        userAreaFilterStatus = false;
        userAreaPanel.add(filterToUserArea);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.add(userAreaPanel, gbc);
        
        // add a label to display the number of available scenes
        JPanel availScenesPanel = new JPanel();
        availScenesPanel.setLayout(new GridLayout());
        availScenes = new JLabel("num of avail scenes");
        availScenes.setToolTipText("Number of Available scenes");
        availScenesPanel.add(availScenes);
        gbc.gridheight = GridBagConstraints.REMAINDER;
        mainPanel.add(availScenes, gbc);
 
        // set up the buttons
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,4));
        okButton = new JButton("Ok");
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.setToolTipText("Set search limits & close search limits");
        okButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setToolTipText("Cancel changes to search limits");
        cancelButton.addActionListener(this);
        clearButton = new JButton("Clear");
        clearButton.setMnemonic(KeyEvent.VK_L);
        clearButton.setToolTipText("Clear search limits");
        clearButton.addActionListener(this);
        applyButton = new JButton("Apply");
        applyButton.setMnemonic(KeyEvent.VK_A);
        applyButton.setToolTipText("Apply search limits");
        applyButton.addActionListener(this);
        applyButton.setEnabled(false);

        buttonPanel.add(okButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        
        // add the year, month, and button panels to the dialog
        getContentPane().add(mainPanel,"North");
        getContentPane().add(buttonPanel,"South");

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(320,350);

        dataVersion = "All";
        cloudCoverValue = md.maxCloudCover;

        // initialize the year range for the current sensor
        configureForSensor(applet.sensorMenu.getCurrentSensor());

        // request the window events
        addWindowListener(this);

        // request the component events
        addComponentListener(this);
         
        // set the default message for the search limits status display
        applet.statusBar.showSearchLimits("No Limits Set");
    }

    // helper method to populate the grid column and row choice
    //---------------------------------------------------------
    private void populateGridColRow(JComboBox gridColRowChoice,
                                    int start, int end)
    {
        gridColRowChoice.removeAllItems();
        for (int i = start; i <= end; i++)
        {
            gridColRowChoice.addItem("" + i);
        }
    }

    // helper method to populate a year choice with the indicated range
    // of years
    // TBD: Skip years 1993-2011 for MSS data
    //-----------------------------------------------------------------
    private void populateYears(JComboBox yearChoice, int start, int end)
    {
        // TBD - this method of populating the years results in the choice
        // widget flickering when populated.  With enough work that could be
        // eliminated, but it probably isn't that important.
        yearChoice.removeAllItems();
        for (int i = start; i <= end; i++)
        {
            yearChoice.addItem("" + i);
        }
        rangeStartYear = start;
        rangeEndYear = end;
    }

    // method to configure the range of years for the indicated sensor
    //----------------------------------------------------------------
    public void configureForSensor(Sensor sensor)
    {
        // get the sensor starting and ending years
        sensorStartYear = sensor.getStartingYear();
        sensorEndYear = sensor.getEndingYear();

        // if a negative number is returned for the ending year, the mission is
        // still collecting data, so use the current year for the ending year
        if (sensorEndYear < 0)
        {
            Calendar today = new GregorianCalendar();
            sensorEndYear = today.get(Calendar.YEAR);
            // just in case the date on the system is really out of line, 
            // make the ending date at least 2013 (ran across once on a Mac)
            if (sensorEndYear < 2013)
                sensorEndYear = 2013;
        }

        // get the sensor maximum/minimum grid column and row
        nm = sensor.navModel;
        sensorMaximumColumn = nm.getMaximumColumn();
        sensorMinimumColumn = nm.getMinimumColumn();
        sensorMaximumRow = nm.getMaximumRow();
        sensorMinimumRow = nm.getMinimumRow();
        
        // get this sensors col and row name and set the labels
        if (sensor.hasGridColRowFilter)
        {
            nm = applet.sensorMenu.getCurrentSensor().navModel;
            String colName = nm.getColName();
            String rowName = nm.getRowName();
            startGridColChoice.setName("Start " + colName + ":");
            endGridColChoice.setName("End " + colName + ":");
            startGridRowChoice.setName("Start " + rowName + ":");
            endGridRowChoice.setName("End " + rowName + ":");
        }
        if (!gridColRowFilterSet)
        {
            startGridCol = sensorMinimumColumn;
            endGridCol = sensorMaximumColumn;
            startGridRow = sensorMinimumRow;
            endGridRow = sensorMaximumRow;
        }

        // If no date limits are set, set the limits to the full range for this
        // sensor
        if (!limitSet)
        {
            startYear = sensorStartYear;
            endYear = sensorEndYear;
            startMonth = 0;
            endMonth = 11;
        }

        // determine which years should be shown in the year selection 
        // dropdowns.  Note that the range shown covers a union of the range
        // for the sensor and the currently enforced limits.  This is an 
        // attempt to make the behavior somewhat predictable when switching
        // between sensors with incompatible date coverages.
        int sy = sensorStartYear;
        int ey = sensorEndYear;
        if (startYear < sy)
            sy = startYear;
        if (endYear > ey)
            ey = endYear;

        // only populate the years if range is different than the current range
        // to eliminate flicker as they reload
        if ((rangeStartYear != sy) || (rangeEndYear != ey))
        {
            populateYears(startYearChoice,sy,ey);
            populateYears(endYearChoice,sy,ey);
        }

        // set the selected start/end year to the correct indices.  Note 
        // that the check to make sure the startYear isn't earlier than
        // the range start (or the endYear isn't later than the range end)
        // aren't really needed anymore.  But they are left in just in case
        // the code changes since most Java implementations don't care if 
        // it is set to an illegal index, but a few do...
        if (startYear < rangeStartYear)
        {
            System.out.println("Bug: Unexpected starting search year");
            startYearChoice.setSelectedIndex(0);
        }
        else
            startYearChoice.setSelectedIndex(startYear - rangeStartYear);
        if (endYear > rangeEndYear)
        {
            System.out.println("Bug: Unexpected ending search year");
            endYearChoice.setSelectedIndex(rangeEndYear-rangeStartYear);
        }
        else
            endYearChoice.setSelectedIndex(endYear-rangeStartYear);

        // set the date limit visibility based on whether the sensor has an
        // acquisition date
        datePanel.setVisible(sensor.hasAcqDate);

        // control the visibility of the cloud cover selection
        if (sensor.hasCloudCover)
            cloudCoverPanel.setVisible(true);
        else
            cloudCoverPanel.setVisible(false);

        // control the visibility of the quality selection
        if (sensor.numQualityValues > 0)
            qualityPanel.setVisible(true);
        else
            qualityPanel.setVisible(false);

        // control the visibility of the data version selection
        if (sensor.hasDataVersions)
        {
            int dataVersionIndex = 0;
            // set up the data version dropdown
            dataVersionChoice.removeAllItems();
            dataVersionChoice.addItem("All");
            for (int i = 0; i < sensor.dataVersions.length; i++)
            {
                dataVersionChoice.addItem(sensor.dataVersions[i]);
                if (!dataVersion.equals("All")
                    && dataVersion.equals(sensor.dataVersions[i]))
                {
                    dataVersionIndex = i + 1; // add 1 to account for "All"
                }
            }
            dataVersionChoice.setSelectedIndex(dataVersionIndex);
            dataVersionPanel.setVisible(true);
        }
        else
            dataVersionPanel.setVisible(false);
        
        // control the visibility of the grid col/row selection
        if (sensor.hasGridColRowFilter)
        {
            populateGridColRow(startGridColChoice, sensorMinimumColumn,
                                               sensorMaximumColumn);
            populateGridColRow(endGridColChoice, sensorMinimumColumn,
                                               sensorMaximumColumn);
            populateGridColRow(startGridRowChoice,sensorMinimumRow,
                                               sensorMaximumRow);
            populateGridColRow(endGridRowChoice, sensorMinimumRow,
                                               sensorMaximumRow);
            
            // the sensor has changed and the grid rows need to be updated
            // to the valid range for the sensor displayed.
            if ((startGridRow > sensorMaximumRow) || 
                (endGridRow < sensorMinimumRow))
            {
                startGridRow = sensorMinimumRow;
                endGridRow = sensorMaximumRow;    
                gridColRowFilterSet = false;
            }
            
            startGridColChoice.setSelectedIndex(startGridCol - 
                                               sensorMinimumColumn);
            endGridColChoice.setSelectedIndex(endGridCol - 
                                              sensorMinimumColumn);
            startGridRowChoice.setSelectedIndex(startGridRow - 
                                               sensorMinimumRow);
            endGridRowChoice.setSelectedIndex((endGridRow - 
                                               sensorMinimumRow));
            
            gridColRowPanel.setVisible(true);
        }
        else
        {
            gridColRowFilterSet = false;
            gridColRowPanel.setVisible(false);
        }
        
        // control the visibilty of the downloadable checkbox
        if (sensor.mightBeDownloadable)
            downloadablePanel.setVisible(true);
        else
            downloadablePanel.setVisible(false);

        // control the visibilty of the user defined area checkbox
        if (sensor.hasUserDefinedArea)
            userAreaPanel.setVisible(true);
        else
            userAreaPanel.setVisible(false);

        // force the dialog box layout code to run to pick up the visibility
        // changes
        this.validate();

        // build the message the gets displayed in the status bar indicating
        // which limits have been set.  Call here because switching sensors
        // may change the limits that can be selected (i.e. quality, cloud 
        // cover, and data versions are not available for all data sets).
        buildStatusLimitMsg();
    }

    // method to handle the windowClosing event
    //-----------------------------------------
    public void windowClosing(WindowEvent e)
    {
        setVisible(false);
    }

    // dummy window event handlers for unhandled events of the WindowListener
    //-----------------------------------------------------------------------
    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }

    // method to handle the opening of the dialog box.  It sets the constraints
    // for the current sensor when the dialog is opened.
    //-------------------------------------------------------------------------
    public void windowOpened(WindowEvent e) 
    { 
        // set the cloud cover choice to the current value in the applet when
        // the dialog is opened
        ccPercent.setCloudCover(md.maxCloudCover);

        // make sure the date settings are at the last accepted values when the
        // dialog is opened.  Note that this depends on the choice widget
        // clamping values to the valid range since the startYear or endYear
        // can be outside the range if the sensor is changed after the limit is
        // set.
        startYearChoice.setSelectedIndex(startYear - rangeStartYear);
        endYearChoice.setSelectedIndex(endYear - rangeStartYear);
        startMonthChoice.setSelectedIndex(startMonth);
        endMonthChoice.setSelectedIndex(endMonth);

        Sensor sensor = applet.sensorMenu.getCurrentSensor();
        
        if (sensor.hasGridColRowFilter)
        {
            startGridColChoice.setSelectedIndex(startGridCol - 
                                            sensorMinimumColumn);
            endGridColChoice.setSelectedIndex(endGridCol - sensorMinimumColumn);
            
            startGridRowChoice.setSelectedIndex(startGridRow - 
                                               sensorMinimumRow);
            endGridRowChoice.setSelectedIndex((endGridRow - 
                                               sensorMinimumRow));
        }                                
        // set the quality field the last previously set value
        qualityChoice.setSelectedIndex(QUAL_RANGE - minQuality);
    }

    // dummy component event handlers for unhandled 
    // events of the ComponentListener
    //---------------------------------------------
    public void componentHidden(ComponentEvent e) { }
    public void componentMoved(ComponentEvent e) { }
    public void componentResized(ComponentEvent e) { }
                            
    // method to update TOC array and number of available scenes when 
    // the dialog has been made visible
    //---------------------------------------------------------------
    public void componentShown(ComponentEvent e)
    {
        updateTOC();
        updateNumOfAvailScenes();
    }
         
    // method to detect when a year or month is selected
    //--------------------------------------------------
    public void updateYear()
    {
        // update current year
        int sy = startYearChoice.getSelectedIndex() + rangeStartYear;
        int ey = endYearChoice.getSelectedIndex() + rangeStartYear;
        if (ey < sy)
        {
            if (endYearChoice.getItemCount() > 0)
                endYearChoice.setSelectedIndex(sy-rangeStartYear);
            ey = sy;
        }

        // a year or month limit has been set, so clear the limitCleared flag
        limitCleared = false;

        // check if apply button should be enabled
        setApplyButtonState();

        // update the number of available scenes if the dialog is visible
        if (this.isVisible())
            updateNumOfAvailScenes();
    }

    // method to set the state of the apply button
    //--------------------------------------------
    private void setApplyButtonState()
    {
        // flags if various filter values have changed
        boolean dateChange = true;
        boolean cloudChange = true;
        boolean qualityChange = true;
        boolean dataVersionChange = true;
        boolean sceneListChange = true;
        boolean downloadableChange = true;
        boolean userAreaChange = true;
        boolean gridColChanged = true;
        boolean gridRowChanged = true;
        
        Sensor sensor = applet.sensorMenu.getCurrentSensor();

        // check if any of the date values (month or year) have changed
        if ((startYear == (startYearChoice.getSelectedIndex() + rangeStartYear))
            && (endYear == (endYearChoice.getSelectedIndex() + rangeStartYear))
            && (startMonth == startMonthChoice.getSelectedIndex())
            && (endMonth == endMonthChoice.getSelectedIndex()))
        {
            dateChange = false;
        }

        // check if any of the gridcol/gridrow changed
        if (sensor.hasGridColRowFilter)
        {
            if ((startGridCol == startGridColChoice.getSelectedIndex() + 
                                                        sensorMinimumColumn) &&
                (endGridCol == endGridColChoice.getSelectedIndex()+ 
                                                        sensorMinimumColumn))
            {
                gridColChanged = false;
            }

            if ((startGridRow == startGridRowChoice.getSelectedIndex() + 
                                                        sensorMinimumRow) &&
                (endGridRow == endGridRowChoice.getSelectedIndex() + 
                                                        sensorMinimumRow))
            {
                gridRowChanged = false;
            }
        }
        
        // check if scene list only check box has changed state
        if (sceneListFilterStatus == filterToSceneList.isSelected())
            sceneListChange = false;

        // check if downloadable only check box has changed state
        if (sensor.mightBeDownloadable)
        {
            if (downloadableFilterStatus == filterToDownloadable.isSelected())
                downloadableChange = false;
        }
        else
        {
            downloadableChange = false;
        }

        // check if user defined area check box has changed state
        if (userAreaFilterStatus == filterToUserArea.isSelected())
            userAreaChange = false;

        // see if minimum image quality value has been changed
        if (minQuality == (QUAL_RANGE - qualityChoice.getSelectedIndex()))
            qualityChange = false;
        
        // check if the data version has been changed
        if (sensor.hasDataVersions)
        {
            int dataVersionIndex = dataVersionChoice.getSelectedIndex();
            if (dataVersionIndex <= 0)
            {
                if (dataVersion.equals("All"))
                {
                    dataVersionChange = false;
                }
            }
            else
            {
                if (dataVersion.equals(
                    dataVersionChoice.getSelectedItem().toString()))
                {
                    dataVersionChange = false;
                }
            }
        }
        else
        {
            dataVersionChange = false;
        }

        // check and see if the cloud cover value has been changed
        if (sensor.hasCloudCover)
        {
            if (cloudCoverValue == ccPercent.getCloudCover())
                cloudChange = false;
        }
        else
        {
            cloudChange = false;
        }

        // if at least one of date, cloud, quality, version, or scene list 
        // checkbox is no longer in its previous accepted state, enable the 
        // apply button
        if (dateChange || cloudChange || qualityChange || dataVersionChange
            || sceneListChange || downloadableChange || userAreaChange
            || gridColChanged || gridRowChanged)
        {
            applyButton.setEnabled(true);
        }
        else
        {
            applyButton.setEnabled(false);
        }
    }

    // method to update the cloud cover when it is changed by an external 
    // source
    //-------------------------------------------------------------------
    public void setCloudCover(int cloudCover)
    {
        ccPercent.setCloudCover(cloudCover);
        cloudCoverValue = cloudCover;
    }
    
    // method to update the userDefinedAreaEnabled when it is changed by
    // an external source
    //------------------------------------------------------------------
    public void clearUserDefinedAreaEnabled()
    {
        userAreaFilterStatus = false;   
    }

    // apply the current search limits to a specific scene
    //----------------------------------------------------
    public void applySearchLimits(Metadata scene)
    {
        scene.clearFilter(Metadata.VIEWPORT_FILTER);
        scene.filterToCloudCover(md.maxCloudCover);
        scene.filterToDateRange(startYear,endYear,startMonth,endMonth);
        scene.filterToSceneList(sceneListFilterStatus);
        scene.filterToDownloadable(downloadableFilterStatus);
        scene.filterToQuality(minQuality);
        scene.filterToDataVersion(dataVersion);
        scene.filterToHiddenScene();
        scene.filterToUserArea(userAreaFilterStatus, 
                               applet.userDefinedAreaDialog);
        scene.filterToGridColRowRange(startGridCol,endGridCol,
                                      startGridRow,endGridRow);
    }

    // apply the search limits to the images currently displayed
    //------------------------------------------------------------
    public void applyFilter()
    {
        // update the currently displayed date and cloud cover to match the
        // limits selected
        int cc = ccPercent.getCloudCover();
        md.setSearchLimitValues(startYear,endYear,startMonth,endMonth,
                                cc,minQuality,dataVersion,startGridCol,
                                endGridCol,startGridRow,endGridRow);
    }

    // build the status bar message and send it to the status bar
    //-----------------------------------------------------------
    private void buildStatusLimitMsg()
    {
        // set the search limits area of the status bar to an appropriate
        // message
        StringBuffer msg = new StringBuffer("");
        Sensor sensor = applet.sensorMenu.getCurrentSensor();
        int sensorQualityValues = sensor.numQualityValues;
        boolean hasDataVersions = sensor.hasDataVersions;
        boolean hasUserDefinedArea = sensor.hasUserDefinedArea;

        String colRowName = sensor.navModel.getColName() + "/"
                          + sensor.navModel.getRowName();
        // limitset corresponds to date limits changed
        if (limitSet)
        {
            // check if scene list filter has been set
            if (sceneListFilterStatus)
            {
                // only show quality limit if it is greater than 0 and the
                // sensor supports it; also consider data version limits
                if ((minQuality > 0) && (sensorQualityValues > 0))
                    msg.append("Date, List & Quality Limits");
                else if (hasDataVersions && !dataVersion.equals("All"))
                    msg.append("Date, List & Version Limits");
                else if (hasUserDefinedArea && userAreaFilterStatus)
                {
                    if (gridColRowFilterSet)
                        msg.append("Various Limits");
                    else
                        msg.append("Date, List & Area Limits");
                }
                else
                {
                    if (gridColRowFilterSet)
                        msg.append("Various Limits");
                    else
                        msg.append("Date & List Limits");
                }
            }
            // scene list filter not set
            else
            {
                // see if quality filter has been set to a value greater than
                // 0 and the sensor has quality value; also consider data 
                // version limits
                if ((minQuality > 0) && (sensorQualityValues > 0))
                    msg.append("Date & Quality Limits");
                else if (hasDataVersions && !dataVersion.equals("All"))
                    msg.append("Date & Version Limits");
                else if (hasUserDefinedArea && userAreaFilterStatus)
                {
                    if (gridColRowFilterSet)
                        msg.append("Various Limits");
                    else
                        msg.append("Date & Area Limits");
                }
                else if (gridColRowFilterSet)
                {
                    msg.append("Date & ");
                    msg.append(colRowName);
                    msg.append(" Limits");
                }
                else if (downloadableFilterStatus)
                {
                    msg.append("Date Limits, Downloadable");
                }
                // date only set, so fill in date limits
                else
                {
                    msg = new StringBuffer("Limits: ");
                    if ((startMonth != 0) || (endMonth != 11))
                    {
                        msg.append(monthNames[startMonth]);
                        if (startMonth != endMonth)
                        {
                            msg.append("-");
                            msg.append(monthNames[endMonth]);
                        }
                        msg.append(", ");
                    }
                    msg.append(startYear);
                    if (startYear != endYear)
                    {
                        msg.append("-");
                        msg.append(endYear);
                    }
                }
            }
        }
        // date limits not set
        else
        {
            // check if scene list filter has been set
            if (sceneListFilterStatus)
            {
                // see if quality filter is set greater than 0 and the sensor
                // supports quailty; also consider data version limits
                if ((minQuality > 0) && (sensorQualityValues > 0))
                    msg.append("List & Quality Limits");
                else if (hasDataVersions && !dataVersion.equals("All"))
                    msg.append("List & Version Limits");
                else if (hasUserDefinedArea && userAreaFilterStatus)
                {
                    if (gridColRowFilterSet)
                        msg.append("Various Limits");
                    else
                        msg.append("List & Area Limits");
                }
                else if (gridColRowFilterSet)
                {
                    msg.append("List & ");
                    msg.append(colRowName);
                    msg.append(" Limits");
                }
                else if (downloadableFilterStatus)
                {
                    msg.append("List Limits, Downloadable");
                }
                // no quality or date, so only scene list enabled
                else
                    msg.append("Scene List Only");
            }
            // scene list filter not set
            else
            {
                // see if quality filter is set greater than 0 and the sensor
                // supports quality; also consider data version limits
                if ((minQuality > 0) && (sensorQualityValues > 0))
                    msg.append("Quality Limit: " + minQuality);
                else if (hasDataVersions && !dataVersion.equals("All"))
                    msg.append("Version Limit");
                else if (hasUserDefinedArea && userAreaFilterStatus)
                {
                    if (gridColRowFilterSet)
                    {
                        msg.append(colRowName);
                        msg.append(" & Area Limits");
                    }
                    else
                        msg.append("User Area Limit");
                }
                else if (gridColRowFilterSet)
                {
                    msg.append(colRowName);
                    msg.append(" Limit");
                }
                else if (downloadableFilterStatus)
                {
                    msg.append("Limits: Downloadable");
                }
                // no filter values set
                else
                {
                    msg.append("No Limits Set");
                }
            }
        }
        // display message in the status bar
        applet.statusBar.showSearchLimits(msg.toString());
    }

    // accept and save the the search limit data
    //------------------------------------------
    private void processDialogState(boolean saveState)
    {
        Sensor sensor = applet.sensorMenu.getCurrentSensor();

        // the date limits are being accepted, so save the current
        // selections
        int tmpStartYear = startYearChoice.getSelectedIndex() + rangeStartYear;
        int tmpEndYear = endYearChoice.getSelectedIndex() + rangeStartYear;
        int tmpStartMonth = startMonthChoice.getSelectedIndex();
        int tmpEndMonth = endMonthChoice.getSelectedIndex();

        // set status of scene list filter flag
        boolean tmpSceneListFilterStatus = filterToSceneList.isSelected();

        // set status of downloadable filter flag
        boolean tmpDownloadableFilterStatus = false;
        if (sensor.mightBeDownloadable)
        {
            tmpDownloadableFilterStatus = filterToDownloadable.isSelected();
        }

        // set status of user defined area flag.  If the user defined area
        // is not set, bring up the dialog box.
        boolean tmpUserAreaFilterStatus;
        if (filterToUserArea.isSelected())
        {
            if (applet.userDefinedAreaDialog.isUserDefinedAreaClosed())
            {
                tmpUserAreaFilterStatus = true;
            }
            else
            {
                // no valid area has been set, so uncheck the flag, and
                // bring up the user defined area dialog box to hint to the
                // user that something isn't quite right.
                tmpUserAreaFilterStatus = false;
                filterToUserArea.setSelected(false);
                // put it to the side of where the search limit dialog is
                // if it isn't already showing.
                if (!applet.userDefinedAreaDialog.isVisible())
                {
                    Point loc = this.getLocationOnScreen();
                    Dimension size = this.getToolkit().getScreenSize();
                    // if the dialog box far enough from the right side of
                    // the screen, put the user defined area box on the
                    // right side of this dialog box, otherwise put it on
                    // the left side
                    // 250 is the initial size of the search limit dialog
                    // 275 is the initial size of the user defined area
                    if ((size.width - 525 - loc.x) > 0)
                        loc.x += 250;
                    else
                        loc.x -= 275;
                    applet.userDefinedAreaDialog.setLocation(loc);
                    applet.userDefinedAreaDialog.setVisible(true);
                }
            }
        }
        else
        {
            tmpUserAreaFilterStatus = false;
        }

        // set the minimum image quality shown
        int tmpMinQuality;
        tmpMinQuality = qualityChoice.getSelectedIndex();
        tmpMinQuality = QUAL_RANGE - tmpMinQuality;

        // set the state of the data version filter
        String tmpDataVersion = "All";
        if (sensor.hasDataVersions)
        {
            int dataVersionIndex = dataVersionChoice.getSelectedIndex();
            if (dataVersionIndex <= 0)
            {
                tmpDataVersion = "All";
            }
            else
            {
                tmpDataVersion = dataVersionChoice.getSelectedItem().toString();
            }
        }

        // the grid col/row limits are being accepted, so save the current
        // selections
        int tmpStartGridCol = 0;
        int tmpEndGridCol = 0;
        int tmpStartGridRow = 0;
        int tmpEndGridRow = 0;
        
        if (sensor.hasGridColRowFilter)
        {
            // convert the choice selection to a valid gridCol & gridRow
            tmpStartGridCol = startGridColChoice.getSelectedIndex() + 
                                                    sensorMinimumColumn;
            tmpEndGridCol = endGridColChoice.getSelectedIndex() + 
                                                    sensorMinimumColumn;
            tmpStartGridRow = startGridRowChoice.getSelectedIndex() + 
                                                    sensorMinimumRow;
            tmpEndGridRow = endGridRowChoice.getSelectedIndex() + 
                                                    sensorMinimumRow;
        }
        
        int tmpCloudCoverValue = ccPercent.getCloudCover();
        
        // called by actionPerformed 
        if (saveState)
        {
            // read from temporary variables
            startYear = tmpStartYear;
            endYear = tmpEndYear;
            startMonth = tmpStartMonth;
            endMonth = tmpEndMonth;

            gridColRowFilterSet = false;
            if (sensor.hasGridColRowFilter)
            {
                startGridCol = tmpStartGridCol;
                endGridCol = tmpEndGridCol;
                startGridRow = tmpStartGridRow;
                endGridRow = tmpEndGridRow;

                gridColRowFilterSet = (startGridCol != sensorMinimumColumn)
                        || (endGridCol != sensorMaximumColumn)
                        || (startGridRow != sensorMinimumRow)
                        || (endGridRow != sensorMaximumRow);
            }

            sceneListFilterStatus = tmpSceneListFilterStatus;
            downloadableFilterStatus = tmpDownloadableFilterStatus;
            userAreaFilterStatus = tmpUserAreaFilterStatus;
            minQuality = tmpMinQuality;
            if (sensor.hasDataVersions)
                dataVersion = tmpDataVersion;
            cloudCoverValue = tmpCloudCoverValue;
        } 
        else
        {
            int cc = ccPercent.getCloudCover();
            numOfAvailScenes = getNumOfAvailScenesBySearchLimit(tmpStartYear,
                tmpEndYear, tmpStartMonth, tmpEndMonth, cc,tmpMinQuality, 
                tmpDataVersion, tmpSceneListFilterStatus,
                tmpDownloadableFilterStatus,
                tmpUserAreaFilterStatus,tmpStartGridCol,tmpEndGridCol,
                tmpStartGridRow,tmpEndGridRow);
        }
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();
        Sensor sensor = applet.sensorMenu.getCurrentSensor();

        if (command.equals("Ok") || command.equals("Apply"))
        {
            // accept date range 
            processDialogState(true);
             
            // if the limits have been cleared, clear the limitSet flag
            if (limitCleared)
                limitSet = false;
            else
            {
                // if the date range is the full range, consider it not set
                // (in case the user just set the cloud cover)
                if ((startYear != sensorStartYear) || (endYear != sensorEndYear)
                    || (startMonth != 0) || (endMonth != 11))
                    limitSet = true;
                else
                    limitSet = false;
            }

            limitCleared = false; 

            // call the configure routine in case the year range can now be
            // trimmed.  This is needed in cases where the user has a date
            // range set from a previous sensor that falls outside the current
            // sensor's range.  The years shown are artificially expanded until
            // the user selects a range compatible with this sensor or clears
            // the limits.
            configureForSensor(sensor);

            applyFilter();
   
            setApplyButtonState();
    
            // if ok button, hide the dialog
            if (command.equals("Ok"))
                setVisible(false);
        }
        else if (command.equals("Cancel"))
        {
            // hide the dialog if the cancel button is pressed
            setVisible(false);
            // restore the current limits in the dialog for the next time
            // it is displayed since any changes were cancelled
            startYearChoice.setSelectedIndex(startYear - rangeStartYear);
            endYearChoice.setSelectedIndex(endYear - rangeStartYear);
            startMonthChoice.setSelectedIndex(startMonth);
            endMonthChoice.setSelectedIndex(endMonth);
            ccPercent.setCloudCover(md.maxCloudCover);
            gridColRowFilterSet = false;
            if (sensor.hasGridColRowFilter)
            {
                startGridColChoice.setSelectedIndex(startGridCol - 
                                                sensorMinimumColumn);
                endGridColChoice.setSelectedIndex(endGridCol - 
                                                sensorMinimumColumn);
                
                startGridRowChoice.setSelectedIndex(startGridRow - 
                                               sensorMinimumRow);
                endGridRowChoice.setSelectedIndex((endGridRow - 
                                               sensorMinimumRow));

                gridColRowFilterSet = (startGridCol != sensorMinimumColumn)
                        || (endGridCol != sensorMaximumColumn)
                        || (startGridRow != sensorMinimumRow)
                        || (endGridRow != sensorMaximumRow);
            }

            // reset the limitCleared flag
            limitCleared = false;
            // if the date range is the full range for the sensor, consider it 
            // not set (in case the user just set the cloud cover)
            if ((startYear != sensorStartYear) || (endYear != sensorEndYear)
                || (startMonth != 0) || (endMonth != 11))
                limitSet = true;
            else
                limitSet = false;
            // return scene list filter flag to previous accepted state
            filterToSceneList.setSelected(sceneListFilterStatus);
            // return downloadable filter flag to previous accepted state
            filterToDownloadable.setSelected(downloadableFilterStatus);
            // return user defined are filter to previous accepted state
            filterToUserArea.setSelected(userAreaFilterStatus);
            // return the quality to its previous accepted value
            qualityChoice.setSelectedIndex(QUAL_RANGE - minQuality);
            // return the data version to its previous accepted value
            if (sensor.hasDataVersions)
            {
                if (dataVersion.equals("All"))
                    dataVersionChoice.setSelectedIndex(0);
                else
                {
                    dataVersionChoice.setSelectedItem(dataVersion);
                }
            }
        }
        else if (command.equals("Clear"))
        {
            // set the flags to indicate the clear limits button has been 
            // pushed
            limitCleared = true;
            limitSet = false;
            // update the display to the full range (not really enforced 
            // until accepted)
            startYearChoice.setSelectedIndex(sensorStartYear - rangeStartYear);
            endYearChoice.setSelectedIndex(sensorEndYear - rangeStartYear);
            startMonthChoice.setSelectedIndex(0);
            endMonthChoice.setSelectedIndex(11);
            ccPercent.setCloudCover(100);
            filterToSceneList.setSelected(false);
            filterToDownloadable.setSelected(false);
            filterToUserArea.setSelected(false);
            qualityChoice.setSelectedIndex(QUAL_RANGE - 0);
            gridColRowFilterSet = false;
            if (sensor.hasGridColRowFilter)
            {
                startGridColChoice.setSelectedIndex(sensorMinimumColumn - 
                                                    sensorMinimumColumn);
                endGridColChoice.setSelectedIndex(sensorMaximumColumn - 
                                                    sensorMinimumColumn);
                startGridRowChoice.setSelectedIndex(sensorMinimumRow - 
                                                    sensorMinimumRow); 
                endGridRowChoice.setSelectedIndex(sensorMaximumRow - 
                                                    sensorMinimumRow);
            }
            
            // error caused if this is run when Data Versions isn't up
            if (sensor.hasDataVersions)
                dataVersionChoice.setSelectedIndex(0);

            setApplyButtonState();

            //update the number of available scenes
            updateNumOfAvailScenes();
        }
        else
        {
            // must be a change to a selection
            updateYear();
        }
    }

    // Method to update the number of available scenes
    //------------------------------------------------
    public void updateNumOfAvailScenes()
    {
        processDialogState(false);
        if (numOfAvailScenes == 1)              
            availScenes.setText(numOfAvailScenes + " scene available");
        else
            availScenes.setText(numOfAvailScenes + " scenes available");
    }

    // Observer interface method that allows detecting when a
    // new area is displayed so the number of available scenes
    // can be updated
    //----------------------------------------------------------
    public void update(Observable ob, Object arg)
    {
        if (this.isVisible())
        {
            updateTOC();
        
            //update the number of available scenes
            updateNumOfAvailScenes();
        }
    }

    // method to copy the TOC array if user select an area on the map
    //---------------------------------------------------------------
    private void updateTOC()
    {
        Sensor sensor = applet.sensorMenu.getCurrentSensor();
        String tmpSensorName = sensor.sensorName;

        // Once  the user select an area
        // on the map the TOC array is copied
        if (savedGridCol != md.gridCol
            || savedGridRow != md.gridRow
            || !savedSensorName.equals(tmpSensorName)
            || savedSubCol != md.getSubCol()
            || savedSubRow != md.getSubRow()
            || savedResolution != md.pixelSize)
        {
            copyOfMosaicCells = md.copyTOC();
            savedGridCol = md.gridCol;
            savedGridRow = md.gridRow;
            savedSensorName = tmpSensorName;
            savedSubCol = md.getSubCol();
            savedSubRow = md.getSubRow();
            savedResolution = md.pixelSize;
        }
    }

    // method to count the number of available scenes in
    // a TOC array
    //------------------------------------------------
    private int getCountOfAvailScenes(TOC[] toc, int cellsToDisplay)
    {
        int sum = 0;
        int start = 0;
        int end = toc.length;
        // if there is only a single scene to be displayed, just loop over 
        // that cell (Note that if the cellsToDisplay is 1, still need to
        // loop over them all since ASTER 400m can be displaying data from
        // multiple cells due to the fractional row/column step)
        if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            start = md.getActiveCellIndex();
            end = start + 1;
        }
        for (int cellNum = start; cellNum < end; cellNum++)
        {
            TOC cell = toc[cellNum];

            // protect against invalid array checking
            if (cell.valid)
            {
                for (int num = 0; num < cell.scenes.length; num++)
                {
                    Metadata scene = cell.scenes[num];
                    if (scene.visible)
                            sum++;
                }
            }
        }
        return sum;
    }

    // method to get the number of available scenes by applying
    // search limits to a copy of TOC's(copyOfMosaicCells[]),so
    // users can know how many scenes will be available before
    // hit the "Apply" button on search limit dialog.
    //-----------------------------------------------------------
    private int getNumOfAvailScenesBySearchLimit(int startYear, int endYear,
                    int startMonth, int endMonth, int cloudCoverLimit,
                    int minQuality, String dataVersion,
                    boolean sceneListFilterStatus,
                    boolean downloadableFilterStatus,
                    boolean userAreaFilterStatus,
                    int startGridCol, int endGridCol, int startGridRow, 
                    int endGridRow)
    {

        // apply the date range limits to the TOCs
        md.applyDataLimit(copyOfMosaicCells, startYear, endYear, startMonth,
                          endMonth, cloudCoverLimit, minQuality, dataVersion,
                          userAreaFilterStatus, sceneListFilterStatus,
                          downloadableFilterStatus,
                          startGridCol, endGridCol, startGridRow, endGridRow);
        int num = getCountOfAvailScenes(copyOfMosaicCells, 
                                        md.getCellsToDisplay());
        return num;
    }
}
