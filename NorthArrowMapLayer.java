// NorthArrowMapLayer.java implements a map layer that consists of an arrow
// that points north.
//-------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;

public class NorthArrowMapLayer extends MapLayer
{
    private imgViewer applet;
    private double northAngle;  // X/Y arrow displayed coordinates
    private boolean canDisplay; // flag to indicate the arrow can be displayed
                                // Note: may not be displayable if the current
                                // projection coordinates do not map to lat/long

    // constructor for the NorthArrowMapLayer
    //---------------------------------------
    public NorthArrowMapLayer(imgViewer applet, int menuShortcut)
    {
        super(applet.imgArea, "North Arrow", Color.WHITE, menuShortcut, true);

        this.applet = applet;
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

    // method to read the needed data for the north arrow
    //---------------------------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        // nothing to read
    }

    // method to clip the north arrow layer to the currently displayed area
    //---------------------------------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        // calculate center of display
        int urx = upperLeft.x + (dispSize.width*pixelSize);
        int lly = upperLeft.y - (dispSize.height*pixelSize);
        int centerX = (upperLeft.x + (urx - upperLeft.x)/2);
        int centerY = (upperLeft.y - (upperLeft.y - lly)/2);

        // convert to coordinates
        LatLong arrowLatLong = proj.projToLatLong(centerX,centerY);

        // if the lat/long is null (i.e. falls off the edge of the world for
        // MODIS), search around the displayed area for a place where it is 
        // defined
        if (arrowLatLong == null)
        {
            for (int i = 0; i < 2; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    centerX = (upperLeft.x + (urx - upperLeft.x)/3 * (i + 1));
                    centerY = (upperLeft.y - (upperLeft.y - lly)/3 * (j + 1));

                    arrowLatLong = proj.projToLatLong(centerX,centerY);
                    if (arrowLatLong != null)
                        break;
                }
                if (arrowLatLong != null)
                    break;
            }
        }

        // if the lat/long is still not available, flag that the arrow cannot
        // be displayed and return
        if (arrowLatLong == null)
        {
            canDisplay = false;
            return;
        }
            

        // add one to the latitude to find two points on the display for
        // drawing an arrow and convert the latLong to proj
        LatLong tempLatLong = new LatLong(arrowLatLong.latitude + 1.0, 
                arrowLatLong.longitude);
        Point displayTempMeters = proj.latLongToProj(tempLatLong);

        // calculate the angle of the arrow
        northAngle = -Math.atan2(centerX - displayTempMeters.x, 
                                 displayTempMeters.y - centerY);
        // set the flag that the arrow can be displayed
        canDisplay = true;
    }

    // method to draw the north arrow on the display
    //----------------------------------------------
    public void draw(Graphics g)
    {
        // if the arrow cannot be displayed, just return
        if (!canDisplay)
            return;

        // use the bold font for the "N" above the arrow
        Font curFont = applet.largeBoldFont;
        g.setFont(curFont);

        // cache some values for the rotated arrow calculations
        double sin = Math.sin(northAngle);
        double cos = Math.cos(northAngle);
       
        // arrow variable declared
        int originX = 50;   // center of the arrow on the display
        int originY = 60;
        int x = 0;
        int y = 0;
        int wa = 6;         // width of arrow head
        int arrowend = 4;   // location of the bottom of the arrow head
        int notch = 3;      // notch at the bottom of the arrow head
        int ha = 18;        // height of arrow head
        double wl = 2;    // width of line
        int hl = 18;        // height of line
        double arrowX[] = {x,x-wa,x-wl,x-wl,x+wl,x+wl,x+wa};
        double arrowY[] = {y-ha,y+arrowend,y+arrowend-notch,y+hl,y+hl,
                           y+arrowend-notch,y+arrowend};

        // if the scroll bar is present in the applet make the arrow
        // follow the display area always in the upper left corner
        Point scroll = applet.imgScroll.getViewport().getViewPosition();
        originX = scroll.x + originX;
        originY = scroll.y + originY;

        // calculate the location for the rotated arrow
        int rotatedArrowX[] = new int[arrowX.length];
        int rotatedArrowY[] = new int[arrowY.length];
        for (int i = 0; i < arrowX.length; i++)
        {
            rotatedArrowX[i] = (originX + (int)Math.round(
                       (arrowX[i] * cos) - (arrowY[i] * sin)));
            rotatedArrowY[i] = (originY + (int)Math.round(
                       (arrowX[i] * sin) + (arrowY[i] * cos)));
        }

        // calculations for location of the "N" string
        FontMetrics fontMetrics = g.getFontMetrics();
        int height = fontMetrics.getHeight();
        int width = fontMetrics.charWidth('N');
        int northX = -width/2;
        int northY = -22 - height/2;
        int stringX = (originX + (int)Math.round((northX * cos) - 
                        (northY * sin)));
        int stringY = (originY + (int)Math.round((northX * sin) + 
                        (northY * cos)));        
        stringY += height/4;

        // draw black outline "N" string four times, offset by a little each
        // time (note only needed since white is hard to pick out in some 
        // areas)
        g.setColor(Color.BLACK);
        g.drawString("N",stringX-1,stringY-1);
        g.drawString("N",stringX-1,stringY+1);
        g.drawString("N",stringX+1,stringY+1);
        g.drawString("N",stringX+1,stringY-1);

        // draw the visible arrow
        g.setColor(color);
        g.fillPolygon(rotatedArrowX, rotatedArrowY, 7);

        // draw a black border around the arrow to offset it from imagery
        g.setColor(Color.BLACK);
        g.drawPolygon(rotatedArrowX, rotatedArrowY, 7);

        // draw the "N" for "north"
        g.setColor(color);
        g.drawString("N",stringX,stringY);
    }
}
