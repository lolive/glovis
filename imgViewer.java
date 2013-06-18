// imgViewer.java implements the main applet class for the image viewer.
//
//-----------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.Container;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Observer;
import java.util.Observable;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;
import javax.swing.ToolTipManager;

public class imgViewer extends JApplet implements ActionListener, MouseListener,
    FocusListener, Observer, Printable
{
    public SensorMenu sensorMenu;// Select the current sensor object
    public JFrame mainWindow;          // top level window frame for the applet
    public ImagePane imgArea;          // The image pane object 
    public MosaicData md;              // mosaic data object
                                       // arrows & their events
    public SceneListPanel sceneListPanel;// panel containing the scene list
    public JScrollPane imgScroll;      // Scrolling area for main image canvas
    public JScrollPane locatorMapScroll;// ScrollPane for locator map
    public CloudCoverLimit maxCC;      // cloud cover limit widget
    private JPanel ccPanel;            // panel for the maxCC
    private CardLayout ccLayout;       // layout control for the ccPanel
    public NavigateDate navDate;       // date navigation widget
    private JPanel datePanel;          // panel for date nav controls
    private CardLayout dateLayout;     // layout control for the datePanel
    public SearchForSceneDialog searchForSceneDialog; // search for scene dialog
    public SearchLimitDialog searchLimitDialog; // search limits dialog
    public SceneListDialog sceneListDialog; // dialog for an expanded scene list
    public HideSceneDialog hideSceneDialog; // dialog for hiding scenes
    public UserDefinedAreaDialog userDefinedAreaDialog; // set area of interest
    public PointOfInterestDialog pointOfInterestDialog; // set point of interest
    public SearchForAddressDialog searchForAddressDialog;
                                            // search for address dialog
    public NDVIGraphDialog ndviGraphDialog; // dialog for displaying NDVI data 
                                            // in graph format.
    public StatusBar statusBar;        // status bar for the applet

    public Font normalFont;            // normal font to use
    public Font boldFont;              // bold font to use
    public Font smallFont;             // small font to use
    public Font largeBoldFont;         // bold large font to use

    private ArrowPane controls;        // widget for up, down, left, right
    private JButton dateIncButton;     // Date incr button -- forward in time
    private JButton dateDecButton;     // Date decrement button -- back in time
    private GridCellEntry gridCellEntry;// widget for entering grid column/row
    private LatLongEntry latLongEntry; // widget for entering lat/long
    private LocatorMap locatorMap;     // locator map (world view)
    private SceneInfo sceneInfo;       // scene info widget

    public Cursor crosshairCursor;     // cached crosshair cursor
    public Cursor moveCursor;          // cached move cursor
    public Cursor handCursor;          // cached hand cursor
    private Cursor waitCursor;         // cached wait cursor
    private Cursor defaultCursor;      // cached default cursor

    public MapLayerMenu mapLayerMenu;  // map layer type menu
    public ResolutionMenu resolutionMenu;// resolution selection menu
    public HelpMenu helpMenu;          // help menu
    public ToolsMenu toolsMenu;        // tools selection menu
    public FileMenu fileMenu;          // file selection menu

    public boolean showingWait;        // indicates the wait cursor is active
    private boolean showingBusy;       // indicates the busy indicator is active
    
    // FIXME - flag to enable simulating a slow network connection when 
    // using a fast connection.  Remove before final release.
    public boolean slowdown = false;
    public boolean verboseOutput = false;

    private boolean useMainWindowForDialogs = false; // flag whether the dialogs
                                   // should use the main window as the parent
    public boolean usingIE = false;    // flag for running on IE
    public boolean grantedPrivileges = false; // flag for the applet being 
                                       // granted privileges to escape the Java
                                       // sandbox
    private boolean initialized = false; // flag to show initialization has
                                         // completed
    private boolean limitCollectionMenu = false;  // flag indicating whether
                                   // to show just the specified mission(s)
    private boolean popOut = false;    // whether to put applet in pop-up


    // declare action classes for temporarily disabling the extra map layers
    //----------------------------------------------------------------------
    class EnableMapLayersAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            md.mapLayers.disableExtraMapLayers(false);
        }
    }
    class DisableMapLayersAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            md.mapLayers.disableExtraMapLayers(true);
        }
    }

    // applet initialize method
    //-------------------------
    public void init()
    {
        int imgViewSizeX = 650; // image display area width
        int imgViewSizeY = 650; // image display area height
        String tempSensor;
        String browserIsIE;
        Properties defaultProp = new Properties();
                   defaultProp.setProperty("startwith", "COMBINED");
        Properties gvProp = new Properties(defaultProp);
        String fontName = "SansSerif";
        try 
        {
            // if on Linux, use the Serif font instead since the SansSerif size
            // is messed up on many Linux systems (wrapped in a try block just
            // in case some Java VM doesn't implement the os.name property or
            // considers it a security issue - note haven't actually run into
            // such a case yet)
            if (System.getProperty("os.name").equals("Linux"))
            {
                fontName = "Serif";
                // also, the KDE window manager seems to crash if dialog boxes
                // are attached to a hidden frame instead of the main window,
                // so flag that the dialogs should be attached to the main
                // window
                useMainWindowForDialogs = true;
            }

            // get browser information
            browserIsIE = getParameter("browserIsIE");
            usingIE = false;
            if (browserIsIE.equals("true"))
                usingIE = true;
        }
        catch (Exception e) {}

        // get the fonts used
        normalFont = new java.awt.Font(fontName, Font.PLAIN, 12);
        boldFont = new java.awt.Font(fontName, Font.BOLD, 12);
        smallFont = new java.awt.Font(fontName, Font.PLAIN, 10);
        largeBoldFont = new java.awt.Font(fontName, Font.BOLD, 18);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setFont(normalFont);

        // read the properties file
        try
        {
            URL propertiesURL = new URL(getCodeBase(), "GloVis.Properties");
            InputStream is = propertiesURL.openStream();
            gvProp.load(is);
            // gvProp.list(System.out); // un-comment for debugging
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error reading GloVis properties");
        }

        // determine if the applet has been granted the privileges to escape
        // the Java sandbox
        try
        {
            // attempt to access a system property that would only be available
            // if the applet has been granted permission to get out of the 
            // sandbox
            String homedir = System.getProperty("user.home");

            // got here, so no security exception was thrown.  The applet must
            // have been granted privileges
            grantedPrivileges = true;
        }
        catch (Exception e)
        {
            // TODO - should a message be popped up telling the user that
            // some features will not be available since the applet was not
            // granted the needed privileges?
        }

        // get the current sensor
        tempSensor = getParameter("sensor").toUpperCase();
        // recognize either "ASTERVNIR" or "ASTERL2v" for ASTER
        if (tempSensor.equals("ASTERL2V"))
            tempSensor = "ASTERVNIR";

        // if tempSensor doesn't have a property, then it's not valid
        if (gvProp.getProperty(tempSensor) == null)
        {
            // revert to default
            tempSensor = gvProp.getProperty("startwith").toUpperCase();
            if (tempSensor == null || gvProp.getProperty(tempSensor) == null)
            {
                // properties file must be messed up, revert to default default
                tempSensor = defaultProp.getProperty("startwith");
            }
            if (gvProp.getProperty(tempSensor) == null)
            {
                // give up - let SensorMenu pick any "used" sensor as default
                // (which may happen anyway if tempSensor is hidden)
                tempSensor = null;
            }
        }

        // limit to just one mission?
        try
        {
            String limitMissions = getParameter("limitMissions");
            if (   limitMissions.equalsIgnoreCase("TRUE")
                || limitMissions.equalsIgnoreCase("YES"))
            {
                limitCollectionMenu = true;
            }
        }
        catch (Exception e)
        {
        }

        // create the sensor menu early so it can be used as the source of the
        // selected sensor
        try
        {
            sensorMenu = new SensorMenu(this, gvProp, tempSensor,
                                        limitCollectionMenu);
        }
        catch (Exception e)
        {
            throw new RuntimeException("ERROR: " + e);
        }

        // if user wants to revert to pop-out GloVis, create a mainWindow
        try
        {
            String popOutPreference = getParameter("popout");
            if (   popOutPreference.equalsIgnoreCase("TRUE")
                || popOutPreference.equalsIgnoreCase("YES"))
            {
                popOut = true;
            }
        }
        catch (Exception e)
        {
        }

        if (popOut)
        {
            // create a separate window to use for the applet
            final JFrame f = new JFrame("USGS Global Visualization Viewer");
            f.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent evt)
                {
                    f.setVisible(false);
                }
            });
            mainWindow = f;
            Image icon = getImage(getCodeBase(),"graphics/glovis.gif");
            mainWindow.setIconImage(icon);

            // set the default font for the browse viewer window
            mainWindow.setFont(normalFont);

            // show the message in the applet area of the browser page
            showAppletMessage(fontName);

            // make sure the applet info is visible in the browser so the
            // location can be obtained
            setVisible(true);
            // position the main window in the browser area
            Point loc = this.getLocationOnScreen();
            mainWindow.setLocation(loc);
        }
        else
        {
            // set the default font for the main window
            getContentPane().setFont(normalFont);

            getContentPane().setFocusable(true);
            getContentPane().addFocusListener(this);
        }


        // Create & set Controls, etc.
        // ---------------------------

        // get the cursors we need
        crosshairCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
        handCursor = new Cursor(Cursor.HAND_CURSOR);
        moveCursor = new Cursor(Cursor.MOVE_CURSOR);
        waitCursor = new Cursor(Cursor.WAIT_CURSOR);
        defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

        // create the status bar early so other components can set beginning
        // values in it
        statusBar = new StatusBar(this);

        // show a status bar message with the name of the applet
        statusBar.showStatus("USGS Browse Image Viewer Applet");

        // build the locator map
        locatorMap = new LocatorMap(this);
        locatorMap.setCursor(crosshairCursor);
        locatorMapScroll = new JScrollPane(locatorMap,
                                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,  
                                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);   
        locatorMapScroll.setBackground(Color.LIGHT_GRAY);
        // Adjust the scroll arrows to jump by 10 pixels per click.  The
        // default of 1 pixel is useless
        locatorMapScroll.getHorizontalScrollBar().setUnitIncrement(10);
        locatorMapScroll.getVerticalScrollBar().setUnitIncrement(10);
        locatorMap.setFocusable(false);
        locatorMapScroll.setFocusable(false);
        locatorMapScroll.setMaximumSize(new Dimension(240,400));
        // set an initial size to make things look okay until the real layout
        // is done
        locatorMapScroll.setMinimumSize(new Dimension(240,100));
        locatorMapScroll.setPreferredSize(new Dimension(240,240));

        // Build the image pane and scroll area
        imgArea = new ImagePane(this, locatorMap);
        imgArea.setBackground(Color.BLACK);

        imgScroll = new JScrollPane(imgArea,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        imgScroll.setSize(imgViewSizeX,imgViewSizeY);
        imgScroll.setBackground(Color.LIGHT_GRAY);
        // Adjust the scroll arrows to jump by 10 pixels per click.  The
        // default of 1 pixel is useless
        imgScroll.getHorizontalScrollBar().setUnitIncrement(10);
        imgScroll.getVerticalScrollBar().setUnitIncrement(10);

        // use simple scrolling mode, otherwise the cooperator logo and 
        // north arrow map layer do not draw correctly due to strange clipping
        // behavior when scrollbars are present
        imgScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        // cache a copy of the mosaic data reference
        md = imgArea.md;

        // add workers to monitor to the progress indicator
        statusBar.progress.addWorker(md);
        statusBar.progress.addWorker(md.imageLoader);
        statusBar.progress.addWorker(md.mapLayers);

        // create the area of interest dialog box
        userDefinedAreaDialog = new UserDefinedAreaDialog(getDialogParent(),
                                    this,md);

        // create the point of interest dialog box
        pointOfInterestDialog = new PointOfInterestDialog(getDialogParent(),
                                    this,md,
                                    md.mapLayers.getPointOfInterestMapLayer());
        
        // build the grid cell navigation area
        gridCellEntry = new GridCellEntry(this,md);

        // build the lat/long navigation widget
        latLongEntry = new LatLongEntry(this,md);

        // build the cloud cover limit widget
        maxCC = new CloudCoverLimit(this,md);

        // build the arrow pane for navigating
        controls = new ArrowPane(this);

        // build the Scene information display area
        sceneInfo = new SceneInfo(this,md);

        // build the date navigation widget
        navDate = new NavigateDate(this,md);

        // build a panel with the buttons to navigate through dates
        JPanel dateIncPanel = new JPanel();
        dateIncPanel.setLayout(new GridLayout(1,2));
        dateDecButton = new JButton("Prev Scene");
        dateDecButton.setMnemonic(KeyEvent.VK_P);
        dateDecButton.setToolTipText("Previous scene");
        dateDecButton.addActionListener(this);
        dateIncButton = new JButton("Next Scene");
        dateIncButton.setMnemonic(KeyEvent.VK_N);
        dateIncButton.setToolTipText("Next scene");
        dateIncButton.addActionListener(this);
        dateIncPanel.add(dateDecButton);
        dateIncPanel.add(dateIncButton);

        // add the date navigation controls to a card layout so they can be 
        // made invisible without changing the layout
        JPanel dateControlsPanel = new JPanel();
        dateControlsPanel.setLayout(
                new BoxLayout(dateControlsPanel, BoxLayout.Y_AXIS));
        dateControlsPanel.add(navDate);
        dateControlsPanel.add(Box.createVerticalStrut(3));
        dateControlsPanel.add(dateIncPanel);
        
        datePanel = new JPanel();
        dateLayout = new CardLayout();
        datePanel.setLayout(dateLayout);
        datePanel.add("visible", dateControlsPanel);
        datePanel.add("notVisible", new JPanel());

        // don't allow the date panel to grow vertically
        Dimension datePanelSize = datePanel.getPreferredSize();
        datePanelSize.width = 100;
        datePanel.setMinimumSize(datePanelSize);
        datePanelSize.width = 240;
        datePanel.setMaximumSize(datePanelSize);

        // build the scene list widget
        sceneListPanel = new SceneListPanel(this); 

        // add a menu bar to the applet
        JMenuBar menuBar = new JMenuBar();
        if (popOut)
        {
            mainWindow.setJMenuBar(menuBar);
        }
        else
        {
            this.setJMenuBar(menuBar);
        }

        // add the sensor menu to the menu bar
        sensorMenu.setToolTipText("Collection menu");
        menuBar.add(sensorMenu);

        // add the resolution selection menu
        resolutionMenu = new ResolutionMenu(this,md);
        resolutionMenu.setToolTipText("Resolution menu");
        menuBar.add(resolutionMenu);

        // add the map layer menu to the menu bar
        mapLayerMenu = new MapLayerMenu(imgArea.mapLayers, this);
        mapLayerMenu.setToolTipText("Map Layer menu");
        menuBar.add(mapLayerMenu);

        // add the tools menu
        toolsMenu = new ToolsMenu(this);
        toolsMenu.setToolTipText("Tools menu");
        menuBar.add(toolsMenu);

        // if the applet has been granted privileges, add a File menu
        if (grantedPrivileges)
        {
            fileMenu = new FileMenu(this);
            fileMenu.setToolTipText("File menu");
            menuBar.add(fileMenu);
        }

        // add the help menu to the menu bar
        helpMenu = new HelpMenu(this, "Help");
        helpMenu.setToolTipText("Help menu");
        
        // Note that the MenuBar setHelpMenu method is not implemented in the
        // plug-in, so just add the help menu like a regular menu item
        menuBar.add(helpMenu);

        // add the mosaic data observers
        md.addObserver(sceneInfo);
        if (fileMenu != null)
            md.addObserver(fileMenu);
        md.addObserver(navDate);
        md.addObserver(this);
        md.addObserver(gridCellEntry);
        md.addObserver(latLongEntry);
        md.addObserver(locatorMap);
        md.addObserver(imgArea);
        md.addObserver(sceneListPanel);

        // make a panel for the controls on the left side of the applet
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        // add the locator map to the control panel
        controlPanel.add(locatorMapScroll);

        // add the grid cell and lat/long entry components to the control
        // panel
        controlPanel.add(gridCellEntry);
        controlPanel.add(latLongEntry);

        // make a panel for the maxCC that contains a card layout so the
        // maxCC can be hidden without affecting the placement of the arrows
        ccPanel = new JPanel();
        ccLayout = new CardLayout();
        ccPanel.setLayout(ccLayout);
        ccPanel.add("visible", maxCC);
        ccPanel.add("notVisible", new JPanel());

        // add the cloud cover panel and arrow panel to a temporary panel, then
        // to the real control panel.  This is to work around an issue in
        // IE 5.5 where directly inserting them into the control panel
        // would make the control panel size incorrect. Also provide a little
        // extra space above and below the panel using insets.
        JPanel arrowPanel = new JPanel();
        arrowPanel.setLayout(new BoxLayout(arrowPanel, BoxLayout.X_AXIS));
        arrowPanel.add(ccPanel);
        arrowPanel.add(controls);
        arrowPanel.add(Box.createHorizontalStrut(5));

        // don't allow the arrow panel to grow vertically
        Dimension size = arrowPanel.getPreferredSize();
        size.width = 100;
        arrowPanel.setMinimumSize(size);
        size.width = 240;
        arrowPanel.setMaximumSize(size);

        controlPanel.add(Box.createVerticalStrut(3));
        controlPanel.add(arrowPanel);
        controlPanel.add(Box.createVerticalStrut(3));

        // add the scene info and date controls to the control panel
        controlPanel.add(sceneInfo);
        controlPanel.add(Box.createVerticalStrut(3));
        controlPanel.add(datePanel);
        controlPanel.add(Box.createVerticalStrut(3));

        // add the scene list to the control panel
        controlPanel.add(sceneListPanel);

        // don't allow the control panel to grow in width
        controlPanel.setPreferredSize(new Dimension(240,imgViewSizeY));
        controlPanel.setMinimumSize(new Dimension(240,imgViewSizeY));
        controlPanel.setMaximumSize(new Dimension(240,2000));

        controlPanel.add(Box.createHorizontalStrut(240));

        // make a center panel for the control and image panels and use a
        // grid bag layout
        JPanel centerPanel = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        centerPanel.setLayout(gbl);

        // add the control panel to the frame panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(controlPanel,gbc);

        // add the image scrolling area to the main window, making it 
        // resize as the applet window changes size
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 100;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(imgScroll,gbc);

        // make a panel for the whole frame
        JPanel framePanel = new JPanel();
        framePanel.setLayout(new BorderLayout());

        // add the center panel and status bar to the applet
        framePanel.add(centerPanel,"Center");
        framePanel.add(statusBar,"South");
        framePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // repaint the applet when notification area expands and collapses
        // FIXME: still not repainting the applet in popOut mode
        getContentPane().addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                repaint();
            }
            public void componentMoved(ComponentEvent e)
            {
                repaint();
            }
        });

        if (popOut)
        {
            mainWindow.getContentPane().add(framePanel);

            // show the main window
            mainWindow.validate();
            mainWindow.setSize(800,650);
            mainWindow.setVisible(true);
        }
        else
        {
            getContentPane().add(framePanel);
        }

        // create the search for scene dialog box
        searchForSceneDialog = new SearchForSceneDialog(getDialogParent(),this);

        // create the search limit dialog box
        searchLimitDialog = new SearchLimitDialog(getDialogParent(),this,md);
        
        // create the search for address dialog box
        searchForAddressDialog = new SearchForAddressDialog(getDialogParent(),
                                     this);

        // notify the search limit dialog box when something changes to
        // the available scene count can be updated
        md.addObserver(searchLimitDialog);

        // create the scene list dialog box
        sceneListDialog = new SceneListDialog(getDialogParent(),this,md);
        md.addObserver(sceneListDialog);

        // create the hide scene list dialog box
        hideSceneDialog = new HideSceneDialog(getDialogParent(),this,md);
        md.addObserver(hideSceneDialog);

        // create the NDVI graph dialog box
        ndviGraphDialog = new NDVIGraphDialog(getDialogParent(),this,md);
        md.addObserver(ndviGraphDialog);
        
        // configure the keystroke to toggle the shapefile map layers on/off
        imgArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "disable");
        imgArea.getActionMap().
                put("disable", new imgViewer.DisableMapLayersAction());
        imgArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "enable");
        imgArea.getActionMap().
                put("enable", new imgViewer.EnableMapLayersAction());

        // set the tool tips to 6 seconds (from the default of 4) to allow
        // longer to read the sensor descriptions
        ToolTipManager.sharedInstance().setDismissDelay(6000);

        initialized = true;
    }

    // method to show the message in the applet area of the browser web page
    //----------------------------------------------------------------------
    private void showAppletMessage(String fontName)
    {
        // set the number of lines in the message area of the applet
        int messageLines = 8;

        // allocate space for the message labels to display
        JLabel[] lines = new JLabel[messageLines];
        int currLine = 0;

        // set a grid layout of the correct size
        getContentPane().setLayout(new GridLayout(messageLines,1));

        // add blank lines to maintain correct spacing
        lines[currLine] = new JLabel("The Browse Image Viewer application " 
            + "should open in a separate window.");
        currLine++;

        lines[currLine] = new JLabel();
        currLine++;

        lines[currLine] = new JLabel("The web browser has "
            + "to remain open to this page to use the application window.");
        currLine++;

        lines[currLine] = new JLabel();
        currLine++;

        lines[currLine] = new JLabel("Click the mouse in this " 
            + "area to show the application window if it is not visible.");
        lines[currLine].setFont(boldFont);
        currLine++;

        lines[currLine] = new JLabel();
        currLine++;

        lines[currLine] = new JLabel("NOTE: Some browsers and 'Add-on' "
            + "security products that block pop-up windows could prevent "
            + "the shopping cart,");
        Font italicsFont = new java.awt.Font(fontName, Font.ITALIC, 12);
        lines[currLine].setFont(italicsFont);
        lines[currLine].setForeground(Color.RED);
        currLine++;

        lines[currLine] = new JLabel("metadata, help, tools, and image windows "
            + "from appearing in GloVis.  See the User Guide "
            + "for more information.");
        lines[currLine].setFont(italicsFont);
        lines[currLine].setForeground(Color.RED);
        currLine++;

        // add the lines to the display and listen for mouse events so the user
        // clicking on the browser applet area can be detected
        for (int i = 0; i < lines.length; i++)
        {
            getContentPane().add(lines[i]);
            lines[i].addMouseListener(this);
        }
    }

    // method to start the applet
    //---------------------------
    public void start() 
    {
        if (!initialized)
        {
            // abort
            throw new RuntimeException("Applet initialization failed.");
        }

        // read the starting scene id if it was provided
        String startingSceneID = getParameter("sceneid");

        // default the starting location in case the parameters were 
        // not passed in
        double startLat = 43.55;
        double startLong = -96.7;

        // Get parameters passed to the applet that define the starting
        // position
        try
        {
            String paramLat = getParameter("lat");
            String paramLon = getParameter("lon");
            if (paramLon == null) // if the lon parameter wasn't given
            {
                // We go back and forth on lon vs long, so try once more
                paramLon = getParameter("long");
            }

            // Convert String to Double (may throw NumberFormatException)
            // and convert that to native double
            if (paramLat != null && paramLat != "undefined")
            {
                startLat = Double.valueOf(paramLat).doubleValue();
            }
            if (paramLon != null && paramLon != "undefined")
            {
                startLong = Double.valueOf(paramLon).doubleValue();
            }
        }
        catch (Exception e)
        {
            // only print an error if a starting scene ID wasn't provided
            if (startingSceneID == null || startingSceneID == "undefined")
                System.out.println("exception reading lat/lon parameters");
        }

        // search for the starting scene if one was specified
        Metadata scene = null;
        if (startingSceneID != null && startingSceneID != "undefined")
            scene = searchForSceneDialog.searchForScene(
                        sensorMenu.getCurrentSensor(), startingSceneID);

        // initialize the locator map and mosaic data
        locatorMap.initialize(startLat,startLong);
        md.initialize(startLat,startLong);

        // if a starting scene was specified, go to it
        if (scene != null)
            md.showScene(scene);
        else if (startingSceneID != null && startingSceneID != "undefined")
        {
            // the scene wasn't found, so display a message box indicating
            // it wasn't found - relative to browser applet area even if
            // using popOut because this gets displayed before mainWindow
            JOptionPane.showMessageDialog(getContentPane(),
                    "The scene " + startingSceneID + " was not found",
                    "Scene not found",
                    JOptionPane.ERROR_MESSAGE);
        }
        
        // set the focus on the first simple component
        if (popOut)
        {
            maxCC.requestFocus();
        }
        else
        {
            requestFocusInWindow();
        }
    }

    // method to stop the applet
    //--------------------------
    public void stop()
    {
        if (popOut)
        {
            // hide the main window before starting to destroy stuff
            mainWindow.setVisible(false);
        }

        // stop the threads that are running
        md.killThread();
        md.mapLayers.killThread();

        // dispose of the dialog boxes
        searchForSceneDialog.dispose();
        searchForSceneDialog = null;
        searchLimitDialog.dispose();
        searchLimitDialog = null;
        sceneListDialog.dispose();
        sceneListDialog = null;
        hideSceneDialog.dispose();
        hideSceneDialog = null;
        ndviGraphDialog.dispose();
        ndviGraphDialog = null;
        userDefinedAreaDialog.dispose();
        userDefinedAreaDialog = null;
        pointOfInterestDialog.dispose();
        pointOfInterestDialog = null;
        searchForAddressDialog.dispose();
        searchForAddressDialog = null;

        md.mapLayers.cleanup();

        // call cleanup routines for any classes that have references to 
        // images since the resources allocated to those images are not
        // released unless the images are manually flushed
        controls.cleanup();
        locatorMap.cleanup();
        imgArea.cleanup();
        md.cleanup();

        if (popOut)
        {
            // dispose of the main window
            mainWindow.getIconImage().flush();
            mainWindow.dispose();
        }

        // let the parent class have a chance to stop anything it needs to stop
        super.stop();
    }

    // Handles Button events
    // ---------------------
    public void actionPerformed(ActionEvent event) 
    {
        String command = event.getActionCommand();

        // handle the time navigation buttons
        if (command.equals("Prev Scene"))
            md.sceneFilter.prevDate();
        else if (command.equals("Next Scene"))
            md.sceneFilter.nextDate();
    }     

    // method to return the correct JFrame to act as the parent to dialogs.
    // Using a hidden frame crashes some versions of KDE on Linux, so the 
    // dialogs will not be able to be minimized on Linux.
    //--------------------------------------------------------------------
    public JFrame getDialogParent()
    {
        if (popOut) // not using useMainWindowForDialogs at all??
            return mainWindow;
        else
            return new JFrame();
    }

    // method to return the current location, where dialogs should display
    public Point getDialogLoc()
    {
        if (popOut)
            return this.mainWindow.getLocationOnScreen();
        else
            return this.getLocationOnScreen();
    }

    // method to return the parent for dialogs created on-the-fly
    // JOptionPane and JFileChooser methods just need a Container, not a Frame
    // and mainWindow (JFrame) and this (JApplet) are subclasses of Container
    public Container getDialogContainer()
    {
        if (popOut)
            return mainWindow;
        else
            return this;
    }

    // method to update the state of the cursor and busy indicator based
    // on the state of the loading threads
    //---------------------------------------------------------------------
    public synchronized void updateBusyIndicators()
    {
        boolean showWait = md.isUnstableTOC();
        boolean showBusy = md.isBusy() || md.mapLayers.isBusy();

        // update the cursor being shown
        if (showWait)
        {
            if (!showingWait)
                imgArea.setCursor(waitCursor);
        }
        else
        {
            if (showingWait)
                imgArea.setCursor(imgArea.currentCursor);
        }
        showingWait = showWait;

        // update the state of the busy indicator
        if (showBusy)
        {
            if (!showingBusy)
                statusBar.progress.setBusy();
        }
        else
        {
            if (showingBusy)
                statusBar.progress.clearBusy();
        }
        showingBusy = showBusy;
    }

    // Method for the Observer interface
    //----------------------------------
    public void update(Observable ob, Object arg)
    {
        Metadata scene = md.getCurrentScene();

        if (scene != null)
        {
            // enable/disable the buttons to move forward in time depending
            // on whether there are any scenes after this one
            if (md.sceneFilter.isNextDateAvailable())
                dateIncButton.setEnabled(true);
            else
                dateIncButton.setEnabled(false);

            // enable/disable the buttons to move forward in time depending
            // on whether there are any scenes after this one
            if (md.sceneFilter.isPrevDateAvailable())
                dateDecButton.setEnabled(true);
            else
                dateDecButton.setEnabled(false);
        }
        else
        {
            // disable widgets that depend on a scene being selected
            dateDecButton.setEnabled(false);
            dateIncButton.setEnabled(false);
        }

        // control the visibility of the max cloud cover component
        if (sensorMenu.getCurrentSensor().hasCloudCover)
            ccLayout.show(ccPanel,"visible");
        else
            ccLayout.show(ccPanel,"notVisible");

        // control the visibility of the date navigation component
        if (sensorMenu.getCurrentSensor().hasAcqDate)
            dateLayout.show(datePanel,"visible");
        else
            dateLayout.show(datePanel,"notVisible");
    }

    // method to return the list of available sensors
    //-----------------------------------------------
    public Sensor[] getSensors()
    {
        return sensorMenu.getSensors();
    }

    // mouse pressed listener to show the main window when the browser
    // window is clicked on
    //----------------------------------------------------------------
    public void mousePressed(MouseEvent e)
    {
        // only show the main window if the applet has finished initializing
        // it.  Otherwise, it may be shown before it is fully created and 
        // not work properly.
        if (initialized)
        {
            if (popOut)
            {
                mainWindow.setVisible(true);
                mainWindow.toFront();
                mainWindow.setState(JFrame.NORMAL);
            }
        }
    }

    // dummy event handlers for unneeded mouse listener events
    //--------------------------------------------------------
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}

    // dummy event handlers for unneeded focus listener events
    //--------------------------------------------------------
    public void focusGained(FocusEvent e) {
        requestFocusInWindow();
    }
    public void focusLost(FocusEvent e) {}

    // method to print the main window of the applet
    //----------------------------------------------
    public void print()
    {
        // set up the printer job
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(this);

        // get the page format options from the user
        PageFormat defaultPage = printJob.defaultPage();
        PageFormat pf = printJob.pageDialog(defaultPage);
        // if the page format from the page dialog hasn't changed from the
        // default, the user cancelled
        if (pf != defaultPage)
        {
            // get the print job options from the print dialog
            if (printJob.printDialog())
            {
                // not cancelled, so print the applet window
                try
                {
                    printJob.print();
                }
                catch (Exception ex) { }
            }
        }
    }

    // printable interface method to print the applet content
    //-------------------------------------------------------
    public int print(Graphics g, PageFormat pf, int pageIndex)
        throws PrinterException
    {
        // only a single page is available to print, so if anything past the
        // 0 index, return that the page doesn't exist
        if (pageIndex >= 1)
            return Printable.NO_SUCH_PAGE;

        // Kind of a hack to be able to handle either the embedded JApplet
        // or the popped-out mainWindow JFrame - just print the content pane.
        Container myContentPane;
        if (popOut)
        {
            myContentPane = mainWindow.getContentPane();
        }
        else
        {
            myContentPane = getContentPane();
        }

        // determine how much the image needs to be scaled to fit on the page
        Dimension size = myContentPane.getSize();
        double scale = pf.getImageableWidth() / size.width;
        double hScale = pf.getImageableHeight() / size.height;
        if (hScale < scale)
            scale = hScale;

        // disable double buffering to improve the print quality for images
        RepaintManager currentManager
            = RepaintManager.currentManager(myContentPane);
        currentManager.setDoubleBufferingEnabled(false);

        // set the location to start printing on the page and the correct scale
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        g2.scale(scale, scale);

        // paint the graph on the printed page
        myContentPane.paint(g2);

        // re-enable double buffering
        currentManager.setDoubleBufferingEnabled(true);

        // indicate the page exists
        return Printable.PAGE_EXISTS;
   }
}
