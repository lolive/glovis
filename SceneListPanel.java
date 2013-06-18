// SceneListPanel.java implements a panel for displaying a scene list in
// the main applet.
//  
//---------------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Observer;
import java.util.Observable;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JButton;
import javax.swing.JPanel;

public class SceneListPanel extends JPanel implements ActionListener, Observer,
    ListEntryBuilder, ListDataListener, ListSelectionListener
{
    private imgViewer applet;
    private SceneListList sceneListList;
    private JButton addButton;
    private JButton deleteButton;
    private JButton shopButton;     // add all items in list to Shopping Cart
    
    // Constructor for the SceneListPanel
    //-----------------------------------
    SceneListPanel(imgViewer parent)
    {
        applet = parent;
        JPanel buttonPanel;

        // use the border layout
        setLayout(new GridBagLayout());

        Sensor[] sensors = applet.getSensors();
        
        SceneList[] sceneList = new SceneList[sensors.length];

        // create a separate list component for each sensor
        for (int i = 0; i < sensors.length; i++)
        {
            sceneList[i] = sensors[i].sceneList;
        }

        // create the scene list
        sceneListList = new SceneListList(applet,this,this,applet.smallFont,
                              4,this,true,true,"Scene List",sceneList,true);

        // Make a panel for the buttons and add the buttons
        // Margins are being set to allow the text to fit in the buttons on
        // all Operating Systems.
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());

        addButton = new JButton("Add");
        addButton.addActionListener(this);
        addButton.setMnemonic(KeyEvent.VK_A);
        addButton.setToolTipText("Add scene to scene list");
        addButton.setFont(parent.normalFont);
        addButton.setMargin(new Insets(2,0,2,0));
        
        deleteButton = new JButton("Delete");
        deleteButton.setMnemonic(KeyEvent.VK_D);
        deleteButton.setToolTipText("Delete scene from scene list");
        deleteButton.addActionListener(this);
        deleteButton.setEnabled(false);
        deleteButton.setFont(parent.normalFont);
        deleteButton.setMargin(new Insets(2,0,2,0));
        
        shopButton = new JButton("Send to Cart");
        shopButton.setMnemonic(KeyEvent.VK_S);
        shopButton.setToolTipText("Send scenes to Shopping Cart");
        shopButton.addActionListener(this);
        shopButton.setEnabled(false);
        shopButton.setFont(parent.normalFont);
        shopButton.setMargin(new Insets(2,0,2,0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 100;
        gbc.weightx = 100;
        gbc.fill = GridBagConstraints.BOTH;
        
        buttonPanel.add(addButton, gbc);
        buttonPanel.add(deleteButton, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(shopButton, gbc);

        // add the scene list and button panels
        gbc = new GridBagConstraints();
        gbc.weighty = 100;
        gbc.weightx = 100;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        // add the list panel to the panel
        add(sceneListList,gbc);

        gbc.weighty = 0;
        add(buttonPanel,gbc);

        setSensor(applet.sensorMenu.getCurrentSensor());

        // make sure the height of the panel is large enough to display
        // four lines in the list
        setMinimumSize(new Dimension(240, 115));
    }
    
    // helper method to enable buttons that depend on data in the list
    //----------------------------------------------------------------
    private void enableButtons()
    {
        deleteButton.setEnabled(true);

        Metadata scene = sceneListList.getSelectedScene();

        if ((scene != null)
            &&   (applet.sensorMenu.getCurrentSensor().isOrderable
               || applet.sensorMenu.getCurrentSensor().isDownloadable))
            shopButton.setEnabled(true);
        else
            shopButton.setEnabled(false);
    }

    // helper method to disable buttons that depend on data in the list
    //-----------------------------------------------------------------
    private void disableButtons()
    {
        deleteButton.setEnabled(false);
        shopButton.setEnabled(false);
    }

    // method for the ListEntryBuilder interface to provide the contents for
    // the scene list
    //----------------------------------------------------------------------
    public String getEntry(Metadata scene)
    {
        return scene.getMediumEntityIDForDisplay();
    }

    // set the sensor scene list currently shown
    //------------------------------------------
    public void setSensor(Sensor newSensor)
    {
        sceneListList.setSensor(newSensor);

        // make sure the buttons are the correct state for the displayed
        // scene list
        if (sceneListList.getSceneCount() > 0)
            enableButtons();
        else
            disableButtons();

        // make sure the Send to Cart (order) button is only visible if
        // the dataset is either orderable or downloadable
        shopButton.setVisible(newSensor.isOrderable
                           || newSensor.isDownloadable);
    }
    
    // event handler for a new item being selected in the list
    //--------------------------------------------------------
    public void valueChanged(ListSelectionEvent event)
    {
        // when a new scene is selected, make sure to set the order/download
        // button text properly
        updateOrderButton();
    }

    // helper method to set the Send to Cart (order) button state
    //-----------------------------------------------------------
    private void updateOrderButton()
    {
        Metadata scene = sceneListList.getSelectedScene();

        if ((scene != null)
            &&    (applet.sensorMenu.getCurrentSensor().isOrderable
                || applet.sensorMenu.getCurrentSensor().isDownloadable))
            shopButton.setEnabled(true);
        else
            shopButton.setEnabled(false);
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        // make sure the date navigation date is updated when the when the
        // map layers button is selected for astetics in case it doesn't
        // show the current scenes date
        applet.navDate.update((Observable)null,(Object)null);

        String command = e.getActionCommand();
        if (command.equals("Add"))
            sceneListList.addActiveScene();
        else if (command.startsWith("Del"))
            sceneListList.remove();
        else if (command.equals("Send to Cart"))
            sceneListList.order();
    }

    // update method for the observer interface.  This handles updates when
    // the sensor selection is changed.
    //---------------------------------------------------------------------
    public void update(Observable ob, Object arg)
    {
        if (ob == applet.md)
        {
            updateOrderButton();

            // the selected scene was updated, so enable/disable the add
            // scene button as required
            Metadata scene = applet.md.getCurrentScene();
            if (scene != null)
                addButton.setEnabled(true);
            else
                addButton.setEnabled(false);
        }
    }

    // method to detect when scenes are added to the list and properly enable
    // the buttons
    //-----------------------------------------------------------------------
    public void intervalAdded(ListDataEvent event)
    {
        enableButtons();
    }

    // method to detect when scenes are removed from the list and properly
    // disable the buttons
    //--------------------------------------------------------------------
    public void intervalRemoved(ListDataEvent event)
    {
        // enable the buttons if there are scenes in the list, otherwise
        // disable them
        if (sceneListList.getSceneCount() > 0)
            enableButtons();
        else
            disableButtons();
    }

    // dummy method for the ListDataListener interface
    //------------------------------------------------
    public void contentsChanged(ListDataEvent event) { }
}
