// SceneListDialog.java implements a dialog for a more functional scene list
// that the one that fits in the main applet.
//
//---------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class SceneListDialog extends JDialog implements WindowListener,
    ActionListener, Observer, ListEntryBuilder, ListSelectionListener, 
    ListDataListener
{
    public SceneListList sceneListList;
    
    private JPanel buttonPanel;      // panel for action buttons
    private JButton addButton;       // button to add the applet selected scene
    private JButton deleteButton;    // button to delete the selected scene
    private JButton clearButton;     // button to clear the scene list
    private JButton displayButton;   // button to display the selected scene
    private JButton shopButton;    // button to send scene list to Shopping Cart
    private JButton closeButton;     // button to close the dialog box
    private JButton showBrowseButton;// show browse button
    private JButton showMetadataButton; // show metadata button
    private JButton restoreListButton;  // button to restore scene list

    // TBD - use the observer pattern instead?
    private MosaicData md;          // reference to the MosaicData object
    private imgViewer applet;       // reference to the main applet

    // Constructor for the scene list dialog
    //--------------------------------------
    public SceneListDialog(JFrame parent, imgViewer applet, MosaicData md)
    {
        super(parent,"Scene List",false);
        this.applet = applet;
        this.md = md;

        getContentPane().setLayout(new BorderLayout());
        
        Sensor[] sensors = applet.getSensors();
        SceneList[] sceneList = new SceneList[sensors.length];
        
        // create a separate list component for each sensor
        for (int i = 0; i < sensors.length; i++)
        {
            sceneList[i] = sensors[i].sceneList;
        }

        // set up the scene list panel
        sceneListList = new SceneListList(applet,this,this,applet.normalFont,
                              6,this,false,true,"Scene List",sceneList,true);

        // set up the buttons
        buttonPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        buttonPanel.setLayout(gridbag);

        addButton = new JButton("Add");
        addButton.setMnemonic(KeyEvent.VK_A);
        addButton.setToolTipText("Add scene to scene list");
        addButton.addActionListener(this);

        deleteButton = new JButton("Delete");
        deleteButton.setMnemonic(KeyEvent.VK_E);
        deleteButton.setToolTipText("Delete scene from scene list");
        deleteButton.addActionListener(this);

        clearButton = new JButton("Clear List");
        clearButton.setMnemonic(KeyEvent.VK_L);
        clearButton.setToolTipText("Remove all scenes from scene list");
        clearButton.addActionListener(this);

        displayButton = new JButton("Display");
        displayButton.setMnemonic(KeyEvent.VK_I);
        displayButton.setToolTipText("Display selected scene in Browse Viewer");
        displayButton.addActionListener(this);

        shopButton = new JButton("Send to Cart");
        shopButton.setMnemonic(KeyEvent.VK_S);
        shopButton.setToolTipText("Send scenes to Shopping Cart");
        shopButton.addActionListener(this);

        showMetadataButton = new JButton("Show Metadata");
        showMetadataButton.setMnemonic(KeyEvent.VK_M);
        showMetadataButton.setToolTipText("Show metadata for selected scene");
        showMetadataButton.addActionListener(this);

        showBrowseButton = new JButton("Show Browse");
        showBrowseButton.setMnemonic(KeyEvent.VK_B);
        showBrowseButton.setToolTipText("Show browse for selected scene");
        showBrowseButton.addActionListener(this);

        closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close scene list");
        closeButton.addActionListener(this);

        restoreListButton = new JButton("Restore");
        restoreListButton.setMnemonic(KeyEvent.VK_R);
        restoreListButton.setToolTipText("Restore scenes in scene list");
        restoreListButton.addActionListener(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 0;
        gbc.weightx = 20;
        gbc.fill = GridBagConstraints.BOTH;

        buttonPanel.add(addButton,gbc);
        buttonPanel.add(deleteButton,gbc);
        
        // make the clear button the last one in this row
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(clearButton,gbc);
        
        
        // reset the width to one column
        gbc.gridwidth = 1;
        buttonPanel.add(showMetadataButton,gbc);
        buttonPanel.add(showBrowseButton,gbc);
        
        // make the display button the last one in this row
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(displayButton,gbc);
        
        // reset the width to one column
        gbc.gridwidth = 1;
        buttonPanel.add(shopButton,gbc);

        // since shopButton may or may not be visible, make sure
        // restoreListButton is relative to last button (closeButton)
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        buttonPanel.add(restoreListButton,gbc);

        // make the close button the last one in this row
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(closeButton,gbc);

        // add the scene list and button panels to the dialog
        getContentPane().add(sceneListList,"Center");
        getContentPane().add(buttonPanel,"South");

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(420,200);

        // disable the buttons by default
        disableButtons();
        displayButton.setEnabled(false);
        restoreListButton.setEnabled(false);

        // request the window events
        addWindowListener(this);

        // make sure the dialog starts configured for the correct sensor
        setSensor(applet.sensorMenu.getCurrentSensor());
    }

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

    // method to handle the opening of the dialog box.  It sets the constraints
    // for the current sensor when the dialog is opened.
    public void windowOpened(WindowEvent e) 
    { 
    }

    // helper method to enable buttons that depend on data in the list
    //----------------------------------------------------------------
    private void enableButtons()
    {
        deleteButton.setEnabled(true);
        clearButton.setEnabled(true);
        if (applet.sensorMenu.getCurrentSensor().isOrderable
            || applet.sensorMenu.getCurrentSensor().isDownloadable)
            shopButton.setEnabled(true);
        else
            shopButton.setEnabled(false);

        showBrowseButton.setEnabled(true);
        showMetadataButton.setEnabled(true);
    }

    // helper method to disable buttons that depend on data in the list
    //-----------------------------------------------------------------
    private void disableButtons()
    {
        deleteButton.setEnabled(false);
        clearButton.setEnabled(false);
        displayButton.setEnabled(false);
        shopButton.setEnabled(false);
        showBrowseButton.setEnabled(false);
        showMetadataButton.setEnabled(false);
    }

    // method for the ListEntryBuilder interface to provide the contents for
    // the scene list
    //----------------------------------------------------------------------
    public String getEntry(Metadata scene)
    {
        // get the cloud cover to display, if the sensor has cloud cover values
        String cloudCover;
        String quality;
        String acquiredDate;
        int sceneQuality;
        Sensor sensor = scene.getSensor();

        if (sensor.hasCloudCover)
            cloudCover = new String(", " + scene.cloudCover + "% Cloud");
        else
            cloudCover = new String("");

        // if the sensor supports quality, add it to the entry to display
        sceneQuality = scene.getQuality();
        if (sceneQuality >= 0)
            quality = new String(", Quality: " + sceneQuality);
        else
            quality = new String("");

        // if the sensor has an acquired date, add it to the entry to display
        if (sensor.hasCloudCover)
            acquiredDate = new String(", Acquired: " + scene.getDateString());
        else
            acquiredDate = new String("");

        return scene.getEntityIDForDisplay() + cloudCover + quality
                    + acquiredDate;
    }

    // method to set the currently displayed sensor
    //---------------------------------------------
    public void setSensor(Sensor newSensor)
    {
        sceneListList.setSensor(newSensor);

        // only display the Send to Cart button if
        // the dataset is either orderable or downloadable
        shopButton.setVisible(newSensor.isOrderable
                           || newSensor.isDownloadable);
        // (keep the Restore button because it is used by Clear List)

        // make sure the buttons are the correct state for the displayed
        // scene list
        if (sceneListList.getSceneCount() > 0)
            enableButtons();
        else
            disableButtons();
        updateDisplayButton();
        restoreListButton.setEnabled(sceneListList.isRestoreEnabled());
    }

    // event handler for a new item being selected in the list
    //--------------------------------------------------------
    public void valueChanged(ListSelectionEvent event)
    {
        updateDisplayButton();
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();

        if (command.equals("Add"))
            sceneListList.addActiveScene();
        else if (command.equals("Delete"))
            sceneListList.remove();
        else if (command.equals("Clear List"))
            sceneListList.clear();
        else if (command.equals("Display"))
            sceneListList.showSelectedScene();
        else if (command.equals("Send to Cart"))
            sceneListList.order();
        else if (command.equals("Show Browse"))
            sceneListList.showBrowse(true);
        else if (command.equals("Show Metadata"))
            sceneListList.showBrowse(false);
        else if (command.equals("Restore"))
            sceneListList.restore();
        else if (command.equals("Close"))
        {
            // hide the dialog box
            setVisible(false);
        }
    }

    // public interface for allowing the "display" button to be enabled or
    // disabled for the selected scene in the list when it does/doesn't meet
    // the search limits
    //----------------------------------------------------------------------
    public void updateForChangedSearchLimits()
    {
        updateDisplayButton();
    }

    // method to update the display button enabled state to be enabled if the 
    // selected scene in the list is not filtered out by the search limits, 
    // or disable it if the scene is filtered out
    //-----------------------------------------------------------------------
    private void updateDisplayButton()
    {
        // make sure the display button is in sync with the selected scene
        Metadata scene = sceneListList.getSelectedScene();

        if (scene != null)
        {
            // filter the scene to the current limits
            applet.searchLimitDialog.applySearchLimits(scene);

            displayButton.setEnabled(scene.visible);
        }
        else
        {
            displayButton.setEnabled(false);
        }
            
    }

    // update method for the observer interface.  This handles updates from
    // both the MosaicData class and the SceneList class.
    //---------------------------------------------------------------------
    public void update(Observable ob, Object arg)
    {
        if (ob == applet.md)
        {
            // the selected scene was updated, so enable/disable the add
            // scene button as required
            Metadata scene = applet.md.getCurrentScene();
            if (scene != null)
                addButton.setEnabled(true);
            else
                addButton.setEnabled(false);
        }
    }

    // method to enable the buttons properly when scenes are added to the list
    //------------------------------------------------------------------------
    public void intervalAdded(ListDataEvent event)
    {
        enableButtons();

        // set the restore button to the restore status
        restoreListButton.setEnabled(sceneListList.isRestoreEnabled());
    }

    // method to disable buttons properly when scenes are removed from the list
    //-------------------------------------------------------------------------
    public void intervalRemoved(ListDataEvent event)
    {
        // enable the buttons if there are scenes in the list, otherwise
        // disable them
        if (sceneListList.getSceneCount() > 0)
            enableButtons();
        else
            disableButtons();

        // set the restore button to the restore status
        restoreListButton.setEnabled(sceneListList.isRestoreEnabled());
    }

    // dummy method for the ListDataListener (nothing to do in it)
    //------------------------------------------------------------
    public void contentsChanged(ListDataEvent event) { }
}

