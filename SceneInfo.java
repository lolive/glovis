//----------------------------------------------------------------------
// SceneInfo.java
//
//  The SceneInfo class implements a widget to display information about
//  the currently selected scene
//----------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

class SceneInfo extends JPanel implements Observer
{
    private imgViewer applet;       // reference to main applet
    private JLabel sceneInfoHeader; // Header for Scene info output area
    private MosaicData md;          // mosaic data object
    private JTextArea sceneInfo;    // text area for displaying scene info

    // Constructor for the SceneInfo
    //------------------------------
    SceneInfo(imgViewer applet, MosaicData mdIn)
    {
        super();

        this.applet = applet;
        md = mdIn;

        setLayout(new BorderLayout());

        // build the Scene information display area
        sceneInfoHeader = new JLabel("Scene Information:");
        sceneInfoHeader.setFont(applet.boldFont);

        // place the text area inside a panel so the border can be set around
        // it
        JPanel sceneInfoPanel = new JPanel(new BorderLayout());
        sceneInfoPanel.setBorder(BorderFactory.createEtchedBorder());
        sceneInfo = new JTextArea("",3,24);
        sceneInfo.setEditable(false);
        sceneInfo.setBackground(Color.WHITE);
        sceneInfo.setToolTipText("Scene Information");
        sceneInfoPanel.add(sceneInfo, "Center");

        add(sceneInfoHeader,"North");
        add(sceneInfoPanel,"Center");

        // force the height to be the preferred height so the text area won't
        // get too tall or short
        Dimension size = getPreferredSize();
        size.width = 100;
        setMinimumSize(size);
        size.width = 240;
        setMaximumSize(size);
    }

    // Method for the Observer interface
    //----------------------------------
    public void update(Observable ob, Object arg)
    {
        Metadata currentScene = md.getCurrentScene();
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        String id;
        String cc;
        String date;
        int qual;
        int cloudCover = -1;

        if (currentScene != null)
        {
            // the current scene is valid, so update the scene info
            id = currentScene.getShortEntityIDForDisplay();
            cloudCover = currentScene.cloudCover;
            cc = cloudCover + "%";
            if (currSensor.hasAcqDate)
                date = currentScene.getDateString();
            else
                date = "";
            qual = currentScene.getQuality();
        }
        else
        {
            // the current scene is not valid, so clear out the scene info
            id = "";
            cc = "";
            date = "";
            qual = -1;
        }

        // only show quality when the dataset supports it
        StringBuffer line2 = new StringBuffer();

        // only show the cloud cover when the dataset supports it
        if (currSensor.hasCloudCover)
        {
            if (currSensor.hasCustomSceneInfoLine)
            {
                line2.append("CC: " + cc);
                if (currSensor.hasAcqDate)
                {
                    if (cloudCover < 10)
                        line2.append("    Date: " + date);
                    else if (cloudCover < 100)
                        line2.append("  Date: " + date);
                    else
                        line2.append(" Date: " + date);
                }
            }
            else
            {
                line2.append("Cloud Cover: " + cc);

                // add quality information if its available
                if (currSensor.numQualityValues > 0)
                {
                    // if it's a valid scene
                    if (qual >= 0)
                    {
                        // hack to keep the quality field stable
                        if (cloudCover < 10)
                            line2.append("    Qlty: " + qual);
                        else if (cloudCover < 100)
                            line2.append("  Qlty: " + qual);
                        else
                            line2.append(" Qlty: " + qual);
                    }
                    else
                    {
                        line2.append("          Qlty: ");
                    }

                }
            }
            // there is at least cloud cover, so add a new line
            line2.append("\n");
        }
        else
        {
            // no cloud cover, so only show quality, if available
            if (currSensor.numQualityValues > 0)
            {
                if (qual >= 0)
                {
                    line2.append("Qlty: " + qual + "\n");
                }
                else
                {
                    line2.append("Qlty:\n");
                }
            }
        }

        // set the info in the info display
        if (!currSensor.hasCustomSceneInfoLine)
        {
            String info = "ID: " + id + "\n" + line2.toString();
            if (currSensor.hasAcqDate)
                info += "Date: " + date;
            sceneInfo.setText(info);
        }
        else
        {
            sceneInfo.setText("ID: " + id + "\n" + line2.toString()
                            + currSensor.getCustomSceneInfo(currentScene));
        }
    }
}
