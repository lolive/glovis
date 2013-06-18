// ToolsMenu.java implements the menu for various applet tools.
//-------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.net.URL;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

public class ToolsMenu extends JMenu implements ActionListener, MenuListener
{
    private imgViewer applet;
    private JCheckBoxMenuItem defaultToDateEnabled;
    private JCheckBoxMenuItem swathModeEnabled;
    private JCheckBoxMenuItem toolTipsEnabled;
    private JMenuItem userDefinedAreaDialog;
    private JMenuItem ndviGraphDialog;
    private boolean ndviDataPresent;
    
    // constructor
    //------------
    public ToolsMenu(imgViewer applet)
    {
        // call the parent constructor
        super("Tools");
        setMnemonic(KeyEvent.VK_T);

        // save the applet reference
        this.applet = applet;
        
        // create the Default To Selected Date checkbox menu item
        defaultToDateEnabled = new JCheckBoxMenuItem(
                                "Default To Selected Date", false);
        defaultToDateEnabled.setMnemonic(KeyEvent.VK_D);
        defaultToDateEnabled.addActionListener(this);
        add(defaultToDateEnabled);
                                        
        // add the hidden scene list selection
        JMenuItem item = new JMenuItem("Hide Scene...", KeyEvent.VK_H);
        item.addActionListener(this);
        add(item);
        
        // include the NDVI graph if the NDVI data is installed. 
        ndviDataPresent = false;
        try
        {
            // if the Makefile creates the NDVI link, it will also create
            // this NDVI_EXISTS.TXT file which we can test to see whether
            // NDVI is installed on this system (still, data may or may not
            // exist for a particular collection or cell in that collection)
            String ndviPresentFile = "NDVI_EXISTS.TXT";
            // build the URL for in the ndvi exists file
            URL ndviPresentURL = new URL(applet.getCodeBase(), ndviPresentFile);
            BufferedInputStream file 
                = new BufferedInputStream(ndviPresentURL.openStream());
            // if the file didn't exist, an exception got thrown
            // so if we're still here, close the file and set the flag
            file.close();
            ndviDataPresent = true;
        }
        catch (Exception e) { }

        if (ndviDataPresent)
        {
            // add a NDVI graphic selection
            ndviGraphDialog = new JMenuItem("NDVI Graph...", KeyEvent.VK_N);
            ndviGraphDialog.addActionListener(this);
            add(ndviGraphDialog);
        }

        // add a print item to the menu
        item = new JMenuItem("Print...", KeyEvent.VK_P);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 
                                ActionEvent.CTRL_MASK));
        item.addActionListener(this);
        add(item);

        // add a refresh item to the menu to allow for forcing a re-read
        // of the inventory
        item = new JMenuItem("Refresh", KeyEvent.VK_R);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 
                                ActionEvent.CTRL_MASK));
        item.addActionListener(this);
        add(item);

        // add the scene list selection
        item = new JMenuItem("Scene List...", KeyEvent.VK_L);
        item.addActionListener(this);
        add(item);

        // add the search for scene selection
        item = new JMenuItem("Search For Scene...", KeyEvent.VK_F);
        item.addActionListener(this);
        add(item);

        // add the search limits selection
        item = new JMenuItem("Search Limits...", KeyEvent.VK_S);
        item.addActionListener(this);
        add(item);

        // create the swath mode checkbox menu item
        swathModeEnabled = new JCheckBoxMenuItem("Swath Mode", false);
        swathModeEnabled.setMnemonic(KeyEvent.VK_M);
        swathModeEnabled.addActionListener(this);
        add(swathModeEnabled);

        // create a tool tip enable/disable checkbox menu item
        toolTipsEnabled = new JCheckBoxMenuItem("Tool Tips", false);
        toolTipsEnabled.setMnemonic(KeyEvent.VK_T);
        toolTipsEnabled.setState(ToolTipManager.sharedInstance().isEnabled());
        toolTipsEnabled.addActionListener(this);
        add(toolTipsEnabled);

        // create the user defined area tool menu item
        userDefinedAreaDialog = new JMenuItem("User Defined Area...", 
                                                KeyEvent.VK_U);
        userDefinedAreaDialog.addActionListener(this);
        add(userDefinedAreaDialog);

        // listen for the menu being selected
        addMenuListener(this);

        setSensor(applet.sensorMenu.getCurrentSensor());
    }

    // method to enable/disable options that only available for some sensors
    //----------------------------------------------------------------------
    public void setSensor(Sensor currSensor)
    {
        // set the visibility of the set selected date based on whether the
        // current sensor has an acquisition date
        defaultToDateEnabled.setVisible(currSensor.hasAcqDate);

        // set the visibility of the swath mode option for the current sensor
        swathModeEnabled.setVisible(currSensor.hasSwathMode);

        // add the ndvi line graph if it is available for this sensor
        if (ndviDataPresent)
        {
            if (currSensor.hasNdviLineGraph)
                ndviGraphDialog.setVisible(true);
            else
                ndviGraphDialog.setVisible(false);
        }
            
        // set the visibility of the user defined area option for the current
        // sensor
        userDefinedAreaDialog.setVisible(currSensor.hasUserDefinedArea);
        if (!currSensor.hasUserDefinedArea)
        {
            // hide the user defined dialog box if it has been created and is
            // currently is visible
            if ((applet.userDefinedAreaDialog != null) 
                && applet.userDefinedAreaDialog.isVisible())
            {
                applet.userDefinedAreaDialog.setVisible(false);
            }
        }
    }

    // method to allow checking if swath mode is enabled
    //--------------------------------------------------
    public boolean isSwathModeEnabled()
    {
        return swathModeEnabled.getState();
    }

    // method to allow checking if default to selected date is enabled
    //----------------------------------------------------------------
    public boolean isDefaultToDateEnabled()
    {
        return defaultToDateEnabled.getState();
    }
    
    // implement the menu listener interface so that the state of the tool tips
    // state can be set properly when the menu is opened (since the tool tips
    // enabled/disabled is shared between all applets, need to do this in case
    // it is disabled by a different applet - including a different instance
    // of glovis)
    //-------------------------------------------------------------------------
    public void menuSelected(MenuEvent e)
    {
        toolTipsEnabled.setState(ToolTipManager.sharedInstance().isEnabled());
    }
    public void menuDeselected(MenuEvent e) {}
    public void menuCanceled(MenuEvent e) {}

    // event handler for the menu selections
    //--------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();

        // some buggy older Java implementation (Netscape 4 and Microsoft's
        // Java VM for sure), do not send the action command for a menu
        // shortcut.  So, if the command is null, get the command from the
        // source menu item.
        if (command == null)
        {
            JMenuItem item = (JMenuItem)event.getSource();
            if (item == null)
            {
                // if the source menu item couldn't be obtained, just exit
                // since the command cannot be obtained
                return;
            }
            else
                command = item.getActionCommand();
        }

        // position any opened dialog boxes inside the main window
        Point loc = applet.getDialogLoc();
        loc.y += 30;

        // open the correct dialog
        if (command.equals("Default To Selected Date"))
        {
            if (isDefaultToDateEnabled())
            {
                applet.md.updateDisplay();
            }
            else
            {
                applet.md.resetTargetDate();
            }
        }
        else if (command.equals("Search For Scene..."))
        {
            applet.searchForSceneDialog.setLocation(loc);
            applet.searchForSceneDialog.setVisible(true);
        }
        else if (command.equals("Search Limits..."))
        {
            applet.searchLimitDialog.setLocation(loc);
            applet.searchLimitDialog.setVisible(true);
        }
        else if (command.equals("Scene List..."))
        {
            applet.sceneListDialog.setLocation(loc);
            applet.sceneListDialog.setVisible(true);
        }
	    else if (command.equals("Hide Scene..."))
        {
            applet.hideSceneDialog.setLocation(loc);
            applet.hideSceneDialog.setVisible(true);
        }
        else if (command.equals("NDVI Graph..."))
        {
            applet.ndviGraphDialog.setLocation(loc);
            applet.ndviGraphDialog.setVisible(true);
        }
        else if (command.equals("Print..."))
        {
            applet.print();
        }
        else if (command.equals("Refresh"))
        {
            applet.md.refreshDisplay();
        }
        else if (command.equals("User Defined Area..."))
        {
            applet.userDefinedAreaDialog.setLocation(loc);
            applet.userDefinedAreaDialog.setVisible(true);
        }
        else if (command.equals("Tool Tips"))
        {
            ToolTipManager.sharedInstance().
                setEnabled(toolTipsEnabled.getState());
        }
        else if (command.equals("Swath Mode"))
        {
            if (isSwathModeEnabled())
                applet.md.updateDisplay();
            else
                applet.imgArea.repaint();
        }
    }
}
