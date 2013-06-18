// Metadata class for maintaining the metadata for a single scene
// as read from the TOC data line
//---------------------------------------------------------------
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.util.StringTokenizer;
import java.lang.Math;

public class Metadata
{
    int date;           // Date in YYYYMMDD format
    int gridCol;        // column in inventory grid (equivalent of WRS path)
    int gridRow;        // row in inventory grid (equivalent of WRS row)
    int year;           // Year (obtained by dividing date by 10000
    int month;          // Month (1-12, extracted from date)
    int jDate;          // Julian day of the year
    int ulX;            // upper-left X coordinate (in meters)
    int ulY;            // upper-left Y coordinate (in meters)
    int cloudCover;     // cloud cover percentage
    int browseNumber;   // browse number for sensors that have multiple browse
    int downloadFileSize; // file size if the scene is downloadable
    int[] quality;      // array of quality values for this scene 
    String dataVersion; // data version for sensors that have data versions
    String entityID;    // entity ID for ordering (usually scene ID)
    String secondaryID; // entity ID for the MRLC and MODIS Datasets
    String projectName; // project name (optional)
    int[] sampOffset;   // sample offset to data in this scene (ul,ur,lr,ll)
    int[] lineOffset;   // line offset to data in this scene (ul,ur,lr,ll)
    Point centerXY;     // scene center X/Y coordinate
    Polygon screenLocation; // location on the screen for this scene - only
                            // valid for some sensors
    Image image;        // image for this scene if loaded
    int imageRes;       // resolution of image
    String lookAngle;   // angle of the image
    String level1;      // Level 1 Product name
    boolean isDownloadable; // true if this scene is directly downloadable
                        // Basically, a shortcut for downloadFileSize > 0
    boolean visible;    // flag that this scene is visible (if false, it has
                        // been filtered out due to temporary constraints).
                        // Basically, a shortcut for filterFlags == 0
    private int filterFlags; // integer to hold a set of bit flags indicating 
                        // whether the scene has been filtered out due to
                        // any number of conditions.
    private Sensor sensor; // sensor for this scene
    private LatLong[] sceneCorners; // corners of the scene in lat/long (not
                                    // always filled in)

    // tables to help convert a date to a julian day
    private static final int[] monthToJDate = 
        {0,31,59,90,120,151,181,212,243,273,304,334};
    private static final int[] leapYearMonthToJDate = 
        {0,31,60,91,121,152,182,213,244,274,305,335};

    // bit definitions for the different filters in filterFlags
    final static int VIEWPORT_FILTER          = (int) Math.pow(2, 0);
    final static int CLOUD_COVER_FILTER       = (int) Math.pow(2, 1);
    final static int DATE_FILTER              = (int) Math.pow(2, 2);
    final static int SCENE_LIST_FILTER        = (int) Math.pow(2, 3);
    final static int QUALITY_FILTER           = (int) Math.pow(2, 4);
    final static int DATA_VERSION_FILTER      = (int) Math.pow(2, 5);
    final static int HIDDEN_SCENE_FILTER      = (int) Math.pow(2, 6);
    final static int USER_DEFINED_AREA_FILTER = (int) Math.pow(2, 7);
    final static int GRID_COL_ROW_FILTER      = (int) Math.pow(2, 8);
    final static int DOWNLOADABLE_FILTER      = (int) Math.pow(2, 9);

    // high image quality value
    final static int IMAGE_QUALITY_MAX = 9;

    // Constructor for the metadata object.  the dataLine format is as follows:
    //    date in format YYYYMMDD
    //    julian day of year if julian date available for the sensor
    //    ulX coordinate
    //    ulY coordinate
    //    cloud cover percentage
    //    entity ID
    //    secondary ID (ID vary by sensor)
    //    quality values (number vary by sensor)
    //    offsets for each scene if the sensor has them (sample,line order)
    //    scene number if sensor hasMultipleBrowse
    //    download file size if the sensor isDownloadable
    //    look angle if the sensor hasLookAngle
    //    level 1 product name if the sensor hasLevel1
    //-------------------------------------------------------------------------
    Metadata(String dataLine, Sensor sensor, int gridCol, int gridRow)
    {
        this.sensor = sensor;
        this.gridCol = gridCol;
        this.gridRow = gridRow;
        visible = true;
        isDownloadable = false;
        StringTokenizer st = new StringTokenizer(dataLine,",");

        if (sensor.hasAcqDate)
        {
            date = Integer.parseInt(st.nextToken());
            year = date/10000;
            month = (date - year * 10000)/100;
       
            // read the julian date just for sensors that have it in the
            // metadata
            if (sensor.hasJulianDateMetadata)
                jDate = Integer.parseInt(st.nextToken());
            else
            {
                // the sensor doesn't have the julian date, so calculate it
                // since it is used at times
                if (((year % 4) != 0) 
                    || (((year % 100) == 0) && ((year % 400) != 0)))
                {
                    // non-leap year
                    jDate = monthToJDate[month-1] 
                            + date - year * 10000 - month * 100;
                }
                else
                {
                    // leap year
                    jDate = leapYearMonthToJDate[month-1] 
                            + date - year * 10000 - month * 100;
                }

            }
        }
        if (sensor.hasUpperLeftInToc)
        {
            ulX = Integer.parseInt(st.nextToken());
            ulY = Integer.parseInt(st.nextToken());
        }
        if (sensor.hasCloudCover)
            cloudCover = Integer.parseInt(st.nextToken());
        else
            cloudCover = 0;
        quality = null;
        
        if (sensor.hasSecondaryIDMetadata)
        {
            entityID = st.nextToken();
            secondaryID = st.nextToken();
        }
        else
        {
            entityID = st.nextToken();
        }
        if (sensor.numQualityValues > 0)
        {
            quality = new int[sensor.numQualityValues];
            for (int i = 0; i < sensor.numQualityValues; i++)
            {
                String qual = st.nextToken().trim();
                quality[i] = Character.digit(qual.charAt(0),16);
                if (quality[i] > IMAGE_QUALITY_MAX)
                    quality[i] = IMAGE_QUALITY_MAX;
            }
        }
        if (sensor.hasProjectName)
            projectName = st.nextToken();
        else
            projectName = null;

        if (!sensor.hasConstantOffsets)
        {
            // read the offsets to the data in the image 
            sampOffset = new int[4];
            lineOffset = new int[4];

            for (int i = 0; i < 4; i++)
            {
                sampOffset[i] = Integer.parseInt(st.nextToken());
                lineOffset[i] = Integer.parseInt(st.nextToken());
            }
        }

        // get the browse number
        if (sensor.hasMultipleBrowse)
        {
            if (st.hasMoreTokens())
            {
                browseNumber = Integer.parseInt(st.nextToken());
            }
            else
            {
                browseNumber = 0;
            }
        }
        else
        {
             browseNumber = 0;
        }

        // get the download file size if available for this sensor
        if (sensor.isDownloadable)
        {
            // check whether there is more data on the input line
            if (st.hasMoreTokens())
            {
                downloadFileSize = Integer.parseInt(st.nextToken()); 
                if (downloadFileSize > 0)
                    isDownloadable = true;
            }
        }
        
        // create new string object for the look angle
        lookAngle = new String();
        if (sensor.hasLookAngle)
        {
            // check if the look angle is there (if the value is null in the
            // database, there will be no value in the TOC line)
            if (st.hasMoreTokens())
            {
                lookAngle = new String(st.nextToken());
            }
        }

        // create new string object for the Level 1 product
        level1 = new String();
        if (sensor.hasLevel1)
        {
            // check if the level 1 product is there (may have spaces, BTW)
            // (this field is the  most-recently added so will be at the end
            // if this scene has been updated since the field was added)
            if (st.hasMoreTokens())
            {
                String temp = new String (st.nextToken());
                if (!temp.equalsIgnoreCase("PR"))
                {
                    level1 = temp;
                }
            }
        }

        // complete the metadata if needed
        if (!sensor.hasUpperLeftInToc || sensor.hasConstantOffsets)
            sensor.completeMetadata(this);

        sceneCorners = null;
    }

    // copy constructor.  Note that only real metadata items are copied
    // with this constructor.  The purpose for this constructor is to
    // copy only the metadata that might be of interest to the scene
    // list implementation.
    //-----------------------------------------------------------------
    Metadata(Metadata orig)
    {
        date = orig.date;
        gridCol = orig.gridCol;
        gridRow = orig.gridRow;
        year = orig.year;
        month = orig.month;
        jDate = orig.jDate;
        ulX = orig.ulX;
        ulY = orig.ulY;
        cloudCover = orig.cloudCover;
        browseNumber = orig.browseNumber;
        dataVersion = orig.dataVersion;
        lookAngle = new String(orig.lookAngle);
        projectName = orig.projectName;
        // keep a reference to the original quality (note that it won't 
        // ever be changed, so keeping a reference to it is okay)
        quality = orig.quality;
        entityID = new String(orig.entityID);
        secondaryID = orig.secondaryID;
        sampOffset = null;
        lineOffset = null;
        downloadFileSize = orig.downloadFileSize;
        isDownloadable = orig.isDownloadable;
        if (orig.centerXY != null)
            centerXY = new Point(orig.centerXY);
        // keep a reference to the orig.screenLocation (note that it won't
        // ever be changed, so keeping a reference to it is okay)        
        screenLocation = orig.screenLocation;
        // specifically, do not include the image.  Some OS's have a limit
        // on how many images can be allocated at once.
        image = null;
        imageRes = orig.imageRes;
        visible = false;
        filterFlags = orig.filterFlags;
        sensor = orig.sensor;
        sceneCorners = orig.sceneCorners;
    }

    // method to convert the date into a string
    //-----------------------------------------
    public String getDateString()
    {
        StringBuffer dateString = new StringBuffer();
        dateString.append(year);
        dateString.append("/");
        dateString.append(month);
        dateString.append("/");
        // add the day of the month
        dateString.append((date - ((year * 10000) + (month * 100))));

        return dateString.toString();
    }

    // method to calculate the scene center X/Y coordinates
    //-----------------------------------------------------
    public void calculateSceneCenter(double offsetRes)
    {
        int x = ulX + (int)Math.round(((sampOffset[2] - sampOffset[0])/2 
                                       + sampOffset[0]) * offsetRes);
        int y = ulY - (int)Math.round(((lineOffset[2] - lineOffset[0])/2 
                                       + lineOffset[0]) * offsetRes);
        centerXY = new Point(x,y);
    }

    // method to set the scene lat/long corners using the offsets, the
    // upper-left corner, and the passed in projection transformation
    //----------------------------------------------------------------
    public void setSceneCorners(ProjectionTransformation proj)
    {
        // allocate space for the corners
        sceneCorners = new LatLong[4];

        // get the four corners in projection coordinates and convert them to
        // lat/long
        double offsetRes = sensor.getOffsetResolution();
        for (int index = 0; index < 4; index++)
        {
            int x = (int)Math.round(ulX + sampOffset[index] * offsetRes);
            int y = (int)Math.round(ulY - lineOffset[index] * offsetRes);

            sceneCorners[index] = proj.projToLatLong(x, y);
        }
    }

    // method to return the 4 scene corners in a particular projection.  The
    // order of the corners is UL, UR, LR, LL.
    //----------------------------------------------------------------------
    public void getSceneCornersInProj(ProjectionTransformation proj, int[] x,
                                      int[] y)
    {
        for (int index = 0; index < 4; index++)
        {
            Point p = proj.latLongToProj(sceneCorners[index]);
            x[index] = p.x;
            y[index] = p.y;
        }
    }

    // method to calculate the number of days between scenes
    //   Note: the extra day in leap years is ignored
    //------------------------------------------------------
    public int daysBetween(int targetSceneYear, int targetSceneJdate)
    {
        int days = 0;

        if (targetSceneYear == year)
        {
            days = Math.abs(targetSceneJdate - jDate);
        }
        else if (targetSceneYear < year)
        {
            days = 365 * (year - targetSceneYear);
            days += jDate - targetSceneJdate;
        }
        else //if (targetSceneYear > year)
        {
            days = 365 * (targetSceneYear - year);
            days += targetSceneJdate - jDate;
        }

        return days;
    }

    // method to remove references to the resources used by this class to
    // help the garbage collector of some Java VMs
    //-------------------------------------------------------------------
    public void cleanup()
    {
        quality = null;
        entityID = null;
        secondaryID = null;
        sampOffset = null;
        lineOffset = null;
        screenLocation = null;
        // Note that the call to flush is needed to really make the 
        // Netscape Java VM really release all the image resources
        if (image != null)
            image.flush();
        image = null;
        sensor = null;
    }

    // method to show the original browse image for a scene in browser window
    //-----------------------------------------------------------------------
    public final void showBrowse()
    {
        sensor.showBrowse(this);
    }

    // method to show a separate browser window with metadata
    //-------------------------------------------------------
    public final void showMetadata()
    {
        sensor.showMetadata(this);
    }

    // method to set the visible flag to the correct state based on the 
    // filter flags
    //-----------------------------------------------------------------
    private final void updateVisible()
    {
        if (filterFlags == 0)
            visible = true;
        else
            visible = false;
    }

    // method to clear a particular filter flag
    //-----------------------------------------
    public void clearFilter(int flag)
    {
        filterFlags &= ~flag;
        updateVisible();
    }

    // method to filter the scene to a viewport
    //-----------------------------------------
    public void filterToViewport(Polygon viewport)
    {
        if (viewport.contains(centerXY.x,centerXY.y))
            filterFlags &= ~Metadata.VIEWPORT_FILTER;
        else
            filterFlags |= Metadata.VIEWPORT_FILTER;

        updateVisible();
    }

    // method to filter the scene to meet a cloud cover restriction
    //-------------------------------------------------------------
    public void filterToCloudCover(int maxCloudCover)
    {
        if (cloudCover <= maxCloudCover)
            filterFlags &= ~Metadata.CLOUD_COVER_FILTER;
        else
            filterFlags |= Metadata.CLOUD_COVER_FILTER;

        updateVisible();
    }

    // method to filter the scene to a date range.  Note the months are
    // expected to be in the range of 0-11 when passed in.
    //-----------------------------------------------------------------
    public void filterToDateRange(int startYear, int endYear,
                                  int startMonth, int endMonth)
    {
        // if there is no acquisition date, date range cannot be filtered on
        if (!sensor.hasAcqDate)
        {
            filterFlags &= ~Metadata.DATE_FILTER;
            return;
        }

        // adjust the start and end month to be in the range of 1-12
        startMonth++;
        endMonth++;

        // determine if the range of months crosses the year boundary
        boolean monthsWrapAround = (startMonth > endMonth);

        if ((year >= startYear) && (year <= endYear))
        {
            boolean keep = false;
            if (!monthsWrapAround)
            {
                if ((month >= startMonth) && (month <= endMonth))
                    keep = true;
            }
            else 
            {
                if ((month >= startMonth) || (month <= endMonth))
                    keep = true;
            }
            if (keep)
                filterFlags &= ~Metadata.DATE_FILTER;
            else
                filterFlags |= Metadata.DATE_FILTER;
        }
        else
            filterFlags |= Metadata.DATE_FILTER;

        updateVisible();
    }

    // method to filter the scene to a gridCol gridRow range.
    //-----------------------------------------------------------------
    public void filterToGridColRowRange(int startGridCol, int endGridCol,
                                  int startGridRow, int endGridRow)
    {
        if (sensor.hasGridColRowFilter)
        {
            if ((gridCol >= startGridCol) && (gridCol <= endGridCol))
            {
                boolean keep = false;
                if ((gridRow >= startGridRow) && (gridRow <= endGridRow))
                    keep = true;
           
                if (keep)
                    filterFlags &= ~Metadata.GRID_COL_ROW_FILTER;
                else
                    filterFlags |= Metadata.GRID_COL_ROW_FILTER;
           }
           else
               filterFlags |= Metadata.GRID_COL_ROW_FILTER;
   
            updateVisible();
        }
    }
    
    // method to filter the scene to visible if in sceneList
    //------------------------------------------------------
    public void filterToSceneList(boolean filterEnabled)
    {
        if (filterEnabled)
        {
            if (sensor.sceneList.find(this) != -1)
                filterFlags &= ~Metadata.SCENE_LIST_FILTER;
            else
                filterFlags |= Metadata.SCENE_LIST_FILTER;
        }
        else
        {
            filterFlags &= ~Metadata.SCENE_LIST_FILTER;
        }

        updateVisible();
    }

    // method to filter the scene to based on whether it is downloadable
    //------------------------------------------------------------------
    public void filterToDownloadable(boolean filterEnabled)
    {
        if (filterEnabled && sensor.mightBeDownloadable)
        {
            if (isDownloadable)
                filterFlags &= ~Metadata.DOWNLOADABLE_FILTER;
            else
                filterFlags |= Metadata.DOWNLOADABLE_FILTER;
        }
        else
        {
            // Can't filter on downloadable unless sensor is mightBeDownloadable
            filterFlags &= ~Metadata.DOWNLOADABLE_FILTER;
        }

        updateVisible();
    }

    // method to filter the scene to not visible if Hidden
    // by the user.
    //------------------------------------------------------
    public void filterToHiddenScene()
    {
        
        if (sensor.hiddenSceneList.find(this) != -1)
        {
            filterFlags |= Metadata.HIDDEN_SCENE_FILTER;
        }
        else
        {
            filterFlags &= ~Metadata.HIDDEN_SCENE_FILTER;
        }
        
        updateVisible();
    }

    // method to filter the scene to the user defined area
    //----------------------------------------------------
    public void filterToUserArea(boolean filterEnabled, 
                                 UserDefinedAreaDialog userDefinedAreaDialog)
    {
        if (filterEnabled && sensor.hasUserDefinedArea)
        {
            if (userDefinedAreaDialog.getUserDefinedArea().
                                                        sceneIntersects(this))
            {
                filterFlags &= ~Metadata.USER_DEFINED_AREA_FILTER;
            }
            else
                filterFlags |= Metadata.USER_DEFINED_AREA_FILTER;
        }
        else
            filterFlags &= ~Metadata.USER_DEFINED_AREA_FILTER;

        updateVisible();
    }

    // method to filter the scene to visible if meets minimum quality
    //---------------------------------------------------------------
    public void filterToQuality(int minimumQuality)
    {
        int qual = getQuality();
        if (qual >= 0)
        {
            // set filter bit
            if (qual >= minimumQuality)
                filterFlags &= ~Metadata.QUALITY_FILTER;
            else
                filterFlags |= Metadata.QUALITY_FILTER;

            updateVisible();
        }
    }

    // method to filter the scene to based on the data version
    //--------------------------------------------------------
    public void filterToDataVersion(String dataVersion)
    {
        if (sensor.hasDataVersions)
        {
            // if the dataVersion is "All" or the data version matches,
            // the scene is not filtered out
            if ((dataVersion.equals("All"))
                || (this.dataVersion.equals(dataVersion)))
                filterFlags &= ~Metadata.DATA_VERSION_FILTER;
            else
                filterFlags |= Metadata.DATA_VERSION_FILTER;

            updateVisible();
        }
    }

    // method to build the full entity ID for display
    //-----------------------------------------------
    String getEntityIDForDisplay()
    {
        return sensor.buildEntityID(this);
    }

    // method to build a medium length entity ID for display
    //------------------------------------------------------
    String getMediumEntityIDForDisplay()
    {
        return sensor.buildMediumEntityID(this);
    }

    // method to build a short length entity ID for display
    //-----------------------------------------------------
    String getShortEntityIDForDisplay()
    {
        return sensor.buildShortEntityID(this);
    }

    // method to get the quality of the scene
    //---------------------------------------
    public int getQuality()
    {
        if (sensor.numQualityValues > 0)
        {
            int qual = quality[0];

            // for scenes with more than one quality value, use the worst 
            // quality
            for (int i = 1; i < quality.length; i++)
            {
                if (quality[i] < qual)
                    qual = quality[i];
            }
            return qual;
        }
        else
            return -1;
    }

    // method to return this scene's sensor
    //-------------------------------------
    Sensor getSensor()
    {
        return sensor;
    }
}
