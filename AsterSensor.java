// AsterSensor.java defines the ASTER sensor details
//--------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JDialog;
import java.util.Vector;

public class AsterSensor extends Sensor
{
    private static final int[] resolutions = {1000,400,155};
    private static final int[] borderX = {-4,4,4,-4};
            // 4 corner highlight X border offsets
    private static final int[] borderY = {-4,-4,4,4};
            // 4 corner highlight Y border offsets
    protected String entityIdPrefix;
    protected boolean displayConfirmed = false;

    // Constructor for the AsterSensor class
    //--------------------------------------
    AsterSensor(
        imgViewer applet,       // I: applet reference
        String name,            // I: name to use for the sensor
        String inventoryDir,    // I: name of directory for the data
        String datasetName,     // I: data set name for ordering from EE
        String cgiDatasetName,  // I: data set name for the cgi scripts
        String productInfoUrl,  // I: product info link for the dataset
        Color borderColor       // I: color to use for the highlight border
    )
    {
        super(applet,name,inventoryDir,datasetName,
              "showAsterBrowse.cgi","showAsterMetadata.cgi","NASA_small.gif",
              "http://lpdaac.usgs.gov/products/aster_overview",
              productInfoUrl,
              "https://igskmncnwb001.cr.usgs.gov/aster/estimator/reference_info.asp",
              "Enter the last 10 digits of the granule ID",
              resolutions,borderX,borderY,borderColor);

        // set the dataset name for the cgi scripts
        this.cgiDatasetName = cgiDatasetName;

        // default prefix for a full entity ID
        entityIdPrefix = new String("AST_L1A.");

        // enable the feature to do a full mosaic
        isFullMosaic = true;

        // set ordering flags
        isOrderable = true;

        // display the scene center lat/long in the entry boxes
        displaySceneCenterLatLong = true;

        // disable add all feature
        allowAddAll = false;

        // enable swath mode
        hasSwathMode = true;

        // enable user defined area
        hasUserDefinedArea = true;

        // enable searching by column/row
        hasGridColRowFilter = true;

        // place the logo in the lower right corner
        logoLocation = Sensor.LOGO_LOWER_RIGHT;
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
        // the year and entity id
        imgName = new StringBuffer(getCellDirectory(scene.gridCol,
                                                    scene.gridRow));
        imgName.append("/y");
        imgName.append(scene.year);
        imgName.append("/");

        // the entityID is usually in version:dbID, and files are named with 
        // just the dbID, so strip of the version number
        int filenameStart = scene.entityID.indexOf(':') + 1;
        imgName.append(scene.entityID.substring(filenameStart));

        imgName.append("_");
        imgName.append(Integer.toString(resolution));

        // load correct image based on full res or mosaic view
        if (resolution == 155)
        {
            // full resolution, so use the full resolution jpeg files
            imgName.append(".jpg");
        }
        else
        {
            // mosaic area, so use the 1000- or 400-meter resolution .gif files
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
        else if (resolution == resolutions[1])
            return 1;
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
        return new SwathSceneFilter(md,tocs,this,applet,true);
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
            idList.add(sceneList[i].entityID.substring(4));
        }

        orderUrl = EarthExplorer.buildShoppingCartUrl(this.datasetName, idList);

        return orderUrl;
    }

    // method to return a nominal ASTER scene size in meters
    //------------------------------------------------------
    public Dimension getNominalSceneSize()
    {
        return new Dimension(60000,60000);
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
        if (resolution == 1000)
            return 6000;
        else if (resolution == 400)
            return 25000;
        else
            return 50000;
    }

    // method to convert an entity ID from a scene's metadata to the
    // value to display.
    //--------------------------------------------------------------
    String buildEntityID(Metadata scene)
    {
        return (entityIdPrefix + scene.entityID);
    }

    // method to return the files to download for a given scene when the
    // user downloads metadata and browse.  Returns an array of filenames where
    // the source and destination file names are paired up.  If the source
    // browse name is not known, null is returned in that array entry.
    //-------------------------------------------------------------------------
    public String[] getFilesForScene(Metadata scene)
    {
        String[] files = new String[4];

        int filenameStart = scene.entityID.indexOf(':') + 1;
        String baseFilename = scene.entityID.substring(filenameStart);

        String basename = getCellDirectory(scene.gridCol, scene.gridRow) 
                 + "/y" + scene.year + "/" + baseFilename;

        // set the metadata source and destination names
        files[0] = basename + ".meta";
        files[1] = baseFilename + ".meta";
        // set the browse image source and destination names
        files[2] = basename + ".jpg";
        files[3] = baseFilename + ".jpg";

        return files;
    }

    // method to display a dialog to confirm whether to display this sensor
    //-------------------------------------------------------------------------
    public boolean confirmInitialDisplay()
    {
        boolean continueToSensor = false;

        if (displayConfirmed == true)
        {
            // We have shown the message once, user confirmed display
            return true;
        }

        // We're using a Yes/No/Cancel jOptionPane, where "No"
        // takes the user to Reverb and cancels the display in the browse viewer
        // and "Cancel" goes to the ASTER Policies page and cancels display
        Object[] options = {
            "Continue in GloVis",
            "Go to Reverb",
            "View ASTER Policies"
        };
        String dialogTitle = "ASTER Downloads Restricted";
        JLabel dialogMessage = new JLabel("<html>"
            + "NOTICE: The ASTER Data Access policy has changed.\n<br>"
            + "The ASTER L1A, On-demand and Higher level datasets are\n<br>"
            + "available for metadata search and browse through GloVis\n<br>"
            + "or Reverb. Users must be approved to submit orders. Please\n<br>"
            + "select the [View ASTER Policies] option below or contact\n<br>"
            + "LP DAAC User Services at lpdaac@usgs.gov for additional\n<br>"
            + "information.</html>");

        JOptionPane optionPane = new JOptionPane(dialogMessage,
            JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION,
            null, options, options[0]);
        JDialog dialog = optionPane.createDialog(applet.getDialogContainer(),
            dialogTitle);
        dialog.setVisible(true);
        Object selectedValue = optionPane.getValue();
        if (selectedValue == null)
        {
            continueToSensor = false; // closing dialog cancels all action
        }
        else
        {
            for (int counter = 0; counter < options.length; counter++)
            {
                if (options[counter].equals(selectedValue))
                {
                    if (counter == 0)
                    {
                        continueToSensor = true;
                        displayConfirmed = true;
                        // continue in  GloVis
                    }
                    else if (counter == 1)
                    {
                        continueToSensor = false;
                        // display the Reverb web site
                        try {
                            URL showURL = new URL(
                                "http://reverb.echo.nasa.gov/reverb");
                            applet.getAppletContext().showDocument(
                                showURL,"reverb");
                        }
                        catch (Exception e) {
                            System.out.println("exception: " + e);
                        }
                    }
                    else
                    {
                        continueToSensor = false;
                        // display the ASTER Policies web site
                        try {
                            URL showURL = new URL(
                    "https://lpdaac.usgs.gov/products/aster_policies");
                            applet.getAppletContext().showDocument(
                                showURL,"aster_policies");
                        }
                        catch (Exception e) {
                            System.out.println("exception: " + e);
                        }
                    }
                }
            }
        }

        return continueToSensor;
    }
}

