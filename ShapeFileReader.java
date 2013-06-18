//---------------------------------------------------------------------------
// Name: ShapeFileReader
//
// Description: Implements a class to allow reading the shape information
//  from a shapefile.
//
// References: http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf
//---------------------------------------------------------------------------
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class ShapeFileReader
{
    // define the constants present in the file for each type of supported 
    // shape
    private static final int NULL = 0;
    private static final int POINT = 1;
    private static final int MULTIPOINT = 8;
    private static final int POLYLINE = 3;
    private static final int POLYGON = 5;

    private static final int HEADER_SIZE = 100; // size of the file header

    private FileChannel shapeChannel;       // IO channel for the .shp file
    private FileChannel indexChannel;       // IO channel for the .shx file
    private FileInputStream shapeStream;    // IO stream for the .shp file
    private FileInputStream indexStream;    // IO stream for the .shx file
    private Rectangle2D extent;             // bounding box of the shapefile
    private int numRecords;                 // records in the shapefile

    // open the indicated shape file.  Throws an IOException if there is an
    // error opening the file.
    //---------------------------------------------------------------------
    public void open
    (
        String prefix   // I: name of shapefile without the extension
    ) throws IOException
    {
        // open the shapefile and the associated index file
        File shapeFile = new File(prefix + ".shp");
        shapeStream = new FileInputStream(shapeFile);
        shapeChannel = shapeStream.getChannel();
        File indexFile = new File(prefix + ".shx");
        indexStream = new FileInputStream(indexFile);
        indexChannel = indexStream.getChannel();

        // get the number of records in the file by using the size of the
        // shape index file.  The number of records is the file size minus the
        // header size, then divided by 8 for 8 bytes per record.
        ByteBuffer indexHeader = ByteBuffer.allocate(HEADER_SIZE);
        if (indexChannel.read(indexHeader, 0) != HEADER_SIZE)
        {
            throw new IOException("Error reading header from " + prefix 
                                  + ".shx");
        }
        indexHeader.order(ByteOrder.BIG_ENDIAN);
        numRecords = (indexHeader.getInt(24) * 2 - HEADER_SIZE) / 8;
        indexHeader = null;

        // read the header to get the extents of the shapefile contents
        ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE);
        if (shapeChannel.read(headerBuffer, 0) != HEADER_SIZE)
        {
            throw new IOException("Error reading header from " + prefix 
                                  + ".shp");
        }
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
        extent = new Rectangle2D.Double(headerBuffer.getDouble(36),
                    headerBuffer.getDouble(44),
                    headerBuffer.getDouble(52) - headerBuffer.getDouble(36),
                    headerBuffer.getDouble(60) - headerBuffer.getDouble(44));
        headerBuffer = null;
    }

    // return the number of records in the file
    //-----------------------------------------
    public int getRecordCount()
    {
        return numRecords;
    }

    // method to close the shapefile.  Throws an IOException on an error.
    //-------------------------------------------------------------------
    public void close() throws IOException
    {
        if (shapeChannel != null)
            shapeChannel.close();
        if (shapeStream != null)
            shapeStream.close();
        if (indexChannel != null)
            indexChannel.close();
        if (indexStream != null)
            indexStream.close();
        shapeChannel = null;
        shapeStream = null;
        indexChannel = null;
        indexStream = null;
    }

    // helper method to return the number of bytes in a record at a particular
    // position in the file.  Throws an IOException on error.
    //------------------------------------------------------------------------
    private int getRecordLength(int recordPosition) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        if (shapeChannel.read(buffer, recordPosition) != 8)
            throw new IOException("Error reading record length");

        buffer.order(ByteOrder.BIG_ENDIAN);
        int length = buffer.getInt(4) * 2;
        buffer = null;

        return length;
    }

    // helper method to return the offset in a file to a particular record.
    // Throws an IOException on error.
    //---------------------------------------------------------------------
    private int getRecordPosition(int recordIndex) throws IOException
    {
        int indexPos = HEADER_SIZE + (recordIndex * 8);
        ByteBuffer indexBuffer = ByteBuffer.allocate(8);
        if (indexChannel.read(indexBuffer, indexPos) != 8)
            throw new IOException();

        indexBuffer.order(ByteOrder.BIG_ENDIAN);
        int position = indexBuffer.getInt(0) * 2;
        indexBuffer = null;

        return position;
    }

    // method to extract a single shape point from a buffer at the byte at the
    // index
    //------------------------------------------------------------------------
    private void getPoint(ByteBuffer buffer, int index, double[] point)
    {
        point[0] = buffer.getDouble(index); 
        point[1] = buffer.getDouble(index + 8); 
    }

    // method to return the bounding box for a shape at a given index
    //---------------------------------------------------------------
    public Rectangle2D getBoundingBox(int index) throws IOException
    {
        int recordPosition = getRecordPosition(index);
        int recordLength = getRecordLength(recordPosition);

        // only read enough of the record to get the bounding box
        if (recordLength > 36)
            recordLength = 36;

        ByteBuffer buffer = ByteBuffer.allocate(recordLength);

        if (shapeChannel.read(buffer, recordPosition + 8) != recordLength)
            throw new IOException();

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        Rectangle2D boundingBox = null;
        int shapeType = buffer.getInt(0);
        switch (shapeType)
        {
            case NULL:
                // no bounding box for NULL shapes
                break;

            case POINT:
                // no bounding box for POINT shapes, but dummy one up using the
                // point itself
                double[] point = new double[2];
                getPoint(buffer, 4, point);
                boundingBox 
                    = new Rectangle2D.Double(point[0], point[1], 0.1, 0.1);
                break;

            default:
                double xMin = buffer.getDouble(4);
                double yMin = buffer.getDouble(12);

                // make sure the width and height are reasonable (if they are 
                // zero, the bounding box checks fail)
                double width = buffer.getDouble(20) - xMin;
                if (width < 0.1)
                    width = 0.1;
                double height = buffer.getDouble(28) - yMin;
                if (height < 0.1)
                    height = 0.1;

                boundingBox = new Rectangle2D.Double(xMin, yMin,
                                             width, height);
                break;
        }
        buffer = null;

        return boundingBox;
    }

    // method to read the shape at a given index from the shapefile.  Throws
    // an IOException on error.
    // The bounds parameter allows the shapes to be filtered against a
    // bounding box so that shapes that are outside the bounding box are
    // quickly filtered out.
    //----------------------------------------------------------------------
    public GeneralPath getShape
    (
        int index,          // I: index of shape to read
        Rectangle2D bounds  // I: bounding box of area of interest
    )
        throws IOException
    {
        // get the requested shape's record position and record length
        int recordPosition = getRecordPosition(index);
        int recordLength = getRecordLength(recordPosition);

        // read the record from the file
        ByteBuffer buffer = ByteBuffer.allocate(recordLength);
        if (shapeChannel.read(buffer, recordPosition + 8) != recordLength)
            throw new IOException();

        // data in the shape record is in little endian byte order
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int shapeType = buffer.getInt(0);
        if (shapeType == NULL)
        {
            // for NULL shapes, bail out early
            buffer = null;
            return null;
        }

        // declare variables needed for record processing
        GeneralPath ret = null;
        double[] point = new double[2];
        double[] prevUnusedPoint = new double[2];
        boolean prevOutside = false;
        boolean prevUnused = false;
        boolean pointsAdded = false;

        switch (shapeType) 
        {
            case NULL:
                return null;

            case POINT:
                // for a POINT shape, the point is 4 bytes into the buffer
                getPoint(buffer, 4, point);
                buffer = null;
                if (bounds.contains(point[0], point[1]))
                {
                    // the point is within the requested bounds, so add it to
                    // the returned path
                    ret = new GeneralPath();
                    ret.moveTo((float)point[0], (float)point[1]);
                }
                return ret;

            case MULTIPOINT:
                int nPoints = buffer.getInt(32);
                ret = new GeneralPath();

                // read all the points in this shape
                for (int i = 0; i < nPoints; i++)
                {
                    // for MULTIPOINT, the points start 40 bytes into the buffer
                    // and each take up 16 bytes
                    getPoint(buffer, 40 + (16 * i), point);
                    if (bounds.contains(point[0], point[1]))
                    {
                        // the point is within the bounds of interest, so add it
                        ret.moveTo((float)point[0], (float)point[1]);
                        pointsAdded = true;
                    }
                }

                buffer = null;
                if (!pointsAdded)
                    ret = null;
                return ret;

            case POLYLINE:
                int nParts = buffer.getInt(36);
                ret = new GeneralPath();

                // get the starting indices for each of the parts making up the
                // polyline
                int[] parts = new int[nParts+1];
                for (int i = 0; i < nParts; i++)
                {
                    // the part offsets start at byte 44 in the buffer and take
                    // up 4 bytes each
                    parts[i] = buffer.getInt(44 + (i * 4));
                }
                parts[nParts] = (recordLength - 44 - nParts * 4) / 16;

                // the offset to the points for the polyline start right after
                // the parts information (44 bytes + 4 bytes for each part)
                int offset = 44 + nParts * 4;

                // read each of the polyline parts
                for (int i = 0; i < nParts; i++)
                {
                    prevOutside = false;
                    prevUnused = false;

                    // read the starting point
                    int pointIndex = offset + parts[i] * 16;
                    getPoint(buffer, pointIndex, point);
                    pointIndex += 16;

                    // add the point to the return path if it is in the bounds
                    // provided, otherwise, keep track of the point in case 
                    // it needs to be added later
                    if (bounds.contains(point[0], point[1]))
                    {
                        ret.moveTo((float)point[0], (float)point[1]);
                        pointsAdded = true;
                    }
                    else
                    {
                        prevOutside = true;
                        prevUnused = true;
                        prevUnusedPoint[0] = point[0];
                        prevUnusedPoint[1] = point[1];
                    }

                    // calculate the final index of the current part
                    int endIndex = offset + parts[i+1] * 16;

                    // process the rest of the points.  Ignore the parts of
                    // the line that fall outside the bounds of interest
                    while(pointIndex < endIndex)
                    {
                        getPoint(buffer, pointIndex, point);
                        if (bounds.contains(point[0], point[1]))
                        {
                            if (prevUnused)
                            {
                                ret.moveTo((float)prevUnusedPoint[0],
                                           (float)prevUnusedPoint[1]);
                                prevUnused = false;
                            }
                            prevOutside = false;
                            ret.lineTo((float)point[0], (float)point[1]);
                            pointsAdded = true;
                        }
                        else
                        {
                            if (!prevOutside)
                            {
                                ret.lineTo((float)point[0], (float)point[1]);
                                prevOutside = true;
                                prevUnused = false;
                            }
                            else
                            {
                                prevUnused = true;
                                prevUnusedPoint[0] = point[0];
                                prevUnusedPoint[1] = point[1];
                            }
                        }
                        pointIndex += 16;
                    }
                }

                buffer = null;
                if (!pointsAdded)
                    ret = null;
                return ret;

            case POLYGON:
                nParts = buffer.getInt(36);
                ret = new GeneralPath();
                boolean pointsSkipped = false;

                // get the starting indices for each of the parts making up the
                // polygon
                parts = new int[nParts+1];
                for (int i = 0; i < nParts; i++)
                {
                    // the part offsets start at byte 44 in the buffer and take
                    // up 4 bytes each
                    parts[i] = buffer.getInt(44 + (i * 4));
                }
                parts[nParts] = (recordLength - 44 - nParts * 4) / 16;

                // the offset to the points for the polygon start right after
                // the parts information (44 bytes + 4 bytes for each part)
                offset = 44 + nParts * 4;

                // read each of the polygon parts
                for (int i = 0; i < nParts; i++)
                {
                    prevOutside = false;
                    prevUnused = false;
                    
                    // read the starting point
                    int pointIndex = offset + parts[i] * 16;
                    getPoint(buffer, pointIndex, point);
                    pointIndex += 16;

                    // add the point to the return path if it is in the bounds
                    // provided, otherwise, keep track of the point in case 
                    // it needs to be added later
                    if (bounds.contains(point[0], point[1]))
                    {
                        ret.moveTo((float)point[0], (float)point[1]);
                        pointsAdded = true;
                    }
                    else
                    {
                        pointsSkipped = true;
                        prevOutside = true;
                        prevUnused = true;
                        prevUnusedPoint[0] = point[0];
                        prevUnusedPoint[1] = point[1];
                    }

                    // calculate the final index of the current part
                    int endIndex = offset + parts[i+1] * 16;

                    // process the rest of the points.  Ignore the parts of
                    // the polygon that fall outside the bounds of interest
                    while(pointIndex < endIndex)
                    {
                        getPoint(buffer, pointIndex, point);
                        if (bounds.contains(point[0], point[1]))
                        {
                            if (prevUnused)
                            {
                                ret.moveTo((float)prevUnusedPoint[0],
                                           (float)prevUnusedPoint[1]);
                                prevUnused = false;
                            }
                            prevOutside = false;
                            ret.lineTo((float)point[0], (float)point[1]);
                            pointsAdded = true;
                        }
                        else
                        {
                            if (!prevOutside)
                            {
                                ret.lineTo((float)point[0], (float)point[1]);
                                prevOutside = true;
                                prevUnused = false;
                            }
                            else
                            {
                                pointsSkipped = true;
                                prevUnused = true;
                                prevUnusedPoint[0] = point[0];
                                prevUnusedPoint[1] = point[1];
                            }
                        }
                        pointIndex += 16;
                    }
                }
                
                buffer = null;

                if (!pointsSkipped)
                    ret.closePath();

                if (!pointsAdded)
                    ret = null;
                return ret;

            default:
                // an unsupport shape type was encountered
                throw new RuntimeException("Unknown shape type");
        }
    }

    // return the bounding box of the shape file
    //------------------------------------------
    public Rectangle2D getExtent()
    {
        return extent;
    }
}
