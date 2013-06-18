// LatLongEntry.java implements a widget to allow entry of a latitude and
// longitude location.
//-----------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class LatLongEntry extends JPanel implements ActionListener, Observer
{
    private FocusTextField latitude;
    private FocusTextField longitude;
    private JButton goButton;
    private MosaicData md;
    private imgViewer applet;

    // Constructor for the LatLongEntry widget
    //----------------------------------------
    LatLongEntry(imgViewer applet, MosaicData md)
    {
        this.md = md;
        this.applet = applet;

        // use a box layout for the panel since it does a good job of sizing
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // set the font to use for this component
        setFont(applet.normalFont);

        // build a panel for the label
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new GridLayout(2,1));

        // create the "Lat/Long:" label on two lines
        JLabel latLabel = new JLabel("Lat/");
        latLabel.setFont(applet.boldFont);
        labelPanel.add(latLabel);
        JLabel longLabel = new JLabel("Long:");
        longLabel.setFont(applet.boldFont);
        labelPanel.add(longLabel);
        add(labelPanel);

        // create the latitude entry field
        latitude = new FocusTextField(5);
        latitude.setToolTipText("Latitude entry");
        latitude.addActionListener(this);
        add(latitude);

        // create the longitude entry field
        longitude = new FocusTextField(6);
        longitude.setToolTipText("Longitude entry");
        longitude.addActionListener(this);
        add(longitude);

        // create a go button to apply a new lat/long
        goButton = new JButton("Go");
        goButton.setToolTipText("Go to Lat/Long");
        goButton.addActionListener(this);
        add(goButton);

        // set the size so that it won't grow in height (otherwise the box
        // layout will let it grow too tall)
        Dimension size = getPreferredSize();
        size.width = 100;
        setMinimumSize(size);
        size.width = 240;
        setMaximumSize(size);
    }

    // action handler for the "Go" Button and enter key action
    //--------------------------------------------------------
    public void actionPerformed(ActionEvent event) 
    {
        parseInput();
    }

    // Method for the Observer interface
    //----------------------------------
    public void update(Observable ob, Object arg)
    {
        // get the current lat/long location
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        TOC cell = md.getCurrentCell();
        LatLong latLong = null;

        if ((currSensor.displaySceneCenterLatLong) && cell.valid)
        {
            // the current sensor should display the selected scene center
            // in the entry box and the selected cell is valid, so calculate
            // the lat/long of the scene center

            // calculate the X/Y coordinates of the selected scene center
            Metadata scene = cell.scenes[cell.currentDateIndex];
            latLong = md.getLatLong(scene.centerXY);
        }
        else
        {
            // FIXME - this won't work well for MODIS tiles on the edge of
            // the world
            // display the grid cell lat/long in the entry box
            ProjectionTransformation proj = md.getProjection();
            Point coords = cell.getCenterProjCoords(proj);
            latLong = proj.projToLatLong(coords.x,coords.y);
            // FIXME
            if (latLong == null)
            {
                latitude.setText("");
                longitude.setText("");
                return;
            }
        }

        // convert the lat/long to a single digit after the decimal place
        // and update the entry box
        double temp = latLong.latitude * 10;
        if (temp > 0)
            temp += 0.5;
        else
            temp -= 0.5;
        temp = (double)((int)temp) / 10.0;
        latitude.setText("" + temp);

        temp = latLong.longitude * 10;
        if (temp > 0)
            temp += 0.5;
        else
            temp -= 0.5;
        temp = (double)((int)temp) / 10.0;
        longitude.setText("" + temp);
    }
    
    // Method for the Event-Listeners
    // as the "Go" button and "Enter Key" 
    //------------------------------------------
    private void parseInput()
    {
        double lat;     // latitude read from the entry field
        double lon;     // longitude read from the entry field
        boolean doingLong = false; // flag to track what is being converted

        try
        {
            // get the latitude, trim any leading or trailing spaces,
            // and make sure it contains something
            String temp = latitude.getText();
            temp.trim();
            if (temp.equals(""))
            {
                applet.statusBar.showStatus("Latitude value invalid");
                return;
            }

            // convert the latitude to a double
            Double d = new Double(temp);
            lat = d.doubleValue();

            // flag longitude is being converted for the catch statement
            doingLong = true;

            // get the longitude, trim any leading or trailing spaces, 
            // and make sure it contains something
            temp = longitude.getText();
            temp.trim();
            if (temp.equals(""))
            {
                applet.statusBar.showStatus("Longitude value invalid");
                return;
            }

            // convert the longitude to a double
            d = new Double(temp);
            lon = d.doubleValue();

            // check the range on the latitude and longitude
            if ((lat < -90.0) || (lat > 90.0))
            {
                applet.statusBar.showStatus("Latitude " + lat +
                                 " out of range!");
                latitude.setText("");
                return;
            }
            else if ((lon < -180.0) || (lon > 180.0))
            {
                applet.statusBar.showStatus("Longitude " + lon +
                                " out of range!");
                longitude.setText("");
                return;
            }
            else
            {
                // jump to the new location
                md.gotoLatLong(lat, lon);
            }
        }
        catch (NumberFormatException e)
        {
            // error converting string to a Double, so display a message
            // in the browser status bar
            String badValue;
            if (doingLong)
            {
                badValue = longitude.getText();
                longitude.setText("");
            }
            else
            {
                badValue = latitude.getText();
                latitude.setText("");
            }
            applet.statusBar.showStatus(
               "Illegal lat/long value of \"" + badValue + "\"");
        }
    }
}
