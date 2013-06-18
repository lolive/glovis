// HideSceneDialog.java implements a dialog for a more functional scene list
// that the one that fits in the main applet.
//
//---------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Observable;
import java.util.Observer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class HideSceneDialog extends JDialog implements WindowListener, 
ActionListener, Observer, ListEntryBuilder, ListDataListener
{
    private SceneListList sceneListList;
   
    private JPanel buttonPanel;   // panel for action buttons
    private JButton hideButton;   // button to hide the applet selected scene
    private JButton unhideButton; // button to unhide the selected scene
    private JButton closeButton;  // button to close the dialog box

    // TBD - use the observer pattern instead?
    private MosaicData md;          // reference to the MosaicData object
    private imgViewer applet;       // reference to the main applet
    
    // Constructor for the scene list dialog
    //--------------------------------------
    public HideSceneDialog(JFrame parent, imgViewer applet, MosaicData md)
    {
        super(parent,"Hidden Scene List",false);
        this.applet = applet;
        this.md = md;

        getContentPane().setLayout(new BorderLayout());
        
        Sensor[] sensors = applet.getSensors();
        SceneList[] sceneList = new SceneList[sensors.length];
        
        // build an array of hidden scene lists
        for (int i = 0; i < sensors.length; i++)
        {
            sceneList[i] = sensors[i].hiddenSceneList;
        }

        // set up the scene list panel
        sceneListList = new SceneListList(applet,this,null,applet.normalFont,
                      6,this,false,false,"Hidden Scene List",sceneList,false);

        // set up the buttons
        buttonPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        buttonPanel.setLayout(gridbag);

        hideButton = new JButton("Hide");
        hideButton.setMnemonic(KeyEvent.VK_H);
        hideButton.setToolTipText("Hide scene");
        hideButton.addActionListener(this);

        unhideButton = new JButton("Unhide");
        unhideButton.setMnemonic(KeyEvent.VK_U);
        unhideButton.setToolTipText("Unhide scene");
        unhideButton.addActionListener(this);

        closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close hidden scene list");
        closeButton.addActionListener(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 0;
        gbc.weightx = 20;
        gbc.fill = GridBagConstraints.BOTH;

        buttonPanel.add(hideButton,gbc);
        buttonPanel.add(unhideButton,gbc);
        // make the order button the last one in this row
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        // reset the width to one column
        gbc.gridwidth = 1;
        // make the close button the last one in this row
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(closeButton,gbc);

        // add the scene list and button panels to the dialog
        getContentPane().add(sceneListList,"Center");
        getContentPane().add(buttonPanel,"South");

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(450,200);

        // disable the buttons by default
        disableButtons();

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
    public void windowOpened(WindowEvent e) { }

    // helper method to enable buttons that depend on data in the list
    //----------------------------------------------------------------
    private void enableButtons()
    {
        unhideButton.setEnabled(true);
    }

    // helper method to disable buttons that depend on data in the list
    //-----------------------------------------------------------------
    private void disableButtons()
    {
        unhideButton.setEnabled(false);
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
    
    // hide the image currently displayed
    //-----------------------------------
    public void hideScene(Metadata scene)
    {
        sceneListList.add(scene);
    }

    // method to set the currently displayed sensor
    //---------------------------------------------
    public void setSensor(Sensor newSensor)
    {
        sceneListList.setSensor(newSensor);

        // make sure the buttons are the correct state for the displayed
        // scene list
        if (sceneListList.getSceneCount() > 0)
            enableButtons();
        else
            disableButtons();
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();

        if (command.equals("Hide"))
        {
            sceneListList.addActiveScene();
        }
        else if (command.equals("Unhide"))
        {
            sceneListList.remove();
        }
        else if (command.equals("Close"))
        {
            // hide the dialog box
            setVisible(false);
        }
    }

    // update method for the observer interface.  This handles updates from
    // both the MosaicData class and the SceneList class.
    //---------------------------------------------------------------------
    public void update(Observable ob, Object arg)
    {
        if (ob == applet.md)
        {
            // the selected scene was updated, so enable/disable 
            Metadata scene = applet.md.getCurrentScene();
            if (scene != null)
                hideButton.setEnabled(true);
            else
                hideButton.setEnabled(false);
        }
    }

    // method to handle data being added to the scene list
    //----------------------------------------------------
    public void intervalAdded(ListDataEvent event)
    {
        // update the search limit filters whenever the state of the
        // scene list changes
        applet.searchLimitDialog.applyFilter();

        enableButtons();
    }

    // method to handle data being removed from the scene list
    //--------------------------------------------------------
    public void intervalRemoved(ListDataEvent event)
    {
        // update the search limit filters whenever the state of the
        // scene list changes
        applet.searchLimitDialog.applyFilter();

        // enable the buttons if there are scenes in the list, otherwise
        // disable them
        if (sceneListList.getSceneCount() > 0)
            enableButtons();
        else
            disableButtons();
    }

    // dummy method for the list data listener since nothing needs to be done
    //-----------------------------------------------------------------------
    public void contentsChanged(ListDataEvent event) { }
}
