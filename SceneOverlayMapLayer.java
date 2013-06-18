// WrsGridCenterMapLayer.java implements the class for displaying the WRS
// grid points.
//-----------------------------------------------------------------------
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Vector;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


public class SceneOverlayMapLayer extends MapLayer implements ListDataListener
{
    private imgViewer applet;
    private Vector sceneList;
    private boolean[] enabledSensors;
    private ConfigureSceneOverlayDialog configureDialog;

    private class SceneInfo
    {
        public Polygon polygon;
        public String name;
    }

    // constructor for the MapLayer class
    //-----------------------------------
    public SceneOverlayMapLayer(imgViewer applet, Color layerColor,
                                int menuShortcut)
    {
        super(applet.imgArea, "Scene List Overlay", layerColor, menuShortcut,
              true);

        this.applet = applet;
        Sensor[] sensors = applet.sensorMenu.getSensors();
        enabledSensors = new boolean[sensors.length];
        for (int i = 0; i < sensors.length; i++)
        {
            sensors[i].sceneList.addListDataListener(this);

            // only enable the sensors that are not combined datasets
            if (!sensors[i].hasMultipleDatasets)
                enabledSensors[i] = true;
            else
                enabledSensors[i] = false;
        }
        sceneList = new Vector();

        configureDialog
            = new ConfigureSceneOverlayDialog(applet, this, enabledSensors);
    }

    // method to show the configuration dialog for the scene overlay map layer
    //------------------------------------------------------------------------
    public void showConfigureDialog()
    {
        // set the enabled sensors if the dialog isn't visible
        if (!configureDialog.isVisible())
            configureDialog.setEnabledSensors(enabledSensors);

        // set the dialog box location and make sure it is shown
        Point loc = applet.getDialogLoc();
        loc.y += 30;
        configureDialog.setLocation(loc);
        configureDialog.setVisible(true);
    }

    // method to free the gui resources
    //---------------------------------
    public void cleanup()
    {
        configureDialog.dispose();
        configureDialog = null;
    }

    // method to update which sensors should be shown
    //-----------------------------------------------
    public void updateShownSensors(boolean[] enabled)
    {
        // update the enabled sensors
        for (int i = 0; i < enabledSensors.length; i++)
            enabledSensors[i] = enabled[i];

        // since the user updated the configuration, the map layer should be on
        setLayerOn(true);
        applet.mapLayerMenu.setLayerState(getName(), true, true);

        // force a repaint of the image area to make sure the correct
        // sensor scene lists are shown
        updateForSceneListChange();
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode)
    {
        // nothing to do for this class

        // return 0 since no files to load
        return 0;
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode)
    {
        // nothing to do for this class, but required to implement

        // return 0 since no files to load
        return 0;
    }

    // method to read the map layer file
    //----------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        // nothing to read
    }

    // list data listener methods to allow updating the displayed data when
    // scenes are added/removed from the scene list
    public void contentsChanged(ListDataEvent e)
    {
        updateForSceneListChange();
    }
    public void intervalAdded(ListDataEvent e)
    {
        updateForSceneListChange();
    }
    public void intervalRemoved(ListDataEvent e)
    {
        updateForSceneListChange();
    }
    private void updateForSceneListChange()
    {
        // get the info about the displayed area
        ImagePane imagePane = applet.imgArea;
        Point ul = imagePane.getUpperLeftCorner();
        if (ul == null)
            return;
        ProjectionTransformation proj = imagePane.md.getProjection();
        int pixelSize = imagePane.md.pixelSize;
        Dimension dispSize = imagePane.getSize();

        // clip the data to the current displayed area
        clip(ul, pixelSize, dispSize, proj);

        // force a repaint of the image area
        applet.imgArea.repaint();
    }

    // method to clip the map layer components to the display
    //-------------------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        // empty the list of polygons to show on the display
        sceneList.clear();

        // if the layer isn't on, return
        if (!isLayerOn())
        {
            return;
        }

        double actualPixelSize = applet.imgArea.md.actualPixelSize;
        int[] tempX = new int[4];
        int[] tempY = new int[4];

        // create a 2D rectangle for the display to filter out the scenes
        // that aren't visible
        Rectangle2D dispArea = new Rectangle2D.Double(0.0, 0.0,
                                     dispSize.width, dispSize.height);

        Sensor[] sensors = applet.sensorMenu.getSensors();
        for (int sensorNum = 0; sensorNum < sensors.length; sensorNum++)
        {
            if (!enabledSensors[sensorNum])
                continue;

            Sensor sensor = sensors[sensorNum];

            // get the count of scenes in the scene list
            int count = sensor.sceneList.getSceneCount();

            // consider each of the scenes in the scene list for display
            for (int index = 0; index < count; index++)
            {
                // get the corners in projection coordinates
                Metadata scene = sensor.sceneList.getSceneAt(index);
                scene.getSceneCornersInProj(proj, tempX, tempY);

                // convert the projection coordinates to be relative to the
                // upper left corner in pixels
                for (int i = 0; i < 4; i++)
                {
                    double x = (tempX[i] - upperLeft.x) / actualPixelSize;
                    double y = (upperLeft.y - tempY[i]) / actualPixelSize;
                    tempX[i] = (int)Math.round(x);
                    tempY[i] = (int)Math.round(y);
                }

                // create a polygon
                Polygon poly = new Polygon(tempX, tempY, 4);

                // if the polygon intersects the display area, add it to the
                // list to display
                if (poly.intersects(dispArea))
                {
                    SceneInfo info = new SceneInfo();
                    info.polygon = poly;
                    info.name = sensor.buildShortEntityID(scene);
                    sceneList.add(info);
                }
            }
        }
    }

    // method to find the name associated with a polygon that contains an X/Y
    // coordinate.  It returns the polygon with the smallest bounding box that
    // contains the point.
    //------------------------------------------------------------------------
    public MapLayerFeatureInfo findFeatureName(int x, int y)
    {
        // track the minimum area found and the feature name for that area
        double minArea = 100000000000.0;
        String foundName = null;

        int count = sceneList.size();

        for (int i = 0; i < count; i++)
        {
            SceneInfo info = (SceneInfo)sceneList.get(i);
            if (info.polygon.contains(x,y))
            {
                // if this rectangle has the smallest bounding box area
                // found so far, make it the new choice to report
                Rectangle bounds = info.polygon.getBounds();
                double area = bounds.width * bounds.height;
                if (area < minArea)
                {
                    minArea = area;
                    foundName = info.name;
                }
            }
        }

        // return the name found (null if none found)
        if (foundName != null)
        {
            MapLayerFeatureInfo info = new MapLayerFeatureInfo();
            info.name = foundName;
            info.area = minArea;
            return info;
        }
        else
            return null;
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;

        // set the graphics object to draw lines of the correct width
        Stroke savedStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
                                     BasicStroke.JOIN_ROUND));

        // prepare to draw a black shadow under each scene outline
        g2.setColor(Color.BLACK);
        g2.translate(-1, 1);

        // loop through drawing twice.  Once for the shadow and once for the
        // real scene outline.
        int count = sceneList.size();
        for (int loop = 0; loop < 2; loop++)
        {
            // draw all the polygons
            for (int i = 0; i < count; i++)
            {
                SceneInfo info = (SceneInfo)sceneList.get(i);
                g.drawPolygon(info.polygon);
            }
            // set things up for drawing the polygon in the real color and
            // location
            g2.setColor(color);
            if (loop == 0)
                g2.translate(1, -1);
        }

        // reset the stroke setting
        g2.setStroke(savedStroke);
    }
}

// The ConfigureSceneOverlayDialog class implements a dialog box that allows
// selecting the sensors to show for the scene overlay map layer.
//--------------------------------------------------------------------------
class ConfigureSceneOverlayDialog extends JDialog implements ActionListener
{
    private SceneOverlayMapLayer mapLayer;
    private JCheckBox[] sensorCB;

    // constructor for the ConfigureSceneOverlayDialog
    //------------------------------------------------
    public ConfigureSceneOverlayDialog(imgViewer applet,
                                       SceneOverlayMapLayer mapLayer,
                                       boolean[] enabledSensors)
    {
        // create a modal dialog located by the parent component
        super(new JFrame(), "Configure Scene List Overlay", false);

        this.mapLayer = mapLayer;

        // use a border layout
        getContentPane().setLayout(new BorderLayout());

        // place a message at the top of the dialog box
        JLabel label = new JLabel("Select the scene lists to overlay");
        getContentPane().add(label, "North");

        // create the buttons at the bottom of the dialog
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2,1));
        JPanel firstRowPanel = new JPanel();
        JPanel secondRowPanel = new JPanel();
        JButton setAllButton = new JButton("Set All");
        setAllButton.setMnemonic(KeyEvent.VK_S);
        setAllButton.setToolTipText("Set all checkboxes");
        firstRowPanel.add(setAllButton);
        JButton clearAllButton = new JButton("Clear All");
        clearAllButton.setMnemonic(KeyEvent.VK_L);
        clearAllButton.setToolTipText("Clear all checkboxes");
        firstRowPanel.add(clearAllButton);
        JButton okButton = new JButton("OK");
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.setToolTipText("Accept changes and close dialog");
        secondRowPanel.add(okButton);
        JButton applyButton = new JButton("Apply");
        applyButton.setMnemonic(KeyEvent.VK_A);
        applyButton.setToolTipText("Apply changes");
        secondRowPanel.add(applyButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setToolTipText(
                "Cancel unapplied changes and close dialog");
        secondRowPanel.add(cancelButton);
        buttonPanel.add(firstRowPanel);
        buttonPanel.add(secondRowPanel);
        getContentPane().add(buttonPanel, "South");

        // listen for button presses
        setAllButton.addActionListener(this);
        clearAllButton.addActionListener(this);
        okButton.addActionListener(this);
        applyButton.addActionListener(this);
        cancelButton.addActionListener(this);

        // get the list of sensors to add
        Sensor[] sensors = applet.sensorMenu.getSensors();

        // arrange the checkboxes in two columns in a scrollable pane
        int rows = (sensors.length + 1) / 2;
        JPanel sensorPanel = new JPanel();
        sensorPanel.setLayout(new GridLayout(rows, 2));
        sensorCB = new JCheckBox[sensors.length];

        for (int i = 0; i < sensors.length; i++)
        {
            sensorCB[i] = null;
            // if not a combined dataset, add the checkbox to the panel
            if (!sensors[i].hasMultipleDatasets)
            {
                sensorCB[i] = new JCheckBox(sensors[i].sensorName);
                sensorCB[i].setSelected(enabledSensors[i]);
                sensorCB[i].setToolTipText("Show " + sensors[i].sensorName);
                sensorPanel.add(sensorCB[i]);
            }
        }

        JScrollPane scrollPane = new JScrollPane(sensorPanel);
        getContentPane().add(scrollPane, "Center");

        // set the size of the dialog
        setSize(360,460);
    }

    // method to set the sensors that are enabled
    //-------------------------------------------
    public void setEnabledSensors(boolean[] enabledSensors)
    {
        for (int i = 0; i < enabledSensors.length; i++)
        {
            if (sensorCB[i] != null)
                sensorCB[i].setSelected(enabledSensors[i]);
        }
    }

    // actionPerformed handles the button presses in the dialog
    //---------------------------------------------------------
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();

        if (command.equals("Set All"))
        {
            for (int i = 0; i < sensorCB.length; i++)
            {
                if (sensorCB[i] != null)
                    sensorCB[i].setSelected(true);
            }
        }
        else if (command.equals("Clear All"))
        {
            for (int i = 0; i < sensorCB.length; i++)
            {
                if (sensorCB[i] != null)
                    sensorCB[i].setSelected(false);
            }
        }
        else if (command.equals("OK") || command.equals("Apply"))
        {
            // get the enabled sensors from the checkboxes
            boolean[] enabled = new boolean[sensorCB.length];
            for (int i = 0; i < enabled.length; i++)
            {
                if (sensorCB[i] != null)
                    enabled[i] = sensorCB[i].isSelected();
            }

            // update the map layer with the new list of enabled sensors
            mapLayer.updateShownSensors(enabled);

            if (command.equals("OK"))
                setVisible(false);
        }
        else if (command.equals("Cancel"))
        {
            // cancelled, so close the dialog
            setVisible(false);
        }
    }
}
