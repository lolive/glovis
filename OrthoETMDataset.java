// OrthoETMDataset.java defines a class to encapsulate the Ortho ETM+
// dataset details.
//--------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class OrthoETMDataset extends Sensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor
    OrthoETMDataset(imgViewer applet, String sensorName, String datasetName,
                    String productInfoUrl, boolean isDownloadable)
    {
        super(applet, sensorName, "ortho/etm",datasetName,
              "showOrthoBrowse.cgi", "showOrthoMetadata.cgi","USGS_logo.gif",
              "http://www.usgs.gov", productInfoUrl, null,
              "Enter a 21 character scene ID",
              resolutions,borderX,borderY,Color.YELLOW);

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // set the flags for the optional sensor features available
        isOrderable = false;
        this.isDownloadable = isDownloadable;
        hasNdviLineGraph = true;

        // Ortho data is on the silo, so it might be slow to start downloading
        slowDownloadStart = true;
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
        imgName.append("_");
        imgName.append(resolution);

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
        if (resolution == 1000)
            return 8500;
        else
            return 147500;
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

    // method to return a nominal Ortho scene size in meters
    //-------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(180000,180000);
    }
}
