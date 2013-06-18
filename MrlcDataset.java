// MrlcDataset.java defines a class to encapsulate the MRLC dataset details.
//--------------------------------------------------------------------------
import java.text.DecimalFormat;
import java.awt.Color;
import java.awt.Dimension;

public abstract class MrlcDataset extends LandsatSensor
{
    private DecimalFormat threeDigitFormat; // three digit number formatter
    private DecimalFormat fourDigitFormat;  // four digit number formatter

    // Constructor
    MrlcDataset
    (
        imgViewer applet,       // I: applet reference
        String sensorName,      // I: name to use for the sensor
        String inventoryDir,    // I: name of directory for the data
        String datasetName,     // I: EE data set name for ordering
        String productInfoUrl,  // I: product info link for the dataset
        int[] resolutions,      // I: resolutions available
        int[] borderXIn,        // I: 4 corner highlight X border offsets
        int[] borderYIn,        // I: 4 corner highlight Y border offsets
        Color borderColor       // I: color to use for the highlight border
    )
    {
        super(applet, sensorName, inventoryDir, datasetName,
              "showMrlcBrowse.cgi", "showMrlcMetadata.cgi", "USGS_logo.gif",
              "http://www.usgs.gov", productInfoUrl, null, resolutions,
              borderXIn, borderYIn, borderColor);

        // set the dataset name for the cgi scripts (same as datasetName)
        this.cgiDatasetName = datasetName; // MRLC2K_ARCH = TC, MRLC2K_SITC = RA

        // Set up formatters for creating file names
        threeDigitFormat = new DecimalFormat ("000");
        fourDigitFormat = new DecimalFormat ("0000");

        // set the flags for the optional sensor features available
        hasJulianDateMetadata = false;
        isOrderable = false;
        hasColRowInSceneID = true;
        hasSwathMode = false;
        hasNdviLineGraph = true;
        warnWhenOrderingPoorQuality = false;
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

    // method to return the files to download for a given scene when the
    // user downloads metadata and browse.  Returns an array of filenames where
    // the source and destination file names are paired up.  If the source
    // browse name is not known, null is returned in that array entry.
    //-------------------------------------------------------------------------
    public String[] getFilesForScene(Metadata scene)
    {
        String[] files = new String[4];

        // set the metadata source and destination names
        String bestResImageName = makeImageName(scene,
            resolutions[resolutions.length - 1]);
        int index = bestResImageName.lastIndexOf('_');
        files[0] = bestResImageName.substring(0, index);
        files[0] += ".meta";
        files[1] = scene.entityID + ".meta";

        // set the browse image source and destination names.  Note that
        // the source is set to null since the filename varies and the null
        // tells the file downloader to get the browse name from the metadata
        // file.
        files[2] = bestResImageName;
        files[3] = scene.entityID + ".jpg";

        return files;
    }
}
