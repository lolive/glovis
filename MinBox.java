// MinBox.java implements a routine to calculate the minimum lat/long bounding
// box for a given area in projection coordinates
//----------------------------------------------------------------------------
import java.awt.Point;

class MinBox
{
    static void calculateMinBox
    (
        Point ulMeters,     // I: upper left coordinates
        Point lrMeters,     // I: lower right coordinates
        ProjectionTransformation proj,  // I: projection transformation object
        LatLong ulLatLong,  // O: minimum bounding box upper left lat/long
        LatLong lrLatLong   // O: minimum bounding box lower right lat/long
    )
    {
        // find the lat/long bounding box for the display area by converting
        // each corner of the display area to lat/long and finding the smallest
        // box that will fit around the area.  Start with the upper left corner.
        LatLong latLong = proj.projToLatLong(ulMeters.x,ulMeters.y);
        ulLatLong.latitude = latLong.latitude;
        ulLatLong.longitude = latLong.longitude;
        lrLatLong.latitude = latLong.latitude;
        lrLatLong.longitude = latLong.longitude;
       
        // factor in the lower right corner
        latLong = proj.projToLatLong(lrMeters.x,lrMeters.y);
        ulLatLong.longitude = Math.min(ulLatLong.longitude,latLong.longitude);
        ulLatLong.latitude = Math.max(ulLatLong.latitude,latLong.latitude);
        lrLatLong.longitude = Math.max(lrLatLong.longitude,latLong.longitude);
        lrLatLong.latitude = Math.min(lrLatLong.latitude,latLong.latitude);

        // factor in the lower left corner
        latLong = proj.projToLatLong(ulMeters.x,lrMeters.y);
        ulLatLong.longitude = Math.min(ulLatLong.longitude,latLong.longitude);
        ulLatLong.latitude = Math.max(ulLatLong.latitude,latLong.latitude);
        lrLatLong.longitude = Math.max(lrLatLong.longitude,latLong.longitude);
        lrLatLong.latitude = Math.min(lrLatLong.latitude,latLong.latitude);

        // factor in the upper right corner
        latLong = proj.projToLatLong(lrMeters.x,ulMeters.y);
        ulLatLong.longitude = Math.min(ulLatLong.longitude,latLong.longitude);
        ulLatLong.latitude = Math.max(ulLatLong.latitude,latLong.latitude);
        lrLatLong.longitude = Math.max(lrLatLong.longitude,latLong.longitude);
        lrLatLong.latitude = Math.min(lrLatLong.latitude,latLong.latitude);
    }

}
