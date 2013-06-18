// ImagePane object -- displays mosaicked imagery on-the-fly
//----------------------------------------------------------
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JOptionPane;

public class ImagePane extends JPanel implements KeyListener, MouseListener,
    ActionListener, MouseMotionListener, Observer
{
    imgViewer applet;   // reference to the applet
    MosaicData md;      // mosaic data

    private int[] tempX;        // temporary x coordinate array
    private int[] tempY;        // temporary y coordinate array

    private DecimalFormat LatLonFormat;   // lat/long formatter

    private Image offScreenBuffer = null; // off screen buffer for eliminating
                                          // flicker when drawing the display
    private Image downloadIcon; // download icon shown in upper left of display
    private int osbHeight = -1;    // height of off screen buffer
    private int osbWidth = -1;     // width of off screen buffer
    private Dimension preferredSize; // preferred size of the ImagePane.  It
                                     // is set to the size needed to fit 
                                     // the displayed data in
    private Point offsetToCenterDisplay; // the offset needed to center the
                                   // image data in the display when the area
                                   // is larger than needed

    MapLayers mapLayers;           // reference to the map layers container
    private Point dragMouseLoc = null; // mouse location when the user started
                                   // dragging the display
    private Point dragScrollLoc = null; // scroll area location when the user
                                   // started dragging the display
    private Point dragOffset = null; // offset of the display when dragging the 
                                     // display area around (in pixels)
    public LatLong savedRightClickLoc; // Save the right click lat/long 
    private boolean dragActive = false; // flag to track when a legal drag is
                                // underway.  This shouldn't be needed but some
                                // versions of Netscape mistakenly send drag 
                                // events without the mouse button ever being 
                                // pressed in the canvas.  This seems to happen
                                // with Netscape 4.78 and 6.2 when a goto
                                // date dropdown is dropped down and the mouse
                                // is clicked in the canvas.
    public Cursor currentCursor;   // current cursor shown

    // savedWidth and savedHeight are used to save the height and width of the
    // display area.  These are used to detect when the Canvas setSize method 
    // needs to be called to re-layout the image to possibly add or remove 
    // scrollbars.  This should not be needed, but Netscape 6.2 has a bug that
    // causes display flicker everytime the setSize method is called.  So to
    // avoid that bug, only set the size when it really changes.
    private int savedWidth = 0;
    private int savedHeight = 0;

    private boolean modeChanged;// flag to indicate the display mode changed
    LogoImage logo;             // Data cooperator's logo object
    Rectangle logoLoc;          // current location of the logo on the display
    Rectangle downloadLoc;      // current location of the download icon on 
                                // the display
    SceneMenu sceneMenu;        // right-click popup menu for scene options

    private Timer refreshTimer; // timer for refreshing the display while
                                // data is loading
    private Color swathHighlightColor = new Color(174,255,51); // make the 
                              // swath highlight a lime green

    // Constructor for the ImagePane.  Allocates array space and sets variables.
    // -------------------------------------------------------------------------
    ImagePane(imgViewer parent, LocatorMap locatorMap)
    {
        applet = parent;             // Save ptr to parent
        setBackground(Color.BLACK);  // Set the background black

        // hack to make sure imgArea is set to this component before creating
        // the MosaicData object
        applet.imgArea = this;

        // create the mosaic data class
        md = new MosaicData(applet, this, locatorMap);

        // cache a map layers reference
        mapLayers = md.mapLayers;

        tempX = new int[4];           // temporary x coordinates
        tempY = new int[4];           // temporary y coordinates

        // Set up formatter for lat/lon output
        LatLonFormat = new DecimalFormat (" 0.000000;-0.000000");

        // register for the events handled by this component
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        currentCursor = applet.crosshairCursor;
        setCursor(currentCursor);

        dragOffset = new Point(0,0);
        offsetToCenterDisplay = new Point(0,0);

        // create the data cooperator's logo object
        logo = new LogoImage(applet);


        MediaTracker mt = new MediaTracker(applet);
        // get the icon to be drawn in the upper left corner
        downloadIcon = applet.getImage(applet.getCodeBase(), 
                               "graphics/downloadIcon.gif");
        mt.addImage(downloadIcon,0);
       
        // wait for the Icon to completely load so the size of the 
        // downloadIcon will be known for positioning it on the display
        try
        {
            mt.waitForAll();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        
        if (mt.isErrorID(0))
        {
            // issue an error message to the console that the image load
            // failed (result will be no logo displayed)
            System.out.println("Error loading graphics/" 
                               + downloadIcon);
        }
        
        // create the popup scene menu
        sceneMenu = new SceneMenu(applet,md);

        // create the refresh timer to run every second
        refreshTimer = new Timer(1000, this);

        // set the size of the canvas so that it gets repaint messages.  This
        // is done to allow the applet to start up properly while the images
        // are loading.  Since the loading threads notify the GUI thread of
        // completion by posting a repaint message, the canvas needs to have a
        // size set before repaint messages are delivered.
        super.setSize(300,300);

    } // end of constructor

    // method to return the preferred size of the ImagePane
    //-----------------------------------------------------
    public Dimension getPreferredSize()
    {
        if (preferredSize != null)
            return new Dimension(preferredSize);
        else
            return new Dimension(300,300);
    }
    
    // method to return the center display offset
    //-------------------------------------------
    public Point getOffsetToCenterDisplay()
    {
        return new Point(offsetToCenterDisplay);
    }

    // method to set the size of this component
    //-----------------------------------------
    public void setSize(Dimension size)
    {
        // get the parent's minimum size (scroll area)
        Dimension minSize = getParent().getSize();

        // use the parent's size as a minimum for the size
        int newWidth = minSize.width;
        int newHeight = minSize.height;

        // make sure the width is at least as large as the parent expects
        newWidth = Math.max (size.width, newWidth);
        newHeight = Math.max (size.height, newHeight);

        // if the new dimensions are larger than the needed size, offset
        // the display to center it in the available area
        Dimension ps = getPreferredSize();

        if (newWidth > ps.width)
            offsetToCenterDisplay.x = (newWidth - ps.width)/2;
        else
            offsetToCenterDisplay.x = 0;
        if (newHeight > ps.height)
            offsetToCenterDisplay.y = (newHeight - ps.height)/2;
        else
            offsetToCenterDisplay.y = 0;

        // if the mode changed, set the scroll position to 0,0 to avoid 
        // annoying popup messages on Netscape on Unix
        if (modeChanged)
            applet.imgScroll.getViewport().setViewPosition(new Point(0,0));
            
        // adjust the size of the canvas (only if it has changed to avoid
        // a Netscape 6.2 bug from causing display flicker)
        // TBD - is this still needed with the ScrollArea widget?
        if ((newWidth != savedWidth) || (newHeight != savedHeight))
        {
            super.setSize(newWidth,newHeight);
            super.setPreferredSize(new Dimension(newWidth,newHeight));
            savedWidth = newWidth;
            savedHeight = newHeight;
            // make sure the map layers are clipped to the new size
            mapLayers.clip();
        }

        // cause the parent container to resize its child to make sure the 
        // scroll bars are added if needed
        getParent().doLayout();

        // if the display mode changed, center the display
        if (modeChanged)
        {
            int x = 0;
            int y = 0;
            if ((newWidth > minSize.width) || (newHeight > minSize.height))
            {
                // calculate the difference in size between the full res image
                // and the display area
                x = newWidth - minSize.width;
                y = newHeight - minSize.height;

                // divide the difference by 2 to leave half of the overflow at 
                // either edge
                x /= 2;
                y /= 2;

                // if the difference is negative, the image is smaller than the 
                // display area, so default it to the top left corner
                if (x < 0)
                    x = 0;
                if (y < 0)
                    y = 0;

            }

            applet.imgScroll.getViewport().setViewPosition(new Point(x,y));
        }

        // calculate the visible area in projection coordinates
        Point ulMeters = md.mosaicCoords.getUpperLeftCorner();
        // protect against an invalid ulMeters (eventhough it shouldn't happen)
        if (ulMeters == null)
            return;

        double pixelSize = md.actualPixelSize;

        // adjust the coordinates for the display centering
        ulMeters.x -= (int)Math.round(offsetToCenterDisplay.x * pixelSize);
        ulMeters.y += (int)Math.round(offsetToCenterDisplay.y * pixelSize);

        // calculate the lower right corner
        Point lrMeters = new Point(
                ulMeters.x + (int)Math.round(savedWidth * pixelSize),
                ulMeters.y - (int)Math.round(savedHeight * pixelSize));

        // load the map layers for the displayed area
        mapLayers.load(ulMeters, lrMeters, md.getProjection(), 
                       md.getProjectionCode());
    }

    // Method to return the corners of the image display area in projection
    // coordinates
    //---------------------------------------------------------------------
    public Rectangle2D.Float getDisplayAreaRectangle()
    {
        Point ulMeters = md.mosaicCoords.getUpperLeftCorner();
        double pixelSize = md.actualPixelSize;
        float widthMeters = Math.round(savedWidth * pixelSize);
        float heightMeters = Math.round(savedHeight * pixelSize);

        Rectangle2D.Float rect = new Rectangle2D.Float(ulMeters.x, ulMeters.y,
                                    widthMeters, heightMeters);
        return rect;
    }

    // This method is overridden to reduce flickering on the paint() method...
    //-------------------------------------------------------------------------
    public void update(Graphics g) { paint(g); }

    // Method for the Observer interface
    //----------------------------------
    public void update(Observable ob, Object arg)
    {
        if (md.imageLoader.isBusy())
            refreshTimer.start();

        // if the TOCs are loading, don't do anything here to prevent the
        // display from flickering by changing sizes after an image load
        // completes
        if (md.isUnstableTOC())
        {
            refreshTimer.start();
            return; 
        }

        modeChanged = false;

        if (arg == MosaicData.DISPLAY_MODE_CHANGED)
            modeChanged = true;

        clear();
        preferredSize = new Dimension(md.getDisplaySize());
        setSize(preferredSize);

        // adjust the scrollbar location if told to through the update arg
        if (arg == md.targetXY)
        {
            // calculate the scroll position based on the difference between 
            // the upper left corner and the target X/Y
            Point ul = md.mosaicCoords.getUpperLeftCorner();
            double pixelSize = md.actualPixelSize;
            int x = (int)Math.round((md.targetXY.x - ul.x)/pixelSize);
            int y = (int)Math.round((ul.y - md.targetXY.y)/pixelSize);

            // limit the scrolling amount to the available distance
            Dimension minSize = getParent().getMinimumSize();
            int xspan = savedWidth - minSize.width;
            int yspan = savedHeight - minSize.height;
            if (x < 0)
                x = 0;
            else if (x > xspan)
                x = xspan;
            if (y < 0)
                y = 0;
            else if (y > yspan)
                y = yspan;

            // set the scroll position
            applet.imgScroll.getViewport().setViewPosition(new Point(x,y));
        }

        modeChanged = false;

        // Note: sometimes setSize forces a repaint, but not always.  So until
        // a way is found to disable repaints when setting the size, we
        // sometimes get a double repaint
        repaint();
    }

    // Method to clear the imgArea
    // ---------------------------
    public void clear()
    {
        // only clear the display if the display mode has changed to prevent
        // display flicker when clearing is not really needed.  A better way 
        // to do this would be to not let the display update when 
        // scroll position is changed, but so far a way hasn't been found for
        // that.
        if (modeChanged)
        {
/* TBD - this doesn't appear to be needed anymore.  Commenting it out for now
   to make sure no bad effects happen without it.
            Graphics g = getGraphics();
            g.setColor(getBackground());
            g.fillRect(0,0,getSize().width, getSize().height);
*/
        }
    }

    // Method to draw the scene and swath highlight
    //---------------------------------------------
    private void drawSceneHighlight(Graphics g, Metadata scene, 
                                    boolean swathHighlight, 
                                    boolean drawSwathTop, 
                                    boolean drawSwathBottom)
    {
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        TOC cell = md.getCurrentCell();

        // do not highlight anything if the current cell isn't valid
        if (!cell.valid)
            return;

        // cache the screen location reference
        Polygon poly = scene.screenLocation;

        // calculate the rotation of the image
        double angle = Math.atan2(poly.ypoints[1] - poly.ypoints[0], 
                                  poly.xpoints[1] - poly.xpoints[0]);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        // calculate the corner locations for the highlight, factoring
        // in the rotation of the image so the edges of the areas where
        // there is rotation look good.  Note the dragOffset is factored
        // into the highlight location since it is defined in screen
        // coordinates.
        for (int i = 0; i < 4; i++)
        {
            int x = currSensor.borderX[i];
            int y = currSensor.borderY[i];

            // if drawing a swath highlight, adjust the border size
            if (swathHighlight)
            {
                // make the swath border twice as wide as the normal active
                // scene highlight
                x *= 2;

                // determine how to draw the top/bottom of the swath highlight 
                if (drawSwathTop && (i <= 1))
                {
                    // a border on top is needed and this is a coordinate at
                    // the top of the scene, so double the height of the border
                    y *= 2;
                }
                else if (drawSwathBottom && (i >= 2))
                {
                    // a border on bottom is needed and this is a coordinate at
                    // the bottom of the scene, so double the height of the
                    // border
                    y *= 2;
                }
                else
                {
                    // no top or bottom border is needed so actually make the 
                    // top/bottom highlight smaller to make sure no highlight
                    // is visible at the top/bottom of the scene
                    if (y > 0)
                        y = -1;
                    else
                        y = 1;
                }
            }
            tempX[i] = poly.xpoints[i] + (int)Math.round(x * cos - y * sin)
                        - dragOffset.x;
            tempY[i] = poly.ypoints[i] + (int)Math.round(x * sin + y * cos)
                        + dragOffset.y;
        }

        // set the color for the highlight being drawn.  Note that the active
        // scene highlight is unique for the different sensors since they vary
        // in brightness
        if (swathHighlight)
            g.setColor(swathHighlightColor);
        else
            g.setColor(currSensor.getBorderColor(scene));

        // draw a filled polygon for the highlight
        g.fillPolygon(tempX,tempY,4);

        // since the process to create the browse sometimes results in 
        // transparent pixels sometimes getting created in the middle of an
        // image, make the problem a little less noticeable by drawing
        // a black polygon in the middle of the highlight polygon to eliminate 
        // the highlight "shine-through" (Note this should be fixed some day
        // in the tool used to create the gif files).  Only do it for the 
        // sensors that need it.
        if (currSensor.useHighlightTransparentPixelFix)
        {
            for (int i = 0; i < 4; i++)
            {
                int x = currSensor.borderX[i] > 0 ? -1 : 1;
                int y = currSensor.borderY[i] > 0 ? -1 : 1;
                tempX[i] = poly.xpoints[i] + (int)Math.round(x * cos - y * sin)
                    - dragOffset.x;
                tempY[i] = poly.ypoints[i] + (int)Math.round(x * sin + y * cos)
                    + dragOffset.y;
            }
            g.setColor(Color.BLACK);
            g.fillPolygon(tempX,tempY,4);
        }
    }

    // helper method to determine whether a swath border should be drawn
    // above or below the current scene
    //------------------------------------------------------------------
    private boolean checkSwathTopOrBottom(Metadata currScene, 
                                          Metadata testScene,
                                          boolean checkingBottom)
    {
        boolean drawBorder = false;

        // located the scene in the swath list, so now determine if there is a
        // scene right above or below this scene that is also in the swath
        if ((testScene == null) || !testScene.visible)
        {
            // the test scene is not present or visible, so the border is
            // needed
            drawBorder = true;
        }
        else
        {
            // if the previous scene in the swath does not
            // overlap this one, a top border is needed
            int diff;
            if (checkingBottom)
            {
                diff = currScene.screenLocation.ypoints[3]
                     - testScene.screenLocation.ypoints[0];
            }
            else
            {
                diff = testScene.screenLocation.ypoints[3]
                     - currScene.screenLocation.ypoints[0];
            }
            if (diff < -2)
                drawBorder = true;
        }
        return drawBorder;
    }

    // Paint a the mosaic
    //-------------------
    private void paintMosaic(Graphics g)
    {
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        Point ul = getUpperLeftCorner();
        // if the upper left isn't valid, there isn't anything to paint
        if (ul == null)
            return;
        int imgProjUL_X = ul.x;
        int imgProjUL_Y = ul.y;

        int pixSize = md.pixelSize;
        int cellsToDisplay = currSensor.getNumCellsAtResolution(pixSize);
        double actualPixSize = md.actualPixelSize;
        ZOrderList zOrder = md.getZOrder();

        // set the color for the coverage map polygons
        Color coverageMapColor = new Color(204,255,255);

        // paint the scenes that have been loaded from the server from
        // the bottom of the Z-order to the top
        Metadata scene;
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        // determine whether the swath highlight may need to be drawn
        boolean drawSwathHighlight 
            = currSensor.hasSwathMode && applet.toolsMenu.isSwathModeEnabled();
        Vector swathScenes = null;  // scenes in the current highlighted swath
        int swathDate = 0;          // date of swath scenes
        Sensor swathSensor = null;

        // if the swath mode highlight is possible, get the data required to
        // identify the scenes in the swath
        if (drawSwathHighlight)
        {
            // get the currently selected scene since it is part of the 
            // highlighted swath
            Metadata currScene = md.getCurrentScene();
            if (currScene != null)
            {
                // get the swath that contains the current scene
                swathScenes = md.getScenesInSwath(currScene);
                swathDate = currScene.date;
                swathSensor = currScene.getSensor();
            }
            // if no swathScenes object was obtained, or there are no scenes in
            // the swath, clear the flag to draw the swath highlight
            if ((swathScenes == null) || (swathScenes.size() == 0))
                drawSwathHighlight = false;
        }

        while ((scene = zOrder.up()) != null)
        {
            if (!md.canDisplay(scene))
                continue;

            boolean onTopScene = zOrder.isTop();
            boolean swathHighlight = false;

            // if a swath highlight is to be drawn, determine whether the 
            // current scene is in the active swath and whether the scene's
            // highlight should have the border on the top and/or bottom
            boolean drawSwathTop = false;
            boolean drawSwathBottom = false;
            if (drawSwathHighlight)
            {
                // only scenes on the same date as the swath and from the
                // same sensor as the swath (to cover Landsat combined cases)
                // need the swath highlight
                if ((scene.date == swathDate) 
                    && (swathSensor == scene.getSensor()))
                {
                    swathHighlight = true;

                    // determine whether the top and bottom of the swath 
                    // highlight for this scene should be drawn
                    Metadata prevScene = null;
                    Metadata currScene = null;
                    for (int i = 0; i < swathScenes.size(); i++)
                    {
                        currScene = (Metadata)swathScenes.elementAt(i);
                        if (currScene == scene)
                        {
                            // located the scene in the swath list, so now 
                            // check if the top swath border needs to be drawn
                            // for this scene
                            drawSwathTop = checkSwathTopOrBottom(currScene,
                                                    prevScene, false);

                            // find the next scene in the swath
                            Metadata nextScene = null;
                            if (i < (swathScenes.size() - 1))
                            {
                                nextScene 
                                    = (Metadata)swathScenes.elementAt(i + 1);
                            }

                            // check if the bottom swath border needs to be
                            // drawn for this scene
                            drawSwathBottom = checkSwathTopOrBottom(currScene,
                                                    nextScene, true);

                            break;
                        }
                        prevScene = currScene;
                    }
                }
            }

            // draw the image if it is available
            if ((scene.image != null) && (scene.imageRes == pixSize))
            {
                // draw the scene or swath highlight as needed
                if ((onTopScene || swathHighlight) && md.canShowHighlight())
                {
                    // translate the drawing coordinates to adjust for the 
                    // centering offset since the highlight is drawn in 
                    // screen coordinates and draw the highlight
                    g.translate(offsetToCenterDisplay.x,
                                offsetToCenterDisplay.y);
                    if (swathHighlight)
                    {
                        drawSceneHighlight(g, scene, true, drawSwathTop, 
                                           drawSwathBottom);
                    }
                    if (onTopScene)
                        drawSceneHighlight(g, scene, false, false, false);
                    g.translate(-offsetToCenterDisplay.x,
                                -offsetToCenterDisplay.y);
                }

                // get the scene's location and draw the image
                int xi = (int)Math.round((scene.ulX - imgProjUL_X)
                                         / actualPixSize);
                int yi = (int)Math.round((imgProjUL_Y - scene.ulY)
                                         / actualPixSize);
                g.drawImage (scene.image,xi,yi,this);
            }
            else if (cellsToDisplay != Sensor.SINGLE_SCENE)
            {
                // translate the drawing coordinates to adjust for the
                // centering offset since everything drawn in this 
                // block is in screen coordinates
                g.translate(offsetToCenterDisplay.x,offsetToCenterDisplay.y);

                // draw the highlight for the scene on top
                if ((onTopScene || swathHighlight) && md.canShowHighlight())
                {
                    if (swathHighlight)
                    {
                        drawSceneHighlight(g, scene, true, drawSwathTop, 
                                           drawSwathBottom);
                    }
                    if (onTopScene)
                        drawSceneHighlight(g, scene, false, false, false);
                }

                // make sure the color is the coverage map color
                g.setColor(coverageMapColor);

                // translate the screenLocation to include any drag value
                if (dragActive)
                {
                    int[] xp = scene.screenLocation.xpoints;
                    int[] yp = scene.screenLocation.ypoints;
                    for (int i = 0; i < 4; i++)
                    {
                        xPoints[i] = xp[i] - dragOffset.x;
                        yPoints[i] = yp[i] + dragOffset.y;
                    }
                    // draw a coverage map rectangle since the image is not
                    // available
                    g.fillPolygon(xPoints,yPoints,4);
                }
                else
                    g.fillPolygon(scene.screenLocation);

                g.translate(-offsetToCenterDisplay.x,-offsetToCenterDisplay.y);
            }
        }
    }

    // handle the timer events for refreshing the display
    //---------------------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        repaint();

        if (!md.isBusy() && !md.imageLoader.isBusy())
            refreshTimer.stop();
    }

    // The paint() method redraws all current images
    //----------------------------------------------
    public void paintComponent (Graphics g) 
    {
        Dimension size = applet.imgScroll.getViewport().getSize();

        // if the refresh timer is running and there isn't a reason for it
        // to be running anymore, stop it (this prevents getting extra repaint
        // messages that aren't needed)
        if (refreshTimer.isRunning() && !md.isBusy() 
            && !md.imageLoader.isBusy())
        {
            refreshTimer.stop();
        }

        // get the size of the canvas
        Dimension canvasSize = getSize();

        // set the canvas size to the maximum of the scrolling area and the
        // ImagePane size.  Note this is to make sure the canvas is large
        // enough to hold the logo if the scroll area is larger than needed.
        Dimension scrollSize = getParent().getSize();
        canvasSize.width = Math.max(canvasSize.width,scrollSize.width);
        canvasSize.height = Math.max(canvasSize.height,scrollSize.height);

        // create the off screen (or back) buffer if one hasn't been 
        // created before, or the canvas changed to a larger size
        if ((offScreenBuffer == null) || (osbWidth < canvasSize.width) 
             || (osbHeight < canvasSize.height))
        {
            // if the off screen buffer has already been allocated, flush
            // it so that Netscape will be sure to release the memory it 
            // uses
            if (offScreenBuffer != null)
                offScreenBuffer.flush();

            // establish the size of the off screen buffer.  Use the max of
            // the old size and the current needed size (+20 for some growth
            // room) to make sure it never shrinks since if was needed a 
            // certain size once, it will likely be needed again
            osbWidth = Math.max(canvasSize.width + 20, osbWidth);
            osbHeight = Math.max(canvasSize.height + 20, osbHeight);
            offScreenBuffer = createImage(osbWidth,osbHeight);
        }

        // check whether the mosaic data object is busy.  If it is, check
        // to see whether the load may have just completed.  This is how
        // the loading thread notifies the rest of the applet that an image
        // load completed.
        if (md.isBusy())
            md.checkForCompletedLoad();

        if (offScreenBuffer != null)
        {
            // get a graphics context for the off screen image
            Graphics offg = offScreenBuffer.getGraphics();

            // clear the off screen buffer
            offg.setColor(Color.BLACK);
            offg.fillRect(0,0,osbWidth,osbHeight);
            
            // paint the scenes covering the mosaic
            paintMosaic(offg);

            // draw the map layers in the off screen buffer
            mapLayers.paint(offg);

            // draw user defined area if sensor supports it
            if (applet.sensorMenu.getCurrentSensor().hasUserDefinedArea)
            {
                applet.userDefinedAreaDialog.getUserDefinedArea()
                    .drawArea(offg);
            }

            // get the position of the scroll area
            Point scrollPos = applet.imgScroll.getViewport().getViewPosition();

            // draw download icon if scene is downloadable
            Metadata currScene = md.getCurrentScene();
            if ((currScene != null) && currScene.isDownloadable)
            {
                int downloadWidth = downloadIcon.getWidth(null);
                int downloadHeight = downloadIcon.getHeight(null);
                int y = 0;
                int x = 0;
                x += scrollPos.x;
                y += scrollPos.y;
                
                // draw download image
                offg.setColor(new Color(255,255,255,210));
                offg.fillRect(x,y,downloadWidth,downloadHeight);
                downloadLoc = new Rectangle(x,y,downloadWidth,downloadHeight);
                offg.drawImage(downloadIcon,x,y,this);
            }
            else
                downloadLoc = null;
            
            // calculate the location to draw the logo
            Image logoImage = logo.getLogo();
            int logoWidth = logoImage.getWidth(null);
            int logoHeight = logoImage.getHeight(null);
            int x;
            int y;
            if (logo.getLocation() == Sensor.LOGO_LOWER_RIGHT)
            {
                x = size.width - logoWidth;
                y = size.height - logoHeight;
            }
            else
            {
                x = 0;
                y = size.height - logoHeight;
            }

            // remember the logo location in screen coordinates for the 
            // test to see if it has been clicked
            x += scrollPos.x;
            y += scrollPos.y;

            logoLoc = new Rectangle(x,y,logoWidth,logoHeight);

            // adjust the drawing location for the current scroll position
            // so the logo is drawn in the buffer in the correct location
            offg.drawImage(logoImage,x,y,this);

            // check again for the load being complete, just to make sure it
            // is handled in a timely fashion
            if (md.isBusy())
                md.checkForCompletedLoad();

            // dispose the extra off screen graphics context to help 
            // the java garbage collection
            offg.dispose();

            // copy the off screen buffer to the display
            g.drawImage(offScreenBuffer,0,0,this);
        }
    }

    // Handle Key events
    //------------------
    public void keyPressed(KeyEvent event)
    {
        int key = event.getKeyCode();

        switch (key) 
        {
            case KeyEvent.VK_RIGHT:
                // scroll right
                md.scrollInDirection(1,0,0,0);
                break;

            case KeyEvent.VK_LEFT:
                // scroll left
                md.scrollInDirection(0,1,0,0);
                break;

            case KeyEvent.VK_UP:
                // scroll up
                md.scrollInDirection(0,0,1,0);
                break;

            case KeyEvent.VK_DOWN:
                // scroll up
                md.scrollInDirection(0,0,0,1);
                break;

            case KeyEvent.VK_PAGE_UP:
                // move to the previous date
                md.sceneFilter.prevDate();
                break;

            case KeyEvent.VK_PAGE_DOWN:
                // move to the next date
                md.sceneFilter.nextDate();
                break;

            case KeyEvent.VK_HOME:
                // move to the first date
                md.sceneFilter.gotoFirstDate();
                break;
            case KeyEvent.VK_END:
                // move to the last date
                md.sceneFilter.gotoLastDate();
                break;
        }
    }

    // Dummy handlers for KeyListener that we don't want
    public void keyTyped(KeyEvent event)
    {
    }
    public void keyReleased(KeyEvent e)
    {
    }

    // Handle mouseDown events
    //------------------------
    public void mousePressed(MouseEvent event)
    {
        if (applet.userDefinedAreaDialog.isVisible())
        {
            // User defined area dialog box is showing, so find what location
            // was clicked to see if the user intends to move a point
            int x = event.getX();
            int y = event.getY();
            Point loc = getXYOnScreen(x,y);
            applet.userDefinedAreaDialog.getUserDefinedArea().mousePressed(loc);
        }
        else
        {
            Sensor currSensor = applet.sensorMenu.getCurrentSensor();
            // user defined area isn't visible, so handle the mouse pressed
            // event normally
            int screenX = event.getX();
            int screenY = event.getY();

            // save the right click projection coordinates
            savedRightClickLoc = getLatLongOnScreen(screenX,screenY);
            
            // remember items needed to track the mouse when it is used to drag
            // the image area around
            dragMouseLoc = event.getPoint();
            dragScrollLoc = applet.imgScroll.getViewport().getViewPosition();

            // reset the drag to zero (should be already anyway)
            dragOffset.x = 0;
            dragOffset.y = 0;

            // make sure this widget has the focus so that the keyboard can
            // be used to navigate after it is selected
            this.requestFocusInWindow();

            // if the logo was clicked on, handle it
            if (logoLoc.contains(screenX,screenY))
            {
                logo.clicked();
                return;
            }

            // if the download Icon was clicked on, handle it by popping up
            // an informational box
            if ((downloadLoc != null) && downloadLoc.contains(screenX,screenY)
              && (currSensor.isDownloadable || currSensor.mightBeDownloadable))
            {
                List<String> message = new ArrayList<String>();
                message.add( "Images that display the 'Downloadable'"
                    + " label include a Level 1");
                message.add("product that may be immediately downloaded"
                    + " at no charge.");
                message.add("Scenes that do not display this label may"
                    + " have other products");
                message.add("available for immediate download.");
                if (currSensor.isOrderable)
                {
                    message.add("If you would like the Level 1 product, "
                        + "you may submit a");
                    message.add("processing request.");
                }
                message.add("Add the scenes to your Scene List and click the"
                    + " Send to Cart");
                message.add("button to download any available products.");

                // popup a message box with a download message
                JOptionPane.showMessageDialog(applet.getDialogContainer(),
                            message.toArray(),"Download Information", 
                            JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // don't allow any further action if the TOCs are loading
            if (md.isUnstableTOC())
                return;

            // update the map layer attribute window if needed
            mapLayers.findFeatureName(screenX, screenY, true);

            // assume dragging is becoming active
            dragActive = true;

            // remove the display centering offsets since the scene locations
            // are in screen coordinates without any image pane local offsets
            int scrollX = screenX - offsetToCenterDisplay.x;
            int scrollY = screenY - offsetToCenterDisplay.y;

            Metadata scene = md.findSceneAt(scrollX,scrollY);

            // if no scene at the location, nothing to do
            if (scene == null)
                return;

            switch (event.getClickCount()) 
            {
                case 1: 
                    int modifiers = event.getModifiers();

                    // determine if the click combination is one to lower a 
                    // scene (button 1 with alt/ctrl/shift/meta held down)
                    // Note that this does not check if the event modifiers have
                    // the BUTTON1_MASK set since on Netscape 4.7 on linux 
                    // no bits are set for button 1.
                    boolean lowerClick = 
                        ((modifiers & (InputEvent.BUTTON2_MASK | 
                          InputEvent.BUTTON3_MASK)) == 0) &&
                        ((modifiers & (InputEvent.ALT_MASK | 
                          InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK | 
                          InputEvent.META_MASK)) != 0);

                    // if the mouse button pressed was not the normal button 1, 
                    // lower the selected scene.  Otherwise select the scene.
                    if (lowerClick)
                    {
                        md.lowerScene(scene);
                    }
                    else if ((modifiers & (InputEvent.BUTTON2_MASK | 
                             InputEvent.BUTTON3_MASK)) != 0)
                    {
                        // bring up the popup scene menu, so drag can't 
                        // be active
                        dragActive = false;

                        // for full mosaic sensors, find all the scenes that
                        // intersect the point clicked on
                        Vector intersectingScenes = null;
                        if (currSensor.isFullMosaic)
                        intersectingScenes = md.findScenesAt(scrollX,scrollY);

                        // tell sceneMenu when in single scene mode
                        boolean displayingSingleScene = false;
                        if (currSensor.getNumCellsAtResolution(md.pixelSize) 
                                == Sensor.SINGLE_SCENE)
                        {
                            displayingSingleScene = true;
                        }
                    
                        // show the next/prev scene items for non-full mosaic
                        // sensors
                        boolean showNextPrev = true;
                        if (currSensor.isFullMosaic)
                            showNextPrev = false;
                   
                        // show the setSceneTo item for sensors that support it
                        boolean showSetScenesTo = true;

                        // show the Default Scene item for sensors that 
                        // support it
                        boolean showDefaultScene = true;
                        
                        if (currSensor.isFullMosaic)
                        {
                            showSetScenesTo = false;
                            showDefaultScene = false;
                        }
                        
                        // configure the scene menu for the scene clicked on
                        sceneMenu.configureMenu(scene,intersectingScenes,
                                                displayingSingleScene,
                                                showNextPrev,showSetScenesTo,
                                                showDefaultScene);

                        // show the menu where the mouse was clicked relative
                        // to this widget
                        sceneMenu.show(this,screenX,screenY);
                    }
                    else
                        md.setSelectedScene(scene);
                    break;

                case 2: 
                    // double-click the left mouse to advance in dates
                    if ((event.getModifiers() & InputEvent.BUTTON1_MASK)
                         == InputEvent.BUTTON1_MASK)
                        md.sceneFilter.nextDate();
                    break;
            }
        }
    }

    // mouse button released event
    //----------------------------
    public void mouseReleased(MouseEvent event)
    {
        // if user defined area dialog box is showing, see if polygon 
        // adjustments need to be made
        if (applet.userDefinedAreaDialog.isVisible())
        {
            // get the XY of the release point
            int x = event.getX();
            int y = event.getY();
            Point loc = getXYOnScreen(x,y);

            applet.userDefinedAreaDialog.getUserDefinedArea().mouseRelease(loc);
        }
        else
        {
            // only process the released event if a drag is active
            if (!dragActive)
            return;
            dragActive = false;

            // make sure the cursor is returned to the normal one if the applet
            // isn't busy (i.e. displaying wait cursor)
            currentCursor = applet.crosshairCursor;
            if (!applet.showingWait)
                setCursor(currentCursor);

            // assume a redraw is going to be needed after the mouse button 
            // is released since it likely was dragged to a new location
            boolean needRedraw = true;

            // if no drag offset, no need to redraw anything
            if ((dragOffset.x == 0) && (dragOffset.y == 0))
                needRedraw = false;

            // re-center the display if it has moved significantly.  The 
            // test for ten pixels is to prevent the areas near the poles being
            // recentered by just selecting a new scene.
            if ((Math.abs(dragOffset.x) > 10) || (Math.abs(dragOffset.y) > 10))
            {
                // get the upper left corner and recenter the display if the 
                // coordinate is valid
                Point ul = getUpperLeftCorner();
                if (ul != null)
                {
                    double pixelSize = md.actualPixelSize;

                    // adjust the upper left corner to account for the 
                    // scrollbars
                    Point scrollPos 
                            = applet.imgScroll.getViewport().getViewPosition();
                    ul.x += (int)Math.round(scrollPos.x * pixelSize);
                    ul.y -= (int)Math.round(scrollPos.y * pixelSize);

                    // adjust the coordinates for the display centering offset
                    ul.x += (int)Math.round(offsetToCenterDisplay.x *
                                                                    pixelSize);
                    ul.y -= (int)Math.round(offsetToCenterDisplay.y *
                                                                    pixelSize);

                    // clear the offset in case anything calls 
                    // getUpperLeftCorner since it shouldn't include the 
                    // drag anymore
                    dragOffset.x = 0;
                    dragOffset.y = 0;

                    // goto the new x/y location.  The gotoXY method returns 
                    // true if new data was loaded.  If new data wasn't 
                    // loaded, a redraw is needed to fix display.
                    needRedraw = !md.gotoXY(ul.x,ul.y);
                }
            }

            // make sure the drag is cleared out
            dragOffset.x = 0;
            dragOffset.y = 0;

            // if redrawing is needed, make sure the map layers are properly
            // referenced and send the repaint message
            if (needRedraw)
            {
                mapLayers.clip();
                repaint();
            }
        }
    }

    // Dummy event handlers required for a MouseListener that we don't need
    public void mouseClicked(MouseEvent event)
    {
    }
    public void mouseEntered(MouseEvent event)
    {
    }

    // method to handle the mouse leaving the area
    //--------------------------------------------
    public void mouseExited(MouseEvent event)
    {
        // clear status bar when leaving to clear lat/long
        applet.statusBar.showStatus("");
    }

    // Mouse dragged event handler
    //----------------------------
    public void mouseDragged(MouseEvent event)
    {
        // user must be trying to move a point
        if (applet.userDefinedAreaDialog.isVisible())
        {
            // get screen location
            int x = event.getX();
            int y = event.getY();
            Point loc = getXYOnScreen(x,y);

            applet.userDefinedAreaDialog.getUserDefinedArea().mouseDragged(loc);

            // return because we don't want the normal dragging code to execute
            // when defining the user defined area
            return;
        }

        // only allow dragging if a drag is active
        if (!dragActive)
            return;

        // don't allow any further action if the TOCs are loading
        if (md.isUnstableTOC())
            return;
        
        // show the move cursor when dragging starts
        if (currentCursor != applet.moveCursor)
        {
            currentCursor = applet.moveCursor;
            if (!applet.showingWait)
                setCursor(currentCursor);
        }

        // calculate the amount of scrollable area available
        Dimension size = getSize();
        Dimension minSize = getParent().getSize();

        int xspan = size.width - minSize.width;
        int yspan = size.height - minSize.height;

        // calculate the change in the mouse position since the dragging
        // started
        int xc = dragMouseLoc.x - event.getX();
        int yc = dragMouseLoc.y - event.getY();

        // calculate the new scrolling location, remembering any left over 
        // motion once the edge of the scrolling area is reached
        int newx = dragScrollLoc.x + xc;
        int newy = dragScrollLoc.y + yc;

        if (newx < 0)
        {
            xc = newx;
            newx = 0;
        }
        else if (newx > xspan)
        {
            xc = newx - xspan;
            newx = xspan;
        }
        else
            xc = 0;
        if (newy < 0)
        {
            yc = -newy;
            newy = 0;
        }
        else if (newy > yspan)
        {
            yc = yspan - newy;
            newy = yspan;
        }
        else 
            yc = 0;

        // if there is is "unused" mouse motion, apply it to the drag offset
        if ((xc != dragOffset.x) || (yc != dragOffset.y))
        {
            dragOffset.x = xc;
            dragOffset.y = yc;

            // clip the map layers to the new upper left coordinate
            mapLayers.clip();

            // force the area to redraw - Note: it the Java VM merges multiple
            // paint messages in the event queue, so it doesn't hurt to have
            // another repaint message below.
            repaint();
        }

        // if the location changed, update the scroll position
        if ((newx != dragScrollLoc.x) || (newy != dragScrollLoc.y))
        {
            applet.imgScroll.getViewport().
                    setViewPosition(new Point(newx,newy));

            dragScrollLoc = applet.imgScroll.getViewport().getViewPosition();

            // force a repaint in case setting the viewport position didn't 
            repaint();
        }
    }

    // method to update lat/long display when the mouse moves
    //-------------------------------------------------------
    public void mouseMoved(MouseEvent event)
    {
        // don't display a lat/long if the TOCs are unstable since the display 
        // is in an inconsistent state
        if (md.isUnstableTOC())
        {
            applet.statusBar.showStatus("Loading Imagery...");
            return;
        }

        int x = event.getX();
        int y = event.getY();

        // determine the name of any map layer features under the cursor
        String name = mapLayers.findFeatureName(x, y, false);

        LatLong loc = getLatLongOnScreen(x,y);

        if (loc != null)
        {
            if (name == null)
                name = new String("");
            else
                name = new String(" - " + name);

            applet.statusBar.showStatus("Lat/Long: "
                    + LatLonFormat.format(loc.latitude) + ", "
                    + LatLonFormat.format(loc.longitude) + " degrees" + name);
        }
        else
            applet.statusBar.showStatus("");
    }

    // method to return the upper left corner coordinate (X/Y) of the 
    // image display area in meters.
    //---------------------------------------------------------------
    public Point getUpperLeftCorner()
    {
        // get the mosaic coordinates
        Point ul = md.mosaicCoords.getUpperLeftCorner();
        if (ul == null)
            return null;

        double pixelSize = md.actualPixelSize;

        // adjust the coordinates for the display centering
        ul.x -= (int)Math.round(offsetToCenterDisplay.x * pixelSize);
        ul.y += (int)Math.round(offsetToCenterDisplay.y * pixelSize);

        // if there is a drag offset, apply it too
        if ((dragOffset.x != 0) || (dragOffset.y != 0))
        {
            ul.x += (int)Math.round(dragOffset.x * pixelSize);
            ul.y += (int)Math.round(dragOffset.y * pixelSize);
        }

        return ul;
    }

    // Calculate XY from screen location
    //----------------------------------
    private Point getXYOnScreen
    (
        int x,      // I: input x screen location
        int y       // I: input y screen location
    )
    {
        Point ul = getUpperLeftCorner();
        // if upper left not valid, clear lat/long display and return
        if (ul == null)
            return null;

        int xCoord = ul.x;
        int yCoord = ul.y;

        double pixelSize = md.actualPixelSize;

        // adjust for the x,y location passed in
        xCoord += (int)Math.round(x * pixelSize);
        yCoord -= (int)Math.round(y * pixelSize);

        Point xy = new Point(xCoord,yCoord);
        return xy;
    }

    // Calculate lat,long at x,y on screen
    //------------------------------------
    private LatLong getLatLongOnScreen
    (
        int x,      // I: input x screen location
        int y       // I: input y screen location
    )
    {
        // get XY from screen location
        Point xy = getXYOnScreen(x,y);

        if (xy == null)
            return null;

        // convert to lat/long
        LatLong latLong = md.getLatLong(xy);

        return latLong;
    }
    
    // Return the lat/long of the last position right-clicked on
    //----------------------------------------------------------
    public LatLong getRightClickLatLong()
    {
        return savedRightClickLoc;
    }

    // method to test whether a rectangle (in lat/long) intersects the current
    // display area
    //------------------------------------------------------------------------
    public boolean intersectsGeoBox
    (
        Rectangle2D.Double geoBox   // I: lat/long rectangle to test
    )
    {
        // get the currently displayed area and current projection
        Rectangle2D.Float displayMeters = getDisplayAreaRectangle();
        ProjectionTransformation proj = md.getProjection();

        // get the 4 corners in projection coordinates
        int[] x = new int[4];
        int[] y = new int[4];
        x[0] = (int)displayMeters.x;
        x[1] = (int)(displayMeters.x + displayMeters.width);
        x[2] = (int)(displayMeters.x + displayMeters.width);
        x[3] = (int)displayMeters.x;
        y[0] = (int)displayMeters.y;
        y[1] = (int)displayMeters.y;
        y[2] = (int)(displayMeters.y - displayMeters.height);
        y[3] = (int)(displayMeters.y - displayMeters.height);

        // convert the 4 corners to lat/long
        LatLong[] corners = new LatLong[4];
        for (int i = 0; i < 4; i++)
        {
            corners[i] = proj.projToLatLong(x[i], y[i]);
            // if the conversion to lat/long fails, assume an intersection
            if (corners[i] == null)
                return true;
        }

        // test whether the longitude values cross +/-180 degrees and adjust
        // the corners so they don't have that discontinuity
        boolean crosses180 = false;
        if (corners[0].longitude > 100.0)
        {
            for (int i = 1; i < 4; i++)
            {
                if (corners[i].longitude < -100.0)
                {
                    crosses180 = true;
                    corners[i].longitude += 360.0;
                }
            }
        }
        else if (corners[0].longitude < -100.0)
        {
            for (int i = 1; i < 4; i++)
            {
                if (corners[i].longitude > 100.0)
                {
                    crosses180 = true;
                    corners[i].longitude -= 360.0;
                }
            }
        }

        // check for an intersection of the display area and the geoBox
        GeneralPath path = buildPath(corners);
        if (path.intersects(geoBox))
            return true;

        // if the display area crosses 180 degrees, check for an intersection
        // using the longitudes adjusted by 360 degrees
        if (crosses180)
        {
            double adjustment = 360.0;
            if (corners[0].longitude > 100.0)
                adjustment = -360.0;

            for (int i = 0; i < 4; i++)
                corners[i].longitude += adjustment;

            // check for an intersection of the display area and the geoBox
            path = buildPath(corners);
            if (path.intersects(geoBox))
                return true;
        }

        return false;
    }

    // helper method for building a GeneralPath for an array of points
    //----------------------------------------------------------------
    private GeneralPath buildPath(LatLong[] points)
    {
        // create a general path to hold the display area shape after it has
        // been converted to lat/long
        GeneralPath path = new GeneralPath();

        // get the upper left corner as the starting point of the path
        path.moveTo((float)points[0].longitude, (float)points[0].latitude);

        // add a line for each of the points
        for (int i = 1; i < points.length; i++)
        {
            path.lineTo((float)points[i].longitude,
                        (float)points[i].latitude);
        }

        // close the shape
        path.lineTo((float)points[0].longitude, (float)points[0].latitude);

        return path;
    }

    // method to cleanup any resources when the applet is stopped
    //-----------------------------------------------------------
    public void cleanup()
    {
        logo.cleanup();
        if (offScreenBuffer != null)
        {
            offScreenBuffer.flush();
            offScreenBuffer = null;
        }
    }
}
