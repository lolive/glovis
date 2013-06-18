// AntarcticaEtmDataset.java defines the Antarctica Landsat ETM scene dataset
// details
//---------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;

public class AntarcticaEtmDataset extends Sensor
{
    private static int[] resolutions = {1000,240};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets

    // Constructor
    AntarcticaEtmDataset(imgViewer applet)
    {
        super(applet,"LIMA Antarctica ETM+", "lima", "LIMA",
              "showLandsatL1Browse.cgi", "showLandsatL1Metadata.cgi",
              "USGS_logo.gif", "http://www.usgs.gov",
              "http://lima.usgs.gov", null, "Enter an 18 digit id",
              resolutions, borderX, borderY, Color.YELLOW);

        // set the navigation model to the polar navigation model
        navModel = new PolarNavModel();

        // use the polar stereographic projection as the default
        defaultProjectionCode = CreateProjection.POLAR_STEREOGRAPHIC;

        // reverse some options set by the base class
        hasJulianDateMetadata = false;
        hasSwathMode = false;
        warnWhenOrderingPoorQuality = false;
        qualityLimit = 0;

        // set the flags for the optional sensor features available
        hasNdviLineGraph = false;
        isDownloadable = true;
        hasCloudCover = false;
        useCloudCoverForDefaultScenes = false;
        dataHasGaps = false;
        downloadFileFormat = "GeoTIFF";
        locatorMap = LocatorMap.POLARSTEREOGRAPHIC_MAP;
        // Antarctica data is download only
        isOrderable = false;
        slowDownloadStart = true;
        // treat the data as a full mosaic since it works better that way
        isFullMosaic = true;

        // set the dataset name for the CGI scripts since for L1T
        // we have to pass a dataset name
        cgiDatasetName = "LIMA";
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
        return new SwathSceneFilter(md, tocs, this, applet, false);
    }

    // method to return a nominal Landsat scene size in meters
    //--------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(180000,180000);
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
        return 2006;
    }

    // method to return the estimated size (in bytes) of an image file at the
    // indicated resolution
    //-----------------------------------------------------------------------
    public int getImageFileSize(int resolution)
    {
        if (resolution == 1000)
            return 35000;
        else
            return 120000;
    }
}
