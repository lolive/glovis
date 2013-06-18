// PolarStereographicProjection.java implements support for the Polar
// Stereographic projection.
//------------------------------------------------------------------------
import java.awt.Point;

public class PolarStereographicProjection implements ProjectionTransformation
{
    private double HALF_PI;     // PI / 2.0
    private double TWO_PI;      // PI * 2.0
    private double EPSLN;       // Test for convergence -- practical "zero"
    private double R2D;         // Radians to Degrees conversion factor
    private double D2R;         // Degrees to Radians conversion factor

    private double r_major;     // major axis
    private double r_minor;     // minor axis
    private double es;          // eccentricity squared
    private double e;           // eccentricity
    private double e4;          // e4 calculated from eccentricity*/
    private double center_lon;  // center longitude
    private double center_lat;  // center latitude
    private double fac;         // sign variable
    private double ind;         // flag variable
    private double mcs;         // small m
    private double tcs;         // small t
    private double false_northing;// y offset in meters
    private double false_easting; // x offset in meters


    // Polar Stereographic projection constructor
    //-------------------------------------------
    PolarStereographicProjection
    (
        double r_maj,           // ellipsoid major axis
        double r_min,           // ellipsoid minor axis
        double c_lon,
        double c_lat,
        double false_east,
        double false_north
    )
    {
        HALF_PI = Math.PI / 2.0;
        TWO_PI = Math.PI * 2.0;
        EPSLN = 1.0e-10;
        R2D     = 57.2957795131;
        D2R     = 0.01745329252;


        double temp;        // temporary variable
        double con1;        // temporary angle
        double sinphi;      // sin value
        double cosphi;      // cos value

        this.r_major = r_maj;
        this.r_minor = r_min;
        this.false_northing = false_north;
        this.false_easting = false_east;
        this.center_lon = c_lon * D2R;
        this.center_lat = c_lat * D2R;

        temp = r_minor / r_major;
        es = 1.0 - (temp * temp);
        e = Math.sqrt(es);
        e4 = e4fn(e);

        if (center_lat < 0)
           fac = -1.0;
        else
           fac = 1.0;
        ind = 0;
        if (Math.abs(Math.abs(center_lat) - HALF_PI) > EPSLN)
        {
            ind = 1;
            con1 = fac * center_lat; 
            sinphi = Math.sin(con1);
            cosphi = Math.cos(con1);

            mcs = msfnz(e,sinphi,cosphi);
            tcs = tsfnz(e,con1,sinphi);
        }
    }

    // function to compute the constant e4 from the input of the eccentricity
    // of the spheroid, x
    //-----------------------------------------------------------------------
    private double e4fn(double x)
    {
        double con;
        double com;
        con = 1.0 + x;
        com = 1.0 - x;
        return Math.sqrt(Math.pow(con, con) * Math.pow(com, com));
    }

    // function to compute the constant small m which is the radius of a
    // parallel of latitude, phi, divided by the semimajor axis.
    //------------------------------------------------------------------
    private double msfnz(double eccent, double sinphi, double cosphi)
    {
        double con;

        con = eccent * sinphi;
        return cosphi / Math.sqrt(1.0 - con * con);
    }

    // function to compute the constant small t
    //-----------------------------------------
    private double tsfnz(double eccent, double phi, double sinphi)
    {
        double con;
        double com;
      
        con = eccent * sinphi;
        com = 0.5 * eccent; 
        con = Math.pow(((1.0 - con) / (1.0 + con)), com);
        return Math.tan(.5 * (HALF_PI - phi))/con;
    }

    // method to convert a projection coordinate in meters to lat/long in 
    // degrees
    //-------------------------------------------------------------------
    public LatLong projToLatLong(int xCoord, int yCoord)
    {
        double rh;      // height above ellipsiod
        double ts;      // small value t
        double temp;    // temporary variable
        long   flag;    // error flag

        flag = 0;
        double x = (xCoord - false_easting) * fac;
        double y = (yCoord - false_northing) * fac;
        rh = Math.sqrt(x * x + y * y);
        if (ind != 0)
            ts = rh * tcs/(r_major * mcs);
        else
            ts = rh * e4 / (r_major * 2.0);

        double phi2z_result = phi2z(e, ts);
        // if phi2z fails, return null
        if (phi2z_result > 100.0)
            return null;

        double lat = fac * phi2z_result;
        double lon;
        if (rh == 0)
            lon = fac * center_lon;
        else
        {
            temp = Math.atan2(x, -y);
            lon = adjust_lon(fac * temp + center_lon);
        }

        // convert the result to degrees
        lat *= R2D;
        lon *= R2D;

        return new LatLong(lat, lon);
    }

    // function to compute the latitude angle, phi2
    //---------------------------------------------
    double phi2z
    (
        double eccent,      // Spheroid eccentricity
        double ts           // Constant value t
    )
    {
        double eccnth;
        double phi;
        double con;
        double dphi;
        double sinpi;
        long i;

        eccnth = .5 * eccent;
        phi = HALF_PI - 2 * Math.atan(ts);
        for (i = 0; i <= 15; i++)
        {
            sinpi = Math.sin(phi);
            con = eccent * sinpi;
            dphi = HALF_PI - 2 
                 * Math.atan(ts *(Math.pow(((1.0 - con)/(1.0 + con)),eccnth))) 
                 - phi;
            phi += dphi; 
            if (Math.abs(dphi) <= .0000000001)
                return phi;
        }

        System.out.println("Polar Stereographic convergence error");

        // return a bogus latitude that can be detected
        return 200.0;
    }

    // method to convert a lat/long in degrees to a projection coordinate
    // in meters
    // NOTE: this code is a direct translation of the code in gctp
    //-------------------------------------------------------------------
    public Point latLongToProj(LatLong latLong)
    {
        double con1;        // adjusted longitude
        double con2;        // adjusted latitude
        double rh;          // height above ellipsoid
        double sinphi;      // sin value
        double ts;          // value of small t

        double lat = latLong.latitude * D2R;
        double lon = latLong.longitude * D2R;

        con1 = fac * adjust_lon(lon - center_lon);
        con2 = fac * lat;
        sinphi = Math.sin(con2);
        ts = tsfnz(e,con2,sinphi);
        if (ind != 0)
           rh = r_major * mcs * ts / tcs;
        else
           rh = 2.0 * r_major * ts / e4;
        double x = fac * rh * Math.sin(con1) + false_easting;
        double y = -fac * rh * Math.cos(con1) + false_northing;;

        int int_x = (int)Math.round(x);
        int int_y = (int)Math.round(y);
        return new Point(int_x,int_y);
    }

    // Function to return the sign of an argument
    //-------------------------------------------
    private int sign(double x) 
    { 
        if (x < 0.0) 
            return -1;
        else 
            return 1;
    }

    // Function to adjust longitude to -180 to 180
    //--------------------------------------------
    private double adjust_lon(double x) 
    {
        x = (Math.abs(x) < Math.PI) ? x : (x - (sign(x) * TWO_PI));
        return x;
    }
/*
    public static void main(String[] args)
    {
        PolarStereographicProjection proj = new PolarStereographicProjection(
            6378137.0, 6356752.3142, 0.0, -71.0, 0, 0);

        LatLong latLong = new LatLong(-69.8103561, -0.3459923);
        Point coords = proj.latLongToProj(latLong);
        System.out.println("" + coords.x + " " + coords.y);

        latLong = new LatLong(-69.6883698, 6.3553472);
        coords = proj.latLongToProj(latLong);
        System.out.println("" + coords.x + " " + coords.y);

        latLong = new LatLong(-72.0665436, -0.3903506);
        coords = proj.latLongToProj(latLong);
        System.out.println("" + coords.x + " " + coords.y);

        latLong = new LatLong(-71.928154, 7.1621776);
        coords = proj.latLongToProj(latLong);
        System.out.println("" + coords.x + " " + coords.y);

        LatLong ll = proj.projToLatLong(-13380, 2215680);
        System.out.println("" + ll.latitude + " " + ll.longitude);

        ll = proj.projToLatLong(246780, 2215680);
        System.out.println("" + ll.latitude + " " + ll.longitude);

        ll = proj.projToLatLong(-13380, 1963890);
        System.out.println("" + ll.latitude + " " + ll.longitude);

        ll = proj.projToLatLong(246780, 1963890);
        System.out.println("" + ll.latitude + " " + ll.longitude);
    }
*/
}
