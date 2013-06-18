//---------------------------------------------------------------------------
// Name: ShapeFileMapLayer
//
// Description: Implements a map layer class for loading and displaying an
//  ESRI shapefile.
//---------------------------------------------------------------------------
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;


public class ShapeFileMapLayer extends TiledMapLayer
{
    private imgViewer applet;
    private int lineWidth;          // width to draw the lines
    private Vector clippedShapes;   // vector of shapes clipped to the visible
                                    // area
    private boolean valid;          // flag to indicate the data is valid
    private String filename;        // shapefile name without an extension
    private Vector shapes;          // list of shapes read
    private Rectangle2D.Double visibleArea; // visible area displayed
    private boolean needsLoading = false; // flag to indicate the file should
                                          // be loaded
    private ShapeFileAttributesPanel attributesPanel;// panel in the attributes
                                          // dialog for this shapefile
    private String activeAttribute = ""; // attribute to display when the mouse
                                         // cursor is over a shape
    private String filterAttribute = "nofilter"; // attribute to filter on
    private String filterValue = ""; // value to filter on
    private AffineTransform identityTransform; // identity transform
    private int displayedIndex = -1; // index of shape under the mouse
    private Rectangle2D extent;      // bounding box of the shapefile

    // class to store information about a shape read from a shapefile
    class ShapefileEntry
    {
        int index;              // index of the shape in the file
        GeneralPath path;       // path for the shape
        String attribute;       // attribute saved for the shape
        boolean isLine;         // flag to indicate the shape is a line
    }

    // constructor for the ShapeFileMapLayer class
    //--------------------------------------------
    ShapeFileMapLayer(imgViewer applet, String layerName, String baseFilename,
                      ShapeFileAttributesDialog attributesDialog, File file)
    {
        super(applet.imgArea, layerName, baseFilename, Color.RED, 0, true);
        this.applet = applet;
        this.lineWidth = 1;
        visibleArea = new Rectangle2D.Double();

        setFile(file, attributesDialog);

        identityTransform = new AffineTransform();
    }

    // method to make sure the shapefile is visible in the display when it
    // is turned on
    //--------------------------------------------------------------------
    public void setLayerOn(boolean on)
    {
        if (!isLayerOn() && on)
        {
            Rectangle2D.Double sfGeo = getExtent();
            if (!applet.imgArea.intersectsGeoBox(sfGeo))
            {
                applet.md.gotoLatLong(sfGeo.y + sfGeo.height/2.0,
                                      sfGeo.x + sfGeo.width/2.0);
            }
        }

        super.setLayerOn(on);
    }

    // method to return true if the layer was successfully created
    //------------------------------------------------------------
    public boolean isValid()
    {
        if (attributesPanel != null)
            return true;
        return false;
    }

    // method to return the state of the map layer enabled/disable
    //------------------------------------------------------------
    public boolean isEnabled()
    {
        // always enabled
        return true;
    }

    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode)
    {
        double xMin = to15deg(minboxULdeg.longitude, true);
        double xMax = to15deg(minboxLRdeg.longitude, false) + 15;
        double yMin = to15deg(minboxLRdeg.latitude, true);
        double yMax = to15deg(minboxULdeg.latitude, false) + 15;
        Rectangle2D.Double newVisibleArea 
                    = new Rectangle2D.Double(xMin, yMin, xMax - xMin,
                                             yMax - yMin);
        if (!newVisibleArea.equals(visibleArea))
        {
            visibleArea = newVisibleArea;
            needsLoading = true;
            clippedShapes = null;
            return 1;
        }
        else
            return 0;
    }

    // method to configure the map layer for the displayed area using
    // projection coordinates.
    //   Returns: number of files that will need to be loaded
    //---------------------------------------------------------------
    public int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode)
    {
        // for now, just default to the entire world since this is only used
        // for MODIS data and coverting to min/max lat/long box is a pain
        Rectangle2D.Double newVisibleArea 
                    = new Rectangle2D.Double(-180.0, -90.0, 360.0, 180.0);
        if (!newVisibleArea.equals(visibleArea))
        {
            visibleArea = newVisibleArea;
            needsLoading = true;
            clippedShapes = null;
            return 1;
        }
        else
            return 0;
    }

    // method to determine whether the provided file is a shapefile that can
    // be read and displayed.  It verifies the .dbf, .shp, and .shx files 
    // can be read and if there is a .prj file, it makes sure that it is not
    // in a projection since only geographic files are currently supported.
    //----------------------------------------------------------------------
    public static boolean isValidShapeFile(File file)
    {
        // strip off the extension and make sure the .dbf, .shp, and .shx
        // files can be read
        String fullFilename = file.getAbsolutePath();
        int index = fullFilename.lastIndexOf('.');
        String filename = fullFilename.substring(0, index);

        // verify the .dbf, .shp, and .shx files exist and can be read
        File dbfFile = new File(filename + ".dbf");
        File shpFile = new File(filename + ".shp");
        File shxFile = new File(filename + ".shx");
        if (!dbfFile.canRead() || !shpFile.canRead() || !shxFile.canRead())
        {
            return false;
        }

        // verify that if a .prj file exists, it indicates the file is in 
        // geographic
        File prjFile = new File(filename + ".prj");
        if (prjFile.canRead())
        {
            BufferedReader prjReader = null;
            try
            {
                prjReader = new BufferedReader(new FileReader(prjFile));
                String line = prjReader.readLine().trim();

                if (line != null)
                {
                    // if the .prj file indicates a projected coordinate
                    // system, return false since we currently only support
                    // geographic files
                    if (line.startsWith("PROJCS"))
                    {
                        return false;
                    }
                }
            }
            catch (Exception e)
            {
                // the finally clause will close the file on an error
            }
            finally
            {
                try
                {
                    if (prjReader != null)
                        prjReader.close();
                }
                catch (Exception e1) {}
                prjReader = null;
            }

        }

        // the file appears to be a valid shapefile
        return true;
    }

    // method to set the file to use for this map layer
    //-------------------------------------------------
    private void setFile(File file, ShapeFileAttributesDialog attributesDialog)
    {
        // get the filename without an extension
        String fullFilename = file.getAbsolutePath();
        int index = fullFilename.lastIndexOf('.');
        filename = fullFilename.substring(0, index);

        needsLoading = true;

        // get the extents of the file and the attribute names and update the
        // dialog box
        ShapeFileReader shapefile = null;
        DBFReader dbfFile = null;
        String[] columnNames = null;
        boolean valid = false;
        try
        {
            shapefile = new ShapeFileReader();
            shapefile.open(filename);
            extent = shapefile.getExtent();
            shapefile.close();
            shapefile = null;

            dbfFile = new DBFReader();
            dbfFile.open(filename);
            columnNames = dbfFile.getColumnNames();
            valid = true;
        }
        catch (IOException e)
        {
            // allow the finally clause to close the file on an error
        }
        finally
        {
            if (shapefile != null)
                try {shapefile.close();} catch (Exception e1) {};
            shapefile = null;
            if (dbfFile != null)
                try {dbfFile.close();} catch (Exception e1) {};
            dbfFile = null;
        }

        // if the column names were successfully read, create an entry in the
        // attributes dialog for this shapefile
        if (valid)
        {
            attributesPanel = attributesDialog.addShapeFile(getName(), this,
                                                columnNames);
        }
    }

    // method to return the extents of the shapefile
    //----------------------------------------------
    public Rectangle2D.Double getExtent()
    {
        Rectangle2D.Double ret = new Rectangle2D.Double();
        ret.setRect(extent);
        return ret;
    }

    // method to set the attribute to display in the status bar when the cursor
    // is over a shape
    //-------------------------------------------------------------------------
    public void setActiveAttribute(String attribute)
    {
        if (!attribute.equals(activeAttribute))
        {
            // the attribute is different than the previously selected one, so
            // force the file to be re-read
            activeAttribute = new String(attribute);
            if (!needsLoading)
            {
                needsLoading = true;
                applet.md.mapLayers.showLayers();
            }
        }
    }

    // method to set the attribute to filter shapes on and what that attribute
    // must be equal to in order to be displayed
    //------------------------------------------------------------------------
    public void setFilter(String attribute, String value)
    {
        if (!attribute.equals(filterAttribute)
            || !value.equals(filterValue))
        {
            // the filter attribute or value have changed, so force a re-read
            // of the file
            filterAttribute = new String(attribute);
            filterValue = new String(value);

            if (!needsLoading)
            {
                needsLoading = true;
                applet.md.mapLayers.showLayers();
            }
        }
    }

    // method to read the map layer file
    //----------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        if (!needsLoading)
            return;

        valid = false;

        // if the load has been cancelled, break out of the loop
        if (isLoadCancelled.cancelled)
            return;

        shapes = new Vector();  // vector of shapes read

        if (applet.verboseOutput)
            System.out.println("Reading " + filename);

        ShapeFileReader shapefile = null;
        DBFReader dbfFile = null;
        try
        {
            // if the file intersects the visible area, read the shapes from
            // the file if it overlaps with the visible area
            if (extent.intersects(visibleArea))
            {
                // open the shapefile and associated DBF attributes file
                shapefile = new ShapeFileReader();
                shapefile.open(filename);

                dbfFile = new DBFReader();
                dbfFile.open(filename);

                // get the attribute to use for displaying in the status bar
                int attributeIndex = dbfFile.getColumnIndex(activeAttribute);

                int filterIndex = -1;
                if (!filterValue.equals(""))
                    filterIndex = dbfFile.getColumnIndex(filterAttribute);

                int numRecords = shapefile.getRecordCount();
                for (int index = 0; index < numRecords; index++)
                {
                    // if the load has been cancelled, break out of the
                    // loop
                    if (isLoadCancelled.cancelled)
                        break;

                    // only accept shapes that pass the filter
                    if (filterIndex != -1)
                    {
                        String value = dbfFile.readColumn(index, filterIndex);
                        if (value == null)
                            continue;
                        if (!value.equals(filterValue))
                            continue;
                    }

                    // get the bounding box of the current shape
                    Rectangle2D.Double boundingBox 
                        = (Rectangle2D.Double)shapefile.getBoundingBox(index);
                    if (boundingBox != null)
                    {
                        // if the shape intersects the visible area, read it
                        if (visibleArea.intersects(
                                boundingBox.x, boundingBox.y,
                                boundingBox.width, boundingBox.height))
                        {
                            GeneralPath path = shapefile.getShape(index,
                                                    visibleArea);
                            if (path != null)
                            {
                                // a valid shape was read, so add it to the 
                                // list of shapes
                                ShapefileEntry entry = new ShapeFileMapLayer.ShapefileEntry();
                                entry.index = index;
                                entry.path = path;
                                entry.isLine = false;
                                if (attributeIndex != -1)
                                {
                                    // get the active attribute
                                    entry.attribute = dbfFile.readColumn(index,
                                                        attributeIndex);
                                }
                                else
                                    entry.attribute = null;
                                shapes.addElement(entry);
                            }
                        }
                    }
                }
            }

            if (!isLoadCancelled.cancelled)
                valid = true;
        }
        catch (IOException e)
        {
            System.out.println("Exception:  I/O Error" + e.toString());
            e.printStackTrace();
        }
        finally
        {
            // make sure the data stream is closed
            if (shapefile != null)
            {
                try {shapefile.close();} catch (Exception e1) {};
                shapefile = null;
            }
            if (dbfFile != null)
            {
                try {dbfFile.close();} catch (Exception e1) {};
                dbfFile = null;
            }
        }

        // if the read was cancelled, forget about the shape vector
        if (isLoadCancelled.cancelled)
            shapes = null;
        else
            needsLoading = false;

        // report that a file was read successfully
        fileReadCallback.incrementFileReadCounter();
    }

    // method to clip the map layer components to the display
    //-------------------------------------------------------
    public void clip(Point upperLeft, int intPixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        if (!valid || (shapes == null))
            return;

        // create a new vector object for the clipped shapes
        clippedShapes = new Vector();

        int ulx = upperLeft.x;
        int uly = upperLeft.y; 
        double pixelSize = applet.imgArea.md.actualPixelSize;

        // get number of shapes available
        int numShapes = shapes.size();

        float[] coords = new float[6];

        // create a 2D rectangle for the display to filter out the shapes
        // that aren't visible
        Rectangle2D dispArea = new Rectangle2D.Double(0.0, 0.0,
                                     dispSize.width, dispSize.height);

        // Process each of the shapes to see if it falls in the visible area
        for (int shapeIndex = 0; shapeIndex < numShapes; shapeIndex++)
        {
            // get the current entry
            ShapefileEntry entry = (ShapefileEntry)shapes.elementAt(shapeIndex);
            PathIterator iter = entry.path.getPathIterator(identityTransform);

            boolean isLine = false;
            GeneralPath transformedPath = new GeneralPath();

            while (!iter.isDone())
            {
                // read the current segment from the path
                int type = iter.currentSegment(coords);

                if (type == PathIterator.SEG_CLOSE)
                    transformedPath.closePath();
                else
                {
                    LatLong latLong = new LatLong(coords[1], coords[0]);

                    // convert the point into the correct projection
                    Point point = proj.latLongToProj(latLong);

                    // translate and scale the point to the coordinate
                    // system currently displayed
                    int tempCoordX = (int)((point.x - ulx)/pixelSize);
                    int tempCoordY = (int)((uly - point.y)/pixelSize);

                    // handle the moveto and lineto types
                    if (type == PathIterator.SEG_MOVETO)
                        transformedPath.moveTo(tempCoordX, tempCoordY);
                    else if (type == PathIterator.SEG_LINETO)
                    {
                        transformedPath.lineTo(tempCoordX, tempCoordY);
                        isLine = true;
                    }
                }
                iter.next();
            }

            // test the visibility
            boolean visible = false;
            if (isLine)
            {
                visible = transformedPath.intersects(dispArea);
            }
            else
            {
                // points, so test each point against the display area
                iter = transformedPath.getPathIterator(identityTransform);
                while (!iter.isDone())
                {
                    int type = iter.currentSegment(coords);
                    visible = dispArea.contains(coords[0], coords[1]);
                    if (visible)
                        break;
                    iter.next();
                }
            }

            // if the shape is visible, add it to the clipped shapes list
            if (visible)
            {
                ShapefileEntry transformedEntry = new ShapeFileMapLayer.ShapefileEntry();
                transformedEntry.isLine = isLine;
                transformedEntry.path = transformedPath;
                transformedEntry.attribute = entry.attribute;
                transformedEntry.index = entry.index;
                clippedShapes.add(transformedEntry);
            }
        }
    }

    // method to find the name associated with a polygon that contains an X/Y
    // coordinate.  It returns the polygon with the smallest bounding box that
    // contains the point and the area of that shape.
    //------------------------------------------------------------------------
    public MapLayerFeatureInfo findFeatureName(int x, int y)
    {
        displayedIndex = -1;

        if (clippedShapes == null)
            return null;

        // track the minimum area found and the feature name for that area
        double minArea = 100000000000.0;
        String foundName = null;
        int foundIndex = -1;

        int numShapes = clippedShapes.size();

        float[] coords = new float[6];

        for (int shapeIndex = 0; shapeIndex < numShapes; shapeIndex++)
        {
            ShapefileEntry entry 
                    = (ShapefileEntry)clippedShapes.elementAt(shapeIndex);
            if (entry.isLine)
            {
                // the shape is a line or polygon
                if (entry.path.contains(x,y))
                {
                    // if this rectangle has the smallest bounding box area
                    // found so far, make it the new choice to report
                    Rectangle bounds = entry.path.getBounds();
                    double area = bounds.width * bounds.height;
                    if (area < minArea)
                    {
                        minArea = area;
                        foundName = entry.attribute;
                        foundIndex = entry.index;
                    }
                }
            }
            else
            {
                // the shape is a point
                PathIterator iter
                    = entry.path.getPathIterator(identityTransform);
                while (!iter.isDone())
                {
                    int type = iter.currentSegment(coords);
                    if (type == PathIterator.SEG_MOVETO)
                    {
                        int x1 = (int)coords[0];
                        int y1 = (int)coords[1];
                        if ((x >= x1 - 3) && (x < x1 + 3) && (y >= y1 - 3)
                            && (y < y1 + 3))
                        {
                            foundName = entry.attribute;
                            foundIndex = entry.index;
                            minArea = 1.0;
                            break;
                        }
                    }
                    iter.next();
                }

                // if a point was found, don't bother looking for any more
                if (foundIndex != -1)
                    break;
            }
        }

        // remember the index that was found in case the use clicks on it
        displayedIndex = foundIndex;

        // return the name found and the area if the shape (null if none found)
        if (foundIndex != -1)
        {
            MapLayerFeatureInfo info = new MapLayerFeatureInfo();
            info.name = foundName;
            // when "none" has been selected as the attribute to display, 
            // just provide the map layer name so that the shape can still be
            // clicked on to update the attribute window
            if (info.name == null)
                info.name = getName();
            info.area = minArea;
            return info;
        }
        else
            return null;
    }

    // method to update the attribute window
    //--------------------------------------
    public void updateAttributeWindow()
    {
        // if the mouse is over a shape in the layer, find read the attributes
        // and update the attribute window
        if (displayedIndex != -1)
        {
            DBFReader dbfFile = null;
            String[] displayedAttributes = null;
            try
            {
                dbfFile = new DBFReader();
                dbfFile.open(filename);
                displayedAttributes = dbfFile.readRecord(displayedIndex);
            }
            catch (IOException e)
            {
                // allow the finally clause to close the file
            }
            finally
            {
                if (dbfFile != null)
                {
                    try {dbfFile.close();} catch (Exception e1) {};
                    dbfFile = null;
                }
            }

            // if the attributes were read successfully, update the attributes
            // window
            if (displayedAttributes != null)
                attributesPanel.setAttributeValues(displayedAttributes);
        }
    }


    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        if (clippedShapes == null)
            return;

        int numShapes = clippedShapes.size();
        if (numShapes > 0)
        {
            Graphics2D g2 = (Graphics2D)g;

            // shapes are available, so translate the origin one pixel 
            // down and left (y increases down) and draw the black 
            // shadow underneath the shapes
            g2.translate(-1,1);
            g2.setColor(Color.BLACK);

            // set the graphics object to draw lines of the correct width
            g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                         BasicStroke.JOIN_ROUND));

            // create a path object for drawing lines
            float[] coords = new float[6];

            // loop twice, once to draw the black shadow and once to draw the
            // actual line
            for (int loops = 0; loops < 2; loops++)
            {
                // draw all the shapes
                for (int i = 0; i < numShapes; i++)
                {
                    ShapefileEntry entry 
                            = (ShapefileEntry)clippedShapes.elementAt(i);
                    if (entry.isLine)
                    {
                        // draw the lines/polygons using the path
                        g2.draw(entry.path);
                    }
                    else
                    {
                        // handle points by stepping through the list of points
                        PathIterator iter
                            = entry.path.getPathIterator(identityTransform);
                        while (!iter.isDone())
                        {
                            int type = iter.currentSegment(coords);
                            if (type == PathIterator.SEG_MOVETO)
                            {
                                int x = (int)coords[0];
                                int y = (int)coords[1];
                                g2.fillOval(x-3,y-3,6,6);
                            }
                            iter.next();
                        }
                    }
                } 

                // on the first pass, set the color for this line type
                if (loops == 0)
                {
                    g2.setColor(color);
                    g2.translate(1,-1);
                }
            }
        }
    }
}
