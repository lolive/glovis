// NalcDataset.java implements the NALC dataset support.
//----------------------------------------------------------------------
import java.text.DecimalFormat;
import java.awt.Color;
import java.awt.Dimension;

public class NalcDataset extends Sensor
{
    private static int[] resolutions = {1000,480};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets
    private DecimalFormat threeDigitFormat; // three digit number formatter

    // Constructor
    NalcDataset
    (
        imgViewer applet        // I: applet reference
    )
    {
        super(applet,"NALC Triplicates", "nalc", "NALC", "showNalcBrowse.cgi",
              "showNalcMetadata.cgi", "USGS_logo.gif", "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/NALC_prod",
              null, "Enter a 13 digit scene ID", resolutions,
              borderX, borderY, Color.YELLOW);

        // Set up formatters for creating file names
        threeDigitFormat = new DecimalFormat ("000");

        // set the flags for the optional sensor features available
        isOrderable = false;
        hasCloudCover = false;
        hasColRowInSceneID = true;
        hasAcqDate = false;
        isDownloadable = true;
        slowDownloadStart = true;
        downloadFileFormat = "compressed raw imagery";

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();
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
        imgName.append("/");
        imgName.append(scene.entityID);
        imgName.append("_");
        imgName.append("" + resolution);

        // load correct image based on full res or mosaic view
        if (resolution != 1000)
        {
            // full resolution uses jpeg files
            imgName.append(".jpg");
        }
        else
        {
            // mosaic area uses gif files
            imgName.append(".gif");
        }

        return imgName.toString();
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

    // method to return a nominal Landsat scene size in meters
    //--------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(180000,180000);
    }

    // method to return the files to download for a given scene when the
    // user downloads metadata and browse.  Returns an array of filenames where
    // the source and destination file names are paired up.  If the source
    // browse name is not known, null is returned in that array entry.
    //-------------------------------------------------------------------------
    public String[] getFilesForScene(Metadata scene)
    {
        String[] files = new String[4];

        // set the metadata source and destination names
        files[0] = makeImageName(scene, resolutions[resolutions.length - 1]);
        int index = files[0].lastIndexOf('/');
        files[0] = files[0].substring(0, index + 1);
        files[0] += scene.entityID;
        files[0] += ".meta";
        files[1] = scene.entityID + ".meta";

        // set the browse image source and destination names.  Note that
        // the source is set to null since the filename varies and the null
        // tells the file downloader to get the browse name from the metadata
        // file.
        files[2] = null;
        files[3] = scene.entityID + ".jpg";

        return files;
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 1972;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return 1995;
    }
    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public int getImageFileSize(int resolution)
    {
        if (resolution == 1000)
            return 35000;
        else
            return 50000;
    }
}
