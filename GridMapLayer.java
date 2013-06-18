// GridMapLayer.java implements a wrapper class for the different grid map
// layers available in the applet.  Depending on the sensor shown, it displays
// the grid map layer that makes the most sense.
//----------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;


public class GridMapLayer extends MapLayer
{
    private imgViewer applet;
    private MapLayer current;               // currently displayed grid layer
    private ModisTileMapLayer modisLayer;   // Modis Tile layer
    private UtmZoneMapLayer utmLayer;       // UTM zone map layer
    private WrsGridCenterMapLayer wrsLayer; // WRS grid layer

    // constructor for the GridMapLayer class
    //---------------------------------------
    public GridMapLayer(imgViewer applet, Color layerColor, int menuShortcut)
    {
        super(applet.imgArea, "Collection Grid", layerColor, menuShortcut,
              true);

        this.applet = applet;

        // create the potential grid layers
        wrsLayer = new WrsGridCenterMapLayer(applet, layerColor, menuShortcut);
        modisLayer = new ModisTileMapLayer(applet, layerColor, menuShortcut);
        utmLayer = new UtmZoneMapLayer(applet, layerColor, menuShortcut);
        current = wrsLayer;
    }

    // method to set the color for the map layer
    //------------------------------------------
    public void setColor(Color color)
    {
        wrsLayer.setColor(color);
        modisLayer.setColor(color);
        utmLayer.setColor(color);
        super.setColor(color);
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode)
    {
        if ((applet.sensorMenu.getCurrentSensor() 
                instanceof TriDecEtmMosaicDataset)
            || (applet.sensorMenu.getCurrentSensor() 
                 instanceof TriDecTmMosaicDataset))
        {
            current = utmLayer;
        }
        else
            current = wrsLayer;

        return current.setDisplayAreaUsingLatLong(
            minboxULdeg, minboxLRdeg, projCode);
    }

    // method to configure the map layer for the displayed area in
    // projection coordinates.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode)
    {
        current = modisLayer;

        return current.setDisplayAreaUsingProjCoords(
            ulCoord, lrCoord, projCode);
    }

    // method to read the map layer file
    //----------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        current.read(isLoadCancelled, ulMeters, projCode, fileReadCallback);
    }

    // method to clip the map layer components to the display
    //-------------------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        current.clip(upperLeft, pixelSize, dispSize, proj);
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        current.draw(g);
    }
}
