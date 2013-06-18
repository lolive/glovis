// WorldCitiesMapLayer.java implements the World Cities map layer.
//----------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;


public class WorldCitiesMapLayer extends TiledMapLayer
{
    imgViewer applet;

    // define a class to hold information for a city
    class City
    {
        int x;
        int y;
        int level;
        boolean capital;
        boolean shiftName;
        String name;
    };

    private Vector visibleCities;   // subset of cities that are visible
    private Vector potentiallyVisibleCities;// city subset that might be visible
    private boolean citiesNeedFiltering = false; // flag to indicate the cities
                                    // need to be filtered for the current
                                    // display area
    private Point ulMeters; // upper left X/Y in meters
    private Polygon starPolygon;

    // constructor for the WorldCitiesMapLayer class
    //----------------------------------------------
    public WorldCitiesMapLayer(imgViewer applet, Color color, int menuShortcut)
    {
        super(applet.imgArea, "Cities", "City", color, menuShortcut,
              true);

        this.applet = applet;

        visibleCities = new Vector();
        potentiallyVisibleCities = new Vector();
        starPolygon = new Polygon();
    }

    // method to read the World Cities map layer files
    //------------------------------------------------
    public void read(CancelLoad isLoadCancelled, Point ulMeters, int projCode,
                     MapLayerLoadingCallback fileReadCallback)
    {
        this.ulMeters = new Point(ulMeters);
        citiesNeedFiltering = true;

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

            Vector polygons = new Vector();  // vector of polygons for file
            URL CurrentURL;         // URL for the current linework file

            if (applet.verboseOutput)
                System.out.println("Reading " + files[ii]);

            // build a URL for this file
            try
            {
                CurrentURL = new URL(CodeBase.getGlovisURL(), files[ii]);
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
                nameSize = 50;
                name = new byte[nameSize];

                int numRecs = data.readInt();

                // Read each of the polygons in this file
                for (int i=0; i<numRecs; i++)
                {
                    // if the load has been cancelled, break out of the
                    // loop
                    if (isLoadCancelled.cancelled)
                        break;

                    City city = new WorldCitiesMapLayer.City();

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
                    city.name = new String(name,0,readNameSize - 2);
                    city.level = Integer.parseInt(
                        new String(name,readNameSize-1, 1));
                    if (city.level == 1)
                        city.capital = true;
                    else
                        city.capital = false;

                    int numPts = data.readInt();

                    // read the point
                    city.x = data.readInt();
                    city.y = data.readInt();

                    // add the entry to the polygon vector
                    polygons.addElement(city);
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

            // add whatever polygons were read to the file cache as long
            // as the load hasn't been cancelled (the data may not be 
            // complete if the load was cancelled)
            if (!isLoadCancelled.cancelled)
                fileCache.setCacheContents(files[ii],polygons);

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

        citiesNeedFiltering = true;
    }

    // method to filter the entire list of World cities for cites that are
    // visible or nearly visible.  This is only done once after each call to
    // the read method to prevent dragging the display from making cities
    // flicker on and off.
    // TBD: should the distribution on almost visible cities be chosen
    //      so each quadrant outside of the visible area gets an equal 
    //      number of cities?
    //----------------------------------------------------------------------
    void filter()
    {
        int visibleCount = 0;
        int almostVisibleCount = 0;
        int ii;

        // clear the list of potentially visibile cities
        potentiallyVisibleCities.removeAllElements();

        double pixelSize = applet.imgArea.md.actualPixelSize;

        // for higher resolutions, allow cities of level 3 or lower to be 
        // shown near capitals.  For lower resolutions (i.e. MODIS data), 
        // don't show any cities near capitals
        int cityLevelNearCapital = 3;
        if (pixelSize > 2000)
            cityLevelNearCapital = 0;

        // get the cached data
        Vector data = fileCache.getCachedData();

        // get the display area size.  Note that the imagePane size 
        // cannot be used here since it may be changing yet.  So calculate
        // what it will be when it is set.
        Dimension dispSize = applet.imgArea.md.getDisplaySize();
        Dimension minSize = applet.imgScroll.getMinimumSize();
        dispSize.width = Math.max(minSize.width,dispSize.width);
        dispSize.height = Math.max(minSize.height,dispSize.height);
        int ulx = ulMeters.x;
        int uly = ulMeters.y; 

        // define the potentially visible area to be 650 pixels outside
        // the immediately visible area
        int minX = -650;
        int maxX = dispSize.width + 650;
        int minY = -650;
        int maxY = dispSize.height + 650;
        
        // build the list of cities that are potentially visible, stepping
        // through from the highest "level" to the lowest
        for (int currentLevel = 1; currentLevel <= 8; currentLevel++)
        {
            for (ii = 0; ii < data.size(); ii++)
            {
                Vector cache = (Vector)data.elementAt(ii);
                if (cache == null)
                {
//                    System.out.println(LineworkFiles[ii] + " not in cache!");
                    continue;
                }

                // get number of polygons in the cache
                int numCities = cache.size();

                // look for the cities that are visible
                for (int i = 0; i < numCities; i++)
                {
                    boolean shiftName = false;
                    City foundCapital = null;
                    int closestCapitalDist = 10000000;

                    // get the current city
                    City city = (City)cache.elementAt(i);
                    // only consider cities at the current level
                    if (city.level != currentLevel)
                        continue;
                    int x = city.x;
                    int y = city.y;

                    // convert the x/y coordinate to a screen coordinate
                    x = (int)((x - ulx)/pixelSize);
                    y = (int)((uly - y)/pixelSize);

                    // check whether the city is potentially visible
                    if ((x >= minX) && (x < maxX) && (y >= minY) && (y < maxY))
                    {
                        boolean skip = false;
                        // it is potentially visible, so make sure it isn't
                        // already too close to a visible city that is larger
                        int size = potentiallyVisibleCities.size();
                        for (int z = 0; z < size; z++)
                        {
                            City c =(City)potentiallyVisibleCities.elementAt(z);

                            // calculate the distance from the current city to
                            // the city in the list
                            int xd = (int)((city.x - c.x)/pixelSize);
                            int yd = (int)((city.y - c.y)/pixelSize);
                            int dist = xd * xd + yd * yd;
                            if (dist < 10000)
                            {
                                // within 100 pixels (100 * 100 = 10000), so
                                // only keep the current one if it meets the
                                // level requirement for being near a capital
                                if (c.capital 
                                    && (city.level <= cityLevelNearCapital))
                                {
                                    // remember that city name needs to be
                                    // shifted.  Remember the closest capital in
                                    // case this city falls near more than one
                                    // (east coast).
                                    shiftName = true;
                                    if (dist < closestCapitalDist)
                                    {
                                        foundCapital = c;
                                        closestCapitalDist = dist;
                                    }
                                }
                                else
                                {
                                    // not a capital city, or smaller, so skip
                                    // this one since a larger one is already
                                    // visible
                                    skip = true;
                                    break;
                                }
                            }
                        }

                        if (!skip)
                        {
                            boolean addCity = false;
                            // default to the current city name not being
                            // shifted
                            city.shiftName = false;

                            // add all capital cities and a limited number of 
                            // visible or potentially visible cities to the list
                            if (city.capital)
                            {
                                // add all capitals present
                                addCity = true;
                            }
                            else if ((x >= 0) && (x < dispSize.width) 
                                   && (y >= 20) && (y < (dispSize.height - 20)))
                            {
                                if (visibleCount < 200)
                                {
                                    visibleCount++;
                                    addCity = true;
                                }
                            }
                            else
                            {
                                if (almostVisibleCount < 300)
                                {
                                    almostVisibleCount++;
                                    addCity = true;
                                }
                            }
                            if (addCity)
                            {
                                potentiallyVisibleCities.addElement(city);

                                // if it looks like the name location should be
                                // shifted due to a close capital, do so
                                if (shiftName)
                                {
                                    if (city.y < foundCapital.y)
                                    {
//System.out.println("shifting "+city.city+" because of "+foundCapital.city);
                                        // new city is lower than capital, so
                                        // shift where the name is drawn
                                        city.shiftName = true;
                                        // make sure the capital isn't shifted.
                                        // Note that this isn't necessarily the
                                        // correct thing to do, but it works
                                        // well in practice.  Should really
                                        // track which large city is closest to
                                        // the capital to decide which to shift
                                        foundCapital.shiftName = false;
                                    }
                                    else
                                    {
//System.out.println("shifting "+foundCapital.city+" because of "+city.city);
                                        // shift the capital location since it
                                        // is below the current city
                                        foundCapital.shiftName = true;
                                    }
                                }

                                // if reached the limit of visible and almost
                                // visible cities, bail out of the loop
                                if ((visibleCount >= 200) 
                                    && (almostVisibleCount >= 300))
                                {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // method to clip the cities layer to the currently displayed area
    //----------------------------------------------------------------
    public void clip(Point upperLeft, int pixelSize, Dimension dispSize,
                     ProjectionTransformation proj)
    {
        if (citiesNeedFiltering)
        {
            citiesNeedFiltering = false;
            filter();
        }

        // get the actual pixel size
        // FIXME - should probably be passing the actual in...
        double actualPixelSize = applet.imgArea.md.actualPixelSize;

        // clear out the list of visible cities
        visibleCities.removeAllElements();

        // build the list of cities that are currently visible
        // look for the cities that are visible
        int size = potentiallyVisibleCities.size();
        for (int i = 0; i < size; i++)
        {
            // get the current city
            City city = (City)potentiallyVisibleCities.elementAt(i);
            int x = city.x;
            int y = city.y;

            // convert the x/y coordinate to a screen coordinate
            x = (int)((x - upperLeft.x)/actualPixelSize);
            y = (int)((upperLeft.y - y)/actualPixelSize);

            // check if the city is visible with a little slop in the 
            // Y direction so names aren't drawn off the display
            if ((x >= 0) && (x < dispSize.width) 
                && (y >= 18) && (y < (dispSize.height - 18)))
            {
                // it is visible, so make a copy of the city with
                // the screen coordinates and add it to the visible list
                City movedCity = new WorldCitiesMapLayer.City();
                movedCity.x = x;
                movedCity.y = y;
                movedCity.level = city.level;
                movedCity.capital = city.capital;
                movedCity.name = city.name;
                movedCity.shiftName = city.shiftName;
                visibleCities.addElement(movedCity);
            }
        }
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public void draw(Graphics g)
    {
        // use the bold font for city names
        Font curFont = applet.boldFont;
        g.setFont(curFont);

        // get the font metrics so city names can be placed correctly
        FontMetrics fontMetrics = g.getFontMetrics();
        int lineHeight = fontMetrics.getHeight();

        // loop through the visible cities
        int numCities = visibleCities.size();
        for (int i = 0; i < numCities; i++)
        {
            int yOffset;   // amount to offset the city name from the marker
            City city = (City)visibleCities.elementAt(i);

            // find string width
            int sw = fontMetrics.stringWidth(city.name);

            // draw the correct marker for the city
            if (city.capital)
            {
                // draw a star filled polygon for the capitals
                int x[] = {0,3,10,4,8,0,-8,-4,-10,-3};
                int y[] = {-10,-3,-3,1,9,4,9,1,-3,-3};
                starPolygon = new Polygon(x,y,10);
                g.setColor(color);
                g.translate(city.x,city.y);
                g.fillPolygon(starPolygon);
                g.translate(-city.x,-city.y);
            }
            else
            {
                // draw a circle for non-capital cities
                g.setColor(color);
                g.fillOval(city.x-5,city.y-5,10,10);
            }

            // set the Y offset for the city name
            if (city.shiftName)
                yOffset = 12 + lineHeight;
            else
                yOffset = -12;

            // draw the city name with a black shadow underneath it
            g.setColor(Color.BLACK);
            g.drawString(city.name,city.x-(sw/2)-2,city.y+yOffset+1);
            g.setColor(color);
            g.drawString(city.name,city.x-(sw/2),city.y+yOffset);
        }
    }
}
