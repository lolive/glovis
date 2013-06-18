// NappDataset.java defines the NAPP dataset details.
//---------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

public class NappDataset extends Sensor
{
    private static final int[] resolutions = {45,15};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets
    private static final Color borderColor = Color.YELLOW; 
            // color to use for the highlight border


    // Constructor
    NappDataset
    (
        imgViewer applet        // I: applet reference
    )
    {
        super(applet, "NAPP", "napp", "NAPP","showNappBrowse.cgi",
              "showNappMetadata.cgi","USGS_logo.gif",
              "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/NAPP",
              null, "Enter a Full NAPP scene ID",resolutions,
              borderX, borderY, borderColor);

        // set the flags for the optional sensor features available
        isOrderable = true;            // can request different-micron scan
        hasCloudCover = false;
        hasProjectName = true;
        hasCustomSceneInfoLine = true;
        isDownloadable = true;         // web-enabled August 2008
        slowDownloadStart = true;
        downloadFileFormat = "TIFF";

        // do not factor in cloud cover when picking default scenes to display
        useCloudCoverForDefaultScenes = false;

        // set the default projection code for this sensor
        defaultProjectionCode = CreateProjection.FAKE_GEOGRAPHIC;
        
        // use the US geographic locator map
        locatorMap = LocatorMap.US_GEOGRAPHIC_MAP;
        
        // setup the navigation model
        navModel = new NappFlightlineModel();
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
        imgName.append(scene.entityID);
        if (resolution != resolutions[1])
            imgName.append("_lowRes");
        imgName.append(".jpg");

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

        cellDir.append("/");
        cellDir.append(navModel.getColumnString(gridCol));
        cellDir.append("/");
        cellDir.append(navModel.getRowString(gridRow));

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
        return new LandsatSceneFilter(md);
    }

    // method to return a nominal scene size in projection coordinates
    //----------------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(12000, 12000);
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 1987;
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
        if (resolution == resolutions[0])
            return 9000;
        else
            return 180000;
    }

    // method to return the custom scene info line
    //-----------------------------------------------------------------------
    public String getCustomSceneInfo(Metadata scene)
    {
        if (scene == null)
            return "Roll:            Frame:\nDate:                      Proj:";

        // get the roll and frame from the entity id
        int length = scene.entityID.length();
        String roll = scene.entityID.substring(length - 8, length - 3);
        String frame = scene.entityID.substring(length - 3, length);
        return "Roll: " + roll + "  Frame: " + frame
               + "\nDate: " + scene.getDateString()
               + "  Proj: " + scene.projectName;
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
        files[2] = basename + "_unclipped.jpg";
        files[3] = scene.entityID + ".jpg";

        return files;
    }

    // method to return a string representing a specific resolution index.
    // This is provided to allow sensors to override the string shown for the
    // different resolutions.  The default implementation returns the string
    // showing the resolution value with "m" appended for meters.
    //-----------------------------------------------------------------------
    public String getResolutionString(int index)
    {
        if (index == 0)
            return "Low";
        else
            return "High";
    }
}
