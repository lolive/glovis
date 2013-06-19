//  SensorMenu.java implements a menu for selecting the currently 
//  displayed sensor.
//-------------------------------------------------------------------
import java.util.Properties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.net.URL;

public class SensorMenu extends JMenu implements ActionListener
{
    private imgViewer applet;           // reference to the applet
    private JCheckBoxMenuItem[] cb;     // checkbox array
    private Sensor currentSensor; // reference to the selected sensor object.
    private Sensor[] sensors;     // list of available sensors
    private String[] sensorNames; // list of sensors, in order
    
    private JMenu aerialMenu; // submenus for the different sensors
    private JMenu aquaMenu;
    private JMenu asterMenu;
    private JMenu combinedAquaTerraMenu;
    private JMenu eo1Menu;
    private JMenu landsatArchiveMenu;
    private JMenu glsMenu;
    private JMenu landsatMrlcMenu;
    private JMenu landsatLegacyMenu;
    private JMenu terraLookMenu;
    private JMenu terraMenu;

    // menu items for the descriptions for the different collection areas
    private JMenuItem aerialDescription;
    private JMenuItem aquaDescription;
    private JMenuItem asterDescription;
    private JMenuItem combinedAquaTerraDescription;
    private JMenuItem eo1Description;
    private JMenuItem landsatArchiveDescription;
    private JMenuItem glsDescription;
    private JMenuItem landsatMrlcDescription;
    private JMenuItem landsatLegacyDescription;
    private JMenuItem terraDescription;
    private JMenuItem terraLookDescription;

    // class structure to store the sensor object and its corresponding menu
    private class SensorMenuInfo
    {
        public Sensor sensor;
        public String description;
        public JMenu menu;
    }

    // constructor
    // applet - the imgViewer applet
    // gvProp - properties read from GloVis.Properties
    // startSensorName - name of the sensor to start on
    // limitCollectionMenu - flags whether to only show the requested sensor
    public SensorMenu(imgViewer applet, Properties gvProp,
        String startSensorName, boolean limitCollectionMenu)
        throws RuntimeException
    {
        // call the parent constructor, calling this the Collection menu
        super("Collection");
        setMnemonic(KeyEvent.VK_C);

        // save the applet references
        this.applet = applet;

        initializeSensorNames(); // all possible sensors are in sensorNames[]

        // check and see how many sensors will actually be created
        int numUsedSensors = 0;
        for (int i = 0; i < sensorNames.length; i++)
        {
            if (isUsed(sensorNames[i], gvProp))
            {
                numUsedSensors++;
            }
        }
        if (numUsedSensors < 1)
        {
            // serious error - cannot continue because so much of the applet
            // assumes that SensorMenu.getCurrentSensor() returns something
            JOptionPane.showMessageDialog(applet.getDialogContainer(),
                "No datasets are available.",
                "GloVis Configuration Error",
                JOptionPane.ERROR_MESSAGE);
            // nothing to add to the menu
            throw new RuntimeException("No Datasets Configured in Properties");
        }

        // allocate an array for the sensors in use
        sensors = new Sensor[numUsedSensors];

        // allocate an array (same size) for the checkboxes
        cb = new JCheckBoxMenuItem[numUsedSensors];

        // create the submenus for the different sensor types
        //     (now called "Collections," or "missions" in the GloVis main page)
        // if limitCollectionMenu is true, then disable the submenu
        //     and one of them will be enabled below based on startSensorIndex
        //     (JMenu objects are enabled by default)
        // only the enabled menus that have sensor submenus
        //     will be added to the main menu
        landsatArchiveMenu = new JMenu("Landsat Archive");
        landsatArchiveMenu.setMnemonic(KeyEvent.VK_L);
        if (limitCollectionMenu)
            landsatArchiveMenu.setEnabled(false);
        glsMenu = new JMenu("Global Land Survey");
        glsMenu.setMnemonic(KeyEvent.VK_G);
        if (limitCollectionMenu)
            glsMenu.setEnabled(false);
        landsatMrlcMenu = new JMenu("Landsat MRLC Collections");
        landsatMrlcMenu.setMnemonic(KeyEvent.VK_C);
        if (limitCollectionMenu)
            landsatMrlcMenu.setEnabled(false);
        landsatLegacyMenu = new JMenu("Landsat Legacy Collections");
        landsatLegacyMenu.setMnemonic(KeyEvent.VK_N);
        if (limitCollectionMenu)
            landsatLegacyMenu.setEnabled(false);
        asterMenu = new JMenu("ASTER");
        asterMenu.setMnemonic(KeyEvent.VK_A);
        if (limitCollectionMenu)
            asterMenu.setEnabled(false);
        aquaMenu = new JMenu("MODIS Aqua");
        aquaMenu.setMnemonic(KeyEvent.VK_M);
        if (limitCollectionMenu)
            aquaMenu.setEnabled(false);
        terraMenu = new JMenu("MODIS Terra");
        terraMenu.setMnemonic(KeyEvent.VK_O);
        if (limitCollectionMenu)
            terraMenu.setEnabled(false);
        combinedAquaTerraMenu = new JMenu("MODIS Combined");
        combinedAquaTerraMenu.setMnemonic(KeyEvent.VK_D);
        if (limitCollectionMenu)
            combinedAquaTerraMenu.setEnabled(false);
        eo1Menu = new JMenu("EO-1");
        eo1Menu.setMnemonic(KeyEvent.VK_E);
        if (limitCollectionMenu)
            eo1Menu.setEnabled(false);
        terraLookMenu = new JMenu("TerraLook");
        terraLookMenu.setMnemonic(KeyEvent.VK_T);
        if (limitCollectionMenu)
            terraLookMenu.setEnabled(false);
        aerialMenu = new JMenu("Aerial");
        aerialMenu.setMnemonic(KeyEvent.VK_R);
        if (limitCollectionMenu)
            aerialMenu.setEnabled(false);

        // create menu items for the Data Description entries on the different
        // sub-menus
        aerialDescription = new JMenuItem("Data Descriptions");
        aerialDescription.addActionListener(this);
        aquaDescription = new JMenuItem("Data Descriptions");
        aquaDescription.addActionListener(this);
        asterDescription = new JMenuItem("Data Descriptions");
        asterDescription.addActionListener(this);
        combinedAquaTerraDescription = new JMenuItem("Data Descriptions");
        combinedAquaTerraDescription.addActionListener(this);
        eo1Description = new JMenuItem("Data Descriptions");
        eo1Description.addActionListener(this);
        landsatArchiveDescription = new JMenuItem("Data Descriptions");
        landsatArchiveDescription.addActionListener(this);
        glsDescription = new JMenuItem("Data Descriptions");
        glsDescription.addActionListener(this);
        landsatMrlcDescription = new JMenuItem("Data Descriptions");
        landsatMrlcDescription.addActionListener(this);
        landsatLegacyDescription = new JMenuItem("Data Descriptions");
        landsatLegacyDescription.addActionListener(this);
        terraDescription = new JMenuItem("Data Descriptions");
        terraDescription.addActionListener(this);
        terraLookDescription = new JMenuItem("Data Descriptions");
        terraLookDescription.addActionListener(this);

        // select first sensor from the compacted list to start applet with.
        // in loop below, if the requested sensor is created, set the
        // startSensorIndex to the index corresponding to the sensor requested
        int startSensorIndex = -1; // default

        // for each sensor used, create the sensor object and add a
        // checkbox to the sensor menu
        int index = 0; // index into sensors[] array and cb[] array
        // (index is sensors actually used, i is all possible in sensorNames)
        for (int i = 0; i < sensorNames.length; i++)
        {
            // create a pointer to the stored sensor objects and their 
            // corresponding menus
            SensorMenuInfo info;

            // create a sensor object and checkbox only if the sensor is used
            if (isUsed(sensorNames[i], gvProp))
            {
                // create the sensor object and set the submenu for sensor i
                info = sensorFactory(sensorNames[i]);
                sensors[index] = info.sensor;
                if (!canOrderOrDownload(sensorNames[i], gvProp))
                {
                    sensors[index].isOrderable = false; // override
                    sensors[index].isDownloadable = false; // override this too
                }

                // create new checkbox
                cb[index] = new JCheckBoxMenuItem(sensors[index].sensorName,
                                                                        false);
                cb[index].addActionListener(this);
                if (info.description != null)
                    cb[index].setToolTipText(info.description);

                // add a separator before the Landsat combined entry
                if (sensorNames[i].equals("COMBINED"))
                {
                    info.menu.addSeparator();
                }

                // add checkbox to correct submenu
                info.menu.add(cb[index]);

                if (sensorNames[i].equalsIgnoreCase(startSensorName))
                {
                    // we're up to the sensor that was requested as the start
                    if (info.sensor.confirmInitialDisplay())
                    {
                        startSensorIndex = index;
                    }

                    // if we are limiting the collection menu,
                    // this is the one to enable, so
                    // set this sensor's parent menu to be enabled
                    // (even if sensor wasn't confirmed...)
                    info.menu.setEnabled(true);
                }

                index++;
            }
        }

        // add the sensor menus to the main menu, with separators between the
        // sensors if the menus have datasets that are used.
        if (aerialMenu.getItemCount() > 0 && aerialMenu.isEnabled())
        {
            // add the description menu item to top of the list
            aerialMenu.insert(aerialDescription,0);
            aerialMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(aerialMenu);
        }

        if (asterMenu.getItemCount() > 0 && asterMenu.isEnabled())
        {
            // add the description menu item to top of the list
            asterMenu.insert(asterDescription,0);
            asterMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(asterMenu);
        }

        if (eo1Menu.getItemCount() > 0 && eo1Menu.isEnabled())
        {
            // add the description menu item to top of the list
            eo1Menu.insert(eo1Description,0);
            eo1Menu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(eo1Menu);
        }

        if (landsatArchiveMenu.getItemCount() > 0 && landsatArchiveMenu.isEnabled())
        {
            // add the description menu item to top of the list
            landsatArchiveMenu.insert(landsatArchiveDescription,0);
            landsatArchiveMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(landsatArchiveMenu);
        }

        if (glsMenu.getItemCount() > 0 && glsMenu.isEnabled())
        {
            // add the description menu item to top of the list
            glsMenu.insert(glsDescription,0);
            glsMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(glsMenu);
        }

        if (landsatMrlcMenu.getItemCount() > 0 && landsatMrlcMenu.isEnabled())
        {
            // add the description menu item to top of the list
            landsatMrlcMenu.insert(landsatMrlcDescription,0);
            landsatMrlcMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(landsatMrlcMenu);
        }

        if (landsatLegacyMenu.getItemCount() > 0 && landsatLegacyMenu.isEnabled())
        {
            // add the description menu item to top of the list
            landsatLegacyMenu.insert(landsatLegacyDescription,0);
            landsatLegacyMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(landsatLegacyMenu);
        }

        if (aquaMenu.getItemCount() > 0 && aquaMenu.isEnabled())
        {
            // add the menu item to open the long names definitions to the 
            // top of the aqua list
            aquaMenu.insert(aquaDescription,0);
            aquaMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(aquaMenu);
        }

        if (terraMenu.getItemCount() > 0 && terraMenu.isEnabled())
        {
            // add the menu item to open the long names definitions to the 
            // top of the terra list
            terraMenu.insert(terraDescription,0);
            terraMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(terraMenu);
        }

        if (combinedAquaTerraMenu.getItemCount() > 0 && combinedAquaTerraMenu.isEnabled())
        {
            // add the menu item to open the long names definitions to the 
            // top of the combinedAquaTerra list
            combinedAquaTerraMenu.insert(combinedAquaTerraDescription,0);
            combinedAquaTerraMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(combinedAquaTerraMenu);
        }

        if (terraLookMenu.getItemCount() > 0 && terraLookMenu.isEnabled())
        {
            // add the description menu item to top of the list
            terraLookMenu.insert(terraLookDescription,0);
            terraLookMenu.insertSeparator(1);

            if (getItemCount() > 0)
                addSeparator();

            add(terraLookMenu);
        }

        // start with the correct sensor
        if (startSensorIndex < 0)
        {
            startSensorIndex = 0; // should never get here, but need valid index
        }
        currentSensor = sensors[startSensorIndex];
        setSelectedCB(currentSensor.sensorName);
    }

    // helper method to set the correct checkboxes to selected
    //--------------------------------------------------------
    private int setSelectedCB(String sensorName)
    {
        int sensorIndex = 0;     // selected sensor's index in sensors[]
        // find the index for the selected sensor and update the checked items
        for (int i = 0; i < sensors.length; i++)
        {
            if (sensorName.equals(sensors[i].sensorName))
            {
                // set selected sensor number
                sensorIndex = i;

                cb[i].setState(true);
            }
            else  // clear other checkboxes
            {
                cb[i].setState(false);
            }
        }

        // return currently selected sensor index
        return sensorIndex;
    }

    // Establish all of the sensor names we will look for in the Properties.
    // These should be grouped by the higher-level collections,
    // in the order they appear.
    //---------------------------------------------------------------------
    private void initializeSensorNames()
    {
        int index = 0;
        sensorNames = new String[105];
        // AERIAL
        sensorNames[index++] = "NAPP";
        sensorNames[index++] = "NHAP_BW";
        sensorNames[index++] = "NHAP_CIR";
        // ASTER
        sensorNames[index++] = "ASTERVNIR";
        sensorNames[index++] = "ASTERTIR";
        sensorNames[index++] = "ASTER_VNIR_DATAPOOL";
        sensorNames[index++] = "ASTER_TIR_DATAPOOL";
        // EO1
        sensorNames[index++] = "ALI";
        sensorNames[index++] = "HYP";
        // LANDSAT_ARCHIVE
        sensorNames[index++] = "8OLI";
        sensorNames[index++] = "SLCOFF";
        sensorNames[index++] = "ETM";
        sensorNames[index++] = "TM";
        sensorNames[index++] = "4_5MSS";
        sensorNames[index++] = "1_3MSS";
        sensorNames[index++] = "COMBINED";
        // GLS
        sensorNames[index++] = "GLS2010";
        sensorNames[index++] = "GLS2010_EO1";
        sensorNames[index++] = "GLS2005";
        sensorNames[index++] = "GLS2005_EO1";
        sensorNames[index++] = "GLS2000";
        sensorNames[index++] = "GLS1990";
        sensorNames[index++] = "GLS1975_4_5MSS";
        sensorNames[index++] = "GLS1975_1_3MSS";
        // LANDSAT_MRLC
        sensorNames[index++] = "MRLC_2001TC";
        sensorNames[index++] = "MRLC_2001RA";
        // LANDSAT_LEGACY
        sensorNames[index++] = "ETM_MOSAIC";
        sensorNames[index++] = "TM_MOSAIC";
        sensorNames[index++] = "PANSHARPETM";
        sensorNames[index++] = "ORTHOETM";
        sensorNames[index++] = "ORTHOTM";
        sensorNames[index++] = "ORTHO4_5MSS";
        sensorNames[index++] = "ORTHO1_3MSS";
        sensorNames[index++] = "SYSTEMATIC_L1G";
        sensorNames[index++] = "NALC";
        // AQUA
        sensorNames[index++] = "MYD09A1";
        sensorNames[index++] = "MYD09GA";
        sensorNames[index++] = "MYD09GQ";
        sensorNames[index++] = "MYD09Q1";
        sensorNames[index++] = "MYD11A1DAY";
        sensorNames[index++] = "MYD11A1NIGHT";
        sensorNames[index++] = "MYD11A2DAY";
        sensorNames[index++] = "MYD11A2NIGHT";
        sensorNames[index++] = "MYD11B1DAY";
        sensorNames[index++] = "MYD11B1NIGHT";
        sensorNames[index++] = "MYD13A1EVI";
        sensorNames[index++] = "MYD13A1NDVI";
        sensorNames[index++] = "MYD13A2EVI";
        sensorNames[index++] = "MYD13A2NDVI";
        sensorNames[index++] = "MYD13A3EVI";
        sensorNames[index++] = "MYD13A3NDVI";
        sensorNames[index++] = "MYD13Q1EVI";
        sensorNames[index++] = "MYD13Q1NDVI";
        sensorNames[index++] = "MYD14A1";
        sensorNames[index++] = "MYD14A2";
        sensorNames[index++] = "MYD15A2FPAR";
        sensorNames[index++] = "MYD15A2LAI";
        sensorNames[index++] = "MYD17A2GPP";
        sensorNames[index++] = "MYD17A2NETPSN";
        // TERRA
        sensorNames[index++] = "MOD09A1";
        sensorNames[index++] = "MOD09GA";
        sensorNames[index++] = "MOD09GQ";
        sensorNames[index++] = "MOD09Q1";
        sensorNames[index++] = "MOD11A1DAY";
        sensorNames[index++] = "MOD11A1NIGHT";
        sensorNames[index++] = "MOD11A2DAY";
        sensorNames[index++] = "MOD11A2NIGHT";
        sensorNames[index++] = "MOD11B1DAY";
        sensorNames[index++] = "MOD11B1NIGHT";
        sensorNames[index++] = "MOD13A1EVI";
        sensorNames[index++] = "MOD13A1NDVI";
        sensorNames[index++] = "MOD13A2EVI";
        sensorNames[index++] = "MOD13A2NDVI";
        sensorNames[index++] = "MOD13A3EVI";
        sensorNames[index++] = "MOD13A3NDVI";
        sensorNames[index++] = "MOD13Q1EVI";
        sensorNames[index++] = "MOD13Q1NDVI";
        sensorNames[index++] = "MOD14A1";
        sensorNames[index++] = "MOD14A2";
        sensorNames[index++] = "MOD15A2FPAR";
        sensorNames[index++] = "MOD15A2LAI";
        sensorNames[index++] = "MOD17A2GPP";
        sensorNames[index++] = "MOD17A2NETPSN";
        sensorNames[index++] = "MOD17A3GPP";
        sensorNames[index++] = "MOD17A3NPP";
        sensorNames[index++] = "MOD44BVCF";
        // MODIS_COMBINED
        sensorNames[index++] = "MCD15A2FPAR";
        sensorNames[index++] = "MCD15A2LAI";
        sensorNames[index++] = "MCD15A3FPAR";
        sensorNames[index++] = "MCD15A3LAI";
        sensorNames[index++] = "MCD43A1";
        sensorNames[index++] = "MCD43A2";
        sensorNames[index++] = "MCD43A3";
        sensorNames[index++] = "MCD43A4";
        sensorNames[index++] = "MCD43B1";
        sensorNames[index++] = "MCD43B2";
        sensorNames[index++] = "MCD43B3";
        sensorNames[index++] = "MCD43B4";
        // TERRALOOK
        sensorNames[index++] = "TERRALOOK_ASTERVNIR";
        sensorNames[index++] = "TERRALOOK_GLS2010";
        sensorNames[index++] = "TERRALOOK_GLS2005";
        sensorNames[index++] = "TERRALOOK_GLS2000";
        sensorNames[index++] = "TERRALOOK_GLS1990";
        sensorNames[index++] = "TERRALOOK_GLS1975_L4_5";
        sensorNames[index++] = "TERRALOOK_GLS1975_L1_3";
    }

    // helper method to determine whether to use a sensor at all
    //----------------------------------------------------------
    private boolean isUsed(String sensorName, Properties gvProp)
    {
        // It may be helpful to account for tie-ins, like COMBINED
        // its components, or ASTERVNIR and TERRALOOK_ASTERVNIR
        if (gvProp.getProperty(sensorName) != null
            && (gvProp.getProperty(sensorName).equalsIgnoreCase("enabled")
             || gvProp.getProperty(sensorName).equalsIgnoreCase("view only")))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    // helper method to determine whether a sensor allows ordering/downloading
    // -----------------------------------------------------------------------
    private boolean canOrderOrDownload(String sensorName, Properties gvProp)
    {
        // first check the global setting
        if (   gvProp.getProperty("ordersallowed") == null
            || gvProp.getProperty("ordersallowed").equalsIgnoreCase("FALSE")
            || gvProp.getProperty("ordersallowed").equalsIgnoreCase("NO"))
        {
            return false;
        }

        // check this sensor - we only get to this helper method if isUsed()
        // is true, so we know gvProp.getProperty(sensorName) is not null
        if (gvProp.getProperty(sensorName).equalsIgnoreCase("view only"))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    // actionListener event handler
    //-----------------------------
    public void actionPerformed(ActionEvent event)
    {
        String url = null;

        if (event.getActionCommand().equals("Data Descriptions"))
        {
            JMenuItem source = (JMenuItem)event.getSource();
            if (source == aerialDescription)
                url = "../AboutBrowse.shtml#aerialdescription";
            else if (source == aquaDescription)
                url = "../AboutBrowse.shtml#aquanames";
            else if (source == asterDescription)
                url = "../AboutBrowse.shtml#asterdescription";
            else if (source == combinedAquaTerraDescription)
                url = "../AboutBrowse.shtml#combinedaquaterranames";
            else if (source == eo1Description)
                url = "../AboutBrowse.shtml#eo1description";
            else if (source == landsatArchiveDescription)
                url = "../AboutBrowse.shtml#landsatarchivedescription";
            else if (source == glsDescription)
                url = "../AboutBrowse.shtml#glsdescription";
            else if (source == landsatMrlcDescription)
                url = "../AboutBrowse.shtml#landsatmrlcdescription";
            else if (source == landsatLegacyDescription)
                url = "../AboutBrowse.shtml#landsatlegacydescription";
            else if (source == terraDescription)
                url = "../AboutBrowse.shtml#terranames";
            else if (source == terraLookDescription)
                url = "../AboutBrowse.shtml#terralookdescription";
        }

        if (url != null)
        {
            // open a new window to the web page with the requested URL
            try 
            {
                URL showURL = new URL(applet.getCodeBase(), url);
                applet.getAppletContext().showDocument(showURL,"glovishelp");
            }
            catch (Exception except)
            {
                System.out.println("exception: "+ except);
            }
        }
        else
        {
            // not MODIS long name request, so must be a new sensor selected
            int sensorIndex = 0;     // selected sensor

            // set the new checkbox and clear previously selected box
            String sensorName = event.getActionCommand();
            sensorIndex = setSelectedCB(sensorName);

            // if a new sensor was chosen, update the application
            if (currentSensor != sensors[sensorIndex])
            {
                // first, make sure the user wants to continue to the new sensor
                if (!sensors[sensorIndex].confirmInitialDisplay())
                {
                    setSelectedCB(currentSensor.sensorName); // revert
                    return;
                }

                // we're still here, so switch everything to the new sensor
                currentSensor = sensors[sensorIndex];

                // TBD - should probably use the observer/observable pattern
                // for notification of when the sensor changes
                applet.mapLayerMenu.configureForSensor(currentSensor);
                applet.imgArea.md.setSensor(currentSensor);
                applet.searchLimitDialog.configureForSensor(currentSensor);
                applet.sceneListPanel.setSensor(currentSensor);
                applet.hideSceneDialog.setSensor(currentSensor);
                applet.sceneListDialog.setSensor(currentSensor);
                applet.helpMenu.setSensor(currentSensor);
                applet.imgArea.logo.setSensor(currentSensor);
                applet.searchForSceneDialog.setSensor(currentSensor);
                applet.toolsMenu.setSensor(currentSensor);
            }
        }
    }

    // method to return the currently selected sensor's object
    //--------------------------------------------------------
    public Sensor getCurrentSensor()
    {
        return currentSensor;
    }

    // method to return an array of the available sensors
    //---------------------------------------------------
    public Sensor[] getSensors()
    {
        return sensors;
    }

    // method to set the sensor and menu values of of the info class to the 
    // sensor object and submenu specificed by the given sensor name
    //-------------------------------------------------------------------
    private SensorMenuInfo sensorFactory(String sensorToCreate)
    {
        SensorMenuInfo info = new SensorMenuInfo();
        // If we were using Java 7, we could use a String in a switch!

        if (sensorToCreate.equals("8OLI"))
        {
                info.sensor = new Landsat8OLISensor(applet);
                info.menu = landsatArchiveMenu;
                info.description = "Landsat 8 Operational Land Imager "
                    + "- 2013 to present";
        }
        else if (sensorToCreate.equals("ETM"))
        {
                info.sensor = new LandsatETMSensor(applet);
                info.menu = landsatArchiveMenu;
                info.description = "Landsat 7 Enhanced Thematic Mapper Plus "
                    + "- 1999 to May 2003";
        }
        else if (sensorToCreate.equals("SLCOFF"))
        {
                info.sensor = new LandsatETMSlcOffSensor(applet);
                info.menu = landsatArchiveMenu;
                info.description = "Landsat 7 Enhanced Thematic Mapper Plus "
                    + "Scan Line Corrector Off - July 2003 to present";
        }
        else if (sensorToCreate.equals("TM"))
        {
                info.sensor = new LandsatTMSensor(applet);
                info.menu = landsatArchiveMenu;
                info.description = "Landsats 4 and 5 Thematic Mapper - 1982 "
                    + "to present";
        }
        else if (sensorToCreate.equals("4_5MSS"))
        {
                info.sensor = new Landsat4_5MssSensor(applet);
                info.menu = landsatArchiveMenu;
                info.description = "Landsats 4 and 5 Multispectral Scanner - "
                    + "1982 to 1992";
        }
        else if (sensorToCreate.equals("1_3MSS"))
        {
                info.sensor = new Landsat1_3MssSensor(applet);
                info.menu = landsatArchiveMenu;
                info.description = "Landsats 1,2 and 3 Multispectral Scanner "
                    + "- 1972 to 1983";
        }

        else if (sensorToCreate.equals("GLS2010"))
        {
                info.sensor = new Gls2010Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 2010; "
                    + "Landsat 7 ETM+ and Landsat 5 TM (2008-2011)";
        }

        else if (sensorToCreate.equals("GLS2010_EO1"))
        {
                info.sensor = new Gls2010EO1Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 2010; "
                    + "Earth Observing One (2009-2011)";
        }

        else if (sensorToCreate.equals("GLS2005"))
        {
                info.sensor = new Gls2005Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 2005; "
                    + "Landsat 7 ETM+ and Landsat 5 TM (2003-2008)";
        }

        else if (sensorToCreate.equals("GLS2005_EO1"))
        {
                info.sensor = new Gls2005EO1Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 2005; "
                    + "Earth Observing One (2004-2008)";
        }

        else if (sensorToCreate.equals("GLS2005_ANTARCTICA"))
        {
                info = null; // NOT IMPLEMENTED YET
// DEBUG
System.out.println("Sensor  " + sensorToCreate + " NOT IMPLEMENTED YET");
        }

        else if (sensorToCreate.equals("GLS2000"))
        {
                info.sensor = new Gls2000Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 2000; "
                    + "Landsat 7 ETM+ and Landsat 5 TM (1999-2003)";
        }

        else if (sensorToCreate.equals("GLS1990"))
        {
                info.sensor = new Gls1990Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 1990; "
                    + "Landsat 4-5 TM (1987-1997)";
        }

        else if (sensorToCreate.equals("GLS1975_4_5MSS"))
        {
                info.sensor = new Gls1975Mss4_5Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 1975; "
                    + "Landsat 4-5 MSS (1982-1987)";
        }

        else if (sensorToCreate.equals("GLS1975_1_3MSS"))
        {
                info.sensor = new Gls1975Mss1_3Dataset(applet);
                info.menu = glsMenu;
                info.description = "Global Land Survey 1975; "
                    + "Landsat 1-3 MSS (1972-1983)";
        }

        else if (sensorToCreate.equals("MRLC_2001TC"))
        {
                info.sensor = new Mrlc2001TCDataset(applet);
                info.menu = landsatMrlcMenu;
                info.description = "Multi-resolution Land Characterization - "
                    + "Terrain Corrected";
        }

        else if (sensorToCreate.equals("MRLC_2001RA"))
        {
                info.sensor = new Mrlc2001RADataset(applet);
                info.menu = landsatMrlcMenu;
                info.description = "Multi-resolution Land Characterization - "
                    + "Reflectance Adjusted";
        }

        else if (sensorToCreate.equals("ORTHOTM"))
        {
                info.sensor = new OrthoTMDataset(applet, "TM (1987-1997)",
                  "ESAT_TM",
                  "https://lta.cr.usgs.gov/Tri_Dec_GLOO",
                  true);
                info.menu = landsatLegacyMenu;
                info.description = "Tri-Decadal Global Landsat Orthorectified "
                    + "Thematic Mapper (1987-1997)";
        }

        else if (sensorToCreate.equals("ORTHOETM"))
        {
                info.sensor = new OrthoETMDataset(applet, "ETM+ (1999-2003)",
                  "ESAT_ETM_NOPAN",
                  "https://lta.cr.usgs.gov/Tri_Dec_GLOO",
                  true);
                info.menu = landsatLegacyMenu;
                info.description = "Tri-Decadal Global Landsat Orthorectified "
                    + "Enhanced Thematic Mapper Plus (1999-2003)";
        }

        else if (sensorToCreate.equals("PANSHARPETM"))
        {
                info.sensor = new OrthoPanSharpETMDataset(applet);
                info.menu = landsatLegacyMenu;
                info.description = "Tri-Decadal Global Landsat Orthorectified "
                    + "Panchromatic Sharpened Enhanced Thematic Mapper Plus "
                    + "(1999-2003)";
        }

        else if (sensorToCreate.equals("ORTHO1_3MSS"))
        {
                info.sensor = new Ortho1_3MssDataset(applet,
                  "MSS 1-3 (1972-1983)", "ORTHO_MSS_SCENE",
                  "https://lta.cr.usgs.gov/Tri_Dec_GLOO",
                  true);
                info.menu = landsatLegacyMenu;
                info.description = "Tri-Decadal Global Landsat Orthorectified "
                    + "Multispectral Scanner 1-3 (1972-1983)";
        }

        else if (sensorToCreate.equals("ORTHO4_5MSS"))
        {
                info.sensor = new Ortho4_5MssDataset(applet,
                                                     "MSS 4-5 (1982-1987)", 
                                                     "ORTHO_MSS_SCENE", true);
                info.menu = landsatLegacyMenu;
                info.description = "Tri-Decadal Global Landsat Orthorectified "
                    + "Multispectral Scanner 4-5 (1982-1987)";
        }

        else if (sensorToCreate.equals("SYSTEMATIC_L1G"))
        {
                info.sensor = new SystematicL1GDataset(applet);
                info.menu = landsatLegacyMenu;
                info.description = "Landsat 7 Enhanced Thematic Mapper Plus "
                    + "Systematically Corrected (1999-May 2003)";
        }

        else if (sensorToCreate.equals("ASTERVNIR"))
        {
                info.sensor = new AsterVNIRSensor(applet);
                info.menu = asterMenu;
                info.description = "Advanced Spaceborne Thermal Emission and "
                    + "Reflection Radiometer Level 1A Day "
                    + "(VNIR data shown)";
        }

        else if (sensorToCreate.equals("ASTERTIR"))
        {
                info.sensor = new AsterTIRSensor(applet);
                info.menu = asterMenu;
                info.description = "Advanced Spaceborne Thermal Emission and "
                    + "Reflection Radiometer Level 1A Night "
                    + "(TIR data shown)";
        }

        else if (sensorToCreate.equals("ASTER_VNIR_DATAPOOL"))
        {
                info.sensor = new AsterVNIRDataPoolSensor(applet);
                info.menu = asterMenu;
                info.description = "Advanced Spaceborne Thermal Emission and "
                    + "Reflection Radiometer Level 1B US Day "
                    + "(VNIR data shown)";
        }

        else if (sensorToCreate.equals("ASTER_TIR_DATAPOOL"))
        {
                info.sensor = new AsterTIRDataPoolSensor(applet);
                info.menu = asterMenu;
                info.description = "Advanced Spaceborne Thermal Emission and "
                    + "Reflection Radiometer Level 1B US Night "
                    + "(TIR data shown)";
        }

        else if (sensorToCreate.equals("MOD09A1"))
        {
                info.sensor = new ModisSensor(applet,"MOD09A1",
                    "modis/mod09a1", "LPDAAC_MODIS", "MOD09A1", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Reflectance 8-Day L3 "
                    + "Global 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MOD09GA"))
        {
                info.sensor = new ModisSensor(applet,"MOD09GA",
                    "modis/mod09ga", "LPDAAC_MODIS", "MOD09GA", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Reflectance Daily L2G "
                    + "Global 1km and 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MOD09GQ"))
        {
                info.sensor = new ModisSensor(applet,"MOD09GQ",
                    "modis/mod09gq", "LPDAAC_MODIS", "MOD09GQ", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Reflectance Daily L2G "
                    + "Global 250m SIN Grid";
        }

        else if (sensorToCreate.equals("MOD09Q1"))
        {
                info.sensor = new ModisSensor(applet,"MOD09Q1",
                    "modis/mod09q1", "LPDAAC_MODIS", "MOD09Q1", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Reflectance Daily L2G "
                    + "Global 250m SIN Grid";
        }

        else if (sensorToCreate.equals("MOD11A1DAY"))
        {
                info.sensor = new ModisSensor(applet,"MOD11A1 Day",
                    "modis/mod11a1_day", "LPDAAC_MODIS", "MOD11A1_DAY", true);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Temperature/Emissivity"
                    + " Daily L3 Global 1km SIN Grid Day";
        }

        else if (sensorToCreate.equals("MOD11A1NIGHT"))
        {
                info.sensor = new ModisSensor(applet,"MOD11A1 Night",
                    "modis/mod11a1_night", "LPDAAC_MODIS", "MOD11A1_NIGHT", true);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Temperature/Emissivity"
                    + " Daily L3 Global 1km SIN Grid Night";
        }

        else if (sensorToCreate.equals("MOD11A2DAY"))
        {
                info.sensor = new ModisSensor(applet,"MOD11A2 Day",
                     "modis/mod11a2_day", "LPDAAC_MODIS", "MOD11A2_DAY", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Temperature/Emissivity"
                    + " 8-Day L3 Global 1km SIN Grid Day";
        }

        else if (sensorToCreate.equals("MOD11A2NIGHT"))
        {
                info.sensor = new ModisSensor(applet,"MOD11A2 Night",
                    "modis/mod11a2_night", "LPDAAC_MODIS", "MOD11A2_NIGHT", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Temperature/Emissivity"
                    + " 8-Day L3 Global 1km SIN Grid Night";
        }

        else if (sensorToCreate.equals("MOD11B1DAY"))
        {
                info.sensor = new ModisSensor(applet,"MOD11B1 Day",
                    "modis/mod11b1_day", "LPDAAC_MODIS", "MOD11B1_DAY", true);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Temperature/Emissivity"
                    + " Daily L3 Global 5km SIN Grid Day";
        }

        else if (sensorToCreate.equals("MOD11B1NIGHT"))
        {
                info.sensor = new ModisSensor(applet,"MOD11B1 Night",
                    "modis/mod11b1_night", "LPDAAC_MODIS", "MOD11B1_NIGHT", true);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Surface Temperature/Emissivity"
                    + " Daily L3 Global 5km SIN Grid Night";
        }

        else if (sensorToCreate.equals("MOD13A1EVI"))
        {
                info.sensor = new ModisSensor(applet,"MOD13A1 EVI",
                    "modis/mod13a1_evi", "LPDAAC_MODIS", "MOD13A1_EVI", true); 
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices 16-Day L3 "
                        + "Global 500m SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MOD13A1NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MOD13A1 NDVI", 
                    "modis/mod13a1_ndvi", "LPDAAC_MODIS", "MOD13A1_NDVI", true);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices 16-Day L3 "
                    + "Global 500m SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MOD13A2EVI"))
        {
                info.sensor = new ModisSensor(applet,"MOD13A2 EVI",
                    "modis/mod13a2_evi", "LPDAAC_MODIS", "MOD13A2_EVI", true);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices 16-Day L3 "
                    + "Global 1km SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MOD13A2NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MOD13A2 NDVI", 
                    "modis/mod13a2_ndvi", "LPDAAC_MODIS", "MOD13A2_NDVI", true);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices 16-Day L3 "
                    + "Global 1km SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MOD13A3EVI"))
        {
                info.sensor = new ModisSensor(applet,"MOD13A3 EVI",
                    "modis/mod13a3_evi", "LPDAAC_MODIS", "MOD13A3_EVI", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices Monthly L3 "
                    + "Global 1km SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MOD13A3NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MOD13A3 NDVI", 
                    "modis/mod13a3_ndvi", "LPDAAC_MODIS", "MOD13A3_NDVI", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices Monthly L3 "
                    + "Global 1km SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MOD13Q1EVI"))
        {
                info.sensor = new ModisSensor(applet,"MOD13Q1 EVI",
                    "modis/mod13q1_evi", "LPDAAC_MODIS", "MOD13Q1_EVI", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices 16-Day L3 "
                    + "Global 250m SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MOD13Q1NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MOD13Q1 NDVI", 
                    "modis/mod13q1_ndvi", "LPDAAC_MODIS", "MOD13Q1_NDVI", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Indices 16-Day L3 "
                    + "Global 250m SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MOD14A1"))
        {
                info.sensor = new ModisSensor(applet,"MOD14A1",
                    "modis/mod14a1", "LPDAAC_MODIS", "MOD14A1", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Thermal Anomalies/Fire Daily "
                    + "L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MOD14A2"))
        {
                info.sensor = new ModisSensor(applet,"MOD14A2",
                    "modis/mod14a2", "LPDAAC_MODIS", "MOD14A2", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Thermal Anomalies/Fire 8-Day "
                    + "L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MOD15A2FPAR"))
        {
                info.sensor = new ModisSensor(applet, "MOD15A2 FPAR", 
                    "modis/mod15a2_fpar", "LPDAAC_MODIS", "MOD15A2_FPAR", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Leaf Area Index/FPAR 8-Day L4 "
                    + "Global 1km SIN Grid FPAR";
        }

        else if (sensorToCreate.equals("MOD15A2LAI"))
        {
                info.sensor = new ModisSensor(applet, "MOD15A2 LAI", 
                    "modis/mod15a2_lai", "LPDAAC_MODIS", "MOD15A2_LAI", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Leaf Area Index/LAI 8-Day L4 "
                    + "Global 1km SIN Grid LAI";
        }

        else if (sensorToCreate.equals("MOD17A2GPP"))
        {
                info.sensor = new ModisSensor(applet,"MOD17A2 GPP",
                    "modis/mod17a2_gpp", "LPDAAC_MODIS", "MOD17A2_GPP", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Gross Primary Productivity "
                    + "8-Day L4 Global 1km SIN Grid GPP";
        }

        else if (sensorToCreate.equals("MOD17A2NETPSN"))
        {
                info.sensor = new ModisSensor(applet, "MOD17A2 Net Photosynthesis", 
                    "modis/mod17a2_netpsn", "LPDAAC_MODIS", "MOD17A2_NETPSN", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Gross Primary Productivity "
                    + "8-Day L4 Global 1km SIN Grid Net Photosynthesis";
        }

        else if (sensorToCreate.equals("MOD17A3GPP"))
        {
                info.sensor = new ModisSensor(applet,"MOD17A3 GPP",
                    "modis/mod17a3_gpp", "LPDAAC_MODIS", "MOD17A3_GPP", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Gross Primary Productivity/GPP "
                    + "Yearly L4 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MOD17A3NPP"))
        {
                info.sensor = new ModisSensor(applet, "MOD17A3 NPP", 
                    "modis/mod17a3_npp", "LPDAAC_MODIS", "MOD17A3_NPP", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Net Primary Production/NPP "
                    + "Yearly L4 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MOD43B1"))
        {
                info.sensor = new ModisSensor(applet,"MOD43B1",
                    "modis/mod43b1", "LPDAAC_MODIS", "MOD43B1", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra BRDF/Albedo Model-1 16-Day L3 "
                    + "Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MOD43B3"))
        {
                info.sensor = new ModisSensor(applet,"MOD43B3",
                    "modis/mod43b3", "LPDAAC_MODIS", "MOD43B3", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Albedo 16-Day L3 Global 1km "
                    + "SIN Grid";
        }

        else if (sensorToCreate.equals("MOD43B4"))
        {
                info.sensor = new ModisSensor(applet,"MOD43B4",
                    "modis/mod43b4", "LPDAAC_MODIS", "MOD43B4", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Nadir BRDF-Adjusted "
                    + "Reflectance 16-Day L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MOD44BVCF"))
        {
                info.sensor = new ModisSensor(applet,"MOD44B VCF",
                    "modis/mod44b_vcf", "LPDAAC_MODIS", "MOD44B_VCF", false);
                info.menu = terraMenu;
                info.description = "MODIS/Terra Vegetation Continuous Fields "
                    + "Yearly L3 Global 250m SIN Grid";
        }

        else if (sensorToCreate.equals("MYD09A1"))
        {
                info.sensor = new ModisSensor(applet,"MYD09A1",
                    "modis/myd09a1", "LPDAAC_MODIS", "MYD09A1", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Reflectance 8-Day L3 "
                    + "Global 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MYD09GA"))
        {
                info.sensor = new ModisSensor(applet,"MYD09GA",
                    "modis/myd09ga", "LPDAAC_MODIS", "MYD09GA", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Reflectance Daily L2G "
                    + "Global 1km and 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MYD09GQ"))
        {
                info.sensor = new ModisSensor(applet,"MYD09GQ",
                    "modis/myd09gq", "LPDAAC_MODIS", "MYD09GQ", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Reflectance Daily L2G "
                    + "Global 250m SIN Grid";
        }

        else if (sensorToCreate.equals("MYD09Q1"))
        {
                info.sensor = new ModisSensor(applet,"MYD09Q1",
                    "modis/myd09q1", "LPDAAC_MODIS", "MYD09Q1", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Reflectance 8-Day L3 "
                    + "Global 250m SIN Grid";
        }

        else if (sensorToCreate.equals("MYD11A1DAY"))
        {
                info.sensor = new ModisSensor(applet,"MYD11A1 Day",
                    "modis/myd11a1_day", "LPDAAC_MODIS", "MYD11A1_DAY", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Temperature/Emissivity "
                    + "Daily L3 Global 1km SIN Grid Day";
        }

        else if (sensorToCreate.equals("MYD11A1NIGHT"))
        {
                info.sensor = new ModisSensor(applet,"MYD11A1 Night",
                    "modis/myd11a1_night", "LPDAAC_MODIS", "MYD11A1_NIGHT", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Temperature/Emissivity "
                    + "Daily L3 Global 1km SIN Grid Night";
        }

        else if (sensorToCreate.equals("MYD11A2DAY"))
        {
                info.sensor = new ModisSensor(applet,"MYD11A2 Day",
                    "modis/myd11a2_day", "LPDAAC_MODIS", "MYD11A2_DAY", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Temperature/Emissivity "
                    + "8-Day L3 Global 1km SIN Grid Day";
        }

        else if (sensorToCreate.equals("MYD11A2NIGHT"))
        {
                info.sensor = new ModisSensor(applet,"MYD11A2 Night",
                    "modis/myd11a2_night", "LPDAAC_MODIS", "MYD11A2_NIGHT", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Temperature/Emissivity "
                    + "8-Day L3 Global 1km SIN Grid Night";
        }

        else if (sensorToCreate.equals("MYD11B1DAY"))
        {
                info.sensor = new ModisSensor(applet,"MYD11B1 Day",
                    "modis/myd11b1_day", "LPDAAC_MODIS", "MYD11B1_DAY", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Temperature/Emissivity "
                    + "Daily L3 Global 5km SIN Grid Day";
        }

        else if (sensorToCreate.equals("MYD11B1NIGHT"))
        {
                info.sensor = new ModisSensor(applet,"MYD11B1 Night",
                    "modis/myd11b1_night", "LPDAAC_MODIS", "MYD11B1_NIGHT", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Surface Temperature/Emissivity "
                    + "Daily L3 Global 5km SIN Grid Night";
        }

        else if (sensorToCreate.equals("MYD13A1EVI"))
        {
                info.sensor = new ModisSensor(applet,"MYD13A1 EVI",
                    "modis/myd13a1_evi", "LPDAAC_MODIS", "MYD13A1_EVI", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices 16-Day L3 "
                    + "Global 500m SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MYD13A1NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MYD13A1 NDVI", 
                    "modis/myd13a1_ndvi", "LPDAAC_MODIS", "MYD13A1_NDVI", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices 16-Day L3 "
                    + "Global 500m SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MYD13A2EVI"))
        {
                info.sensor = new ModisSensor(applet,"MYD13A2 EVI",
                    "modis/myd13a2_evi", "LPDAAC_MODIS", "MYD13A2_EVI", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices 16-Day L3 "
                    + "Global 1km SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MYD13A2NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MYD13A2 NDVI", 
                    "modis/myd13a2_ndvi", "LPDAAC_MODIS", "MYD13A2_NDVI", true);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices 16-Day L3 "
                    + "Global 1km SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MYD13A3EVI"))
        {
                info.sensor = new ModisSensor(applet,"MYD13A3 EVI",
                "modis/myd13a3_evi", "LPDAAC_MODIS", "MYD13A3_EVI", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices Monthly L3 "
                    + "Global 1km SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MYD13A3NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MYD13A3 NDVI", 
                "modis/myd13a3_ndvi", "LPDAAC_MODIS", "MYD13A3_NDVI", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices Monthly L3 "
                    + "Global 1km SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MYD13Q1EVI"))
        {
                info.sensor = new ModisSensor(applet,"MYD13Q1 EVI",
                "modis/myd13q1_evi", "LPDAAC_MODIS", "MYD13Q1_EVI", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices 16-Day L3 "
                    + "Global 250m SIN Grid EVI";
        }

        else if (sensorToCreate.equals("MYD13Q1NDVI"))
        {
                info.sensor = new ModisSensor(applet, "MYD13Q1 NDVI", 
                    "modis/myd13q1_ndvi", "LPDAAC_MODIS", "MYD13Q1_NDVI", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Vegetation Indices 16-Day L3 "
                    + "Global 250m SIN Grid NDVI";
        }

        else if (sensorToCreate.equals("MYD14A1"))
        {
                info.sensor = new ModisSensor(applet,"MYD14A1",
                    "modis/myd14a1", "LPDAAC_MODIS", "MYD14A1", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Thermal Anomalies/Fire Daily "
                    + "L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MYD14A2"))
        {
                info.sensor = new ModisSensor(applet,"MYD14A2",
                    "modis/myd14a2", "LPDAAC_MODIS", "MYD14A2", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Thermal Anomalies/Fire 8-Day "
                    + "L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MYD15A2FPAR"))
        {
                info.sensor = new ModisSensor(applet, "MYD15A2 FPAR", 
                    "modis/myd15a2_fpar", "LPDAAC_MODIS", "MYD15A2_FPAR", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Leaf Area Index/FPAR 8-Day L4 "
                    + "Global 1km SIN Grid FPAR";
        }

        else if (sensorToCreate.equals("MYD15A2LAI"))
        {
                info.sensor = new ModisSensor(applet, "MYD15A2 LAI", 
                    "modis/myd15a2_lai", "LPDAAC_MODIS", "MYD15A2_LAI", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Leaf Area Index/LAI 8-Day L4 "
                    + "Global 1km SIN Grid LAI";
        }

        else if (sensorToCreate.equals("MYD17A2GPP"))
        {
                info.sensor = new ModisSensor(applet,"MYD17A2 GPP",
                    "modis/myd17a2_gpp", "LPDAAC_MODIS", "MYD17A2_GPP", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Gross Primary Productivity "
                    + "8-Day L4 Global 1km SIN Grid GPP";
        }

        else if (sensorToCreate.equals("MYD17A2NETPSN"))
        {
                info.sensor = new ModisSensor(applet, "MYD17A2 Net Photosynthesis", 
                    "modis/myd17a2_netpsn", "LPDAAC_MODIS", "MYD17A2_NETPSN", false);
                info.menu = aquaMenu;
                info.description = "MODIS/Aqua Gross Primary Productivity "
                    + "8-Day L4 Global 1km SIN Grid Net Photosynthesis";
        }

        else if (sensorToCreate.equals("MCD15A2FPAR"))
        {
                info.sensor = new ModisSensor(applet,"MCD15A2 FPAR",
                    "modis/mcd15a2_fpar", "LPDAAC_MODIS", "MCD15A2_FPAR", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Leaf Area Index/FPAR "
                    + "8-Day L4 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MCD15A2LAI"))
        {
                info.sensor = new ModisSensor(applet,"MCD15A2 LAI",
                    "modis/mcd15a2_lai", "LPDAAC_MODIS", "MCD15A2_LAI", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Leaf Area Index/LAI "
                    + "8-Day L4 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MCD15A3FPAR"))
        {
                info.sensor = new ModisSensor(applet,"MCD15A3 FPAR",
                    "modis/mcd15a3_fpar", "LPDAAC_MODIS", "MCD15A3_FPAR", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Fraction of "
                    + "Photosynthetically Active Radiation/FPAR "
                    + "4-Day L4 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MCD15A3LAI"))
        {
                info.sensor = new ModisSensor(applet,"MCD15A3 LAI",
                    "modis/mcd15a3_lai", "LPDAAC_MODIS", "MCD15A3_LAI", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Leaf Area Index/LAI "
                    + "4-Day L4 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43A1"))
        {
                info.sensor = new ModisSensor(applet,"MCD43A1",
                    "modis/mcd43a1", "LPDAAC_MODIS", "MCD43A1", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua BRDF/Albedo "
                    + "Model Parameters 16-Day L3 Global 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43A2"))
        {
                info.sensor = new ModisSensor(applet,"MCD43A2",
                    "modis/mcd43a2", "LPDAAC_MODIS", "MCD43A2", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua BDRF/Albedo "
                    + "16-Day L3 Global 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43A3"))
        {
                info.sensor = new ModisSensor(applet,"MCD43A3",
                    "modis/mcd43a3", "LPDAAC_MODIS", "MCD43A3", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Albedo "
                    + "16-Day L3 Global 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43A4"))
        {
                info.sensor = new ModisSensor(applet,"MCD43A4",
                    "modis/mcd43a4", "LPDAAC_MODIS", "MCD43A4", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Nadir BRDF-Adjusted "
                    + "Reflectance 16-Day L3 Global 500m SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43B1"))
        {
                info.sensor = new ModisSensor(applet,"MCD43B1",
                    "modis/mcd43b1", "LPDAAC_MODIS", "MCD43B1", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua BRDF/Albedo Model-1 "
                    + "16-Day L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43B2"))
        {
                info.sensor = new ModisSensor(applet,"MCD43B2",
                    "modis/mcd43b2", "LPDAAC_MODIS", "MCD43B2", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua BDRF/Albedo "
                    + "16-Day L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43B3"))
        {
                info.sensor = new ModisSensor(applet,"MCD43B3",
                    "modis/mcd43b3", "LPDAAC_MODIS", "MCD43B3", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Albedo "
                    + "16-Day L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("MCD43B4"))
        {
                info.sensor = new ModisSensor(applet,"MCD43B4",
                    "modis/mcd43b4", "LPDAAC_MODIS", "MCD43B4", false);
                info.menu = combinedAquaTerraMenu;
                info.description = "MODIS/Terra+Aqua Nadir BRDF-Adjusted "
                    + "Reflectance 16-Day L3 Global 1km SIN Grid";
        }

        else if (sensorToCreate.equals("ALI"))
        {
                info.sensor = new EO1AliSensor(applet);
                info.menu = eo1Menu;
                info.description = "Earth Observing One - Advanced Land Imager";
        }

        else if (sensorToCreate.equals("HYP"))
        {
                info.sensor = new EO1HypSensor(applet);
                info.menu = eo1Menu;
                info.description = "Earth Observing One - Hyperion";
        }

        else if (sensorToCreate.equals("TERRALOOK_ASTERVNIR"))
        {
                info.sensor = new TerraLookAsterVNIRSensor(applet);
                info.menu = terraLookMenu;
                info.description = "TerraLook Advanced Spaceborne Thermal "
                    + "Emission and Reflection Radiometer - visible "
                    + "and near infrared bands";
        }

        else if (sensorToCreate.equals("TERRALOOK_GLS2010"))
        {
                info.sensor = new Gls2010Dataset(applet,
                  "TL GLS2010 (2008-2011)", "TERRA_GLS2010",
                  "http://terralook.cr.usgs.gov", true, false);
                info.menu = terraLookMenu;
                info.description = "TerraLook Global Land Survey 2010 "
                  + "Landsat 5 TM & 7 ETM+ (2008-2011)";
        }

        else if (sensorToCreate.equals("TERRALOOK_GLS2005"))
        {
                info.sensor = new Gls2005Dataset(applet,
                  "TL GLS2005 (2003-2008)", "TERRA_GLS2005",
                  "http://terralook.cr.usgs.gov", true, false);
                info.menu = terraLookMenu;
                info.description = "TerraLook Global Land Survey 2005 "
                  + "Landsat 5 TM & 7 ETM+ (2003-2008)";
        }

        else if (sensorToCreate.equals("TERRALOOK_GLS2000"))
        {
                info.sensor = new Gls2000Dataset(applet,
                      "TL GLS2000 (1999-2003)", "TERRA_GLS2000",
                      "http://terralook.cr.usgs.gov", true, false);
                info.menu = terraLookMenu;
                info.description = "TerraLook Global Land Survey 2000 "
                  + "Landsat 7 ETM+ (1999-2003)";
        }

        else if (sensorToCreate.equals("TERRALOOK_GLS1990"))
        {
                info.sensor = new Gls1990Dataset(applet,
                      "TL GLS1990 (1984-1997)", "TERRA_GLS1990",
                      "http://terralook.cr.usgs.gov", true, false);
                info.menu = terraLookMenu;
                info.description = "TerraLook Global Land Survey 1990 "
                    + "Landsat 4 & 5 TM (1984-1997)";
        }

        else if (sensorToCreate.equals("TERRALOOK_GLS1975_L4_5"))
        {
                info.sensor = new Gls1975Mss4_5Dataset(applet,
                      "TL GLS1975 (1982-1987)", "TERRA_GLS1975",
                      "http://terralook.cr.usgs.gov", true, false);
                info.menu = terraLookMenu;
                info.description = "TerraLook Global Land Survey 1975 "
                    + "Landsat 4-5 MSS (1982-1987)";
        }

        else if (sensorToCreate.equals("TERRALOOK_GLS1975_L1_3"))
        {
                info.sensor = new Gls1975Mss1_3Dataset(applet,
                      "TL GLS1975 (1972-1983)", "TERRA_GLS1975",
                      "http://terralook.cr.usgs.gov", true, false);
                info.menu = terraLookMenu;
                info.description = "TerraLook Global Land Survey 1975 "
                    + "Landsat 1-3 MSS (1972-1983)";
        }
        else if (sensorToCreate.equals("NAPP"))
        {
                info.sensor = new NappDataset(applet);
                info.menu = aerialMenu;
                info.description = "National Aerial Photography Program";
        }
        else if (sensorToCreate.equals("NHAP_BW"))
        {
                info.sensor = new NhapDataset(applet, "BW NHAP", "nhap/bw",
                                              16);
                info.menu = aerialMenu;
                info.description
                    = "Black and White National High Altitude Photography";
        }
        else if (sensorToCreate.equals("NHAP_CIR"))
        {
                info.sensor = new NhapDataset(applet, "CIR NHAP", "nhap/cir",
                                              24);
                info.menu = aerialMenu;
                info.description
                    = "Color Infrared National High Altitude Photography";
        }
        else if (sensorToCreate.equals("NALC"))
        {
                info.sensor = new NalcDataset(applet);
                info.menu = landsatLegacyMenu;
                info.description = "North American Landscape Characterization";
        }
        else if (sensorToCreate.equals("ETM_MOSAIC"))
        {
                info.sensor = new TriDecEtmMosaicDataset(applet);
                info.menu = landsatLegacyMenu;
                info.description = "Tri-Decadal Global Landsat Orthorectified "
                    + "Panchromatic Sharpened Enhanced Thematic Mapper "
                    + "Plus Mosaics(1999-2003)";
        }
        else if (sensorToCreate.equals("TM_MOSAIC"))
        {
                info.sensor = new TriDecTmMosaicDataset(applet);
                info.menu = landsatLegacyMenu;
                info.description = "Tri-Decadal Global Landsat Orthorectified "
                    + "Thematic Mapper Mosaics (1987-1997)";
        }
        else if (sensorToCreate.equals("LANDSAT_ETM_COMBINED"))
        {
                info = null; // NOT IMPLEMENTED YET - merge SLC-on and SLC-off
// DEBUG
System.out.println("Sensor " + sensorToCreate + " NOT IMPLEMENTED YET");
        }
        else if (sensorToCreate.equals("COMBINED"))
        {
                // find the sensor objects for the sensors that are part of
                // the combined Landsat dataset
                Sensor[] combinedDataset = new Sensor[5];
                int index = 0;
                // keep the order established by initializeSensorNames
                for (int i = 0; i < sensors.length; i++)
                {
                    Sensor sensor = sensors[i];
                    if (sensor != null)
                    {
                        if (sensor.sensorName.startsWith("Landsat 4-5")
                            || sensor.sensorName.startsWith("L7 SLC-on")
                            || sensor.sensorName.startsWith("L7 SLC-off")
                            || sensor.sensorName.startsWith("Landsat 8"))
                        {
                            combinedDataset[index] = sensors[i];
                            index++;
                        }
                    }
                }

                // if some of the sensors making up the combined dataset
                // are not enabled, shrink the list to the correct size
                if (index < 5)
                {
                    Sensor[] temp = new Sensor[index];
                    for (int i = 0; i < index; i++)
                        temp[i] = combinedDataset[i];
                    combinedDataset = temp;
                }

                info.sensor = new LandsatCombined(applet,combinedDataset);
                info.menu = landsatArchiveMenu;
                info.description = "Landsats 4 and 5 Multispectral Scanner "
                    + "and Thematic Mapper, Landsat 7 Enhanced Thematic "
                    + "Mapper Plus, and Landsat 8 Operational Land Imager "
                    + "- 1982 to present";
        }
        else
        {
                info = null;
// DEBUG
System.out.println("No info for sensor " + sensorToCreate);
        }

        return info;
    }
}

