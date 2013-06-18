// Name: PolygonIntersectTester.java
//
// Description: Implements a class that tests whether two polygons intersect.
//---------------------------------------------------------------------------
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

public class PolygonIntersectTester 
{
    private Polygon ref;    // reference polygon to test other polygons against
    private Rectangle boundingBox; // bounding box of polygon to provide
                                   // for quick test if scene is close
    private int marginSize; // size of the boundingBox margin

    // Constructor for the polygon intersection tester
    //------------------------------------------------
    public PolygonIntersectTester(Polygon refPolygon)
    {
        ref = new Polygon(refPolygon.xpoints, refPolygon.ypoints, 
                          refPolygon.npoints);
    }

    // method to set the boundingBox size.  Note that this should only be
    // called when the polygons to be tested against the reference polygon
    // are all roughly the same shape and size.  If not called, an optimization
    // will not be used.
    //-------------------------------------------------------------------------
    private void setBoundingBoxMargins(int marginSize)
    {
        boundingBox = ref.getBounds();
        boundingBox.grow(marginSize, marginSize);
        this.marginSize = marginSize;
    }

    // method to translate the reference polygon if needed
    //----------------------------------------------------
    public void translate(int x, int y)
    {
        ref.translate(x,y);
    }

    // method to check if two lines intersect
    //---------------------------------------
    public static boolean linesIntersect
    (
        Point line1Start,
        Point line1End,
        Point line2Start,
        Point line2End
    )
    {
        double cross1 = crossProd(line1Start, line1End, line2Start);
        double cross2 = crossProd(line1Start, line1End, line2End);

        double result = cross1 * cross2;

        // if the result is non positive, the target line straddles this
        // line, so then test if this line straddles target
        if (result <= 0)
        {
            cross1 = crossProd(line2Start, line2End, line1Start);
            cross2 = crossProd(line2Start, line2End, line1End);

            result = cross1 * cross2;

            // if both lines straddle each other, it must intersect
            if (result <= 0)
                return true;
        }
        return false;
    }

    // method to calculate the cross product between two vectors.  Create
    // one vector from A to B, and the other from A to C.
    //-------------------------------------------------------------------
    private static final double crossProd(Point A, Point B, Point C)
    {
        double vectorAx = (double)(A.x-B.x);
        double vectorAy = (double)(A.y-B.y);
        double vectorBx = (double)(A.x-C.x);
        double vectorBy = (double)(A.y-C.y);

        double result = (vectorAx * vectorBy) - 
            (vectorAy * vectorBx);
        return result;
    }

    // method to see if scene is within polygon
    //-----------------------------------------
    public boolean intersects(Polygon poly)
    {
        Point polyCenter = new Point((poly.xpoints[0] + poly.xpoints[2])/2,
                                     (poly.ypoints[0] + poly.ypoints[2])/2);

        // calculate the size of the polygon
        int maxX = poly.xpoints[0];
        int minX = maxX;
        int maxY = poly.ypoints[0];
        int minY = maxY;
        for (int i = 1; i < poly.npoints; i++)
        {
            maxX = Math.max(maxX, poly.xpoints[i]);
            minX = Math.min(minX, poly.xpoints[i]);
            maxY = Math.max(maxY, poly.ypoints[i]);
            minY = Math.min(minY, poly.ypoints[i]);
        }
        int ySize = (maxY - minY + 1)/2;
        int xSize = (maxX - minX + 1)/2;
        int tempSize;
        if (ySize > xSize)
            tempSize = ySize;
        else
            tempSize = xSize;

        // if the bounding box isn't defined yet, or the polygon is larger
        // than the previous margin, set the bounding box
        if ((boundingBox == null) || (tempSize > marginSize))
            setBoundingBoxMargins(tempSize);

        // if the polygon isn't even near the reference polygon, they can't 
        // intersect
        if (!boundingBox.contains(polyCenter))
            return false;

        // if the reference polygon contains the polygon center, they intersect
        if (ref.contains(polyCenter))
            return true;

        // check if polygon contains the upper left corner of the reference
        // area (or potentially the entire reference polygon), they intersect.
        // (a degenerate case in case the reference polygon is a single point)
        if (poly.contains(ref.xpoints[0], ref.ypoints[0]))
            return true;

        // go through the lines of the reference and test polygon to detect an  
        // intersect
        int[] testX = poly.xpoints;
        int[] testY = poly.ypoints;
        Point refStart = new Point();
        Point refEnd;
        Point testStart = new Point();
        Point testEnd;
        refEnd = new Point(ref.xpoints[ref.npoints-1], 
                           ref.ypoints[ref.npoints-1]);
        testEnd = new Point(testX[poly.npoints-1], testY[poly.npoints-1]);

        for (int i = 0; i < ref.npoints; i++)
        {
            refStart.setLocation(refEnd);
            refEnd.setLocation(ref.xpoints[i], ref.ypoints[i]);

            for (int j = 0; j < poly.npoints; j++)
            {
                testStart.setLocation(testEnd);
                testEnd.setLocation(testX[j], testY[j]);

                if (linesIntersect(refStart, refEnd, testStart, testEnd))
                    return true;
            }
        }

        return false;
    }
}
