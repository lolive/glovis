// NDVIGraphDialog.java implements a dialog to display the NDVI data in
// line graph
//---------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JEditorPane;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JScrollPane;
// WORKAROUND for JEditorPane bug in 1.6.0_22
// (see http://kr.forums.oracle.com/forums/thread.jspa?threadID=1997861)
import javax.swing.text.html.parser.ParserDelegator;

public class NDVIGraphDialog extends JDialog implements WindowListener,
    Observer, ActionListener, ComponentListener, HyperlinkListener
{
    private MosaicData md;          // reference to the MosaicData object
    private imgViewer applet;       // reference to the main applet
    private JCheckBox[] checkBoxes; // checkbox for each landcover
    private JButton defaultButton;  // set default button
    private JEditorPane landcoverHelp;
    private JScrollPane landcoverHelpScrollPane;
    private String[] landcoverName = {"DECIDUOUS FOREST","EVERGREEN FOREST",
                                      "HERB. GRASSLANDS","HERB. WETLANDS",
                                      "MIXED FOREST","PASTURE HAY","SHRUBLAND",
                                      "ROW CROPS","SMALL GRAINS",
                                      "TRANSITIONAL","WOODY WETLANDS",
                                      "Cultivated Crops",
                                      "Deciduous Forest",
                                      "Evergreen Forest",
                                      "Herb. Grasslands",
                                      "Herb. Wetlands",
                                      "Mixed Forest",
                                      "Pasture/Hay",
                                      "Shrub/Scrub",
                                      "Woody Wetlands"
                                     };
                                      // sorted in alphabetical order.
    private NDVILineGraph ndviLineGraph; // object that has methods
    private boolean [] savedSelectedLandcover; // save the state of the seleted
                                        // landcovers 
    
    // Default display size
    static final int WIDTH = 500;
    static final int HEIGHT = 430;
    static final int MIN_WIDTH = 500;
    static final int MIN_HEIGHT = 430;
    
    // Constructor for the NDVI Graph dialog
    //--------------------------------------
    public NDVIGraphDialog(JFrame parent, imgViewer applet, MosaicData md)
    {
        super(parent,"NDVI Graph",false);
        this.applet = applet;
        this.md = md;
        new ParserDelegator(); // WORKAROUND for JEditorPane bug in 1.6.0_22
      
        // add the ndviLineGraph to the dialog
        ndviLineGraph = new NDVILineGraph(applet,md,this,landcoverName);
        JPanel ndviGraphPanel = new JPanel();
        ndviGraphPanel.setLayout(new BorderLayout());
        ndviGraphPanel.add(ndviLineGraph, "Center");

        JPanel buttonPanel = new JPanel();

        JButton printButton = new JButton("Print");
        printButton.setMnemonic(KeyEvent.VK_P);
        printButton.setToolTipText("Print NDVI Line Graph");
        printButton.addActionListener(this);
        buttonPanel.add(printButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close NDVI Line Graph");
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);

        ndviGraphPanel.add(buttonPanel,"South");

        // create the landcover panel
        JPanel landcoverPanel = new JPanel();
        landcoverPanel.setLayout(new GridLayout(12,2));
        landcoverPanel.setToolTipText("Landcover Selecting");

        // create and set the checkboxes for the landCover.
        checkBoxes = new JCheckBox[landcoverName.length];
        for (int i = 0; i <landcoverName.length ; i++)
        {
            // We have two different lists, starting at position 0
            // and then at position 11
            // add a label to indicate the difference
            // (this also makes each list come out even)
            if (i == 0)
            {
                landcoverPanel.add(new JLabel("2008 and prior"));
            }
            if (i == 11)
            {
                landcoverPanel.add(new JLabel(""));
                landcoverPanel.add(new JLabel(""));
                landcoverPanel.add(new JLabel("2009 forward"));
            }
            checkBoxes[i] = new JCheckBox(landcoverName[i]+": (0)");
            checkBoxes[i].setEnabled(false);
            checkBoxes[i].setToolTipText("LandCover "+landcoverName[i]);
            landcoverPanel.add(checkBoxes[i]);
        }
        
        buttonPanel = new JPanel();
        defaultButton = new JButton("Set Default");
        defaultButton.addActionListener(this);
        defaultButton.setMnemonic(KeyEvent.VK_S);
        defaultButton.setToolTipText("Set default landcovers");
        buttonPanel.add(defaultButton);

        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close NDVI Line Graph");
        buttonPanel.add(closeButton);
        
        // create select land cover main panel
        JPanel landcoverMainPanel = new JPanel();
        landcoverMainPanel.setLayout(new BorderLayout());
        landcoverMainPanel.add(landcoverPanel,BorderLayout.NORTH);
        landcoverMainPanel.add(buttonPanel,BorderLayout.SOUTH); 
        
        // create Help panel contents
        landcoverHelp = new JEditorPane();
        landcoverHelp.setEditable(false);
        try
        {
            URL ndviHelpUrl = new URL(applet.getCodeBase(), "LandcoverHelp.html");
            landcoverHelp.setPage(ndviHelpUrl.toString());
        }
        catch (MalformedURLException e)
        {
            System.err.println("Malformed URL for NDVI Help");
        }
        catch (IOException e)
        {
            System.err.println("Cannot read NDVI Help URL");
        }
        catch (Exception e)
        {
            System.err.println("Error reading NDVI Help URL");
        }
        landcoverHelp.addHyperlinkListener(this); // hyperlinkUpdate() below
        landcoverHelpScrollPane = new JScrollPane(landcoverHelp);
        landcoverHelpScrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        landcoverHelpScrollPane.setMinimumSize(new Dimension(10,10));
        landcoverHelpScrollPane.setPreferredSize(new Dimension(WIDTH,300));

        // buttons for Help panel
        buttonPanel = new JPanel();
        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close NDVI Line Graph");
        buttonPanel.add(closeButton);

        // Create tab pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Add the three panels to the tab pane 
        tabbedPane.addTab("Line Graph", ndviGraphPanel);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_L);
        tabbedPane.setToolTipTextAt(0, "Line graph");

        tabbedPane.addTab("Select Landcover",landcoverMainPanel);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_S);
        tabbedPane.setToolTipTextAt(1, "Select Landcover");

        tabbedPane.addTab("FAQ", landcoverHelpScrollPane);
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_F);
        tabbedPane.setToolTipTextAt(1, "Frequently Asked Questions about NDVI");

        // add the tab to the dialog box
        getContentPane().add(tabbedPane);
        
        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(WIDTH,HEIGHT);
        
        // request the window events
        addWindowListener(this);

        //request the component events
        addComponentListener(this);
    }

    // display hyperlinks
    //-------------------
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            applet.getAppletContext().showDocument(e.getURL(), "_blank");
        }
    }
    
    // set the min. size of the dialog window
    //---------------------------------------
    public void componentResized(ComponentEvent e)
    {
        int width = getWidth();
        int height = getHeight();
        
        boolean resize = false;

        if (width < MIN_WIDTH)
        {
            resize = true;
            width = MIN_WIDTH;
        }
        
        if (height < MIN_HEIGHT)
        {
            resize = true;
            height = MIN_HEIGHT;
        }

        if (resize)
        {
            setSize(width, height);
        }
    }
   
    // dummy component event handlers for events that do not need handling
    //---------------------------------------------------------------------
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentHidden(ComponentEvent e) {}
        
    // method to handle the windowClosing event
    //-----------------------------------------
    public void windowClosing(WindowEvent e)
    {
        setVisible(false);
    }

    // dummy window event handlers for events that do not need handling
    //-----------------------------------------------------------------
    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) { }
    
    // set the checkBoxes to the 5 default landcovers for the selected scene
    //----------------------------------------------------------------------
    public void setDefault(String landcoverName)
    {
        String [] landcoverLabel = landcoverName.split(":");
        int numTypes = checkBoxes.length;
        // Determine which item has changed state
        for (int i = 0; i < numTypes; i++)
        {
            String checkBoxLabel = checkBoxes[i].getText();
            String [] checkBoxLabelTemp;
            checkBoxLabelTemp = checkBoxLabel.split(":");
            // find the checkbox that is the same one as the landcover
            if (landcoverLabel[0].equals(checkBoxLabelTemp[0]))
            {
                checkBoxes[i].setEnabled(true);
                checkBoxes[i].setSelected(true);
                checkBoxes[i].setText(landcoverName);
            }
        }
    }
    
    // save the landcover that is selected
    //------------------------------------
    private void saveLandcoverSelected()
    {
        int numTypes = checkBoxes.length;
        savedSelectedLandcover = new boolean[checkBoxes.length];
        for (int i =0; i < numTypes; i++)
        {
            savedSelectedLandcover[i] = checkBoxes[i].isSelected();
        }
    }

    // clear the landcover check boxes that are set and set the default
    //-----------------------------------------------------------------
    public void clearLandcoverSelected()
    {
        int numTypes = checkBoxes.length;
        for (int i = 0; i < numTypes; i++)
        {
            String checkBoxLabel = checkBoxes[i].getText();
            String [] checkBoxLabelTemp;
            checkBoxLabelTemp = checkBoxLabel.split(":");
            checkBoxes[i].setEnabled(false);
            checkBoxes[i].setSelected(false);
            checkBoxes[i].setText(checkBoxLabelTemp[0]+": (0)");
        }

        // set the default landcover
        ndviLineGraph.setDefaultLandcover();
    }
    
    // return true if any of the landcover types are selected
    //-------------------------------------------------------
    public boolean isLandcoverSelected()
    {
       int numTypes = checkBoxes.length;
       for (int i = 0; i < numTypes; i++)
       {
          if (checkBoxes[i].isSelected())
          {
              return true;
          }
       }
       return false;
    }
    
    // get the color for this landcover. We want the same landcover to always
    // use the same color.
    //-----------------------------------------------------------------------
    public Integer getColors(String landcoverName)
    {
        String [] landcoverLabel = landcoverName.split(":");
        int numTypes = checkBoxes.length;
        int count = 0;
        int [] colors = new int[checkBoxes.length];
        for (int i = 0; i < numTypes; i++)
        {
            String checkBoxLabel = checkBoxes[i].getText();
            String [] checkBoxLabelTemp;
            checkBoxLabelTemp = checkBoxLabel.split(":");
            if (landcoverLabel[0].equals(checkBoxLabelTemp[0]))
            {
                if (checkBoxes[i].isSelected())
                {
                    return new Integer(i); 
                }
            }
        }
        return new Integer(0);
    }

    // set the landcover if it isn't already set
    //------------------------------------------
    public boolean setLandcover(String landcoverName)
    {   
        String [] landcoverLabel = landcoverName.split(":");
        int numTypes = checkBoxes.length;
        // Determine which item has changed state
        for (int i = 0; i < numTypes; i++)
        {
            String checkBoxLabel = checkBoxes[i].getText();
            String [] checkBoxLabelTemp;
            checkBoxLabelTemp = checkBoxLabel.split(":");
            if (landcoverLabel[0].equals(checkBoxLabelTemp[0]))
            {
                if (checkBoxes[i].isSelected())
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    // set the available landcover 
    //----------------------------
    public void setAvailLandcover(String landcoverName)
    {
        int numTypes = checkBoxes.length;
        String [] landcoverLabel = landcoverName.split(":");
        
        // Determine which item has changed state
        for (int i = 0; i < numTypes; i++)
        {
            String checkBoxLabel = checkBoxes[i].getText();
            String [] checkBoxLabelTemp;
            checkBoxLabelTemp = checkBoxLabel.split(":");
            if (landcoverLabel[0].equals(checkBoxLabelTemp[0]))
            {
                checkBoxes[i].setEnabled(true);
                checkBoxes[i].setText(landcoverName);
            }
        }
    }

    // set the saved landcover
    //------------------------
    public void restoreLandcoverSelected()
    {
        int numTypes = checkBoxes.length;
        for(int i = 0; i < numTypes; i++)
        {
            checkBoxes[i].setSelected(savedSelectedLandcover[i]);
        }
    }
    
    // Method to update the data read for display
    //-------------------------------------------
    private void updateData()
    {
        if (isVisible())
        {
            Sensor currSensor = applet.sensorMenu.getCurrentSensor();
            Metadata currentScene = md.getCurrentScene();

            boolean dataAvailable = ndviLineGraph.isDataAvailable();
            
            // Only load new graph if a change was either the gridCol, gridRow
            // or year.
            if (currSensor.hasNdviLineGraph)
            {
                Metadata scene = ndviLineGraph.getScene();
                Sensor sensor = ndviLineGraph.getSensor();

                if ((scene == null) || (currentScene == null)
                    || ((currentScene.gridCol != scene.gridCol) 
                        || (currentScene.gridRow != scene.gridRow))
                    || (currSensor != sensor) || (!dataAvailable))
                {
                    // save the landcover selected
                    if (isLandcoverSelected())
                        saveLandcoverSelected();
                    
                    // clear saved landcover selected.
                    if ((scene == null) || (currentScene == null) 
                        || (currentScene.gridCol != scene.gridCol) 
                        || (currentScene.gridRow != scene.gridRow)
                        || (currSensor != sensor))
                    {
                        savedSelectedLandcover = null;
                    }
                    
                    clearLandcoverSelected();
                    
                    // set the saved landcover
                    if (!dataAvailable && isLandcoverSelected() 
                        && savedSelectedLandcover != null)
                    {
                        restoreLandcoverSelected(); 
                    }
                }
                ndviLineGraph.performAction();
                repaint();
            }
            else
            {
                clearLandcoverSelected();
                setVisible(false);
            }
        }
    }

    // Method to show the dialog box
    //------------------------------
    public void setVisible(boolean makeVisible)
    {
      if (makeVisible)
      {
        boolean vis = isVisible();
        super.setVisible(true);

        // if the dialog wasn't previously visible, update the data it displays
        if (!vis)
            updateData();
      }
      else
      {
          super.setVisible(false);
      }
    }
    
    // Method to disable the set default button
    //-----------------------------------------
    public void disableButtons()
    {
        defaultButton.setEnabled(false); 
    }
   
    // Method to enable the set default button
    //----------------------------------------
    public void enableButtons()
    {
        defaultButton.setEnabled(true);
    }
    
    // Method for the Observer interface
    //----------------------------------
    public void update(Observable ob, Object arg)
    {
        if (ob == applet.md)
            updateData();
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();

        if (command.equals("Close"))
        {
            // hide the dialog box
            setVisible(false);
        }
        else if (command.equals("Set Default"))
        {
            clearLandcoverSelected();
        }
        else if (command.equals("Print"))
        {
            // print the graph
            PrinterJob printJob = PrinterJob.getPrinterJob();
            printJob.setPrintable(ndviLineGraph);

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
                    // not cancelled, so print the graph
                    try
                    {
                        printJob.print();
                    }
                    catch (Exception ex) { }
                }
            }
        }
    }
}
