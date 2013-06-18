//  ResolutionMenu.java implements a menu for selecting the currently 
//  displayed resolution.
//-------------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JCheckBoxMenuItem;

public class ResolutionMenu extends JMenu implements ActionListener
{
    private imgViewer applet;           // reference to the applet
    private MosaicData md;              // reference to the mosaic data
    private JCheckBoxMenuItem[] cb;     // checkbox array
    private int currentRes;             // the current resolution in meters

    // constructor
    public ResolutionMenu(imgViewer applet, MosaicData md)
    {
        // call the parent constructor, setting the dialog to be modal
        super("Resolution");
        setMnemonic(KeyEvent.VK_R);

        // save the applet and mosaic data references
        this.applet = applet;
        this.md = md;

        // set up the menu for the current sensor
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        setMenuItems(currSensor);

        // set the default resolution
        cb[0].setState(true);
        currentRes = currSensor.resolutions[0];
    }

    // helper method to set the contents of the menu
    //----------------------------------------------
    private void setMenuItems(Sensor sensor)
    {
        // remove the current menu items
        removeAll();

        // allocate an array for the checkboxes
        int length = sensor.resolutions.length;
        cb = new JCheckBoxMenuItem[length];

        // add a checkbox menu item for each resolution
        for (int i = 0; i < length; i++)
        {
            cb[i] = new JCheckBoxMenuItem(sensor.getResolutionString(i), false);
            cb[i].addActionListener(this);
            add(cb[i]);
        }
    }

    // event handler for menu items changing state
    //--------------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        int resolution = currentRes;     // selected resolution

        // strip the units off of the resolution and convert to an int
        JCheckBoxMenuItem target = (JCheckBoxMenuItem)event.getSource();
        String resString = target.getActionCommand();

        // clear the checkmarks from all the resolutions
        for (int i = 0; i < cb.length; i++)
            cb[i].setState(false);

        // set the checkbox on the selected resolution
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        int found = -1; 
        for (int i = 0; i < currSensor.resolutions.length; i++)
        {
            if (resString.equals(currSensor.getResolutionString(i)))
            {
                cb[i].setState(true);
                found = i;
                resolution = currSensor.resolutions[i];
                break;
            }
        }

        // if the resolution really changes (the Java VM actually sends events
        // even if the item was already selected), update the resolution
        if (resolution != currentRes)
        {
            currentRes = resolution;
            md.setResolution(resolution);
            applet.statusBar.showResolution(
                    currSensor.getResolutionString(found));
        }
    }

    // set the resolution to keep the same level of mosaic shown when 
    // switching sensors.  So, if the user is looking at a 3x3 mosaic in
    // one sensor, they will be looking at a 3x3 mosaic in the new sensor.
    // Some special handling is required for sensors with more than 2 
    // resolutions (like ASTER).
    //  Note: This routine assumes the calling routine will handle actually
    //        setting the resolution, otherwise the resolution could be
    //        accidently set multiple times which could cause a significant
    //        amount of extra work to be done, or cause other problems.
    // Returns: the current resolution in meters
    //---------------------------------------------------------------------
    public int setSensor(Sensor oldSensor, Sensor newSensor)
    {
        // get the current resolution index
        int resolutionIndex = 0;
        if (oldSensor != null)
        {
            // determine which index in the resolutions array is selected
            for (resolutionIndex = 0; 
                 resolutionIndex < oldSensor.resolutions.length - 1;
                 resolutionIndex++)
            {
                if (currentRes == oldSensor.resolutions[resolutionIndex])
                    break;
            }

            // if the number of resolutions available in the two sensors is
            // different, handle it specially (otherwise the same index is
            // used for the new sensor)
            if (oldSensor.resolutions.length != newSensor.resolutions.length)
            {
                // the zero index is kept when switching, but handle the
                // non-zero values
                if (resolutionIndex != 0)
                {
                    // if the resolution index is a single scene, keep a single
                    // scene shown, otherwise, any mosaic should default to the
                    // lowest resolution mosaic.  The end result is that when
                    // switching away from either aster mosaic resolution the
                    // mosaic resolution is displayed for the new dataset
                    if (resolutionIndex == oldSensor.resolutions.length - 1)
                        resolutionIndex = newSensor.resolutions.length - 1;
                    else
                        resolutionIndex = 0;
                }
            }
        }

        // set up the menu for the current sensor
        setMenuItems(newSensor);

        // remember the new resolution
        currentRes = newSensor.resolutions[resolutionIndex];

        // this try/catch block is used to avoid an exception when
        // firefox 1.0 uses Java plugin 1.5 on a linux box.  
        try
        {
            // set the checkbox for the closest resolution
            cb[resolutionIndex].setState(true);
        }
        catch (Exception e) {};

        // show the current resolution in the status bar
        applet.statusBar.showResolution(
                    newSensor.getResolutionString(resolutionIndex));

        return currentRes;
    }
}
