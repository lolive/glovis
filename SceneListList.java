// SceneListList.java implements a List GUI component for displaying the 
// scene list.
//---------------------------------------------------------------------------
import java.awt.Component;
import java.text.DecimalFormat;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.util.Observer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

public class SceneListList extends JPanel implements MouseListener,
    ListDataListener
{
    private imgViewer applet;
    private JList currDisplayedList;
    private JLabel title;
    private SceneList currSceneList;
    private ListEntryBuilder entryBuilder;
    private ListSelectionListener selectionListener;
    private SceneListMenu sceneMenu;
    private SceneListPanel sceneListPanel;
    private JPanel titlePanel;
    private String listTitle;
    private SceneList[] sceneLists;
    private boolean doubleClickAction;
    private Font cellFont;
    private ImageIcon downloadIcon;
    private DecimalFormat digitFormat;

    class SceneCellRenderer extends JLabel implements ListCellRenderer
    {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            Metadata scene = (Metadata)value;
            String entry = entryBuilder.getEntry(scene);

            // if only some of the scenes are downloadable, add an indication
            // to the ones that are downloadable
            setIcon(null);
            if (scene.getSensor().mightBeDownloadable)
            {
                if (scene.isDownloadable)
                    setIcon(downloadIcon);
            }
            setText(entry);
            setHorizontalTextPosition(SwingConstants.LEADING);
            setOpaque(true);
            setFont(cellFont);
            setBackground(isSelected ? list.getSelectionBackground() 
                            : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() 
                            : list.getForeground());
            return this;
        }
    }
    
    // Constructor for the SceneListList
    //-----------------------------------
    SceneListList
    (
        imgViewer applet,   // I: reference to the main applet
        ListDataListener parent, // I: reference for observing changes
        ListSelectionListener selectionListener, 
                            // I: list selection listener when notification of
                            // an item being selected in the list is wanted
                            // (null if not wanted)
        Font font,          // I: font to use for displaying the list
        int lines,          // I: number of lines to display in the list
        ListEntryBuilder entryBuilder,// I: interface reference for building 
                            // the line for each scene in the list
        boolean showDialogOptionOnMenu, // I: flag for showing the scene list 
                            // dialog option on the right-click menu
        boolean orderingSceneList, // I: flag to indicate the list supports
                                   // ordering
        String listBoxTitle,// I: name of the dialog box being used
        SceneList[] sceneLists, // I: passing in the scenelist for the correct
                                // dialog box
        boolean doubleClickAction  // I: flag to allow list to respond to 
                                   // double click of list item
    )
    {
        this.applet = applet;
        this.entryBuilder = entryBuilder;
        this.selectionListener = selectionListener;
        this.sceneLists = sceneLists;
        this.doubleClickAction = doubleClickAction;
        this.cellFont = font;

        // create a formatter
        digitFormat = new DecimalFormat ("#0.0");

        // use the gridbag layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 100;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // save the scenelist title
        listTitle = listBoxTitle;  // create a panel for the scene list 
                                // title and set the title
        titlePanel = new JPanel();
        title = new JLabel("Scene List");
        title.setFont(applet.boldFont);
        titlePanel.add(title);
        add(titlePanel, gbc);

        Sensor[] sensors = applet.getSensors();

        // create the list to store the values that will be displayed
        currDisplayedList = new JList();
        currDisplayedList.setToolTipText("Scene list");
        currDisplayedList.setFont(font);
        currDisplayedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        currDisplayedList.setCellRenderer(new SceneListList.SceneCellRenderer());
        currDisplayedList.setFixedCellHeight(15);

        // create a scroll pane for the scene list
        JScrollPane scrollPane = new JScrollPane(currDisplayedList);

        // make at least 4 rows visible
        currDisplayedList.setVisibleRowCount(4);

        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weighty = 100;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        // add the list panel to the panel
        add(scrollPane, gbc);

        // create a separate list component for each sensor
        for (int i = 0; i < sensors.length; i++)
        {
            sceneLists[i].addListDataListener(this);
            // notify the parent when the scene list changes so it can 
            // update the state of any buttons shown
            sceneLists[i].addListDataListener(parent);
        }

        if (selectionListener != null)
            currDisplayedList.addListSelectionListener(selectionListener);
        currDisplayedList.addMouseListener(this);

        // create the popup menu for the scene list
        sceneMenu = new SceneListMenu(applet,applet.md,showDialogOptionOnMenu,
                                      orderingSceneList);
        add(sceneMenu);

        // get the icon to be drawn to indicate a scene is downloadable
        Image downloadIconImage = applet.getImage(applet.getCodeBase(), 
                               "graphics/download_extra_small.gif");
        MediaTracker mt = new MediaTracker(applet);
        mt.addImage(downloadIconImage,0);
       
        // wait for the Icon to completely load so the size of the 
        // downloadIcon will be known for positioning it on the display
        try
        {
            mt.waitForAll();
            downloadIcon = new ImageIcon(downloadIconImage);
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }

        setSensor(applet.sensorMenu.getCurrentSensor());

    } // end of constructor

    // set the sensor scene list currently shown
    //------------------------------------------
    public void setSensor(Sensor newSensor)
    {
        int length;
        Metadata scene;

        // get the list of available sensors
        Sensor[] sensors = applet.getSensors();

        // find the selected sensor, and then the corresponding scene list
        // then clear out the display list and repopulate with the values 
        // stored in the sensor's scenelist
        for (int i = 0; i < sensors.length; i++)
        {
            if (sensors[i] == newSensor)
            {
                // create title for new sensor.  If the sensor name is too
                // long, make the list Title shorter by dropping the first word.
                String localTitle = listTitle;
                if (newSensor.sensorName.length() > 17)
                {
                    int index = listTitle.indexOf(' ');
                    if (index > 0)
                        localTitle = listTitle.substring(index + 1);
                }
                String newTitle = newSensor.sensorName + " " + localTitle;
                title.setText(newTitle);

                // get number of scenes to display for this sensor
                length = sceneLists[i].getSceneCount();

                // set the new data model into the displayed list
                currDisplayedList.setModel(sceneLists[i].getModel());

                // select the last item of the list, if not empty
                if (length > 0)
                {
                    currDisplayedList.setSelectedIndex(length-1);
                }

                // force the layout to be updated for the title panel, 
                // otherwise the title can get clipped at the end if going
                // from a small title to a larger title.
                titlePanel.doLayout();
                // remember the current scene list
                currSceneList = sceneLists[i];

                break;
            }
        }

        setSceneCountDisplay();
    }

    // method to return the number of scenes in the scene list for the current
    // sensor
    //------------------------------------------------------------------------
    public int getSceneCount()
    {
        return currSceneList.getSceneCount();
    }

    // method to return the metadata reference for the current scene selected
    // in the list (null if none selected)
    //-----------------------------------------------------------------------
    public Metadata getSelectedScene()
    {
        // If nothing is selected, return null
        int selectedIndex = currDisplayedList.getSelectedIndex();
        if (selectedIndex < 0)
            return null;

        if (selectedIndex < getSceneCount())
            return currSceneList.getSceneAt(selectedIndex);
        else
            return null;
    }

    // Add the currently selected scene in the applet ImagePane to the scene
    // list
    //----------------------------------------------------------------------
    public void addActiveScene()
    {
        Metadata scene = applet.imgArea.md.getCurrentScene();

        if (scene != null)
            add(scene);
    }

    // method to open a new browser window with the selected scene's original
    // browse image or metadata
    //-----------------------------------------------------------------------
    public void showBrowse(boolean showBrowse)
    {
        // If nothing is selected, just ignore the button press
        int selectedIndex = currDisplayedList.getSelectedIndex();
        if (selectedIndex < 0)
            return;

        Metadata scene = currSceneList.getSceneAt(selectedIndex);

        if (showBrowse)
            scene.showBrowse();
        else
            scene.showMetadata();
    }

    // method to add the scene passed in to the current scene list
    //------------------------------------------------------------
    public void add(Metadata scene)
    {
        currSceneList.add(scene);

        // make sure the added one is the selected one (in case it was
        // already in the list since a data listener event won't happen
        // then)
        int index = currSceneList.find(scene);
        currDisplayedList.setSelectedIndex(index);
        currDisplayedList.ensureIndexIsVisible(index);
    }

    // method to remove the currently selected item from the scene list
    //-----------------------------------------------------------------
    public void remove()
    {
        if (getSceneCount() <= 0) 
            return;

        Metadata scene = (Metadata)currDisplayedList.getSelectedValue();
        currSceneList.remove(scene);
    }

    // method to remove ALL items from the scene list
    //-----------------------------------------------
    public void clear()
    {
        if (getSceneCount() <= 0) 
            return;

        currSceneList.clear();
    }

    // Order the contents of the scene list for most sensors.
    //-----------------------------------------------------------------------
    public void order()
    {
        boolean allowOrder = true;
        // There aren't currently any limitations or info regarding orders

        if (allowOrder)
            currSceneList.order();
    }

    // Restore scene list after order
    //--------------------------------------
    public void restore()
    {
        currSceneList.restore();
    }

    // Method to determine if restore buttons should be enabled or not
    //----------------------------------------------------------------
    public boolean isRestoreEnabled()
    {
        return currSceneList.isRestoreEnabled();
    }

    // method to show the the selected scene in the image area
    //--------------------------------------------------------
    public void showSelectedScene()
    {
        int index = currDisplayedList.getSelectedIndex();
        Metadata scene = currSceneList.getSceneAt(index);

        applet.md.showScene(scene);
    }

    // action performed event handler to show a scene when it is double-clicked
    //-------------------------------------------------------------------------
    public void mouseClicked(MouseEvent event)
    {
        if (event.getClickCount() == 2)
            showSelectedScene();
    }

    // event handler for mouse pressed events
    //---------------------------------------
    public void mousePressed(MouseEvent event)
    {
        // show the right-click menu when the right mouse button is clicked
        if ((event.getModifiers() & (InputEvent.BUTTON2_MASK 
                | InputEvent.BUTTON3_MASK)) != 0)
        {
            Metadata scene = getSelectedScene();

            sceneMenu.configureMenu(this,scene);
            sceneMenu.show(event.getComponent(),event.getX(),event.getY());
        }
    }

    // dummy event handlers for the unused events needed for a MouseListener
    //----------------------------------------------------------------------
    public void mouseEntered(MouseEvent event){}
    public void mouseExited(MouseEvent event){}
    public void mouseReleased(MouseEvent event){}

    // set the tool tip to include info on the number of scenes in the list
    //---------------------------------------------------------------------
    private void setSceneCountDisplay()
    {
        int count = getSceneCount();
        StringBuffer message = new StringBuffer("Scene list");
        if (count > 0)
        {
            message.append(" with ");
            message.append(count);
            message.append(" scene");
            if (count > 1)
                message.append("s");
        }
        currDisplayedList.setToolTipText(message.toString());
    }

    // method to detect when scenes are added to the list and make sure a
    // scene is selected and visible
    //-------------------------------------------------------------------
    public void intervalAdded(ListDataEvent event)
    {
        int index = event.getIndex0();
        currDisplayedList.setSelectedIndex(index);
        currDisplayedList.ensureIndexIsVisible(index);
        setSceneCountDisplay();
    }

    // method to detect when scenes are removed from the list and make sure a
    // scene is selected and visible if scenes remain
    //-----------------------------------------------------------------------
    public void intervalRemoved(ListDataEvent event)
    {
        int index = event.getIndex0();
        if (index > getSceneCount() - 1)
            index = getSceneCount() - 1;
        currDisplayedList.setSelectedIndex(index);
        currDisplayedList.ensureIndexIsVisible(index);
        setSceneCountDisplay();
    }

    // method to detect when scenes are changed so the changed one can be made
    // the selected and visible one
    //------------------------------------------------------------------------
    public void contentsChanged(ListDataEvent event)
    {
        int index = event.getIndex0();
        currDisplayedList.setSelectedIndex(index);
        currDisplayedList.ensureIndexIsVisible(index);
        setSceneCountDisplay();
    }
}
