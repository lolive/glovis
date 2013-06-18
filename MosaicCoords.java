// MosaicCoords is a class to encasulate the tracking of the coordinates 
// for the current mosaic area
//-----------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;

public class MosaicCoords
{
    private Dimension mosaicPixels;// size of the mosaicked images in pixels
    private Point mosaicUL;        // upper left X/Y of the mosaicked data in 
                                   // meters
    private boolean mosaicULValid; // valid flag for mosaicUL
    private Point displayedUL;     // upper left X/Y of the display area in
                                   // meters
    private boolean displayedULValid; // valid flag for displayedUL
    private Polygon cellViewport;  // single cell viewport coordinates
    private Dimension viewportPixels;// single cell viewport size in pixels
    private int[] tempX;        // temporary x coordinate array
    private int[] tempY;        // temporary y coordinate array


    // method to return the size of the mosaic area in pixels
    //-------------------------------------------------------
    public Dimension getMosaicPixels()
    {
        return mosaicPixels;
    }

    // method to return the size of the displayed viewport
    //----------------------------------------------------
    public Dimension getViewportPixels()
    {
        return viewportPixels;
    }

    // constructor
    //------------
    MosaicCoords()
    {
        mosaicUL = new Point(0,0);
        displayedUL = new Point(0,0);
        tempX = new int[4];           // temporary x coordinates
        tempY = new int[4];           // temporary y coordinates
        viewportPixels = new Dimension(300,300);
    }

    // method to update the mosaic coordinate data when a change occurs
    //-----------------------------------------------------------------
    public void update
    (
        imgViewer applet,         // I: reference to the main applet class
        TOC[] mosaicCells,        // I: array of TOC files in mosaic
        Sensor currSensor,        // I: currently selected sensor
        Dimension minMosaicPixels,// I: minimum size of mosaic area in pixels
        ProjectionTransformation proj,// I:projection object for converting X/Y
                                  //    to lat/long
        int cellsToDisplay,       // I: number of cells being displayed
        int activeCellIndex,      // I: index in array of TOCs for active cell
        double pixelSize,         // I: actual displayed pixel size in meters
        int subCol,               // I: factional column number
        int subRow                // I: fractional row number
    )
    {
        TOC cell;
        mosaicULValid = false;
        displayedULValid = false;

        // calculate the size and location of the mosaic area
        calculateMosaicPixelsAndLocation(mosaicCells, currSensor, proj);

        // the upper left corner is now valid
        mosaicULValid = true;

        // find the width and height of the display area which is the maximum
        // of the smallest displayed size and the actual mosaic area
        int width = mosaicPixels.width;
        int height = mosaicPixels.height;
        width = Math.max(width,minMosaicPixels.width);
        height = Math.max(height,minMosaicPixels.height);

        // calculate the single cell viewport coordinates
        calcCellViewport(applet, mosaicCells, proj);

        // update the displayed upper left corner location
        if (cellsToDisplay == 1)
        {
            // single cell, so adjust the viewport to the correct fractional
            // column/row
            adjustViewport(applet,mosaicCells,proj,pixelSize,subCol,subRow);
        }
        else if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            // single scene, so calculate upper left from the cell
            cell = mosaicCells[activeCellIndex];
            if (cell.valid)
            {
                // cell is valid, so use the metadata info
                Metadata scene = cell.scenes[cell.currentDateIndex];
                updateDisplayedUL(scene.ulX,scene.ulY,cell.valid);
            }
            else
            {
                // no valid info in the cell, so estimate the projection
                // coordinate based on the center point of the cell
                Point cellCenter = cell.getCenterProjCoords(proj);

                // TBD - should probably tie this to the image area size,
                // but currently don't have an easy way to get it from here
                cellCenter.x -= 300 * pixelSize;
                cellCenter.y += 300 * pixelSize;
                updateDisplayedUL(cellCenter.x,cellCenter.y,true);
            }
        }
        else
        {
            // mosaic, so use the mosaic X/Y location
            updateDisplayedUL(mosaicUL.x,mosaicUL.y,mosaicULValid);
        }

        // calculate the location of all the available scenes on the display
        calcScreenLocations(mosaicCells, currSensor, pixelSize);
    }

    // method to calculate the coordinates of a single cell viewport
    //--------------------------------------------------------------
    private void calcCellViewport
    (
        imgViewer applet,         // I: reference to the main applet class
        TOC[] mosaicCells,        // I: array of TOC files in mosaic
        ProjectionTransformation proj// I: projection object for converting X/Y
                                  //    to/from lat/long (not used anymore)
    )
    {
        // Use the min and max of the center cell TOC
        // (This may catch some scenes from nearby cells, but it gives us
        // a big enough window so the selected scene is not filtered-out
        // if it happens to be on the edge of the cell boundary.)
        TOC centerCell = mosaicCells[applet.md.getMosaicCenterIndex()];

        // upper left
        tempX[0] = centerCell.maxX;
        tempY[0] = centerCell.maxY;
        // upper right
        tempX[1] = centerCell.minX;
        tempY[1] = centerCell.maxY;
        // lower right
        tempX[2] = centerCell.minX;
        tempY[2] = centerCell.minY;
        // lower left
        tempX[3] = centerCell.maxX;
        tempY[3] = centerCell.minY;

        cellViewport = new Polygon(tempX,tempY,4);
    }

    // method to adjust the viewport location and filter the TOC cells based
    // on the fraction grid column/row step.  Note the viewport isn't actually
    // updated, just the scenes filtered, the displayed UL coordinate set, and
    // the viewport size calculated.
    //------------------------------------------------------------------------
    private void adjustViewport
    (
        imgViewer applet,         // I: reference to the main applet class
        TOC[] mosaicCells,        // I: array of TOC files in mosaic
        ProjectionTransformation proj,// I: projection object for converting X/Y
                                  //    to/from lat/long
        double pixelSize,         // I: currently displayed pixel size in meters
        int subCol,               // I: factional column number
        int subRow                // I: fractional row number
    )
    {
        Polygon viewport = this.cellViewport;

        // convert the center cell coordinate to projection coordinates
        Point center = mosaicCells[applet.md.getMosaicCenterIndex()]
            .getCenterProjCoords(proj);

        // start out with no viewport adjustment
        int x = 0;
        int y = 0;

        // adjust the viewport for the for the sub-column shift
        int mosaicHeight = applet.md.getMosaicHeight();
        int mosaicCenterIndex = applet.md.getMosaicCenterIndex();
        int next = mosaicCenterIndex;  // index into mosaicCells
        if (subCol < 0)
            next += mosaicHeight; // use the cell one column right in mosaic
        else if (subCol > 0)
            next -= mosaicHeight; // use the cell one column left in mosaic
        Point active = mosaicCells[next].getCenterProjCoords(proj);
        int diffx = -(center.x - active.x) * Math.abs(subCol)/MosaicData.STEPS;
        int diffy = (center.y - active.y) * Math.abs(subCol)/MosaicData.STEPS;

        x += diffx;
        y += diffy;

        // adjust the viewport for the for the sub-row shift
        // unless we're already at the top or bottom edge of the world
        int centerGridRow = mosaicCells[mosaicCenterIndex].gridRow;
        NavigationModel nm = applet.sensorMenu.getCurrentSensor().navModel;
        next = mosaicCenterIndex;
        if (subRow < 0 && centerGridRow > nm.getMinimumRow())
            next--; // use the cell one row "up" in mosaic
        else if (subRow > 0 && centerGridRow < nm.getMaximumRow())
            next++; // use the cell one row "down" in mosaic
        active = mosaicCells[next].getCenterProjCoords(proj);
        diffx = -(center.x - active.x) * Math.abs(subRow)/MosaicData.STEPS;
        diffy = (center.y - active.y) * Math.abs(subRow)/MosaicData.STEPS;

        x += diffx;
        y += diffy;

        // create a new viewport adjusted with the sub-column and sub-row shifts
        int[] tempX = new int[4];
        int[] tempY = new int[4];
        for (int i = 0; i < 4; i++)
        {
            tempX[i] = viewport.xpoints[i] + x;
            tempY[i] = viewport.ypoints[i] - y;
        }
        viewport = new Polygon(tempX,tempY,4);

        // filter the scenes in the mosaic against the viewport
        for (int i = 0; i < mosaicCells.length; i++)
            mosaicCells[i].filterScenesToViewport(viewport);

        // adjust the displayed upper left point for the current
        // viewport location
        int xMin = tempX[0];
        int xMax = tempX[0];
        int yMin = tempY[0];
        int yMax = tempY[0];
        for (int i = 1; i < 4; i++)
        {
            if (tempX[i] < xMin)
                xMin = tempX[i];
            if (tempX[i] > xMax)
                xMax = tempX[i];
            if (tempY[i] < yMin)
                yMin = tempY[i];
            if (tempY[i] > yMax)
                yMax = tempY[i];
        }

        // adjust the upper left corner to account for the viewport plus
        // half a scene (since the scene center might fall right on the edge
        // of the viewport and half the scene may fall outside of it)
        Dimension sceneSize = mosaicCells[applet.md.getMosaicCenterIndex()]
            .maxSceneSize;
        updateDisplayedUL(xMin - sceneSize.width/2,
                          yMax + sceneSize.height/2, true);
        // calculate the size of the viewport display area
        viewportPixels = new Dimension(
                  (int)Math.round((xMax - xMin + sceneSize.width)/pixelSize),
                  (int)Math.round((yMax - yMin + sceneSize.height)/pixelSize));
    }

    // method to calculate the mosaic size in pixels and the upper left 
    // corner of the mosaic.  Sets mosaicPixels and mosaicUL.
    //-----------------------------------------------------------------
    private void calculateMosaicPixelsAndLocation(TOC[] mosaicCells, 
                        Sensor currSensor, ProjectionTransformation proj)
    {
        int width = 0;
        int height = 0;

        // initialize the mosaic upper left corner
        mosaicUL.x = 100000000;
        mosaicUL.y = -100000000;

        // find the upper left corner of the mosaic using the minX and maxY
        // from the valid cells
        for (int i = 0; i < mosaicCells.length; i++) 
        {
            TOC cell = mosaicCells[i];
            
            // only include valid cells
            if (cell.valid)
            {
                if (cell.minX < mosaicUL.x) 
                    mosaicUL.x = cell.minX;
                if (cell.maxY > mosaicUL.y) 
                    mosaicUL.y = cell.maxY;
            }
        }

        // get the lowest resolution (i.e. the one used for the mosaic)
        double res = currSensor.getLowestResolution();

        // calculate the projection space extents using the cell centers
        int minX = 100000000;
        int maxX = -100000000;
        int minY = 100000000;
        int maxY = -100000000;
        for (int index = 0; index < mosaicCells.length; index++)
        {
            TOC cell = mosaicCells[index];
            Point current = cell.getCenterProjCoords(proj);
            if (current.x < minX)
                minX = current.x;
            if (current.x > maxX)
                maxX = current.x;
            if (current.y < minY)
                minY = current.y;
            if (current.y > maxY)
                maxY = current.y;
        }

        // using the projection space extents, estimate the lines and samples
        // in the mosaic (*3/2 to adjust for the X/Y being calculated to the 
        // center of the mosaic'ed scenes instead of the edges)
        int lines = (int)((maxY - minY) / res) * 3 / 2;
        int samps = (int)((maxX - minX) / res) * 3 / 2;

        // set the initial estimate for the width and height
        width = samps;
        height = lines;

        // adjust the minX and maxY to account for them only being to the 
        // center of the scenes
        minX -= ((maxX - minX)/4);
        maxY += ((maxY - minY)/4);

        // factor the maxY and minX calculated from the projection space 
        // extents into the upper left of the mosaic (to account for cells
        // that do not have any data)
        if (maxY > mosaicUL.y)
            mosaicUL.y = maxY;
        if (minX < mosaicUL.x)
            mosaicUL.x = minX;

        // consider each of the valid cells to see if they impact the estimated
        // width or height of the mosaic
        for (int index = 0; index < mosaicCells.length; index++)
        {
            TOC cell = mosaicCells[index];
            if (cell.valid)
            {
                lines = (int)Math.round((mosaicUL.y - cell.minY)/res);
                samps = (int)Math.round((cell.maxX - mosaicUL.x)/res);
                width = Math.max (samps, width);
                height = Math.max (lines, height);
            }
        }

        // set the mosaic size
        mosaicPixels = new Dimension(width,height);
    }

    // method to calculate the screen location of all the available scenes.
    // The end result is that the Metadata entry for each scene has its 
    // screen location polygon set.
    //---------------------------------------------------------------------
    private void calcScreenLocations(TOC[] mosaicCells, Sensor currSensor,
                                     double pixelSize)
    {
        for (int n = 0; n < mosaicCells.length; n++)
        {
            TOC cell = mosaicCells[n];
            if (!cell.valid)
                continue;

            // get the offset resolution 
            double offsetRes = currSensor.getOffsetResolution();

            // loop through all the scenes in this cell
            for (int index = 0; index < cell.numImg; index++)
            {
                // temporary pointer to the current scene
                Metadata scene = cell.scenes[index];
                int sceneSamp = (scene.ulX - displayedUL.x);
                int sceneLine = (displayedUL.y - scene.ulY);

                // calculate the line/sample offsets for this scene
                for (int i = 0; i < 4; i++)
                {
                    // the offsetRes is to scale the offsets for the current
                    // resolution since the offsets are defined for the 
                    // resolution below the full resolution size, and convert
                    // from meters to pixels
                    tempX[i] = (int)Math.round((sceneSamp + 
                        scene.sampOffset[i] * offsetRes) / pixelSize);
                    tempY[i] = (int)Math.round((sceneLine +
                        scene.lineOffset[i] * offsetRes) / pixelSize);
                }

                // set the scene's polygon
                scene.screenLocation = new Polygon(tempX,tempY,4);
            }
        }
    }

    // method to return the current upper left corner coordinate in X,Y
    //-----------------------------------------------------------------
    public Point getUpperLeftCorner()
    {
        if (displayedULValid)
            return new Point(displayedUL);
        else
            return null;
    }

    // method to update the displayed upper left corner coordinate
    //------------------------------------------------------------
    public void updateDisplayedUL(int x, int y, boolean valid)
    {
        displayedUL.x = x;
        displayedUL.y = y;
        if (valid)
            displayedULValid = true;
        else
            displayedULValid = false;
    }

    // method to invalidate the displayed upper left corner coordinate
    //----------------------------------------------------------------
    public void invalidate()
    {
        displayedULValid = false;
    }
}
