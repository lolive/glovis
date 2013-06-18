// WrsGridCenterMapLayer.java implements the class for displaying the WRS
// grid points.
//-----------------------------------------------------------------------
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;


public class WrsGridCenterMapLayer extends MapLayer
{
    private imgViewer applet;
    private Point[] gridCenters; // proj coords of grid centers
    private Point[] gridOnScreen;// screen coords of grid centers

    // constructor for the MapLayer class
    //-----------------------------------
    public WrsGridCenterMapLayer(imgViewer applet, Color layerColor,
                                 int menuShortcut)
    {
        super(applet.imgArea, "WRS Centers", layerColor, menuShortcut, true);

        this.applet = applet;

        gridCenters = null;
        gridOnScreen = null;
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

    // method to clip the map layer components to the display
    //-------------------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        // get the navigation model
        NavigationModel nm = applet.sensorMenu.getCurrentSensor().navModel;

        // get the gridCenters set up for the current displayed area
        // calculate grid cell centers in projection coordinates for the 
        // currently displayed cells
        TOC[] mosaicCells = applet.imgArea.md.getMosaicCells();
        gridCenters = new Point[mosaicCells.length];
        gridOnScreen = new Point[mosaicCells.length];
        for (int i = 0; i < mosaicCells.length; i++)
        {
            int row = mosaicCells[i].gridRow;

            // get the grid centers for the legal row numbers (since some
            // row numbers may be off the edge of the grid in some situations)
            if (nm.checkRowBounds(row) == row)
                gridCenters[i] = mosaicCells[i].getCenterProjCoords(proj);
            else
                gridCenters[i] = null;
        }

        // convert available grid centers to screen coordinates
        for (int i = 0; i < gridCenters.length; i++)
        {
            if (gridCenters[i] == null)
                gridOnScreen[i] = null;
            else
            {
                int x = (gridCenters[i].x - upperLeft.x)/pixelSize;
                int y = (upperLeft.y - gridCenters[i].y)/pixelSize;
                gridOnScreen[i] = new Point(x,y);
            }
        }
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        if (gridOnScreen == null)
            return;

        // draw grid centers if they are defined
        for (int i = 0; i < gridOnScreen.length; i++)
        {
            // only draw the defined centers
            if (gridOnScreen[i] != null)
            {
                g.setColor(Color.BLACK);
                g.fillRect(gridOnScreen[i].x - 6, gridOnScreen[i].y - 6, 12,12);
                g.setColor(color);
                g.fillRect(gridOnScreen[i].x - 5, gridOnScreen[i].y - 5, 10,10);
            }
        }
    }
}
