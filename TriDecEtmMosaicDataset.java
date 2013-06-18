// TriDecEtmMosaicDataset.java defines the Tri-Decadal ETM+ mosaic details.
//-------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

public class TriDecEtmMosaicDataset extends Sensor
{
    private static final int[] resolutions = {1000, 285};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets
    private static final Color borderColor = Color.YELLOW; 
            // color to use for the highlight border


    // Constructor
    TriDecEtmMosaicDataset
    (
        imgViewer applet        // I: applet reference
    )
    {
        super(applet, "ETM+ Pan Mosaics", "ortho/etm_mosaic","ORTHO_MOSAIC_ETM",
              "showMosaicBrowse.cgi", "showMosaicMetadata.cgi","USGS_logo.gif",
              "http://www.usgs.gov",
              "https://lta.cr.usgs.gov/Tri_Dec_GLOO",
              null,
              "Enter a Full ETM+ Pan Mosaic entity ID",resolutions,
              borderX, borderY, borderColor);

        // set the flags for the optional sensor features available
        isOrderable = false;
        hasCloudCover = false;
        hasAcqDate = false;
        isDownloadable = true;
        slowDownloadStart = true;
        hasColRowInSceneID = true;
        hideGridEntry = true;

        // do not factor in cloud cover when picking default scenes to display
        useCloudCoverForDefaultScenes = false;

        // setup the navigation model
        navModel = new TriDecEtmMosaicModel();
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

        // load correct image based on the resolution
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

    // method to return the directory name for a given cell.
    //------------------------------------------------------
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
        return new Dimension(360000, 280000);
    }

    // method to return the starting year for the sensor
    //--------------------------------------------------
    public int getStartingYear()
    {
        return 1999;
    }

    // method to return the ending year for the sensor (or -1 if collections
    // continue)
    //----------------------------------------------------------------------
    public int getEndingYear()
    {
        return 2003;
    }
    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public int getImageFileSize(int resolution)
    {
        if (resolution == resolutions[0])
            return 74500;
        else
            return 331000;
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
                 + "/" + scene.entityID;

        // set the metadata source and destination names
        files[0] = basename + ".meta";
        files[1] = scene.entityID + ".meta";
        // set the browse image source and destination names
        files[2] = basename + ".jpg";
        files[3] = scene.entityID + ".jpg";

        return files;
    }
}
