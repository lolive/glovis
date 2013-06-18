// UserDefinedArea.java implements a class stores and manipulates a 
// user defined area created by clicking points on the screen.
//------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.util.Stack;

public class UserDefinedArea 
{
    private imgViewer applet;           // pointer to applet
    private MosaicData md;              // pointer to mosaic data
    private UserDefinedAreaDialog userDefinedAreaDialog;
                                        // pointer to the dialog box that
                                        // creates this object
    private Polygon area;               // Polygon with XY dimensions
    private Polygon displayArea;        // Polygon in display coordinates
    private boolean polygonIsClosed;    // flag to show when polygon is closed
    private Stack undoStack;            // stack of old vectors for undo
    private LatLong startPos;           // LatLong of the first polygon point
    private int selectedPointIndex;     // index to point user clicked on
    private int projectionCode;         // stores projection code of area
    private PolygonState prevState;     // holds previous state of polygon/flag
    private PolygonIntersectTester intersectTester; // object to test whether 
                                        // the user defined area and a scene
                                        // intersect
    private static final int THRESHOLD_FACTOR = 5; // stores how close to point
                                              // is needed to move the point
    private Point prevUpperLeft;        // store the last know upper left corner

    private class PolygonState
    {
        boolean closedFlag;
        Polygon areaPolygon;
    }


    // Constructor for the user defined area 
    //--------------------------------------
    public UserDefinedArea(imgViewer applet, MosaicData md, 
                                UserDefinedAreaDialog dialogBox)
    {
        this.applet = applet;
        this.md = md;
        userDefinedAreaDialog = dialogBox;

        // create new polygon to store the XY location of each point in the 
        // user defined area
        area = new Polygon();

        // create a new stack to hold the previous states of the polygon for
        // the undo feature
        undoStack = new Stack();

        polygonIsClosed = false;

        projectionCode = -1;
        prevUpperLeft = null;
    }

    // method to add to area polygon
    //-------------------------------
    private boolean addPoint(Point newPoint)
    {
        boolean polygonChange = false;
        // if there are two or less points currently added, don't worry about
        // intersects

        if (area.npoints < 3)
        {
            // grab the LatLong location of the first point and reset the 
            // polygon and the storage stack
            if (area.npoints == 0)
            {
                startPos = md.getLatLong(newPoint);

                // reset the area polygon and storage stack because we don't
                // want to keep an infinite number of undos.  Just since a 
                // clear, or the beginning, whichever is more recent
                area = new Polygon();
                undoStack = new Stack();
                PolygonState temp = new PolygonState();
                temp.areaPolygon = new Polygon();
                temp.closedFlag = false;
                undoStack.push(temp);
                projectionCode = md.getProjectionCode();
            }
            // add this point to the polygon
            area.addPoint(newPoint.x, newPoint.y);
            polygonChange = true;
        }
        else if (doesAddCauseIntersect(newPoint))
        {
            applet.statusBar.showStatus("Point causes invalid polygon");
        }
        else
        {
            area.addPoint(newPoint.x, newPoint.y);
            polygonChange = true;
        }
        return polygonChange;
    }

    // method to determine which point the user clicked on
    //----------------------------------------------------
    private int whichPointSelected(Point clickPoint)
    {
        if (area.npoints == 0)
            return -1;

        // check all the points and stop once you reach one that is close 
        // enough to the mouse click point
        for (int i = 0; i < area.npoints; i++)
        {
            Point currPoint = new Point(area.xpoints[i], area.ypoints[i]);

            double distSquared = findDistSquared(currPoint, clickPoint);
            double pixelSize = md.actualPixelSize;

            if (distSquared < Math.pow(THRESHOLD_FACTOR*pixelSize,2.0))
            {
                return i;
            }
        }
        return -1;
    }

    // method to find the euclidian distance between two points
    //---------------------------------------------------------
    private double findDistSquared(Point A, Point B)
    {
        double distSquared = (Math.pow((A.x-B.x),2.0) +
                              Math.pow((A.y-B.y),2.0));

        return distSquared;
    }

    // method to determine if polygon has been closed.  Return true if the 
    // polygon has been closed by the previous move
    //--------------------------------------------------------------------
    private boolean polygonClosed(Point newPoint)
    {
        // if its already closed, it must be closed
        if (polygonIsClosed)
        {
            return false;
        }

        // if it it doesn't have three points, then it cannot be closed
        if (area.npoints < 3)
            return false;

        int currSize = area.npoints;

        // if the point moved is the first point, check if the move closes the
        // polygon by being close to the last point
        if (selectedPointIndex == 0) 
        {
            Point lastPoint = new Point(area.xpoints[currSize-1],
                                        area.ypoints[currSize-1]);
            double distSquared = findDistSquared(newPoint,lastPoint);
            double pixelSize = md.actualPixelSize;

            if (distSquared < Math.pow(THRESHOLD_FACTOR*pixelSize,2.0))
            {
                polygonIsClosed = true;
                area.npoints--;
                return true;
            }
        }

        // if the point moved was the last point, check if the polygon is now
        // closed if the last point is really close to the first point
        if (selectedPointIndex == currSize-1) 
        {
            Point firstPoint = new Point(area.xpoints[0],area.ypoints[0]);
            double distSquared = findDistSquared(firstPoint, newPoint);
            double pixelSize = md.actualPixelSize;

            if (distSquared < Math.pow(THRESHOLD_FACTOR*pixelSize,2.0))
            {
                polygonIsClosed = true;
                area.npoints--;
                return true;
            }
        }

        // if the user clicks on the first point, see if it is possible to 
        // close the polygon.  We need to distinquish between moving the last
        // point and clicking on it.
        int releasePoint = whichPointSelected(newPoint);

        if ((selectedPointIndex == 0) && (releasePoint == 0))
        {
            // get the last saved state of the polygon.  use the prevState
            // because the area polygon may be invalid if the user dragged 
            // points.
            Point orig = new Point();
            orig.x = prevState.areaPolygon.xpoints[selectedPointIndex];
            orig.y = prevState.areaPolygon.ypoints[selectedPointIndex];

            double distSquared = findDistSquared(newPoint,orig);
            double pixelSize = md.actualPixelSize;

            // if the point released at by the user is the close the first
            // point of the area stored in the undoStack, then the first point
            // was not dragged, so the user intended to close the polygon
            if (!doesAddCauseIntersect(orig) &&
                    (distSquared < Math.pow(THRESHOLD_FACTOR*pixelSize,2.0)))
            {
                polygonIsClosed = true;
                return true;
            }
        }

        return false;
            
    }

    // method to determine if the given point should replace an existing point
    // in the user defined area polygon.  Return true if the point should be 
    // replaced/
    //-------------------------------------------------------------------------
    private boolean replacePoint(Point newPoint)
    {
        if (selectedPointIndex == -1)
            return false;

        // if the point to change is very close to what it was previously, 
        // no reason to replace it
        Point orig = new Point();
        orig.x = prevState.areaPolygon.xpoints[selectedPointIndex];
        orig.y = prevState.areaPolygon.ypoints[selectedPointIndex];

        double distSquared = findDistSquared(newPoint,orig);
        double pixelSize = md.actualPixelSize;

        if (distSquared < Math.pow(THRESHOLD_FACTOR*pixelSize,2.0))
            return false;

        // if there are three or less sides, any move is ok
        int currSize = area.npoints;
        if (currSize <= 3)
        {
            area.xpoints[selectedPointIndex] = newPoint.x;
            area.ypoints[selectedPointIndex] = newPoint.y;
            return true;
        }

        Point point1;
        Point point2;
        
        // find the next and previous points to the selected point
        int tempIndex = (selectedPointIndex-1+currSize)%currSize;
        Point prevPoint = new Point(area.xpoints[tempIndex],
                                        area.ypoints[tempIndex]);
        tempIndex = (selectedPointIndex+1)%currSize;
        Point nextPoint = new Point(area.xpoints[tempIndex],
                                        area.ypoints[tempIndex]);

        // case where the point moving is the first one (selectedPointIndex = 0)
        // when the polygon isn't closed, moving this point only changes one 
        // line
        if ((selectedPointIndex == 0) && !polygonIsClosed)
        {
            tempIndex = selectedPointIndex + 2;
            point2 = new Point(area.xpoints[tempIndex%currSize],
                               area.ypoints[tempIndex%currSize]);
            
            // make current size - 3 checks because the two adjacent segments
            // will always return true when test for intersection.  So skip
            // those.
            for (int i = 0; i < currSize - 3; i++)
            {
                point1 = point2;
                point2 = new Point(area.xpoints[(tempIndex+1)%currSize],
                                   area.ypoints[(tempIndex+1)%currSize]);
                tempIndex++;

                if (PolygonIntersectTester.linesIntersect(newPoint, nextPoint,
                        point1, point2))
                {
                    return false;
                }
            }
        }

        // case where the point moving is the last one added.  When the polygon
        // isn't closed, there is only one line to check for intersections
        else if ((selectedPointIndex == (currSize - 1)) && !polygonIsClosed)
        {
            tempIndex = selectedPointIndex + 1;
            point2 = new Point(area.xpoints[tempIndex%currSize],
                               area.ypoints[tempIndex%currSize]);
     
            // make current size - 3 checks because the two adjacent segments
            // will always return true when test for intersection.  So skip
            // those.
            for (int i = 0; i < currSize-3; i++)
            {
                point1 = point2;
                point2 = new Point(area.xpoints[(tempIndex+1)%currSize],
                                   area.ypoints[(tempIndex+1)%currSize]);
                tempIndex++;

                if (PolygonIntersectTester.linesIntersect(prevPoint, newPoint,
                        point1, point2))
                {
                    return false;
                }
            }
        }
        // case where the point is in the middle somewhere or the polygon is 
        // closed, and two new lines will be created from the move.  Both new
        // lines need to be checked for intersections
        else
        {
            tempIndex = selectedPointIndex + 1;
            point2 = new Point(area.xpoints[tempIndex%currSize],
                               area.ypoints[tempIndex%currSize]);

            // make current size - 3 checks because the two adjacent segments
            // will always return true when test for intersection.  So skip
            // those.
            for (int i = 0; i < currSize - 3; i++)
            {
                // skip the line between the first and last points if the
                // polygon isn't closed
                if (((tempIndex+1)%currSize == 0) && !polygonIsClosed)
                {
                    tempIndex++;
                    continue;
                }

                point1 = point2;
                point2 = new Point(area.xpoints[(tempIndex+1)%currSize],
                                   area.ypoints[(tempIndex+1)%currSize]);
                tempIndex++;

                if (PolygonIntersectTester.linesIntersect(prevPoint, newPoint,
                        point1, point2))
                {
                    return false;
                }
            }

            tempIndex = selectedPointIndex + 2;
            point2 = new Point(area.xpoints[tempIndex%currSize],
                               area.ypoints[tempIndex%currSize]);
            
            // make current size - 3 checks because the two adjacent segments
            // will always return true when test for intersection.  So skip
            // those.
            for (int i = 0; i < currSize - 3; i++)
            {
                // skip the line between the first and last points if the 
                // polygon ins't closed
                if (((tempIndex+1)%currSize == 0) && !polygonIsClosed)
                {
                    tempIndex++;
                    continue;
                }
                point1 = point2;
                point2 = new Point(area.xpoints[(tempIndex+1)%currSize],
                                   area.ypoints[(tempIndex+1)%currSize]);
                tempIndex++;

                if (PolygonIntersectTester.linesIntersect(newPoint, nextPoint,
                        point1, point2))
                {
                    return false;
                }
            }
        }

        // if we get this far, no intersections were found, so the move is ok
        area.xpoints[selectedPointIndex] = newPoint.x;
        area.ypoints[selectedPointIndex] = newPoint.y;
        return true;
    }

    // method to determine if adding the point will cause an invalid polygon
    // return true if the potential add causes an invalid polygon
    //----------------------------------------------------------------------
    private boolean doesAddCauseIntersect(Point newPoint)
    {
        int currentSize = area.npoints;
        if (currentSize <= 1)
            return false;

        Point point1 = new Point();
        Point point2 = new Point();

        // potential new line segment
        Point startPoint = new Point(area.xpoints[currentSize-1], 
                                     area.ypoints[currentSize-1]);
        Point endPoint = newPoint;

        // if new point is the first point (i.e. closing polygon)
        // don't check the first line segment
        int i = 0;
        if ((newPoint.x == area.xpoints[0]) && (newPoint.y == area.ypoints[0]))
            i = 1;

        // check if any of the current line segments intersect the potential
        // new line
        point2 = new Point(area.xpoints[i],area.ypoints[i]);
        for ( ; i < currentSize - 2; i++)
        {
            point1 = point2;
            point2 = new Point(area.xpoints[i+1],area.ypoints[i+1]);
            if (PolygonIntersectTester.linesIntersect(startPoint, endPoint,
                    point1, point2))
            {
                return true;
            }
        }
        return false;
    }

    // method to build a polygon to display
    //-------------------------------------
    private Polygon getAreaPolygonInScreenCoords()
    {
        if (area.npoints == 0)
            return null;

        Point ul = applet.imgArea.getUpperLeftCorner();
        if (ul == null)
            return null;

        // only rebuild the display polygon if one hasn't been made or the
        // screen has been shifted
        if ((displayArea == null) || (ul != prevUpperLeft))
        {
            // the intersection tester will need to be regenerated if the 
            // display area is null or the screen has shifted
            intersectTester = null;

            double pixelSize = md.actualPixelSize;
            int x;
            int y;
            int xcoord;
            int ycoord;
            displayArea = new Polygon();

            // convert the projection XY to screen coordinates
            for (int index = 0; index < area.npoints; index++)
            {
                x = area.xpoints[index];
                y = area.ypoints[index];
                xcoord = (int)Math.round(((x-ul.x)/pixelSize)); 
                ycoord = (int)Math.round(((-y+ul.y)/pixelSize));
                displayArea.addPoint(xcoord,ycoord);
            }
            prevUpperLeft = new Point(ul);
        }
        return displayArea;
    }

    // method to see if scene is within polygon
    //-----------------------------------------
    public boolean sceneIntersects(Metadata scene)
    {
        // make sure scene exists
        if (scene == null)
            return false;

        if (polygonIsClosed)
        {
            // make sure screenLocation polygon for scene exists
            if (scene.screenLocation == null)
                return false;

            if ((displayArea == null) || (intersectTester == null))
            {
                displayArea = getAreaPolygonInScreenCoords();
                Point offset = applet.imgArea.getOffsetToCenterDisplay();
                intersectTester = new PolygonIntersectTester(displayArea);
                intersectTester.translate(-offset.x, -offset.y);
            }

            return intersectTester.intersects(scene.screenLocation);
        }

        return false;
    }

    // method to return if the polygon is closed or not
    //-------------------------------------------------
    public boolean getPolygonIsClosed()
    {
        return polygonIsClosed;
    }

    // method to set the polygonIsClosed flag
    //---------------------------------------
    public void setPolygonIsClosed(boolean state)
    {
        polygonIsClosed = state;
    }

    // method to return the number of points in the polygon
    //-----------------------------------------------------
    public int numberOfPolygonPoints()
    {
        return area.npoints;
    }

    // method to return if the undo stack is empty or not
    //---------------------------------------------------
    public boolean isUndoStackEmpty()
    {
        return undoStack.empty();
    }

    // method to determine if closing the polygon will cause an intersection
    //----------------------------------------------------------------------
    public boolean doesCloseCauseIntersect()
    {
        return doesAddCauseIntersect(new Point(area.xpoints[0],
                                                area.ypoints[0]));
    }

    // method to apply the user defined area
    //--------------------------------------
    public void applyUserDefinedArea()
    {
        // apply filter, and move to scene within user defined area.
        applet.searchLimitDialog.applyFilter();
        md.sceneFilter.filter();
        applet.imgArea.repaint();
    }

    // method to respond to user closing and applying the area
    //--------------------------------------------------------
    public void closeAndApplyArea()
    {
        displayArea = null;
        saveState();
        polygonIsClosed = true;
        applyUserDefinedArea();

        // see if the currently selected scene is in the user defined area
        // if its not, make current scene one in the user defined area
        TOC cell = md.getCurrentCell();
        Metadata currScene = null;
        if (cell != null)
        {
            if (cell.valid && (cell.currentDateIndex >= 0))
                currScene = cell.scenes[cell.currentDateIndex];
        }
        if ((currScene == null) || !sceneIntersects(currScene))
        {
            md.sceneFilter.gotoLastDate();
        }
    }

    // method to respond to user unclosing the user defined area
    //----------------------------------------------------------
    public void uncloseArea()
    {
        displayArea = null;
        saveState();
        polygonIsClosed = false;
        applet.imgArea.repaint();
    }

    // method to draw the lines for the polygon
    //-----------------------------------------
    public void drawArea(Graphics g)
    {
        // if the projection code used when the area was first defined is not
        // the current area, don't draw the polygon.  It may show up in an
        // unintended part of the world.
        if (projectionCode != md.getProjectionCode())
            return;

        displayArea = null;
        getAreaPolygonInScreenCoords();

        // draw the polygon if there is at least one point and the 
        // either the area is closed or the dialog box is visible.
        if ((displayArea != null) && (userDefinedAreaDialog.isVisible() 
                                                || polygonIsClosed))
        {
            // draw five polygons or polylines to make the line
            // appear thicker
            int[] xpoints = displayArea.xpoints;
            int[] ypoints = displayArea.ypoints;
            int npoints = displayArea.npoints;
            g.setColor(Color.BLACK);

            // draw the points in black for a shadow below the polygon
            for (int i = 0; i < npoints; i++)
            {
                g.fillOval(xpoints[i]-6,ypoints[i]-2,9,9);
            }

            // once the area is closed, draw the closed polygon
            if (polygonIsClosed)
            {
                displayArea.translate(-2,2);
                g.drawPolygon(displayArea);
                displayArea.translate(1,-1);
                g.drawPolygon(displayArea);
                g.setColor(Color.WHITE);
                displayArea.translate(1,-1);
                g.drawPolygon(displayArea);
                displayArea.translate(1,-1);
                g.drawPolygon(displayArea);
                displayArea.translate(-1,1);
            }
            // area is not closed, so just draw the segments between points
            else 
            {
                displayArea.translate(-2,2);
                g.drawPolyline(xpoints, ypoints, npoints);
                displayArea.translate(1,-1);
                g.drawPolyline(xpoints, ypoints, npoints);
                g.setColor(Color.WHITE);
                displayArea.translate(1,-1);
                g.drawPolyline(xpoints, ypoints, npoints);
                displayArea.translate(1,-1);
                g.drawPolyline(xpoints, ypoints, npoints);
                displayArea.translate(-1,1);
            }

            // draw the points selected
            for (int i = 0; i < npoints; i++)
            {
                g.fillOval(xpoints[i]-4,ypoints[i]-4,9,9);
            }
        }
    }

    // method to save the state of closed flag and area polygon
    //-------------------------------------------------------------
    private void saveState()
    {
        // save previous state of points and flag
        prevState = new PolygonState();
        prevState.closedFlag = polygonIsClosed;
        prevState.areaPolygon = new Polygon(area.xpoints,
                                                    area.ypoints,area.npoints);

        undoStack.push(prevState);
    }

    // method to take appropriate action from mouse release
    //-----------------------------------------------------
    public void mouseRelease(Point loc)
    {
        boolean polygonChanged = false;

        // call method to determine if user action closed the polygon
        polygonChanged = polygonClosed(loc);

        // call function to see if the point can be added to the polygon
        if (!polygonIsClosed && (selectedPointIndex == -1) && !polygonChanged)
        {
            polygonChanged = addPoint(loc);
        }

        // call method to determing if the user moved a point
        if ((selectedPointIndex != -1) && !polygonChanged)
        {
            polygonChanged = replacePoint(loc);
        }
        
        // if polygon was closed, apply the user defined area
        if (polygonIsClosed)
        {
            displayArea = null;
            applyUserDefinedArea();

            applet.searchLimitDialog.applyFilter();
            md.sceneFilter.filter();
        }
        
        // if the polygon didn't change, discard the last stored state
        if (!polygonChanged)
        {
            undoStack.pop();
        }
        else
        {
            // clear ths polygon box for testing if a scene is close
            displayArea = null;
            applet.imgArea.repaint();
        }

        userDefinedAreaDialog.enableButtons();

        selectedPointIndex = -1;
    }

    // method to handle events when the mouse is pressed on the Image Pane
    //--------------------------------------------------------------------
    public void mousePressed(Point XYLocation)
    {
        selectedPointIndex = whichPointSelected(XYLocation);
        saveState();
    }

    // method to handle events when the mouse is dragged on the Image Pane
    //--------------------------------------------------------------------
    public void mouseDragged(Point XYLocation)
    {
        boolean changeFlag = replacePoint(XYLocation);

        if (changeFlag)
        {
            displayArea = null;
            applet.imgArea.repaint();
        }
    }
    
    // method to clear the polygon
    //----------------------------
    public void clearPolygon()
    {
        if (area.npoints > 0)
        {
            saveState();

            // remove all elements in the area polygon
            area.npoints = 0;

            // reset all polygons
            polygonIsClosed = false;
            userDefinedAreaDialog.enableButtons();
            displayArea = null;

            // reset the search limit dialog box user defined checkbox and
            // update the number of scene available.
            applet.searchLimitDialog.clearUserDefinedAreaEnabled();
            applet.searchLimitDialog.updateNumOfAvailScenes();
            applet.searchLimitDialog.applyFilter();
            
            // refresh screen
            applet.imgArea.repaint();
        }
    }
    
    // method to implement the undo feature
    //-------------------------------------
    public void returnToPrevState()
    {
        // restore previous settings
        polygonIsClosed = prevState.closedFlag;
        area = prevState.areaPolygon;

        // prevState stores the top of the stack.  Used as an small optimization
        // to avoid peeking at the top of the stack to see if the state has 
        // changed.  So we can now get rid of the top of the stack and set
        // the prevState to the new top of the stack.
        undoStack.pop();
        if (!undoStack.empty())
            prevState = (PolygonState)undoStack.peek();

        // reset checkbox and buttons
        userDefinedAreaDialog.enableButtons();

        // reset box to see if scene is close enough
        displayArea = null;
        
        // reset the search limit dialog box user defined checkbox and
        // update the number of scene available.
        if (applet.searchLimitDialog.isUserDefinedAreaEnabled())
	    {
            applet.searchLimitDialog.clearUserDefinedAreaEnabled();
            applet.searchLimitDialog.updateNumOfAvailScenes();
            applet.searchLimitDialog.applyFilter();
        }

        // refresh screen
        applet.imgArea.repaint();
    }

    // method to move screen to current polygon
    //-----------------------------------------
    public void moveScreenToPolygon()
    {
        if (area.npoints > 0)
        {
            md.gotoLatLong(startPos.latitude, startPos.longitude);
        }
    }
}
