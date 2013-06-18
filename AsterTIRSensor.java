// AsterTIRSensor.java defines a class to encapsulate the ASTER TIR sensor 
// details.
//------------------------------------------------------------------------
import java.awt.Color;

public class AsterTIRSensor extends AsterSensor
{
    // Constructor for the AsterSensor class
    //--------------------------------------
    AsterTIRSensor(imgViewer applet)
    {
        super(applet,"L1A Night (SWIR/TIR)", "aster/tir",
              "LPDAAC_ASTER", "ASTER_TIR",
              "http://lpdaac.usgs.gov/products/aster_products_table",
              Color.YELLOW);

        // set the navigation model to the WRS-2 ascending model
        navModel = new WRS2AscendingModel();
    }
}
