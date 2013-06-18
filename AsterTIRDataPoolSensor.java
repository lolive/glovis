// AsterTIRDataPoolSensor.java defines a class to encapsulate details for the
// ASTER L1B US Archive - TIR sensor.
//------------------------------------------------------------------------
import java.awt.Color;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class AsterTIRDataPoolSensor extends AsterSensor
{
    // Constructor for the AsterTIRDataPoolSensor class
    //----------------------------------------------
    AsterTIRDataPoolSensor(imgViewer applet)
    {
        super(applet, "L1B US Night (SWIR/TIR)", "aster_datapool/tir",
              "LPDAAC_ASTER", "ASTER_TIR_DATAPOOL",
              "http://lpdaac.usgs.gov/products/aster_products_table",
               Color.YELLOW);

        // set the navigation model to the WRS-2 ascending model
        navModel = new WRS2AscendingModel();

        // override the prefix for a full entity ID
        this.entityIdPrefix = new String("AST_L1B.");
    }

    // override the method to confirm whether to display - the
    // L1B archive over the US is open to everyone
    // -------------------------------------------------------------
    public boolean confirmInitialDisplay()
    {
        return true;
    }
}
