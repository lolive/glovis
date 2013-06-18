// Sensor.java defines an abstract class for isolating sensor specific details.
// Each new sensor type should create a concrete class that provides the needed
// methods.
//
//-----------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;

public abstract class Sensor
{
    protected imgViewer applet;     // reference to the main applet

    public static final int SINGLE_SCENE = 0;
        // flag for drawing only a single scene at a time

    public String sensorName;     // name of the sensor
    private String showBrowseCGI; // cgi script for showing a raw browse image
    private String showMetadataCGI;// cgi script for showing a scene's metadata
    protected String datasetName; // EE data set name for order submission
    protected String cgiDatasetName; // data set name for CGI scripts when
                                     // needed to find the right directory
    protected String directory;   // name of directory for the sensor's data
    public int[] resolutions;     // available resolutions
    public int[] borderX;         // 4 corner highlight border X offsets
    public int[] borderY;         // 4 corner highlight border Y offsets
    public int defaultProjectionCode; // default projection code for this sensor
    private Color borderColor;    // color to use for the border
    public boolean hasAcqDate;    // indicates metadata has an acquisition date
    public boolean hasJulianDateMetadata; // indicates metadata has julian date
    public boolean hasSecondaryIDMetadata; // indicates metadata has order id
    public boolean isFullMosaic;  // flag to indicate all available scenes are 
                                  // loaded and displayed at once
    public boolean isOrderable;   // flag to indicate ordering is supported
    public int maxScenesPerOrder; // if orderable, max # of scenes per order
                                  // with 0 being no limit
    public boolean isDownloadable; // flag to indicate downloading is supported
    public boolean mightBeDownloadable; // flag to indicate that even though
                                        // the isDownloadable flag is set, that
                                        // not all the scenes are downloadable
    public boolean slowDownloadStart;   // flag to indicate the download might
                                        // be slow to start (i.e. on silo)
    public String downloadFileFormat;   // message shown for the format of the
                                        // file to download
    public boolean hasGeographicBumper; // flag to indicate if geographic
                                        // bumper is available for sensor
                                        // selected
    public boolean hasColRowInSceneID;// flag to indicate the sensor's scene 
                                      // id contains the grid col/row number
    public boolean displaySceneCenterLatLong; // flag to display the scene
                                  // center in the lat/long entry boxes instead
                                  // of the grid cell center (for sensors like
                                  // ASTER)
    public boolean hasConstantOffsets; // flag to indicate the sensor has 
                                  // constant offsets to the image data
    public boolean hasUpperLeftInToc; // flag to indicate the TOC holds the
                                  // upper left coordinate for the scenes
    public boolean hasCloudCover; // indicates dataset has cloud cover values
    public boolean useCloudCoverForDefaultScenes; // when true, the default
                                  // scene displayed to the user is the most 
                                  // recent with the lowest cloud cover.  When
                                  // false, the cloud cover is ignored and the
                                  // most recent scene is displayed.
    public boolean hasCustomSceneInfoLine; // when true, the sensor has a
                                  // custom line to include in the scene info
                                  // area
    public boolean allowAddAll;   // show if sensor supports add all feature
    public boolean hasDataVersions;// flag to indicate the sensor has data 
                                   // versions that can be filtered on
    public boolean hasMultipleBrowse; // flag to indicate the sensor may have
                                  // multiple browse associated w/ each entityID
    public boolean hasGridColRowFilter; // flag to indicate the sensor can be
                                        // filtered by Grid column and row
    public boolean hasNdviLineGraph; // flag to indicate the sensor supports 
                                     // NDVI line graph
    public boolean hasSwathMode;  // flag to indicate sensor supports swath mode
    public boolean hasUserDefinedArea;  // flag to indicate if the sensor
                                        // supports user defined area
    public boolean dataHasGaps;   // flag to show when the sensor data is gap
                                  // filled.
    public boolean useHighlightTransparentPixelFix;// fill the highlight polygon
                                  // with black if true to work around sensors
                                  // with transparent pixels in image data
    public boolean hasLookAngle;  // flag to indicate whether the sensor
                                  // has look angle in its TOC
    public boolean hasProjectName;// flag to indicate the metadata has a 
                                  // project name
    public boolean hasMultipleDatasets; // flag to indicate the sensor contains
                                        // scenes from more than one dataset
    public boolean hasLevel1;     // flag to indicate the sensor can have
                                        // Level 1 product information
    public boolean hideGridEntry; // flag to hide the grid entry input fields
                                  // since some sensors don't have useful
                                  // grid info to enter
    public boolean warnWhenOrderingPoorQuality; // flag to warn the user when
                                  // they are ordering poor quality data
    public int qualityLimit;      // the lowest quality that can be ordered
                                  // without a warning if the user should be
                                  // warned about poor quality scenes
    public int numQualityValues;  // number of quality values contained in
                                  // the TOC file for this sensor
    public String[] dataVersions; // data versions available (if supported)
    public String logoName;       // name of logo image for this sensor
    public String logoLink;       // web link to follow for logo image
    public int logoLocation;      // the location for the logo (upper-left or
                                  // lower-right.
    public String productInfoURL; // web link for the product info selection
    public String acquisitionScheduleURL; // web link for the acquisition
                                  // schedule for the sensor
    public String dataAcqRequestURL; // web link for the data acquisition
                                  // request for the sensor (null if not
                                  // available
    public String orderWindowName;// browser window name for this sensor's
                                  // shopping cart
    public String sceneIdHint;    // hint for entering a scene id
    public NavigationModel navModel; // navigation model object
    public SceneList sceneList; //scene list for this sensor
    public SceneList hiddenSceneList;   // hidden scene list for this sensor
    public int locatorMap;        // locator map to use
    private DecimalFormat threeDigitFormat; // three digit number formatter

    // define the valid values for the logoLocation
    public final static int LOGO_LOWER_LEFT = 1;
    public final static int LOGO_LOWER_RIGHT = 2;


    // Constructor for the sensor class
    //---------------------------------
    Sensor
    (
        imgViewer applet,       // I: applet reference
        String sensorNameIn,    // I: name to use for the sensor
        String inventoryDir,    // I: name of directory for the data
        String inDatasetName,   // I: EE data set name for order submission
        String showBrowse,      // I: name of script to show a raw browse image
        String showMetadata,    // I: name of script to show a scene's metadata
        String logoName,        // I: name of logo image for this sensor
        String logoLink,        // I: web link to follow for the logo image
        String productInfoURL,  // I: product info link for the sensor
        String acquisitionScheduleURL, // I: acquisition schedule link
        String sceneIdHint,     // I: hint for scene id entry
        int[] resolutionsIn,    // I: resolutions available
        int[] borderXIn,        // I: 4 corner highlight X border offsets
        int[] borderYIn,        // I: 4 corner highlight Y border offsets
        Color borderColorIn     // I: color to use for the highlight border
    )
    {
        // save the values passed in
        this.applet = applet;
        sensorName = sensorNameIn;
        directory = inventoryDir;
        datasetName = inDatasetName;
        showBrowseCGI = showBrowse;
        showMetadataCGI = showMetadata;
        this.logoName = logoName;
        this.logoLink = logoLink;
        this.productInfoURL = productInfoURL;
        if (acquisitionScheduleURL != null)
            this.acquisitionScheduleURL = acquisitionScheduleURL;
        else
        {
            // set the default acquisition page if none provided
            this.acquisitionScheduleURL = new String("acqSchedule.html");
        }
        dataAcqRequestURL = null;
        this.sceneIdHint = sceneIdHint;

        resolutions = resolutionsIn;
        borderX = borderXIn;
        borderY = borderYIn;
        borderColor = borderColorIn;

        // by default, sensors have an acquisition date
        hasAcqDate = true;

        // default all optional items to not present.  Derived classes can set
        // them to true if needed.  Not passing them as parameters to the
        // constructor allows adding more flags in the future with impacting
        // existing sensor support.
        hasJulianDateMetadata = false;
        hasSecondaryIDMetadata = false;
        isFullMosaic = false;
        isOrderable = false;
        maxScenesPerOrder = 0;
        isDownloadable = false;
        mightBeDownloadable = false;
        slowDownloadStart = false;
        hasColRowInSceneID = false;
        displaySceneCenterLatLong = false;
        hasConstantOffsets = false;
        hasUpperLeftInToc = true;
        hasCloudCover = true;
        useCloudCoverForDefaultScenes = true;
        hasCustomSceneInfoLine = false;
        allowAddAll = true;
        hasDataVersions = false;
        hasMultipleBrowse = false;
        hasGridColRowFilter = false;
        hasSwathMode = false;
        hasNdviLineGraph = false;
        hasUserDefinedArea = false;
        dataHasGaps = false;
        cgiDatasetName = null;
        useHighlightTransparentPixelFix = true;
        hasLookAngle = false;
        hasProjectName = false;
        hasMultipleDatasets = false;
        hasLevel1 = false;
        hideGridEntry = false;
        warnWhenOrderingPoorQuality = false;
        hasGeographicBumper = true;
        qualityLimit = 0;
        logoLocation = LOGO_LOWER_LEFT;

        // assume the downloadable files are compressed GeoTIFF and let the
        // classes modify it if needed
        downloadFileFormat = "compressed GeoTIFF";

        // no default projection for the sensor
        defaultProjectionCode = -1;

        // default to the geographic locator map
        locatorMap = LocatorMap.GEOGRAPHIC_MAP;

        // default to the EE shopping cart window and let derived sensors
        // override it if needed 
        orderWindowName = new String("EE_shopping_cart");

        numQualityValues = 0;

        sceneList = new SceneList(applet, this);
        hiddenSceneList = new SceneList(applet, this);

        threeDigitFormat = new DecimalFormat ("000");
    }

    // dummy method to complete any metadata that is constant for each scene
    // for the sensor or for fields that can be calculated from other fields.
    //-----------------------------------------------------------------------
    public void completeMetadata(Metadata scene)
    {
        // nothing to do for the default case
    }

    // method to return the sensor's data directory
    //---------------------------------------------
    public String getSensorDirectory()
    {
        return directory;
    }

    // method to return the directory name for a given cell.  Note this 
    // implementation assumes a path/row naming scene, but individual 
    // sensors may override this if needed.
    //-----------------------------------------------------------------
    public String getCellDirectory
    (
        int gridCol,    // I: grid column
        int gridRow     // I: grid row
    )
    {
        StringBuffer cellDir = new StringBuffer(getSensorDirectory());

        cellDir.append("/p");
        cellDir.append(threeDigitFormat.format(gridCol));
        cellDir.append("/r");
        cellDir.append(threeDigitFormat.format(gridRow));

        return cellDir.toString();
    }

    // method to read a TOC file and store the corresponding scenes
    //-------------------------------------------------------------
    public void readTOC(TOC cell)
    {
        cell.read(this);
    }

    // abstract method to return an image file name for a given metadata 
    // reference and resolution
    //  Returns: full file name, including directory
    //------------------------------------------------------------------
    public abstract String makeImageName
    (
        Metadata scene,     // I: metadata for scene
        int resolution      // I: resolution requested in meters
    );

    // abstract method to return the number of cells to display for a given
    // resolution.  Sensor.SINGLE_SCENE is returned if only one scene should
    // be displayed.
    //----------------------------------------------------------------------
    public abstract int getNumCellsAtResolution
    (
        int resolution      // I: resolution in meters
    );

    // abstract method to return a scene filter that is compatible with this
    // sensor.  Returns a SceneFilter object.
    //----------------------------------------------------------------------
    public abstract SceneFilter getSceneFilter
    (
        MosaicData md,      // I: the mosaic data object
        TOC[] tocs          // I: array of TOC's to build the scene filter for
    );

    // method to build the URL for ordering a list of scenes.  The provided
    // default method allows ordering from EarthExplorer
    //---------------------------------------------------------------------
    public String buildOrderURL
    (
        Metadata[] sceneList    // I: array of scenes to order
    )
    {
        Vector idList = new Vector();
        String orderUrl = new String();
        if (sceneList.length < 1)
        {
            return null;
        }

        for (int i = 0; i < sceneList.length; i++) 
        {
            idList.add(sceneList[i].entityID);
        }

        orderUrl = EarthExplorer.buildShoppingCartUrl(this.datasetName, idList);

        return orderUrl;
    }
    
    // method to build the URL for downloading a scene
    //------------------------------------------------
    public String buildDownloadURL(Metadata scene)
    {
        if (isDownloadable)
        {
            Vector id = new Vector();
            id.add(scene.entityID);
            String url = EarthExplorer.buildShoppingCartUrl(datasetName, id);

            return url;
        }

        // return null if not downloadable
        return null;
    }

    // method to return the lowest resolution (in meters) for this sensor
    //-------------------------------------------------------------------
    public double getLowestResolution()
    {
        return getActualResolution(resolutions[0]);
    }

    // method to return the resolution (in meters) used to define the offsets
    // in the TOC for this sensor
    //-----------------------------------------------------------------------
    public double getOffsetResolution()
    {
        // Note this works for the current sensors, but may need to be 
        // overridden for others
        int numRes = resolutions.length;
        return getActualResolution(resolutions[numRes - 2]);
    }

    // method to convert the current advertised resolution into the actual
    // resolution.  This was created since MODIS pixel size is advertised to
    // be 1000 meters, but it is roughly 926 meters.  This routine allows
    // sensors to override the resolution with the real value when it is 
    // used in calculations.  The default implementation just returns the 
    // same resolution that is passed in since most sensors are implemented
    // that way.
    //----------------------------------------------------------------------
    public double getActualResolution(int resolution)
    {
        return (double)resolution;
    }

    // method to return a sensor's nominal scene size in meters
    //---------------------------------------------------------
    public abstract Dimension getNominalSceneSize();

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public abstract int getStartingYear();

    // method to return the ending year for the sensor (or -1 if data 
    // collections have not ended for this sensor)
    //---------------------------------------------------------------
    public abstract int getEndingYear();

    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public abstract int getImageFileSize(int resolution);

    // method to show the original browse image for a scene in browser window
    //-----------------------------------------------------------------------
    public void showBrowse(Metadata scene)
    {
        showSceneURL(showBrowseCGI,scene);
    }

    // method to show the metadata for a scene in browser window
    //----------------------------------------------------------
    public void showMetadata(Metadata scene)
    {
        showSceneURL(showMetadataCGI,scene);
    }

    // method to display a new browser window with the results of a CGI script 
    // for a given scene
    //------------------------------------------------------------------------
    private void showSceneURL(String scriptName, Metadata scene)
    {
        try 
        {
            String showScene = scriptName + "?scene_id=" 
                             + URLEncoder.encode(scene.entityID, "UTF-8");

            // for sensors that do not have col/row info in the scene ID, add
            // the col/row passed in to the URL
            if (!hasColRowInSceneID)
            {
                showScene += "&path=" + navModel.getColumnString(scene.gridCol) 
                          + "&row=" + navModel.getRowString(scene.gridRow);
            }

            // for the MRLC dataset it uses the order ID and scene ID to find 
            // which MRLC dataset is used (TC 2001 or RA 2001)  
            if (hasSecondaryIDMetadata)
            {
                showScene += "&secondary_id="
                          + URLEncoder.encode(scene.secondaryID, "UTF-8");
            }

            // for the MODIS datasets, each scene may have multiple browse
            if (hasMultipleBrowse)
            {
                showScene += "&browse_number="
                          + scene.browseNumber;
            }
            
            // if there is a cgiDatasetName defined, include it in the URL too
            if (cgiDatasetName != null)
            {
                showScene += "&dataset=" 
                          + URLEncoder.encode(cgiDatasetName, "UTF-8");
            }

            // open a new window with the URL
            URL showURL = new URL(applet.getCodeBase(),showScene);
            applet.getAppletContext().showDocument(showURL,"_blank");
        }
        catch (Exception except)
        {
            System.out.println("exception: "+ except);
        }
    }

    // method to convert an entity ID from a scene's metadata to the
    // value to display.  The default case is to simply return the
    // value from the metadata.
    //--------------------------------------------------------------
    String buildEntityID(Metadata scene)
    {
        return (scene.entityID);
    }

    // method to convert an entity ID from a scene's metadata to the value
    // to display in the scene list in the left panel of the applet.  The 
    // default implementation calls buildEntityID so that derived classes 
    // only need to implement one method if this routines is the same.
    //--------------------------------------------------------------------
    String buildMediumEntityID(Metadata scene)
    {
        return buildEntityID(scene);
    }

    // method to convert an entity ID from a scene's metadata to the value
    // short enough to display.  The default default implementation calls
    // buildEntityID so that derived classes only need to implement one 
    // method if this routines is the same.
    //--------------------------------------------------------------------
    String buildShortEntityID(Metadata scene)
    {
        return buildEntityID(scene);
    }

    // method to return the custom scene info line (default is to not support
    // it, so the default returns null)
    //-----------------------------------------------------------------------
    public String getCustomSceneInfo(Metadata scene)
    {
        return null;
    }

    // method to return the files to download for a given scene when the
    // user downloads metadata and browse.  Returns an array of filenames where
    // the source and destination file names are paired up.  If the source
    // browse name is not known, null is returned in that array entry.
    //-------------------------------------------------------------------------
    public String[] getFilesForScene(Metadata scene)
    {
        String[] files = new String[4];

        String basename = getCellDirectory(scene.gridCol, scene.gridRow) 
                 + "/y" + scene.year + "/" + scene.entityID;

        // set the metadata source and destination names
        files[0] = basename + ".meta";
        files[1] = scene.entityID + ".meta";
        // set the browse image source and destination names
        files[2] = basename + ".jpg";
        files[3] = scene.entityID + ".jpg";

        return files;
    }

    // method to get the border color for this sensor
    //-----------------------------------------------
    public Color getBorderColor(Metadata scene)
    {
        return borderColor;
    }

    // method to create the metadata for the results of a search for scene
    // operation
    //--------------------------------------------------------------------
    public Metadata createMetadata(String dataLine, int gridCol, int gridRow)
    {
        // remove the sensor identifier from the start of the dataLine since
        // it isn't needed in the default case
        int index = dataLine.indexOf(',');
        String line = dataLine.substring(index + 1);

        return new Metadata(line, this, gridCol, gridRow);
    }

    // method to return the list of sensors in this sensor (for datasets
    // that combine multiple datasets).  The default just returns the current
    // sensor.
    //-----------------------------------------------------------------------
    public Sensor[] getSensorList()
    {
        Sensor[] sensors = new Sensor[1];
        sensors[0] = this;
        return sensors;
    }

    // method to return a string representing a specific resolution index.
    // This is provided to allow sensors to override the string shown for the
    // different resolutions.  The default implementation returns the string
    // showing the resolution value with "m" appended for meters.
    //-----------------------------------------------------------------------
    public String getResolutionString(int index)
    {
        return "" + resolutions[index] + "m";
    }

    // default method to confirm the display of this sensor
    // (used for restricted sensors)
    //-----------------------------------------------------------------------
    public boolean confirmInitialDisplay()
    {
        return true;
    }

    // default method to determine whether a given sensor name is one of
    // this sensor's aliases (most sensors just have sensorName)
    //------------------------------------------------------------------
    public boolean validName(String sensorNameToCheck)
    {
        return (sensorNameToCheck.equals(sensorName));
    }
}

