// ModisSensor.java defines the MODIS sensor details.
//---------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Vector;

public class ModisSensor extends Sensor
{
    private DecimalFormat twoDigitFormat;   // two digit number formatter

    private static final int[] resolutions = {10000,5000};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets
    private static final Color borderColor = Color.YELLOW; 
            // color to use for the highlight border
    private LatLongToModisTile tileConverter; 
            // for converting tile numbers to upper-left coordinates
    private static final int[] lineOffsets = {0, 0, 239, 239};
    private static final int[] sampOffsets = {0, 239, 239, 0};
            // constant line and sample offsets to the image data


    // Constructor
    ModisSensor
    (
        imgViewer applet,       // I: applet reference
        String sensorName,      // I: name to use for the sensor
        String inventoryDir,    // I: name of directory for the data
        String datasetName,     // I: data set name for ordering
        String cgiDatasetName,  // I: data set name for the cgi scripts
        boolean hasCloudCover   // I: flag to indicate product has cloud cover
    )
    {
        super(applet,sensorName,inventoryDir,datasetName,"showModisBrowse.cgi",
              "showModisMetadata.cgi","NASA_small.gif",
              "http://lpdaac.usgs.gov/products/modis_overview",
              "https://lpdaac.usgs.gov/products/modis_products_table",
              null,"Enter a MODIS scene ID",resolutions,
              borderX,borderY,borderColor);

        // set the dataset name for the show browse/metadata cgi scripts so
        // the datasets with two browse can be displayed
        this.cgiDatasetName = cgiDatasetName;

        // Set up formatter for creating file names
        twoDigitFormat = new DecimalFormat ("00");

        // set the flags for the optional sensor features available
        isOrderable = true;
        hasColRowInSceneID = true;
        hasConstantOffsets = true;
        hasUpperLeftInToc = false;
        hasSecondaryIDMetadata = true;
        hasMultipleBrowse = true;
        hasCustomSceneInfoLine = true;
        hasDataVersions = true;
        hasGeographicBumper = false;
        
        this.hasCloudCover = hasCloudCover;

        // do not factor in cloud cover when picking default scenes to display
        useCloudCoverForDefaultScenes = false;

        // set the default projection code for this sensor
        defaultProjectionCode = CreateProjection.SINUSOIDAL;

        // use the sinusoidal locator map
        locatorMap = LocatorMap.SINUSOIDAL_MAP;

        // setup the navigation model
        navModel = new ModisTileModel();

        // setup the object to convert tile numbers to upper-left coordinates
        tileConverter = new LatLongToModisTile();

        // read the data versions from the server
        dataVersions = getDataVersions();

        // place the logo in the lower right corner
        logoLocation = Sensor.LOGO_LOWER_RIGHT;
    }

    // getDataVersions reads all of the available versions for this sensor
    // from a file on the server.
    //-----------------------------------------------------------------------
    private String[] getDataVersions()
    {
        BufferedReader versionData = null; // stream for reading the file
        ArrayList dataVersionList = new ArrayList();

        try
        {
            // open the version data file
            URL versionURL = new URL(applet.getCodeBase(),"modis/versions.txt");
            versionData = new BufferedReader(
                        new InputStreamReader(versionURL.openStream()));

            // read the version numbers from the file (they are sorted)
            String dataLine;
            while ((dataLine = versionData.readLine()) != null)
            {
                dataVersionList.add(dataLine.trim());
            }

            // close the data stream
            versionData.close();
            versionData = null;
        }
        catch (Exception e)
        {
            // error reading or parsing the file
            dataVersionList.add("ERROR");

            // make sure the stream is closed
            if (versionData != null)
            {
                try {versionData.close();} catch (Exception e1){};
            }
        }

        dataVersionList.trimToSize();
        String[] dataVersions = new String[dataVersionList.size()];
        for (int i = 0; i < dataVersionList.size(); i++)
        {
            dataVersions[i] = dataVersionList.get(i).toString();
        }
        return dataVersions;
    }

    // method to populate the scene metadata fields that are constants or
    // can be calculated from other fields
    //-------------------------------------------------------------------
    public void completeMetadata(Metadata scene)
    {
        // set the line and sample offsets
        scene.lineOffset = lineOffsets;
        scene.sampOffset = sampOffsets;

        // Finding the 3rd and 4th "." and pulling only the version number out
        // to create the orderID that needs to be passed in the URL.
        int index = scene.secondaryID.indexOf('.');
        int index2 = scene.secondaryID.indexOf('.',index + 1);
        int index3 = scene.secondaryID.indexOf('.',index2 + 1);
        int index4 = scene.secondaryID.indexOf('.',index3 + 2);
        scene.dataVersion = scene.secondaryID.substring(index3+1,index4);
        scene.entityID = "." + scene.dataVersion + ":" + scene.entityID;

        // fill in the upper left X/Y based on the tile coordinates
        ModisTile tile = new ModisTile(scene.gridCol, scene.gridRow);
        Point coordinate = tileConverter.tileToCoordinate(tile,false);
        scene.ulX = coordinate.x;
        scene.ulY = coordinate.y;
    }

    // method to return an image file name for a given metadata reference and
    // resolution
    //  Returns: full file name, including directory
    //-----------------------------------------------------------------------
    public String makeImageName
    (
        Metadata scene,     // I: metadata for scene
        int resolution      // I: resolution requested in meters
    )
    {
        StringBuffer imgName;

        // build the image name, starting with the TOC directory and adding
        // the items that make up the file name
        imgName = new StringBuffer(getCellDirectory(scene.gridCol,
                                                    scene.gridRow));
        imgName.append("/y");
        imgName.append(scene.year);
        imgName.append("/");

        if (resolution != 5000)
        {
            // the image is named using the scene ID up to the production date,
            // so get the scene ID up to the production date
            int index = scene.secondaryID.lastIndexOf('.');
            int index2 = scene.secondaryID.lastIndexOf('.',index - 1);
            imgName.append(scene.secondaryID.substring(0,index2));
            // append the browse number
            if (scene.browseNumber > 0)
            {
                imgName.append("_");
                imgName.append(scene.browseNumber);
            }

            imgName.append(".");

            // convert the resolution to kilometers and add it to the file name
            imgName.append((resolution/1000));
            imgName.append("km.jpg");
        }
        else 
        {
            // for the 5km browse, just replace the "hdf" at the end with "jpg"
            // and insert the browse number
            int index = scene.secondaryID.lastIndexOf('.');
            imgName.append(scene.secondaryID.substring(0,index));
            if (scene.browseNumber > 0)
            {
                imgName.append("_");
                imgName.append(scene.browseNumber);
            }
            imgName.append(".jpg");
        }

        return imgName.toString();
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

        cellDir.append("/h");
        cellDir.append(twoDigitFormat.format(gridCol));
        cellDir.append("/v");
        cellDir.append(twoDigitFormat.format(gridRow));

        return cellDir.toString();
    }

    // method to return the resolution (in meters) used to define the offsets
    // in the TOC for this sensor
    //-----------------------------------------------------------------------
    public double getOffsetResolution()
    {
        // use the 5km resolution for offsets
        int numRes = resolutions.length;
        return getActualResolution(resolutions[numRes - 1]);
    }

    // method to convert the current advertised resolution into the actual
    // resolution.  This was created since MODIS pixel size is advertised to
    // be 1000 meters, but it is roughly 926 meters.
    //----------------------------------------------------------------------
    public double getActualResolution(int resolution)
    {
        return ((double)resolution) * 926.625433/1000.0;
    }

    // method to return the number of cells to display for a given resolution.
    // Sensor.SINGLE_SCENE is returned if only one scene should be displayed.
    //------------------------------------------------------------------------
    public int getNumCellsAtResolution
    (
        int resolution      // I: resolution in meters
    )
    {
        if (resolution == resolutions[0])
            return applet.md.getMosaicSize();
        else
            return SINGLE_SCENE;
    }

    // method to return a scene filter that is compatible with this sensor.
    // Returns a SceneFilter object.
    //---------------------------------------------------------------------
    public SceneFilter getSceneFilter
    (
        MosaicData md,      // I: the mosaic data object
        TOC[] tocs          // I: array of TOC's to build the scene filter for
    )
    {
        return new ModisSceneFilter(md);
    }

    // method to build the URL for ordering a list of scenes.  ASTER/MODIS
    // orders go to the EE shopping cart.
    //------------------------------------------------------------------
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
            // add the entity id without the version number
            // (entityID formatted as .VVV: so start at position 5)
            idList.add(sceneList[i].entityID.substring(5));
        }

        orderUrl = EarthExplorer.buildShoppingCartUrl(this.datasetName, idList);

        return orderUrl;
    }


    // method to return a nominal scene size in meters
    //------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(1111951,1111951);
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 2000;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return -1;
    }
    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public int getImageFileSize(int resolution)
    {
        if (resolution == 10000)
            return 3500;
        else if (resolution == 5000)
            return 55000;
        else
            return 1500;
    }

    // method to return the custom scene info line
    // ----------------------------------------------------------------------
    public String getCustomSceneInfo(Metadata scene)
    {
        // if this sensor has cloud cover, SceneInfo adds CC: AND Date in line2
        // otherwise, SceneInfo does not add a second line because this
        // sensor hasCustomSceneInfoLine, so we need to include Date

        // either way, add the Granule ID and Browse Number
        // (Metadata has browseNumber from TOC, not BrowseType from .meta)

        if (hasCloudCover)
        {
            // we just have 1 line worth of display area, Date already done
            if (scene == null)
                return "Granule ID:      #";
            else
                return "Granule ID: " + scene.entityID.substring(5)
                   + "  # " + scene.browseNumber;
        }
        else
        {
            // line with Date hasn't been added, so add the Date
            if (scene == null)
                return "Date:\nGranule ID:      #";

            return "Date: " + scene.getDateString()
               + "\nGranule ID: " + scene.entityID.substring(5)
               + "  # " + scene.browseNumber;
        }
    }

    // method to convert an entity ID from a scene's metadata to the value
    // medium length for display in the scene list in the left panel of the 
    // applet.  The production timestamp and ".hdf" are dropped from the end.
    //-----------------------------------------------------------------------
    String buildMediumEntityID(Metadata scene)
    {
        int index = scene.secondaryID.lastIndexOf('.');
        int index2 = scene.secondaryID.lastIndexOf('.',index - 1);
        return (scene.secondaryID.substring(0,index2));
    }

    // method to convert an entity ID from a scene's metadata to the value
    // short enough to display.  The dataset name is dropped from the front
    // of the secondary ID and the production timestamp and ".hdf" are
    // dropped from the end.
    //-------------------------------------------------------------------------
    String buildShortEntityID(Metadata scene)
    {
        int index1 = scene.secondaryID.indexOf('.') + 1;
        int index = scene.secondaryID.lastIndexOf('.');
        int index2 = scene.secondaryID.lastIndexOf('.',index - 1);
        return (scene.secondaryID.substring(index1,index2));
    }

    // method to convert an entity ID from a scene's metadata to the
    // value to display.
    //--------------------------------------------------------------
    String buildEntityID(Metadata scene)
    {
        return (scene.secondaryID);
    }

    // method to return the files to download for a given scene when the
    // user downloads metadata and browse.  Returns an array of filenames where
    // the source and destination file names are paired up.  If the source
    // browse name is not known, null is returned in that array entry.
    //-------------------------------------------------------------------------
    public String[] getFilesForScene(Metadata scene)
    {
        String[] files = new String[4];

        int index = scene.secondaryID.lastIndexOf('.');
        StringBuffer baseFilename = new
            StringBuffer(scene.secondaryID.substring(0, index));
        if (scene.browseNumber > 0)
        {
            baseFilename.append('_');
            baseFilename.append(scene.browseNumber);
        }

        String basename = getCellDirectory(scene.gridCol, scene.gridRow) 
                 + "/y" + scene.year + "/" + baseFilename.toString();

        // set the metadata source and destination names
        files[0] = basename + ".meta";
        files[1] = baseFilename.toString() + ".meta";
        // set the browse image source and destination names
        files[2] = basename + ".jpg";
        files[3] = baseFilename.toString() + ".jpg";

        return files;
    }
}
