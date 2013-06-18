// TerraLookAsterVNIRSensor.java defines a class to encapsulate the ASTER VNIR
// sensor details for the TerraLook support.
//----------------------------------------------------------------------------
import java.awt.Color;

public class TerraLookAsterVNIRSensor extends AsterSensor
{
    // Constructor for the TerraLookAsterVNIRSensor class
    //---------------------------------------------------
    TerraLookAsterVNIRSensor(imgViewer applet)
    {
        super(applet, "TL ASTER (2000->)","aster/vnir","TERRA_ASTER",
              "TERRA_ASTER", "http://terralook.cr.usgs.gov", Color.YELLOW);

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();

        // make sure ordering is enabled (in case AsterSensor turns it off)
        isOrderable = true;

        // pretend this is regular ASTER for CGI scripts
        cgiDatasetName = "ASTER_VNIR";
    }
    
    // override the method to confirm whether to display - TerraLook
    // should always display ASTER (it is on the "Free List")
    // -------------------------------------------------------------
    public boolean confirmInitialDisplay()
    {
        return true;
    }
}
