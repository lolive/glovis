// LogoImage.java implements a class for handling the logo to show for 
// the available sensors.
// 
// TBD - do away with this and just load the logo in the Sensor class?
//------------------------------------------------------------------------
import java.awt.Image;
import java.awt.MediaTracker;
import java.net.URL;

public class LogoImage
{
    private imgViewer applet;
    private Image[] sensorLogos;
    private Image currentLogo;
    private int logoLocation;

    // Constructor
    //------------
    LogoImage(imgViewer parent)
    {
        applet = parent;    // Save ptr to parent

        Sensor[] sensors = applet.getSensors();
        sensorLogos = new Image[sensors.length];

        MediaTracker mt = new MediaTracker(applet);

        for (int i = 0; i < sensors.length; i++)
        {
            // Load the logo images from the server
            sensorLogos[i] = applet.getImage(applet.getCodeBase(), 
                                       "graphics/" + sensors[i].logoName);
            mt.addImage(sensorLogos[i],i);
        }

        // wait for all the logos to load so the size of the logo will be
        // known for positioning it on the display
        try
        {
            mt.waitForAll();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }

        // make sure all the images loaded correctly
        for (int i = 0; i < sensors.length; i++)
        {
            if (mt.isErrorID(i))
            {
                // issue an error message to the console that the image load
                // failed (result will be no logo displayed)
                System.out.println("Error loading graphics/" 
                                   + sensors[i].logoName);
            }
        }
        setSensor(applet.sensorMenu.getCurrentSensor());
    }

    // set the current sensor logo to display
    //---------------------------------------
    public void setSensor(Sensor currentSensor)
    {
        Sensor[] sensors = applet.getSensors();
        for (int i = 0; i < sensors.length; i++)
        {
            if (sensors[i] == currentSensor)
            {
                currentLogo = sensorLogos[i];
                logoLocation = sensors[i].logoLocation;
                break;
            }
        }
    }

    // method to return the current logo image for display
    //----------------------------------------------------
    public Image getLogo()
    {
        return currentLogo;
    }

    // method to return the current logo location (LOGO_LOWER_LEFT or
    // LOGO_LOWER_RIGHT)
    //---------------------------------------------------------------
    public int getLocation()
    {
        return logoLocation;
    }

    // method to perform the correct action when a logo is clicked on
    //---------------------------------------------------------------
    public void clicked()
    {
        try
        {
            URL linkURL = new URL(applet.sensorMenu.getCurrentSensor().
                                  logoLink);
            applet.getAppletContext().showDocument(linkURL,"_blank");
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }

    // method to cleanup any resources when the applet is stopped
    //-----------------------------------------------------------
    public void cleanup()
    {
        for (int i = 0; i < sensorLogos.length; i++)
        {
            if (sensorLogos[i] != null)
            {
                sensorLogos[i].flush();
                sensorLogos[i] = null;
            }
        }
    }
}
