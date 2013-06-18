// SceneListMenu.java implements the popup menu brought up by right-clicking
// on the scene list.
//
//-------------------------------------------------------------------------
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.Observer;
import java.util.Observable;
import java.net.URL;

public class SceneListMenu extends JPopupMenu implements ActionListener
{
    private imgViewer applet;
    private MosaicData md;
    private SceneListList sceneListList;
    private Metadata scene;     // the target scene for the menu actions
    private JMenuItem sceneID;  // menu item for the scene id (no action
                                // associated with it)
    private JMenuItem sceneInfo;// menu item for scene information (no action
                                // associated with it)
    private JMenuItem shopList; // menu item to send all scenes to Shopping Cart
    private JMenuItem clearList;// menu item to empty the Scene List
    private JMenuItem display;  // display scene menu item
    private JMenuItem showMetadata;// menu item to show metadata
    private JMenuItem showBrowse;  // menu item to show browse image
    private JMenuItem deleteScene; // menu item to delete selected scene
    private JMenuItem unhideScene; // menu item to unhide the selected scene
    private JMenuItem restore;  // menu item to restore to saved list      
    private JMenuItem more;     // menu item to display scene list dialog box

    private final static int NORMAL = 0;    // flags to show the current state
    private final static int REMOVE = 1;    // of the menu to prevent unneeded
    private final static int EMPTY_LIST = 2;// adds and removes.
    private int currentState;   // flag to hold the current state

    // constructor for the popup scene menu
    //-------------------------------------
    public SceneListMenu(imgViewer applet, MosaicData md, 
                         boolean showOpenDialogOption, 
                         boolean configureForOrdering)
    {
        // call the parent constructor
        super("Scene List Menu");

        this.applet = applet;
        this.md = md;
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();

        // create the menu items
        sceneID = new JMenuItem("Scene ID");
        sceneID.setFont(applet.normalFont);
        sceneID.addActionListener(this);

        sceneInfo = new JMenuItem("Scene Info");
        sceneInfo.setFont(applet.normalFont);
        sceneInfo.addActionListener(this);

        showMetadata = new JMenuItem("Show Metadata");
        showMetadata.setFont(applet.normalFont);
        showMetadata.addActionListener(this);

        showBrowse = new JMenuItem("Show Browse");
        showBrowse.setFont(applet.normalFont);
        showBrowse.addActionListener(this);

        if (configureForOrdering)
        {
            display = new JMenuItem("Display");
            display.setFont(applet.normalFont);
            display.addActionListener(this);

            deleteScene = new JMenuItem("Delete");
            deleteScene.setFont(applet.normalFont);
            deleteScene.addActionListener(this);

            restore = new JMenuItem("Restore");
            restore.setFont(applet.normalFont);
            restore.addActionListener(this);

            shopList = new JMenuItem("Send to Cart");
            shopList.setFont(applet.normalFont);
            shopList.addActionListener(this);

            clearList = new JMenuItem("Clear List");
            clearList.setFont(applet.normalFont);
            clearList.addActionListener(this);
        }
        else
        {
            // set up for unhiding scenes
            unhideScene = new JMenuItem("Unhide");
            unhideScene.setFont(applet.normalFont);
            unhideScene.addActionListener(this);
        }

        // create a menu item to allow opening the scene list dialog if wanted
        if (showOpenDialogOption)
        {
            more = new JMenuItem("More...");
            more.setFont(applet.normalFont);
            more.addActionListener(this);
        }

        // set current state to none of the three valid states
        currentState = -1;
    }

    // method to configure the menu.  If the list is empty or no scene is
    // selected, the only menu options may be 'restore' and 'more'. If 
    // neither option is available, don't panic.  An empty list will not be
    // shown.  If a scene is selected, add the other buttons to the list.
    //----------------------------------------------------------------------
    public void configureMenu(SceneListList sceneListList, Metadata scene)
    {
        this.sceneListList = sceneListList;
        this.scene = scene;
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();

        // create the menu if there is a scene selected
        if (scene != null)
        {
            sceneID.setText("ID: " + scene.getEntityIDForDisplay());

            // set the second line of the title based on whether the sensor
            // has a cloud cover value, quality value, and an acquisition date
            String line2 = "";
            boolean line2Empty = true;
            if (currSensor.hasCloudCover)
            {
                line2 += "" + scene.cloudCover + "% Cloud";
                line2Empty = false;
            }
            if (currSensor.numQualityValues > 0)
            {
                if (!line2Empty)
                    line2 += ", ";
                else
                    line2Empty = false;
                line2 += "Qlty " + scene.getQuality();
            }
            if (currSensor.hasAcqDate)
            {
                if (!line2Empty)
                    line2 += ", ";
                else
                    line2Empty = false;
                line2 += scene.getDateString();
            }
            sceneInfo.setText(line2);
            sceneInfo.setVisible(!line2Empty);

            // configure the display menu item
            if (display != null)
            {
                applet.searchLimitDialog.applySearchLimits(scene);
                
                if (scene.visible)
                    display.setEnabled(true);
                else
                    display.setEnabled(false);
            }

            // configure the Send to Cart (order) menu item
            if (shopList != null)
            {
                // reset the menu state if the order state changed
                if ((currSensor.isOrderable || currSensor.isDownloadable)
                    && !shopList.isEnabled())
                {
                    shopList.setEnabled(true);
                    currentState = -1;
                }
                else if (shopList.isEnabled()
                    && (!currSensor.isOrderable && !currSensor.isDownloadable))
                {
                    shopList.setEnabled(false);
                    currentState = -1;
                }
            }

            // only create it if the menu is in a different state
            if (currentState != NORMAL)
            {
                removeAll();

                // set the title of the menu
                add(sceneID);

                add(sceneInfo);
                addSeparator();

                // add options to show metadata and browse image
                add(showMetadata);
                add(showBrowse);
            
                if (display != null)
                    add(display);
                if (deleteScene != null)
                    add(deleteScene);
                if (unhideScene != null)
                    add(unhideScene);

                addSeparator();

                // configure the Send to Cart (order) menu item
                if (shopList != null)
                {
                    if (currSensor.isOrderable || currSensor.isDownloadable)
                    {
                        shopList.setEnabled(true);
                        add(shopList);
                    }
                    else if (shopList.isEnabled())
                    {
                        shopList.setEnabled(false);
                        remove(shopList);
                    }
                }

                // add option to clear the entire list
                add(clearList);

                // add more option if it is available
                if (more != null)
                    add(more);

                currentState = NORMAL;
            }
        }       
        // create the menu in the remove style
        else if (sceneListList.isRestoreEnabled())
        {
            // only build it if it's in a different state
            if (currentState != REMOVE)
            {
                removeAll();
                // add the restore option
                add(restore);

                // see if more option should be added
                if (more != null)
                {
                    add(more);
                }
                currentState = REMOVE;
            }
        }       
        // build the empty menu, which might contain the more option
        else if (currentState != EMPTY_LIST)
        {
            removeAll();
            // add the more option if it is available
            if (more != null)
                add(more);

            currentState = EMPTY_LIST;
        }
    }

    // action listener event handler
    //------------------------------
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();

        if (command.equals("Show Metadata"))
            scene.showMetadata();
        else if (command.equals("Show Browse"))
            scene.showBrowse();
        else if (command.equals("Display"))
            md.showScene(scene);
        else if (command.equals("Delete"))
            sceneListList.remove();           
        else if (command.equals("Unhide"))
            sceneListList.remove();           
        else if (command.equals("Send to Cart"))
            sceneListList.order();
        else if (command.equals("Clear List"))
            sceneListList.clear();
        else if (command.equals("Restore"))
            sceneListList.restore();
        else if (command.equals("More..."))
        {
            Point loc = applet.sceneListPanel.getLocationOnScreen();
            applet.sceneListDialog.setLocation(loc);
            applet.sceneListDialog.setVisible(true);
        }
    }
}

