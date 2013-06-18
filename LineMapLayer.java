// LineMapLayer.java implements the base class for map layers that are 
// made up of lines.
//--------------------------------------------------------------------
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.Hashtable;


public class LineMapLayer extends TiledMapLayer
{
    private imgViewer applet;
    private float lineWidth;          // width to draw the lines
    private Vector clippedPaths;    // vector of general paths clipped to the
                                    // visible area
    private boolean antialias;      // flag to draw lines using antialiasing
    private boolean hasNames;       // flag to indicate layer has feature
                                    // names available in the file
    private AffineTransform identityTransform; // identity transform


    // class to store the name of a feature (if it has a name) and the shape
    // associated with the feature
    class NamedEntry
    {
        String name;            // name associated with the feature
        int numPoints;          // number of points in the path
        GeneralPath path;       // the path representing the feature
    }

    // constructor for the LineMapLayer class
    //---------------------------------------
    LineMapLayer(imgViewer applet, String layerName, String baseFilename, 
                 Color layerColor, float lineWidth, boolean hasNames,
                 boolean antialias, int menuShortcut, boolean lowResAvailable)
    {
        super(applet.imgArea, layerName, baseFilename, layerColor,
              menuShortcut, lowResAvailable);

        this.applet = applet;
        this.lineWidth = lineWidth;
        this.hasNames = hasNames;
        this.antialias = antialias;
        identityTransform = new AffineTransform();
    }

    // method to read the map layer file
    //----------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        // get the list of files to read
        String[] files = fileCache.getFilesToLoad();
        if (files == null)
            return;

        int numFiles = files.length;

        // read the files
        for (int ii = 0; ii < numFiles; ii++)
        {
            // if the load has been cancelled, break out of the loop
            if (isLoadCancelled.cancelled)
                break;

            Vector paths = new Vector(); // vector of general paths for file
            URL CurrentURL;         // URL for the current linework file

            if (applet.verboseOutput)
                System.out.println("Reading " + files[ii]);

            // build a URL for this file
            try
            {
                CurrentURL = new URL(applet.getCodeBase(), files[ii]);
            }
            catch(IOException e)
            {
                System.out.println("Exception: Making map layer URL");
                return;
            }

            // Open the data stream, and read the file
            DataInputStream data = null;
            try
            {
                data = new DataInputStream(
                        new BufferedInputStream(CurrentURL.openStream()));
                byte[] name = null; // buffer for reading feature names into
                int nameSize = 0;   // size of the name buffer

                // if the layer has feature names, allocate space for reading
                // names from the file
                if (hasNames)
                {
                    nameSize = 50;
                    name = new byte[nameSize];
                }

                int numRecs = data.readInt();

                // Read each of the polygons in this file
                for (int i=0; i<numRecs; i++)
                {
                    // if the load has been cancelled, break out of the
                    // loop
                    if (isLoadCancelled.cancelled)
                        break;

                    NamedEntry entry = new LineMapLayer.NamedEntry();
                    entry.numPoints = 0;

                    // if the layer has feature names, read them from the file
                    if (hasNames)
                    {
                        byte readNameSize = data.readByte();

                        // allocate a larger buffer if needed
                        if (readNameSize > nameSize)
                        {
                            nameSize = readNameSize;
                            name = new byte[nameSize];
                        }

                        int numRead = data.read(name,0,readNameSize);

                        // try again if the full name wasn't read.  This is to
                        // work around a bug on netscape 4.7x where it doesn't
                        // read across a buffer boundary when reading bytes
                        if (numRead != readNameSize)
                        {
                            numRead += data.read(name,numRead,
                                                 readNameSize - numRead);
                        }

                        if (numRead != readNameSize)
                        {
                            throw new IOException("Error reading feature name");
                        }

                        // convert the byte array to a string and save it
                        entry.name = new String(name,0,readNameSize);
                    }

                    int numPts = data.readInt();

                    // assume there is only one line/polygon
                    int numPolys = 1;

                    // if the number of points is zero, that is a flag that
                    // the record contains multiply polygons and that the next
                    // integer holds the polygon count
                    if (numPts == 0)
                    {
                        // read the number of polygons since there is more than
                        // one polygon
                        numPolys = data.readInt();
                    }

                    // read all the polygons in this record, converting them
                    // into a general path
                    GeneralPath path = new GeneralPath();
                    for (int poly = 0; poly < numPolys; poly++)
                    {
                        boolean compressFlag = false; // assume not compressed

                        // if more than one polygon or the number of points is
                        // zero (in the unlikely case that the file indicated
                        // multiple polygons, but there was really only one),
                        // read the number of points for the polygon
                        if ((numPolys > 1) || (numPts == 0))
                            numPts = data.readInt();

                        if (numPts < 0)
                        {
                            // delta compression was used
                            compressFlag = true;
                            // fix numPts so we can use it
                            numPts = -numPts;
                        }

                        int x;
                        int y;
                        short deltaX = 0;
                        short deltaY = 0;

                        // read the first point
                        x = data.readInt();
                        y = data.readInt();
                        path.moveTo(x, y);

                        // read the remaining points in for this polygon
                        if (compressFlag == true)
                        {
                            for (int j=1; j<numPts; j++)
                            {
                                // after the first point, each x and y is a
                                // delta from the previous point
                                deltaX = data.readShort();
                                deltaY = data.readShort();
                                x += deltaX;
                                y += deltaY;
                                path.lineTo(x, y);
                            }
                        }
                        else
                        {
                            // not compressed - read points as they are
                            for (int j=1; j<numPts; j++)
                            {
                                x = data.readInt();
                                y = data.readInt();
                                path.lineTo(x, y);
                            }
                        }
                    }
                    // add the entry to the polygon vector
                    entry.path = path;
                    entry.numPoints += numPts;
                    paths.addElement(entry);
                }
                
                data.close();
                data = null;
            }
            catch (IOException e)
            {
                // make sure the data stream is closed
                try {data.close();} catch (Exception e1) {};
                System.out.println("Exception:  I/O Error");
            }
            catch (NumberFormatException e)
            {
                // make sure the data stream is closed
                try {data.close();} catch (Exception e1) {};
                System.out.println("Exception:  Read/Format Error");
            }

            // add whatever paths were read to the file cache as long
            // as the load hasn't been cancelled (the data may not be 
            // complete if the load was cancelled)
            if (!isLoadCancelled.cancelled)
                fileCache.setCacheContents(files[ii],paths);

            // FIXME - test code to allow simulating a slow
            // network connection
/*            if (applet.slowdown)
            {
                try {loadThread.sleep(2000);}
                catch (InterruptedException e) {}
            }
*/

            // report that a file was read successfully
            fileReadCallback.incrementFileReadCounter();
        }

        // purge the unneeded contents of the file cache
        fileCache.purge();
    }

    // method to clip the map layer components to the display
    //-------------------------------------------------------
    public void clip(Point upperLeft, int intPixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        int i,j,ii;             // Loop counters
        int numPts;             // Number of points in a line record
        double[] coords = new double[6];

        // create a new vector object for the clipped paths
        clippedPaths = new Vector();

        int ulx = upperLeft.x;
        int uly = upperLeft.y; 

        double pixelSize = applet.imgArea.md.actualPixelSize;

        // get the cached data
        Vector data = fileCache.getCachedData();

        // create a 2D rectangle for the display to filter out the paths
        // that aren't visible (in projection space)
        Rectangle2D dispAreaProjCoords
            = new Rectangle2D.Double(ulx, uly - dispSize.height * pixelSize,
                                     dispSize.width * pixelSize,
                                     dispSize.height * pixelSize);

        // create a transform to convert the path coordinates from projection
        // space to screen space
        AffineTransform transform = new AffineTransform();
        transform.scale(1.0/pixelSize, -1.0/pixelSize);
        transform.translate(-ulx, -uly);

        // get the needed paths from the cached files
        for (ii = 0; ii < data.size(); ii++)
        {
            Vector cache = (Vector)data.elementAt(ii);
            if (cache == null)
            {
//                System.out.println(LineworkFiles[ii] + " not in cache!");
                continue;
            }

            // get number of paths in the cache
            int numPaths = cache.size();

            // Process each of the paths to see if it falls in the visible
            // area
            for (i = 0; i < numPaths; i++)
            {
                // get the current entry
                NamedEntry entry = (NamedEntry)cache.elementAt(i);

                // test whether the path is visible
                boolean visible = false;
                if (entry.numPoints != 1)
                {
                    // it is a general path, so use the intersects test
                    visible = entry.path.intersects(dispAreaProjCoords);
                }
                else
                {
                    // single point, so use the contains test instead
                    PathIterator iter
                            = entry.path.getPathIterator(identityTransform);
                    int type = iter.currentSegment(coords);
                    visible = dispAreaProjCoords.contains(coords[0], coords[1]);
                }

                // if the path is visible, add it to the clipped list
                if (visible)
                {
                    NamedEntry newEntry = new LineMapLayer.NamedEntry();
                    newEntry.name = entry.name;
                    newEntry.numPoints = entry.numPoints;

                    // transform the path to the screen coordinates
                    newEntry.path = (GeneralPath)entry.path.
                            createTransformedShape(transform);
                    clippedPaths.addElement(newEntry);
                }
            }
        }
    }

    // method to find the name associated with a feature that contains an X/Y
    // coordinate.  It returns the polygon with the smallest bounding box that
    // contains the point.
    //------------------------------------------------------------------------
    public MapLayerFeatureInfo findFeatureName(int x, int y)
    {
        if (!hasNames)
            return null;

        if (clippedPaths == null)
            return null;

        // track the minimum area found and the feature name for that area
        double minArea = 100000000000.0;
        String foundName = null;
        float[] coords = new float[6];

        int numPaths = clippedPaths.size();

        for (int i = 0; i < numPaths; i++)
        {
            NamedEntry entry = (NamedEntry)clippedPaths.elementAt(i);
            if (entry.numPoints != 1)
            {
                if (entry.path.contains(x, y))
                {
                    // if this rectangle has the smallest bounding box area
                    // found so far, make it the new choice to report
                    Rectangle bounds = entry.path.getBounds();
                    double area = bounds.width * bounds.height;
                    if (area < minArea)
                    {
                        minArea = area;
                        foundName = entry.name;
                    }
                }
            }
            else
            {
                // single point
                PathIterator iter
                        = entry.path.getPathIterator(identityTransform);
                int type = iter.currentSegment(coords);
                int x1 = (int)coords[0];
                int y1 = (int)coords[1];
                if ((x >= x1 - 5) && (x < x1 + 5) && (y >= y1 - 5)
                    && (y < y1 + 5))
                {
                    minArea = 1.0;
                    foundName = entry.name;
                }
            }
        }

        // return the name found (null if none found)
        if (foundName != null)
        {
            MapLayerFeatureInfo info = new MapLayerFeatureInfo();
            info.name = foundName;
            info.area = minArea;
            return info;
        }
        else
            return null;
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        if (clippedPaths == null)
            return;
        int numPaths = clippedPaths.size();
        if (numPaths > 0)
        {
            Graphics2D g2 = (Graphics2D)g;
            Object savedRenderingHint = null;
            float[] coords = new float[6];

            // turn on antialiasing for layers that request it.  Can't turn it
            // on for all layers since it significantly slows down drawing.
            if (antialias)
            {
                // save the current antialiasing setting and turn it on
                savedRenderingHint = g2.getRenderingHint(
                                    RenderingHints.KEY_ANTIALIASING);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                    RenderingHints.VALUE_ANTIALIAS_ON);
            }

            // paths are available, so translate the origin one pixel 
            // down and left (y increases down) and draw the black 
            // shadow underneath the paths
            g2.translate(-1,1);
            g2.setColor(Color.BLACK);

            // set the graphics object to draw lines of the correct width
            Stroke savedStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                         BasicStroke.JOIN_ROUND));

            // loop twice, once to draw the black shadow and once to draw the
            // actual line
            for (int loops = 0; loops < 2; loops++)
            {
                // draw all the paths
                for (int i = 0; i < numPaths; i++)
                {
                    NamedEntry entry = (NamedEntry)clippedPaths.elementAt(i);
                    if (entry.numPoints != 1)
                        g2.draw(entry.path);
                    else
                    {
                        // single point
                        PathIterator iter
                                = entry.path.getPathIterator(identityTransform);
                        int type = iter.currentSegment(coords);
                        int x = (int)coords[0];
                        int y = (int)coords[1];
                        g2.fillOval(x-5,y-5,10,10);
                    }
                } 

                // on the first pass, set the color for this line type
                if (loops == 0)
                {
                    g2.setColor(color);
                    g.translate(1,-1);
                }
            }

            // restore the original rendering hint if needed
            if (antialias)
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                    savedRenderingHint);
            }

            // restore the standard stroke
            g2.setStroke(savedStroke);
        }
    }
}
