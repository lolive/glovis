// MapLayerMenu.java implements a menu for selecting the types of map layers 
// to display.
//--------------------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import javax.swing.filechooser.FileFilter;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class MapLayerMenu extends JMenu implements ItemListener, 
    ActionListener
{
    private imgViewer applet;           // applet reference
    private ArrayList checkboxList;     // array for the checkboxes
    private MapLayers mapLayers;        // map layers container
    private JMenuItem showAttributesMenuItem; // menu item to open the
                                        // shapefile attributes dialog
    private File currentDirectory;      // holds the most recently opened
                                        // directory so it can be the initial
                                        // directory the next time

    // The ShapeFileFilter class implements a file filter for only showing
    // shapefiles in the file chooser dialog
    //--------------------------------------------------------------------
    class ShapeFileFilter extends FileFilter
    {
        // method to indicate which files can be shown
        //--------------------------------------------
        public boolean accept(File file)
        {
            // allow directories to be shown
            if (file.isDirectory())
                return true;

            // allow files with the extension of ".dbf" to be shown
            String filename = file.getName();
            int index = filename.lastIndexOf('.');
            if (index != -1)
            {
                String extension = filename.substring(index);
                if (extension.equals(".dbf"))
                    return true;
            }

            // the file shouldn't be shown
            return false;
        }

        // return the filter description
        //------------------------------
        public String getDescription()
        {
            return "ShapeFiles";
        }
    }

    // constructor
    public MapLayerMenu(MapLayers mapLayers, imgViewer applet)
    {
        // call the parent constructor, setting the dialog to be modal
        super("Map Layers");
        setMnemonic(KeyEvent.VK_M);

        this.applet = applet;

        // add a select all menu item
        JMenuItem selectAll = new JMenuItem("All Map Layers", KeyEvent.VK_A);
        selectAll.addActionListener(this);
        add(selectAll);

        // remember the map layers object
        this.mapLayers = mapLayers;

        int numMapLayers = mapLayers.getNumberOfLayers();

        // create the checkbox array
        checkboxList = new ArrayList();

        // create the checkboxes
        for (int i = 0; i < numMapLayers; i++)
        {
            MapLayer layer = mapLayers.getLayerAt(i);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(layer.getName(),
                                                           layer.isLayerOn());
            if (!layer.isHidden())
            {
                int shortcut = layer.getMenuShortcut();
                if (shortcut != 0)
                    item.setMnemonic(shortcut);
                item.addItemListener(this);
            }
            else
            {
                // make any hidden map layers not visible in the menu
                item.setVisible(false);
            }
            add(item);
            checkboxList.add(item);
        }

        // add a select none menu item
        JMenuItem selectNone = new JMenuItem("No Map Layers", KeyEvent.VK_N);
        selectNone.addActionListener(this);
        add(selectNone);

        addSeparator();

        // add menu item for customizing the map layer colors
        JMenuItem modifyColorsMenuItem = 
                new JMenuItem("Modify Colors...", KeyEvent.VK_M);
        modifyColorsMenuItem.addActionListener(this);
        add(modifyColorsMenuItem);

        // add a menu item to open a search for address dialog box if it is
        // enabled
        if (!mapLayers.getAddressSearchMapLayer().isHidden())
        {
            JMenuItem addressSearchMenuItem = 
                    new JMenuItem("Search for Address...", KeyEvent.VK_E);
            addressSearchMenuItem.addActionListener(this);
            add(addressSearchMenuItem);
        }

        // add a menu item to open a point of interest dialog box 
        JMenuItem pointOfInterestMapLayer = 
                new JMenuItem("Set Point of Interest...", KeyEvent.VK_O);
        pointOfInterestMapLayer.addActionListener(this);
        add(pointOfInterestMapLayer);

        // add a menu item for opening a dialog to select the scene lists to
        // include in the scene list overlay map layer
        JMenuItem configureSceneList = new JMenuItem(
                    "Configure Scene List Overlay...", KeyEvent.VK_F);
        configureSceneList.addActionListener(this);
        add(configureSceneList);

        // add a menu item to read an ESRI shapefile
        if (applet.grantedPrivileges)
        {
            JMenuItem readShapefileMenuItem = 
                    new JMenuItem("Read Shapefile...", KeyEvent.VK_S);
            readShapefileMenuItem.addActionListener(this);
            add(readShapefileMenuItem);

            showAttributesMenuItem = new JMenuItem(
                "Show Shapefile Attributes...", KeyEvent.VK_H);
            showAttributesMenuItem.addActionListener(this);
            showAttributesMenuItem.setEnabled(false);
            add(showAttributesMenuItem);
        }

        // configure the menu for the current sensor
        configureForSensor(applet.sensorMenu.getCurrentSensor());
    }

    // event handler for the select all/none menu items
    //-------------------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        int numStandardMapLayers = mapLayers.getNumberOfStandardLayers();
        int totalLayers = mapLayers.getNumberOfLayers();

        String command = event.getActionCommand();

        if (command.equals("All Map Layers"))
        {
            // if the lowest resolution for the sensor is greater than 2000
            // meters, consider it as needing low resolution map layer support
            boolean lowRes = false;
            if (applet.sensorMenu.getCurrentSensor().getLowestResolution()
                    > 2000)
            {
                lowRes = true;
            }

            // all lines selected so turn them on and show them
            for (int i = 0; i < numStandardMapLayers; i++)
            {
                MapLayer layer = mapLayers.getLayerAt(i);
                boolean available = layer.isEnabled();

                // enable the map layer selections if not displaying a low 
                // resolution dataset, or if the current map layer has support
                // for low resolutions
                if (available && (!lowRes || layer.isLowResAvailable()))
                {
                    JCheckBoxMenuItem item
                        = (JCheckBoxMenuItem)checkboxList.get(i);
                    item.setState(true);
                    layer.setLayerOn(true);
                }
            }
            mapLayers.showLayers();
        }
        else if (command.equals("No Map Layers"))
        {
            // normal map layers all being turned off, so turn them off and
            // if no user provided shape files are displayed, turn off the map
            // layers
            for (int i = 0; i < numStandardMapLayers; i++)
            {
                JCheckBoxMenuItem item
                        = (JCheckBoxMenuItem)checkboxList.get(i);
                item.setState(false);
                mapLayers.getLayerAt(i).setLayerOn(false);
            }

            // determine whether any non-standard map layers (shapefiles) are
            // displayed.  If so, it is not okay to clear all map layers
            boolean okayToClear = true;
            for (int i = numStandardMapLayers; i < totalLayers; i++)
            {
                if (mapLayers.getLayerAt(i).isLayerOn())
                {
                    okayToClear = false;
                    break;
                }
            }
            if (okayToClear)
                mapLayers.clearLayers();
            else
                mapLayers.showLayers();
        }
        else if (command.equals("Modify Colors..."))
        {
            mapLayers.showModifyLayerColors();
        }
        else if (command.equals("Search for Address..."))
        {
            Point loc = applet.getDialogLoc();
            loc.y += 30;
            applet.searchForAddressDialog.setLocation(loc);
            applet.searchForAddressDialog.setVisible(true);
        }
        else if (command.equals("Set Point of Interest..."))
        {
            PointOfInterestMapLayer pointOfInterestMapLayer = 
                mapLayers.getPointOfInterestMapLayer();
            LatLong latLong = 
                pointOfInterestMapLayer.getPointOfInterestLatLong();
            if (latLong != null)
                applet.pointOfInterestDialog.setLatLong(latLong);
            // position and show dialog box inside the main window
            Point loc = applet.getDialogLoc();
            loc.y += 30;
            applet.pointOfInterestDialog.setLocation(loc);
            applet.pointOfInterestDialog.setVisible(true);
        }
        else if (command.equals("Configure Scene List Overlay..."))
        {
            mapLayers.showSceneOverlayConfiguration();
        }
        else if (command.equals("Read Shapefile..."))
        {
            // read a shapefile
            readShapeFile();
        }
        else if (command.equals("Show Shapefile Attributes..."))
        {
            mapLayers.showAttributes();
        }
    }

    // method to allow reading a shapefile
    //------------------------------------
    private void readShapeFile()
    {
        // create a swing file chooser to allow the user to select a file
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Select shapefile name");

        // only show shapefiles in the file chooser
        fc.setFileFilter(new ShapeFileFilter());

        // start in the last directory that the user was in
        if (currentDirectory != null)
            fc.setCurrentDirectory(currentDirectory);

        int result = fc.showOpenDialog(applet.getDialogContainer());
        if (result == JFileChooser.APPROVE_OPTION)
        {
            int numStandardMapLayers = mapLayers.getNumberOfStandardLayers();
            int totalLayers = mapLayers.getNumberOfLayers();

            // save the directory
            currentDirectory = fc.getCurrentDirectory();

            // get the name of the selected file
            File file = fc.getSelectedFile();
            String fullFilename = file.getName();
            int index = fullFilename.lastIndexOf('.');
            String filename = fullFilename.substring(0, index);

            // verify the shapefile hasn't already been loaded
            boolean alreadyLoaded = false;
            for (int i = numStandardMapLayers; i < totalLayers; i++)
            {
                if (mapLayers.getLayerAt(i).getName().equals(filename))
                {
                    alreadyLoaded = true;
                    break;
                }
            }
            if (alreadyLoaded)
            {
                String [] message = {"The map layer " + filename
                    + "has already been loaded"};

                JOptionPane.showMessageDialog(applet.getDialogContainer(),
                        message,
                        "ShapeFile not loaded", JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                boolean errorEncountered = true;

                // verify the file provided is valid before creating the
                // map layer
                if (ShapeFileMapLayer.isValidShapeFile(file))
                {
                    // create a map layer using the shapefile
                    ShapeFileMapLayer shapefile = new ShapeFileMapLayer(applet,
                            filename, "Shapefile",
                            mapLayers.getAttributesDialog(), file);

                    // if the shapefile map layer was created successfully, add
                    // it to the list
                    if (shapefile.isValid())
                    {
                        // if this is the first shapefile, add a menu
                        // separator
                        if (totalLayers == numStandardMapLayers)
                            addSeparator();

                        mapLayers.addMapLayer(shapefile);
                        int layerIndex = mapLayers.getNumberOfLayers() - 1;
                        MapLayer layer = mapLayers.getLayerAt(layerIndex);
                        JCheckBoxMenuItem item = new JCheckBoxMenuItem(
                                    layer.getName(), layer.isLayerOn());
                        item.addItemListener(this);
                        add(item);
                        checkboxList.add(item);

                        configureForSensor(
                                applet.sensorMenu.getCurrentSensor());
                        setLayerState(layer.getName(),true, true);
                        mapLayers.showLayers();
                        showAttributesMenuItem.setEnabled(true);

                        errorEncountered = false;
                    }
                }
                if (errorEncountered)
                {
                    String [] message = {filename + ".dbf Does not appear "
                            + "to be a supported shapefile.",
                        "Verify the .dbf, .shp, and .shx are readable and "
                            + " that the .prj, ",
                        "if it exists, indicates the file is in "
                            + "geographic coordinates."};

                    JOptionPane.showMessageDialog(applet.getDialogContainer(),
                            message,
                            "ShapeFile not loaded", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // method to check or uncheck the menu item for the requested layer 
    //-----------------------------------------------------------------
    public void setLayerState(String layer, boolean checked, 
                              boolean enabled)
    {
        int numMapLayers = mapLayers.getNumberOfLayers();

        for (int i = 0; i < numMapLayers; i++)
        {
            if (layer.equals(mapLayers.getLayerAt(i).getName()))
            {
                JCheckBoxMenuItem item
                        = (JCheckBoxMenuItem)checkboxList.get(i);
                item.setSelected(checked);
                item.setEnabled(enabled);
                break;
            }
        }
    }
    
    // event handler for menu items changing state
    //--------------------------------------------
    public void itemStateChanged(ItemEvent event)
    {
        boolean changed = false;    // map layer changed flag
        int numMapLayers = mapLayers.getNumberOfLayers();
        String eventCommand = ((JCheckBoxMenuItem)event.getSource())
                                .getActionCommand();

        // Determine which item has changed state
        for (int i = 0; i < numMapLayers; i++)
        {
            MapLayer layer = mapLayers.getLayerAt(i);
            if (eventCommand.equals(layer.getName()))
            {
                // update the state and update the shown lines if the
                // map layer is on
                JCheckBoxMenuItem item
                        = (JCheckBoxMenuItem)checkboxList.get(i);
                layer.setLayerOn(item.getState());
                changed = true;
                break;
            }
        }

        // if nothing changed, just return
        if (!changed)
            return;

        // check if any of the map layers are on
        boolean layersOn = false;
        for (int i = 0; i < numMapLayers; i++)
        {
            // if any types of map layers are selected, turn on the map layers 
            JCheckBoxMenuItem item = (JCheckBoxMenuItem)checkboxList.get(i);
            if (item.getState())
            {
                layersOn = true;
                break;
            }
        }

        if (layersOn)
        {
            // map layers are on, show the layers
            mapLayers.showLayers();
        }
        else
        {
            // map layers are not on, clear the layers
            mapLayers.clearLayers();
        }
    }

    // method to configure the map layers for the current sensor
    //----------------------------------------------------------
    void configureForSensor(Sensor sensor)
    {
        // if the lowest resolution for the sensor is greater than 2000 meters,
        // consider it as needing low resolution map layer support
        boolean lowRes = false;
        if (sensor.getLowestResolution() > 2000)
            lowRes = true;

        boolean layersOn = false; // true if any map layers are on

        int numMapLayers = mapLayers.getNumberOfLayers();

        // decide for each menu item whether it should be enabled
        for (int i = 0; i < numMapLayers; i++)
        {
            MapLayer layer = mapLayers.getLayerAt(i);
            boolean available = layer.isEnabled();
            JCheckBoxMenuItem item = (JCheckBoxMenuItem)checkboxList.get(i);

            // enable the map layer selections if not displaying a low
            // resolution dataset, or if the current map layer has support for
            // low resolutions
            if (available && (!lowRes || layer.isLowResAvailable()))
            {
                item.setEnabled(true);
                if (item.getState())
                {
                    layer.setLayerOn(true);
                    layersOn = true;
                }
            }
            else
            {
                // low resolution not available for this layer, so disable it
                item.setEnabled(false);
                item.setState(false);
                layer.setLayerOn(false);
            }
        }
        mapLayers.layersOn = layersOn;
    }
}
