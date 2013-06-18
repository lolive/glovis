// ModisTileMapLayer.java implements the class for displaying the boundaries
// of the MODIS tiles on the display.
//--------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;


public class ModisTileMapLayer extends MapLayer
{
    private imgViewer applet;
    private Point[] horizontalLines;     // array of horizontal lines to draw
    private Point[] verticalLines;       // array of vertical lines to draw
    private LatLongToModisTile tileConv; // object to convert between tile 
                                         // numbers and projection cocrds
    private Dimension dispSize;          // saved display size

    // track where the edge of the MODIS grid has been reached so the lines
    // are only drawn to that point
    private boolean limitLeft;
    private boolean limitRight;
    private boolean limitTop;
    private boolean limitBottom;

    // constructor for the MapLayer class
    //-----------------------------------
    public ModisTileMapLayer(imgViewer applet, Color layerColor,
                             int menuShortcut)
    {
        super(applet.imgArea, "MODIS Tiles", layerColor, menuShortcut, true);

        this.applet = applet;

        // create the object to convert between tile numbers and projection
        // coordinates
        tileConv = new LatLongToModisTile();
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode)
    {
        // nothing to do for this class, but required to implement

        // return 0 since no files to load
        return 0;
    }

    // method to configure the map layer for the displayed area in
    // projection coordinates.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode)
    {
        // nothing to do for this class

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
        double actualPixelSize = applet.imgArea.md.actualPixelSize;

        // save the display size
        this.dispSize = new Dimension(dispSize);

        // calculate the lower right projection coordinate of the display
        Point lowerRight = 
            new Point((int)(upperLeft.x + dispSize.width * actualPixelSize), 
                      (int)(upperLeft.y - dispSize.height * actualPixelSize));

        // get the tiles visible on the display
        ModisTile ulTile = tileConv.coordinateToTile(upperLeft.x, upperLeft.y);
        ModisTile lrTile = tileConv.coordinateToTile(lowerRight.x,lowerRight.y);

        // make sure the upper left tile's upper left corner is visible
        Point coord = tileConv.tileToCoordinate(ulTile, false);
        if (coord.x < upperLeft.x)
            ulTile.h++;
        if (coord.y > upperLeft.y)
            ulTile.v++;

        // assume MODIS grid limits have not been reached at any side
        limitLeft = false;
        limitRight = false;
        limitTop = false;
        limitBottom = false;

        // only display the real tiles and update the limit flags as needed
        if (ulTile.h <= 0)
        {
            ulTile.h = 0;
            limitLeft = true;
        }
        else if (ulTile.h > 36)
            ulTile.h = 36;
        if (ulTile.v <= 0)
        {
            ulTile.v = 0;
            limitTop = true;
        }
        else if (ulTile.v > 18)
            ulTile.v = 18;
        if (lrTile.h < 0)
            lrTile.h = 0;
        else if (lrTile.h >= 36)
        {
            lrTile.h = 36;
            limitRight = true;
        }
        if (lrTile.v < 0)
            lrTile.v = 0;
        else if (lrTile.v >= 18)
        {
            lrTile.v = 18;
            limitBottom = true;
        }

        // set up the arrays for the visible tile boundaries
        horizontalLines = new Point[lrTile.v - ulTile.v + 1];
        verticalLines = new Point[lrTile.h - ulTile.h + 1];

        // get the coordinates for each horizontal line
        ModisTile tempTile = new ModisTile(ulTile.h, ulTile.v);
        for (int i = ulTile.v; i <= lrTile.v; i++)
        {
            tempTile.v = i;
            horizontalLines[i - ulTile.v] 
                    = tileConv.tileToCoordinate(tempTile, false);
        }

        // get the coordinates for each vertical line
        tempTile.v = ulTile.v;
        for (int i = ulTile.h; i <= lrTile.h; i++)
        {
            tempTile.h = i;
            verticalLines[i - ulTile.h] 
                    = tileConv.tileToCoordinate(tempTile, false);
        }

        // convert the lines to pixels on the display
        for (int i = 0; i < horizontalLines.length; i++)
        {
            horizontalLines[i].x = (int)((horizontalLines[i].x - upperLeft.x)
                    / actualPixelSize);
            horizontalLines[i].y = (int)((upperLeft.y - horizontalLines[i].y)
                    / actualPixelSize);
        }
        for (int i = 0; i < verticalLines.length; i++)
        {
            verticalLines[i].x = (int)((verticalLines[i].x - upperLeft.x)
                    / actualPixelSize);
            verticalLines[i].y = (int)((upperLeft.y - verticalLines[i].y)
                    / actualPixelSize);
        }
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        // if dispSize is null, clipping hasn't happened yet, so nothing to
        // draw
        if (dispSize == null)
            return;

        // set the starting and stopping points for the horizontal lines, 
        // factoring in whether the limit of the MODIS grid has been reached
        // at the left and right.
        int x1 = 0;
        int x2 = dispSize.width;
        if (limitLeft)
            x1 = verticalLines[0].x;
        if (limitRight)
            x2 = verticalLines[verticalLines.length - 1].x;

        // draw the horizontal lines on the display
        for (int i = 0; i < horizontalLines.length; i++)
        {
            int y = horizontalLines[i].y;
            g.setColor(color);
            g.drawLine(x1,y,x2,y);
        }

        // set the starting and stopping points for the vertical lines, 
        // factoring in whether the limit of the MODIS grid has been reached
        // at the top and bottom.
        int y1 = 0;
        int y2 = dispSize.height;
        if (limitTop)
            y1 = horizontalLines[0].y;
        if (limitBottom)
            y2 = horizontalLines[horizontalLines.length - 1].y;

        // draw the vertical lines on the display
        for (int i = 0; i < verticalLines.length; i++)
        {
            int x = verticalLines[i].x;
            g.setColor(color);
            g.drawLine(x,y1,x,y2);
        }
    }
}
