// SceneMenu.java implements the popup menu brought up by right-clicking in
// the ImagePane.
//
//-------------------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

public class SceneMenu extends JPopupMenu implements ActionListener
{
    private imgViewer applet;
    private MosaicData md;
    private Metadata scene;     // the target scene for the menu actions
    private Vector intersectingScenes; // full list of scenes that intersect
                                       // the point clicked on for the menu
    private JMenuItem sceneID;     // menu item for the scene id (no action
                                   // associated with it)
    private JMenuItem sceneInfo;   // menu item for scene information (no action
                                   // associated with it)
    private JMenuItem raise;       // raise scene menu item
    private JMenuItem lower;       // lower scene menu item
    private JMenuItem setScenesToDate; //set Scenes to date menu item
    private JMenuItem setDefaultScene; // set Default scene
    private JMenuItem prevScene;   // previous scene menu item
    private JMenuItem nextScene;   // previous scene menu item
    private JMenuItem addAllScenes;// add all scenes displayed to scene list
    private JMenuItem addSwathScenes; // add the scenes in the swath
                                      // to the scene list
    private JMenuItem hideSwathScenes;// add the scenes in the swath
                                      // to the hidden scene list
    private JMenuItem addUserScenes;// add all scene in user defined area
    private JMenuItem removeScene;  // remove scene from scene list
    private JMenuItem hideScene;    // hide current scene and add to hide list
    private boolean nextPrevPresent;// flag to indicate the next/prev scene
                                    // menu items are in the menu
    private boolean setScenesToDatePresent; // flag to indicate the set Scene
                                            // date menu items are in the menu
    private boolean setDefaultScenePresent; // flag to indicate the Default
                                            // Scene menu items are in the
                                            // menu
    private boolean allScenesPresent;       // flag to indicate Add all to 
                                            // to scene list is present
    private boolean swathScenesPresent;     // flag to indicate Add swath to 
                                            // to scene list is present
    private boolean addUserScenesPresent;   // flag to indicate add user scenes
                                            // menu item is present.
    JMenu selectByScene = null; // select by scene submenu when the intersecting
                                // scenes option is used
    String setScenesToDateTitle;// string to hold the label & date values for
                                // the set Scene to menu item

    // constructor for the popup scene menu
    //-------------------------------------
    public SceneMenu(imgViewer applet, MosaicData md)
    {
        // call the parent constructor
        super("Scene Menu");

        this.applet = applet;
        this.md = md;

        // add the menu items
        sceneID = new JMenuItem("Scene ID");
        sceneID.setFont(applet.normalFont);
        sceneID.addActionListener(this);
        add(sceneID);

        sceneInfo = new JMenuItem("Scene Info");
        sceneInfo.setFont(applet.normalFont);
        sceneInfo.addActionListener(this);
        add(sceneInfo);

        addSeparator();

        JMenuItem menuItem = new JMenuItem("Show Metadata");
        menuItem.setFont(applet.normalFont);
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem("Show Browse");
        menuItem.setFont(applet.normalFont);
        menuItem.addActionListener(this);
        add(menuItem);
        
        menuItem = new JMenuItem("Add To Scene List");
        menuItem.setFont(applet.normalFont);
        menuItem.addActionListener(this);
        add(menuItem);

        removeScene = new JMenuItem("Remove From Scene List");
        removeScene.setFont(applet.normalFont);
        removeScene.addActionListener(this);
        add(removeScene);
        
        hideScene = new JMenuItem("Hide Scene");
        hideScene.setFont(applet.normalFont);
        hideScene.addActionListener(this);
        add(hideScene);

        raise = new JMenuItem("Bring To Front");
        raise.setFont(applet.normalFont);
        raise.addActionListener(this);
        add(raise);

        lower = new JMenuItem("Send To Back");
        lower.setFont(applet.normalFont);
        lower.addActionListener(this);
        add(lower);

        JMenuItem setPointOfInterest = new JMenuItem("Set Point Of Interest");
        setPointOfInterest.setFont(applet.normalFont);
        setPointOfInterest.addActionListener(this);
        add(setPointOfInterest);

        setScenesToDate = new JMenuItem("Set Scenes To");
        setScenesToDate.setFont(applet.normalFont);
        setScenesToDate.addActionListener(this);
        
        setDefaultScene = new JMenuItem("Default Scene");
        setDefaultScene.setFont(applet.normalFont);
        setDefaultScene.addActionListener(this);

        // create the prev/next scene menu items, but don't add them since
        // they will be added/removed when the scene is set
        prevScene = new JMenuItem("Previous Avail. Date");
        prevScene.setFont(applet.normalFont);
        prevScene.addActionListener(this);

        nextScene = new JMenuItem("Next Avail. Date");
        nextScene.setFont(applet.normalFont);
        nextScene.addActionListener(this);
        
        addAllScenes = new JMenuItem("Add All To Scene List");
        addAllScenes.setFont(applet.normalFont);
        addAllScenes.addActionListener(this);

        addSwathScenes = new JMenuItem("Add Swath To Scene List");
        addSwathScenes.setFont(applet.normalFont);
        addSwathScenes.addActionListener(this);

        hideSwathScenes = new JMenuItem("Hide Swath");
        hideSwathScenes.setFont(applet.normalFont);
        hideSwathScenes.addActionListener(this);

        addUserScenes = new JMenuItem("Add User Area Scenes");
        addUserScenes.setFont(applet.normalFont);
        addUserScenes.addActionListener(this);

        // set the initial state of the optional menu items
        nextPrevPresent = false;
        setScenesToDatePresent = false;
        setDefaultScenePresent = false;
        allScenesPresent = false;
        swathScenesPresent = false;
        addUserScenesPresent = false;
    }

    // method to configure the menu for the current scene and options
    //---------------------------------------------------------------
    public void configureMenu(Metadata scene, Vector intersectingScenes,
                              boolean displayingSingleScene, 
                              boolean showNextPrev, boolean showSetScenesTo,
                              boolean showDefaultScene)
    {
        this.scene = scene;
        this.intersectingScenes = intersectingScenes;
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        
        // if the select by scene submenu is defined, remove it
        if (selectByScene != null)
        {
            remove(selectByScene);
            selectByScene = null;
        }

        // set the enabled state of the raise and lower options
        boolean enableZOrderOptions = true;
        if (displayingSingleScene)
            enableZOrderOptions = false;
        raise.setEnabled(enableZOrderOptions);
        lower.setEnabled(enableZOrderOptions);

        // set the title of the menu
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

        // add date to the Set Scene Date menu Item 
        setScenesToDateTitle = "Set Scenes To " + scene.getDateString(); 
        setScenesToDate.setText(setScenesToDateTitle);

        // only allow the setScenesToDate to be shown if the sensor has an
        // acquisition date
        if (!currSensor.hasAcqDate)
            showSetScenesTo = false;

        // configure the next/prev scene menu items
        if (showNextPrev)
        {
            if (!nextPrevPresent)
            {
                // add the next/prev since they are not there currently
                insertMenuItemAfter(prevScene, lower.getText());
                insertMenuItemAfter(nextScene, prevScene.getText());
                nextPrevPresent = true;
            }
            
            // enable/disable the next/prev scene menu items based on the
            // this scene
            if (md.sceneFilter.isPrevDateAvailable(scene))
                prevScene.setEnabled(true);
            else
                prevScene.setEnabled(false);
            if (md.sceneFilter.isNextDateAvailable(scene))
                nextScene.setEnabled(true);
            else
                nextScene.setEnabled(false);
        }
        else
        {
            if (nextPrevPresent)
            {
                // remove the next/prev since they are not wanted currently
                remove(prevScene);
                remove(nextScene);
                nextPrevPresent = false;
            }
        }
        
        // configure the set Scenes Date menu item 
        if (showSetScenesTo)
        {
            if (!setScenesToDatePresent)
            {
                add(setScenesToDate);
                setScenesToDatePresent = true;
            }
        }
        else
        {
            if (setScenesToDatePresent)
            {
                remove(setScenesToDate);
                setScenesToDatePresent = false;
            }
        }

        // configure the rest to default menu item 
        if (showDefaultScene)
        {
            if (!setDefaultScenePresent)
            {
                add(setDefaultScene);
                setDefaultScenePresent = true;
            }
        }
        else
        {
            if (setDefaultScenePresent)
            {
                remove(setDefaultScene);
                setDefaultScenePresent = false;
            }
        }


        // If swath mode is not enabled, a single scene is displayed or the
        // scene list filter is on, remove the "add swath to scene list" and
        // "hide swath" options.  Otherwise, add it.
        if (!(currSensor.hasSwathMode && applet.toolsMenu.isSwathModeEnabled())
            || displayingSingleScene
            || applet.searchLimitDialog.isSceneListFilterEnabled())
        {   
            if (swathScenesPresent == true)
            {
                remove(addSwathScenes);
                remove(hideSwathScenes);
                swathScenesPresent = false;
            }
        }
        else
        {
            if (swathScenesPresent == false)
            {
                insertMenuItemAfter(addSwathScenes, "Add To Scene List");
                insertMenuItemAfter(hideSwathScenes, "Hide Scene");
                swathScenesPresent = true;
            }
        }

        // If sensor does not support the add all feature, a single scene is 
        // displayed or the scene list filter is on, remove the add all to 
        // scene list option. Otherwise, add it.
        if (!currSensor.allowAddAll || displayingSingleScene
                || applet.searchLimitDialog.isSceneListFilterEnabled())
        {   
            if (allScenesPresent == true)
            {
                remove(addAllScenes);
                allScenesPresent = false;
            }
        }
        else
        {
            if (allScenesPresent == false)
            {
                if (swathScenesPresent)
                    insertMenuItemAfter(addAllScenes,"Add Swath To Scene List");
                else
                    insertMenuItemAfter(addAllScenes, "Add To Scene List");
                allScenesPresent = true;
            }
        }

        // If the sensor does not supports the user defined area feature,
        // or the user defined area is not closed, remove the add user
        // defined scenes option.  Otherwise add it.
        if (!currSensor.hasUserDefinedArea ||
            !applet.userDefinedAreaDialog.isUserDefinedAreaClosed())
        {
            if (addUserScenesPresent == true)
            {
                remove(addUserScenes);
                addUserScenesPresent = false;
            }
        }
        else
        {
            if (addUserScenesPresent == false)
            {
                insertMenuItemAfter(addUserScenes, "Add To Scene List");
                addUserScenesPresent = true;
            }
        }

        // Disable/enable remove option depending on scene list
        if (currSensor.sceneList.find(scene) != -1)
        {
            removeScene.setEnabled(true);
        }
        else
        {
            removeScene.setEnabled(false);
        }
            
        // add the intersecting scenes menu if more than one scene
        if ((intersectingScenes != null) && (intersectingScenes.size() > 1))
        {
            selectByScene = new JMenu("Select Scene");
            selectByScene.setFont(applet.normalFont);
            JMenuItem menuItem;

            Enumeration scenes = intersectingScenes.elements();
            while (scenes.hasMoreElements())
            {
                Metadata currScene = (Metadata)scenes.nextElement();
                menuItem = new JMenuItem(currScene.getEntityIDForDisplay() 
                            + " " + currScene.getDateString());
                menuItem.setFont(applet.normalFont);
                menuItem.addActionListener(this);
                selectByScene.add(menuItem);
            }

            // add the select by scene menu to the main menu
            add(selectByScene);
        }
    }

    // insert a menu Item after the item name given
    // ---------------------------------------------
    private void insertMenuItemAfter(JMenuItem newItem, String menuItemName)
    {
        MenuElement[] elements = getSubElements();
        int menuSize = elements.length;

        int insertIndex = -1;
        for (int itemNum = 0; itemNum < menuSize; itemNum++)
        {
            JMenuItem itemTemp = (JMenuItem)elements[itemNum];
            if (itemTemp.getText().equals(menuItemName))
            {
                // note the +2 is to account for the separator in the menu
                // that doesn't get counted by this method
                insertIndex = itemNum + 2;
                break;
            }
        }
        if (insertIndex == -1)
            insertIndex = menuSize;
        insert(newItem, insertIndex);
    }

    // action listener event handler
    //------------------------------
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();

        if (command.equals("Bring To Front"))
            md.setSelectedScene(scene);
        else if (command.equals("Send To Back"))
            md.lowerScene(scene);
        else if (command.equals("Set Point Of Interest"))
        {
            PointOfInterestMapLayer pointOfInterestMapLayer = 
                md.mapLayers.getPointOfInterestMapLayer();
            pointOfInterestMapLayer.setPoint(
                applet.imgArea.getRightClickLatLong());
        }
        else if (command.equals("Add To Scene List"))
            applet.sensorMenu.getCurrentSensor().sceneList.add(scene);
	    else if (command.equals("Hide Scene"))
            applet.hideSceneDialog.hideScene(scene);
        else if (command.equals("Show Metadata"))
            scene.showMetadata();
        else if (command.equals("Show Browse"))
            scene.showBrowse();
        else if (command.equals("Default Scene"))
            md.setDefaultScene(scene);
        else if (command.equals("Previous Avail. Date"))
            md.sceneFilter.prevDate(scene);
        else if (command.equals("Next Avail. Date"))
            md.sceneFilter.nextDate(scene);
        else if (command.equals(setScenesToDateTitle))
            md.setScenesToDate(scene);
        else if (command.equals("Add All To Scene List"))
            md.selectAllScenes(scene);
        else if (command.equals("Add Swath To Scene List"))
            md.selectSwathScenes(scene, false);
        else if (command.equals("Hide Swath"))
            md.selectSwathScenes(scene, true);
        else if (command.equals("Add User Area Scenes"))
            md.selectUserAreaScenes(scene);
        else if (command.equals("Remove From Scene List"))
            applet.sensorMenu.getCurrentSensor().sceneList.remove(scene);
        else if (intersectingScenes != null)
        {
            // must be a scene id, so find it and make it the selected scene
            Enumeration scenes = intersectingScenes.elements();
            String id = command.substring(0,command.indexOf(' '));

            while (scenes.hasMoreElements())
            {
                Metadata currScene = (Metadata)scenes.nextElement();
                if (id.equals(currScene.getEntityIDForDisplay()))
                {
                    md.setSelectedScene(currScene);
                    break;
                }
            }
        }
    }
}

