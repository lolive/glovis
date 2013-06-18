// FileMenu.java implements the File menu for applet operations.
//--------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Observer;
import java.util.Observable;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class FileMenu extends JMenu implements ActionListener, Observer
{
    private imgViewer applet;
    private JMenuItem downloadCellData;
    private JMenuItem downloadSceneListData;

    // class to listen for changes to the scene lists and properly enable
    // or disable the associated menu item
    //-------------------------------------------------------------------
    private class SceneListListener implements ListDataListener
    {
        private JMenuItem menuItem;
        private boolean hiddenList;
        private boolean onlyCheckCurrentSensor;

        // constructor
        //------------
        SceneListListener(JMenuItem menuItem, boolean hiddenList,
                          boolean onlyCheckCurrentSensor)
        {
            this.menuItem = menuItem;
            this.hiddenList = hiddenList;
            this.onlyCheckCurrentSensor = onlyCheckCurrentSensor;
        }

        // method to handle data being added to a list
        //--------------------------------------------
        public void intervalAdded(ListDataEvent event)
        {
            // any time data is added, the menu item can be enabled
            if (!onlyCheckCurrentSensor)
                menuItem.setEnabled(true);
            else
            {
                Sensor sensor = applet.sensorMenu.getCurrentSensor();
                if (sensor.sceneList.getSceneCount() > 0)
                    menuItem.setEnabled(true);
            }
        }

        // method to handle data being removed from a list
        //------------------------------------------------
        public void intervalRemoved(ListDataEvent event)
        {
            // item removed, so need to check whether all the lists are empty.
            // If they are, disable the menu item.
            boolean haveScenes = false;

            if (!onlyCheckCurrentSensor)
            {
                Sensor[] sensors = applet.getSensors();
                for (int i = 0; i < sensors.length; i++)
                {
                    Sensor currSensor = sensors[i];
                    // pick the correct list
                    SceneList list;
                    if (hiddenList)
                        list = currSensor.hiddenSceneList;
                    else
                        list = currSensor.sceneList;

                    // if the list has scenes, set the flag and exit the loop
                    if (list.getSceneCount() > 0)
                    {
                        haveScenes = true;
                        break;
                    }
                }
            }
            else
            {
                Sensor sensor = applet.sensorMenu.getCurrentSensor();
                if (sensor.sceneList.getSceneCount() > 0)
                    haveScenes = true;
            }

            // if there are no scenes, disable the menu item
            if (!haveScenes)
                menuItem.setEnabled(false);
        }

        // method to handle list contents changing (nothing to do)
        //--------------------------------------------------------
        public void contentsChanged(ListDataEvent event) { }
    }

    // constructor
    //------------
    public FileMenu(imgViewer applet)
    {
        // call the parent constructor
        super("File");
        setMnemonic(KeyEvent.VK_F);

        // save the applet reference
        this.applet = applet;
        
        // add the options to save and load the order and hidden scene lists
        JMenuItem saveSceneLists = new JMenuItem("Save All Scene Lists...",
                                                 KeyEvent.VK_S);
        saveSceneLists.addActionListener(this);
        saveSceneLists.setEnabled(false);
        add(saveSceneLists);

        JMenuItem item = new JMenuItem("Load Saved Scene List...",
                                       KeyEvent.VK_L);
        item.addActionListener(this);
        add(item);

        JMenuItem saveHiddenSceneLists
                = new JMenuItem("Save Hidden Scene Lists...", KeyEvent.VK_H);
        saveHiddenSceneLists.addActionListener(this);
        saveHiddenSceneLists.setEnabled(false);
        add(saveHiddenSceneLists);

        item = new JMenuItem("Load Hidden Scene List...", KeyEvent.VK_O);
        item.addActionListener(this);
        add(item);

        addSeparator();

        // add the menu items to download browse and metadata
        item = new JMenuItem("Download Visible Browse & Metadata...",
                             KeyEvent.VK_V);
        item.addActionListener(this);
        add(item);

        downloadCellData = new JMenuItem("Download Cell Browse & Metadata...",
                                         KeyEvent.VK_D);
        downloadCellData.addActionListener(this);
        add(downloadCellData);

        downloadSceneListData 
                = new JMenuItem("Download Scene List Browse & Metadata...",
                                KeyEvent.VK_B);
        downloadSceneListData.addActionListener(this);
        downloadSceneListData.setEnabled(false);
        add(downloadSceneListData);

        // request notification when the state of the scene lists change
        SceneListListener saveListener 
                = new SceneListListener(saveSceneLists, false, false);
        SceneListListener saveHiddenListener 
                = new SceneListListener(saveHiddenSceneLists, true, false);
        SceneListListener downloadSceneListListener
                = new SceneListListener(downloadSceneListData, false, true);
        Sensor[] sensors = applet.getSensors();
        for (int i = 0; i < sensors.length; i++)
        {
            Sensor currSensor = sensors[i];
            currSensor.sceneList.addListDataListener(saveListener);
            currSensor.hiddenSceneList.addListDataListener(saveHiddenListener);
            currSensor.sceneList.addListDataListener(downloadSceneListListener);
        }
    }

    // method to receive observer updates for the scene lists and MosaicData
    // classes
    //----------------------------------------------------------------------
    public void update(Observable ob, Object arg)
    {
        if (ob == applet.md)
        {
            // the MosaicData class updated something, so modify the menu item
            // for downloading the current data cell to have the column and row
            // shown
            Sensor currSensor = applet.sensorMenu.getCurrentSensor();
            NavigationModel nm = currSensor.navModel;
            int gridCol = applet.md.gridCol;
            int gridRow = applet.md.gridRow;
            int cellsToDisplay = currSensor.getNumCellsAtResolution(
                    applet.md.pixelSize);

            String label;
            if (currSensor.hideGridEntry)
            {
                // no grid cell entry shown to the user, so use a generic
                // center cell message
                label = "Download Center Cell Browse & Metadata...";
            }
            else
            {
                // show a message with the column and row
                label = "Download " + nm.getColName() + " " 
                    + nm.getColumnString(gridCol) + ", " + nm.getRowName() + " "
                    + nm.getRowString(gridRow) + " Browse & Metadata...";
            }
            downloadCellData.setText(label);

            // only enable the menu item if not in single scene mode
            downloadCellData.setEnabled(cellsToDisplay != Sensor.SINGLE_SCENE);

            // make sure the scene list download menu item is enabled/disabled
            // properly in case a sensor change happened
            downloadSceneListData.setEnabled(
                        (currSensor.sceneList.getSceneCount() > 0));
        }
    }

    // event handler for the menu selections
    //--------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();

        // open the correct dialog
        if (command.equals("Save All Scene Lists..."))
        {
            SaveOrLoadSceneLists.save(applet, false);
        }
        else if (command.equals("Load Saved Scene List..."))
        {
            SaveOrLoadSceneLists.load(applet, false);
        }
        else if (command.equals("Save Hidden Scene Lists..."))
        {
            SaveOrLoadSceneLists.save(applet, true);
        }
        else if (command.equals("Load Hidden Scene List..."))
        {
            SaveOrLoadSceneLists.load(applet, true);
        }
        else if (command.equals("Download Visible Browse & Metadata..."))
        {
            DownloadData.downloadData(applet, false, false);
        }
        else if (command.equals("Download Scene List Browse & Metadata..."))
        {
            DownloadData.downloadData(applet, false, true);
        }
        else if (command.equals(downloadCellData.getText()))
        {
            DownloadData.downloadData(applet, true, false);
        }
    }
}
