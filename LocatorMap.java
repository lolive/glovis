// LocatorMap.java provides a class for a locator map.
//----------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Observable;
import java.util.Observer;
import java.text.DecimalFormat;
import javax.swing.JPanel;
import javax.swing.JViewport;

public class LocatorMap extends JPanel implements MouseListener, 
    MouseMotionListener, Observer
{
    private imgViewer applet;   // reference to the applet
    private Image markImage;    // image for marking the current location
    private Point loc;          // location of position indicator (in pixels)
    private DecimalFormat LatLonFormat;
    private Image offScreenBuffer = null; // off screen drawing buffer for
                                       // eliminating flicker on some browsers

    // define the options for the locator map
    public static final int GEOGRAPHIC_MAP = 0;
    public static final int US_GEOGRAPHIC_MAP = 1;
    public static final int SINUSOIDAL_MAP = 2;
    public static final int POLARSTEREOGRAPHIC_MAP = 3;
    private static final int NUM_MAPS = 4;

    private LocatorMapImpl currentMap;  // reference to the current locator map
    private LocatorMapImpl maps[];      // array for the locator maps


    // Constructor for the locatorMap
    //-------------------------------
    LocatorMap(imgViewer parent)
    {
        applet = parent;                // Save parent reference
        setBackground(Color.BLACK);     // Set the background black

        // load the location marker image
        markImage = applet.getImage(applet.getCodeBase(), 
                "graphics/ctxmarker.gif");

        // create the locator map objects
        maps = new LocatorMapImpl[NUM_MAPS];

        // default to not showing a map
        currentMap = null;

        loc = new Point(1,1);

        // Set up the formatter for lat/lon output
        LatLonFormat = new DecimalFormat (" 0.00;-0.00");

        // route wanted events to this object
        addMouseListener(this);
        addMouseMotionListener(this);

        // default the size to the geographic image size until one is actually
        // specified
        setSize(300,300);
    }

    // initialize the starting location
    //---------------------------------
    public void initialize(double startLat, double startLong)
    {
        // set the correct map for the current sensor
        setCurrentMap(applet.sensorMenu.getCurrentSensor().locatorMap);

        // initialize the displayed location
        LatLong latLong = new LatLong(startLat, startLong);
        setLoc(latLong);
    }

    // method to return a map's configuration object
    //----------------------------------------------
    public GeographicLocatorMapConfig getMapConfig(int mapNumber)
    {
        LocatorMapImpl map = getMap(mapNumber);
        return map.getMapConfig();
    }

    // method to return the locator map for a given map number.  If the map
    // has not been created yet, it is created.
    //---------------------------------------------------------------------
    private LocatorMapImpl getMap(int mapNumber)
    {
        // load the locator maps only when they are really needed in case
        // the user never looks at a dataset that doesn't need one of them
        if (maps[mapNumber] == null)
        {
            if (mapNumber == SINUSOIDAL_MAP)
                maps[SINUSOIDAL_MAP] = new SinusoidalLocatorMap(applet);
            else if (mapNumber == US_GEOGRAPHIC_MAP)
            {
                maps[US_GEOGRAPHIC_MAP] = new GeographicLocatorMap(applet,
                        new USLocatorMapConfig());
            }
            else if (mapNumber == POLARSTEREOGRAPHIC_MAP)
            {
                maps[POLARSTEREOGRAPHIC_MAP]
                        = new PolarStereographicLocatorMap(applet);
            }
            else
            {
                maps[mapNumber] = new GeographicLocatorMap(applet,
                        new LocatorMapConfig());
            }
        }
        return maps[mapNumber];
    }

    // method to set the currently display map image
    //----------------------------------------------
    private void setCurrentMap(int newMapNumber)
    {
        currentMap = getMap(newMapNumber);

        // if there is an offscreen buffer, black fill it in case the new 
        // map is smaller than the previous map (otherwise a portion of the
        // old map might be visibile if the applet window is dragged to be
        // very large)
        if (offScreenBuffer != null)
        {
            Graphics offg = offScreenBuffer.getGraphics();
            int width = offScreenBuffer.getWidth(null);
            int height = offScreenBuffer.getHeight(null);
            offg.setColor(Color.BLACK);
            offg.fillRect(0, 0, width, height);
            offg.dispose();
        }

        setSize(currentMap.imageSize);
        setPreferredSize(currentMap.imageSize);

        // make sure the parent scrolling area picks up the new size
        applet.locatorMapScroll.doLayout();
    }

    // Method to set the location from a Latitude, Longitude value
    //------------------------------------------------------------
    private void setLoc(LatLong latLong)
    {
        loc = currentMap.latLongToPixel(latLong);
        updateScroll();
    }

    // Method to set the location from a grid col/row
    //-----------------------------------------------
    private void setLoc(int gridCol, int gridRow)
    {
        loc = currentMap.gridToPixel(gridCol, gridRow);
        updateScroll();
    }

    // Method to set the displayed scroll area location to display the 
    // current location
    //----------------------------------------------------------------
    private void updateScroll()
    {
        int xScrollLoc;
        int yScrollLoc;

        // calculate the correct scroll location for the locator map
        Dimension parentSize = applet.locatorMapScroll.getSize();
        int width = applet.locatorMapScroll.getVerticalScrollBar().getWidth();
        int height 
            = applet.locatorMapScroll.getHorizontalScrollBar().getHeight();
        int adjustX = (parentSize.width - width)/2;
        int adjustY = (parentSize.height - height)/2;
        xScrollLoc = loc.x - adjustX;

        if (xScrollLoc < 1) 
            xScrollLoc = 1;
        else if (xScrollLoc > (currentMap.imageSize.width - parentSize.width 
                               + width))
        {
            xScrollLoc = currentMap.imageSize.width - parentSize.width 
                       + width + 3;
        }
        yScrollLoc = loc.y - adjustY;
        if (yScrollLoc < 1) 
            yScrollLoc = 1;
        else if (yScrollLoc > (currentMap.imageSize.height - parentSize.height
                               + height))
        {
            yScrollLoc = currentMap.imageSize.height - parentSize.height 
                       + height + 3;
        }

        // set the new scroll position and force a repaint
        JViewport view = applet.locatorMapScroll.getViewport();
        Point upperleft = new Point(xScrollLoc, yScrollLoc);
        view.setViewPosition(upperleft);
        applet.locatorMapScroll.revalidate();
        repaint();
    }

    // This method is overwritten to reduce flickering on the paint() method...
    //-------------------------------------------------------------------------
    public void update(Graphics g) { paint(g); }

    // Method for the Observer interface
    //----------------------------------
    public void update(Observable ob, Object arg)
    {
        int sensorMapNumber = applet.sensorMenu.getCurrentSensor().locatorMap;
        if ((maps[sensorMapNumber] != currentMap) || (currentMap == null))
            setCurrentMap(sensorMapNumber);

        // update the icon location in the locator map
        MosaicData md = applet.imgArea.md;
        setLoc(md.gridCol,md.gridRow);
    }

    // The paint() method redraws all current images
    //----------------------------------------------
    public void paintComponent(Graphics g) 
    {
        super.paintComponent(g);

        // don't paint anything if the map hasn't been set
        if (currentMap == null)
            return; 

        Dimension mapSize = getSize();

        // make sure the offScreenBuffer is large enough (if it exists)
        // in case the different locator maps are different sizes
        if ((offScreenBuffer != null) && 
            ((offScreenBuffer.getHeight(null) < mapSize.height)
             ||(offScreenBuffer.getWidth(null) < mapSize.width)))
        {
            // not large enough, so eliminate
            offScreenBuffer.flush();
            offScreenBuffer = null;
        }

        // allocate the off screen drawing buffer if not available
        if (offScreenBuffer == null)
        {
            offScreenBuffer = createImage(mapSize.width,mapSize.height);
        }
        
        // get a graphics context for drawing to the off screen buffer
        Graphics offg = offScreenBuffer.getGraphics();
        
        // draw the background image, lines, and location marker
        offg.drawImage(currentMap.mapImage,0,0,this);
        if (currentMap.useBoundaryImage)
        {
            offg.drawImage(currentMap.worldBoundaries, 0, 0, this);
        }
        offg.drawImage(markImage, loc.x-markImage.getWidth(null)/2, 
                       loc.y-markImage.getHeight(null)/2, this);

        // dispose the off screen graphics context to help garbage collection
        offg.dispose();

        // copy the off screen buffer to the display
        g.drawImage(offScreenBuffer,0,0,this);
    }

    // Process Mouse pressed events
    //-----------------------------
    public void mousePressed(MouseEvent event)
    {
        if (currentMap == null)
            return;

        int x = event.getX();
        int y = event.getY();

        currentMap.moveTo(x, y);
    }

    // method to handle the mouse leaving the area
    //--------------------------------------------
    public void mouseExited(MouseEvent event)
    {
        // clear status bar when leaving to clear lat/long
        applet.statusBar.showStatus("");
    }

    // Dummy event handlers required for a MouseListener interface
    public void mouseReleased(MouseEvent event) { }
    public void mouseClicked(MouseEvent event) { }
    public void mouseEntered(MouseEvent event) { }

    // Display the lat/long and grid location under the mouse cursor
    //--------------------------------------------------------------
    public void mouseMoved(MouseEvent event)
    {
        if (currentMap == null)
            return;

        int x = event.getX();
        int y = event.getY();
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        NavigationModel nm = currSensor.navModel;

        // get the current lat/long and grid location under the mouse cursor
        LatLong latLong = currentMap.pixelToLatLong(x, y);
        Point gridLoc = currentMap.pixelToGrid(x,y);

        // build the location message using a string buffer for efficiency
        StringBuffer msg = new StringBuffer();
        // add the lat/long to the message if it is available
        if (latLong != null)
        {
            msg.append("Lat/Long: ");
            msg.append(LatLonFormat.format(latLong.latitude));
            msg.append(", ");
            msg.append(LatLonFormat.format(latLong.longitude));
            msg.append(" degrees");
        }

        // add the grid location to the message if grid entry is supported
        if (!currSensor.hideGridEntry)
        {
            msg.append(", ");
            msg.append(nm.getModelName());
            msg.append(" ");
            msg.append(nm.getColName());
            msg.append("/");
            msg.append(nm.getRowName());
            msg.append("  ");  
            msg.append(nm.getColumnString(gridLoc.x));
            msg.append(", ");
            msg.append(nm.getRowString(gridLoc.y));
        }

        // display the message in the status bar
        applet.statusBar.showStatus(msg.toString());
    }

    // Dummy event handler required for a MouseMotionListener interface
    public void mouseDragged(MouseEvent event) { }

    public void cleanup()
    {
        if (offScreenBuffer != null)
        {
            for (int i = 0; i < maps.length; i++)
            {
                if (maps[i] != null)
                    maps[i].cleanup();
            }
            offScreenBuffer.flush();
            offScreenBuffer = null;
        }
    }
}
