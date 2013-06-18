// AsterVNIRSensor.java defines a class to encapsulate the ASTER VNIR sensor 
// details.
//--------------------------------------------------------------------------
import java.awt.Color;

public class AsterVNIRSensor extends AsterSensor
{
    // Constructor for the AsterSensor class
    //--------------------------------------
    AsterVNIRSensor(imgViewer applet)
    {
        super(applet, "L1A Day (VNIR/SWIR/TIR)", "aster/vnir",
              "LPDAAC_ASTER", "ASTER_VNIR",
              "http://lpdaac.usgs.gov/products/aster_products_table",
              Color.YELLOW);

        // set the navigation model to the WRS-2 descending model
        navModel = new WRS2Model();
    }
}
