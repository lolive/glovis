// NDVILineGraph.java implements a class that reads and displays the 
// ndvi data on a line graph.
//------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import javax.swing.JButton;
import javax.swing.JPanel;

public class NDVILineGraph extends JPanel implements MouseListener, Printable
{
    private imgViewer applet;   // reference to applet
    private MosaicData md;      // reference to the mosaicData class
    private LineGraph lineGraph;// reference to the lineGraph class
    private NDVIGraphDialog ndviGraphDialog; // reference to the dialog box
    private String[] landcoverNames; // array of Landcover Names
    private int gridCol;        // grid column of landcover data
    private int gridRow;        // grid row of landcover data
    private int displayYear = 0;// year currently displayed
    private int topMargin;      // graph top margin
    private int bottomMargin;   // graph bottom margin
    private int leftMargin;     // graph left margin
    private int rightMargin;    // graph right margin
    private Metadata currentScene; // current selected scene
    private Sensor currSensor;  // current selected sensor
    private Landcover landcover;// class to hold the ndvi values
    private Vector availLandcover; // vector that holds the landcover class
    private int firstYear;      // first year of landcover data available
    private DecimalFormat threeDigitFormat; // three digit path/row formatter
    private static String[] XAxisBottomValues = 
        {"Jan 14","Jan 28","Feb 11","Feb 25","Mar 11", "Mar 25","Apr 8",
         "Apr 22","May 6","May 20","Jun 3","Jun 17","Jul 1","Jul 15",
         "Jul 29","Aug 12","Aug 26","Sep 9","Sep 23","Oct 7","Oct 21",
         "Nov 4","Nov 18","Dec 2","Dec 16","Dec 31"};
    private static String[] XAxisTopValues = 
        {"14","28","42","56","70", "84","98",
         "112","126","140","154","168","182","196",
         "210","224","238","252","266","280","294",
         "308","322","336","350","365"};
    private boolean dataAvailable; //flag to indicate if data is available 
    
    // class to store the ndvi data 
    class Landcover
    {
        String name;
        int year;
        int count;
        double[] ndviValues;
    }
    
    // Constructor for the NDVI Line graph
    //--------------------------------------
    public NDVILineGraph(imgViewer applet, MosaicData md,
                         NDVIGraphDialog dialogBox, String[] landcoverNames )
    {
        this.applet = applet;
        this.md = md;
        ndviGraphDialog = dialogBox;
        this.landcoverNames = landcoverNames;
        
        availLandcover = new Vector();

        setBackground(Color.WHITE);
        addMouseListener(this);

        // graph line colors
        // must match number and order of elements in landcoverNames array
        Color[] spectrum = new Color[20];
        spectrum[0]  = new Color(0,0,0);       // black - DECIDUOUS FOREST
        spectrum[1]  = new Color(255,0,255);   // pink - EVERGREEN FOREST
        spectrum[2]  = new Color(0,135,0);     // green - HERB. GRASSLANDS
        spectrum[3]  = new Color(255,0,0);     // red - HERB. WETLANDS
        spectrum[4]  = new Color(0,0,252);     // blue - MIXED FOREST
        spectrum[5]  = new Color(156,85,0);    // brown - PASTURE HAY
        spectrum[6]  = new Color(145,0,205);   // purple - SHRUBLAND
        spectrum[7]  = new Color(255,111,0);   // orange - ROW CROPS
        spectrum[8]  = new Color(153,153,153); // grey - SMALL GRAINS
        spectrum[9]  = new Color(0,153,153);   // greenish - TRANSITIONAL
        spectrum[10] = new Color(204,204,0);   // yellowish - WOODY WETLANDS
        spectrum[11] = new Color(255,111,0);   // orange - Cultivated Crops
        spectrum[12] = new Color(0,0,0);       // black - Deciduous Forest
        spectrum[13] = new Color(255,0,255);   // pink - Evergreen Forest
        spectrum[14] = new Color(0,135,0);     // green - Herb. Grasslands
        spectrum[15] = new Color(255,0,0);     // red - Herb. Wetlands
        spectrum[16] = new Color(0,0,252);     // blue - Mixed Forest
        spectrum[17] = new Color(156,85,0);    // brown - Pasture/Hay
        spectrum[18] = new Color(145,0,205);   // purple - Shrub/Scrub
        spectrum[19] = new Color(204,204,0);   // yellowish - Woody Wetlands

        // graph layout settings
        topMargin = 50;
        bottomMargin = 110;
        leftMargin = 50;
        rightMargin = 30;
        int padding = 20;
        double yScaleMax = 1;
        double yScaleMin = 0;
        int yRange = 10;
        int numXSteps = 26; // # elements in XAxisTopValues, XAxisBottomValues
        double yAxisStep = 0.1;
        boolean decimalFormat = true;
       
        lineGraph = new LineGraph(spectrum, topMargin, bottomMargin,
                                 leftMargin, rightMargin, padding,
                                 yScaleMax, yScaleMin, numXSteps,
                                 yAxisStep, yRange, decimalFormat);

        threeDigitFormat = new DecimalFormat("000");
    }

    // get the current scene the graph is drawn to
    //--------------------------------------------
    public Metadata getScene()
    {
        return currentScene;
    }

    // get the current sensor the graph is drawn for
    //----------------------------------------------
    public Sensor getSensor()
    {
        return currSensor;
    }

    // check to see if data was available for the last selected scene
    //---------------------------------------------------------------
    public void checkAvailability(double[][] values)
    {
        dataAvailable = false;
        // detect when no data is available for display
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] != null)
            {
                dataAvailable = true;
                break;
            }
        }
    }
    
    // check if data is available
    //-----------------------------------
    public boolean isDataAvailable()
    {
        return dataAvailable;
    }
    
    // read the ndvi file.
    //-------------------------------
    private void read()
    {
        // Get the current sensor and scene.
        currSensor = applet.sensorMenu.getCurrentSensor();
        TOC currentCell = md.getCurrentCell();
        gridCol = currentCell.gridCol;
        gridRow = currentCell.gridRow;

        currentScene = md.getCurrentScene();
        if (currentScene == null)
        {
            availLandcover.removeAllElements();
            return;
        }
        displayYear = currentScene.year;

        // get the current scenes column, row to find the
        // right ndvi file and data.
        BufferedReader data = null;
        
        // clear out the available landcovers vector.
        availLandcover.removeAllElements();

        int numLines = 0; 
        
        // Note: the landcovers are sorted in the order of NDVI counts
        // which is the largest ndvi count to the smallest.
        try 
        {
            // open the NDVI file
            URL ndviURL = new URL(applet.getCodeBase(),"NDVI/p"
                                    + threeDigitFormat.format(gridCol)
                                    + "/r" + threeDigitFormat.format(gridRow)
                                    + "/NDVI.gz");
            InputStream is = ndviURL.openStream();
            data = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(is)));
            String dataLine = data.readLine();
            if (dataLine == null) 
            {
                System.out.println(
                    "Error reading NDVI Data file for gridCol/gridRow "
                    + gridCol + "/" + gridRow);
                data.close();
                return;
            }
            try 
            {
                // parse the data line read in
                StringTokenizer st = new StringTokenizer(dataLine,",");
                int tempVal = Integer.parseInt(st.nextToken());
                if (tempVal != gridCol) 
                {
                    System.out.println(
                        "Error in NDVI Data file -- incorrect Path specified.");
                    System.out.println(" " + tempVal + " " + gridCol + "\n");
                    data.close();
                    return;
                }
                tempVal = Integer.parseInt(st.nextToken());
                if (tempVal != gridRow) 
                {
                    System.out.println(
                        "Error in NDVI Data file -- incorrect Row specified.");
                    data.close();
                    return;
                }
                numLines = Integer.parseInt(st.nextToken());
            }
            catch (NoSuchElementException e) 
            {
                System.out.println("Exception:  " + e.getMessage());
                data.close();
                return;
            }
            catch (NumberFormatException e) 
            {
                System.out.println("Exception:  " + e.getMessage());
                data.close();
                return;
            }

            firstYear = -1;
            // for each line store the values in the landcover class and
            // add them to the availLandcover vector.
            for (int i = 0; i < numLines; i++) 
            {
                dataLine = data.readLine();
                StringTokenizer st = new StringTokenizer(dataLine,",");
                int tempYear = Integer.parseInt(st.nextToken());
                if (firstYear == -1)
                    firstYear = tempYear;
                if (dataLine == null) 
                {
                    System.out.println(
                        "Error reading NDVI Data file for gridCol/gridRow "
                        + gridCol + "/" + gridRow);
                    data.close();
                    return;
                }
                try 
                {
                    Landcover landcover = new NDVILineGraph.Landcover();
                    
                    // get the landcover name, count and string of ndvi
                    // data values.
                    String [] temp = null;
                    String landcoverName = "";
                    double[] dataValues = new double[26];
                    String landcoverCount = "";
                    temp = dataLine.split(",");
                    double d = 0;
                    
                    for (int j = 0 ; j < temp.length ; j++)
                    {
                        if (j == 1)
                        {
                            landcoverCount = temp[j];
                        }
                        else if (j == 2)
                        {
                            landcoverName = temp[j];
                        }
                        else if (j >= 3)
                        {
                            int value = Integer.parseInt(temp[j]);
                            // convert to NDVI scale
                            d = (value - 100);
                            d = d/100;
                            dataValues[j - 3] = d;
                        }
                    }
                    // do a little tweaking of the long class names
                    if (landcoverName.equals("Grassland/Herbaceous"))
                    {
                        landcoverName = "Herb. Grasslands";
                    }
                    else if (landcoverName.equals("Emergent Herbaceous Wetlands"))
                    {
                        landcoverName = "Herb. Wetlands";
                    }
                    // assign the values to the landcover class
                    landcover.ndviValues = dataValues;
                    landcover.year = tempYear;
                    landcover.name = landcoverName;
                    landcover.count = Integer.parseInt(landcoverCount);
                    // populate the vector
                    availLandcover.addElement(landcover);
                }
                catch (NoSuchElementException e) 
                {
                    System.out.println("Exception:  "+ e.getMessage());
                    data.close();
                    return;
                }
                catch (NumberFormatException e) 
                {
                    System.out.println("Exception:  "+ e.getMessage());
                    data.close();
                    return;
                }
            }
        }
        catch(IOException e)
        {
            // if the data file was opened, close it and issue a message.
            // if it was never successfully opened, skip issuing a message
            // since the file probably just doesn't exist.
            if (data != null)
            {
                try {data.close();} catch (Exception e1){};
                System.out.println("Exception:  "+e.getMessage());
            }
        }
    }
        
    // method to draw the all parts of the line graph 
    //-----------------------------------------------
    public void paintComponent(Graphics g)
    {
        Dimension displaySize = getSize();

        String title = "NDVI Data for ";
        if (currentScene != null)
            title += "Year " + currentScene.year;
        title += " Path " + gridCol + " Row " + gridRow;
        String xAxisLabel = ""; // label not needed
        String yAxisLabel = "NDVI Values";

        Vector values = new Vector();
        Vector names = new Vector();
        Vector colors = new Vector();
        int count = 0;
        
        // get the values, names and colors in an array to be passed to the
        // line graph class, setting the checkbox for the dialog.
        int numAvailable = availLandcover.size();
        for (int i = 0; i < numAvailable; i++)
        {
            Landcover landcover = (Landcover)availLandcover.elementAt(i);
            if (landcover.year == currentScene.year)
            {
                if (ndviGraphDialog.setLandcover(landcover.name + ": ("
                                                 + landcover.count + ")"))
                {
                    values.addElement(landcover.ndviValues);
                    names.addElement(landcover.name);
                    colors.addElement(ndviGraphDialog.getColors(landcover.name
                                    + ": (" + landcover.count + ")"));
                    count++;
                }
            }
        }
        
        // legacy functions expect arrays, not Vectors
        double[][] valuesArray = new double[values.size()][];
        values.toArray(valuesArray);
        String[] namesArray = new String[names.size()];
        names.toArray(namesArray);
        Integer[] colorsArray = new Integer[colors.size()];
        colors.toArray(colorsArray);
        // check if data is available 
        checkAvailability(valuesArray);
        
        // draw the layout of the linegraph.
        lineGraph.drawGraphLayout(g,XAxisTopValues,XAxisBottomValues,
                                  displaySize,title,xAxisLabel,yAxisLabel);

        // get the currently displayed scenes date
        if ((currentScene != null) && (isDataAvailable()))
        {
            int daysInScale = getDaysInScale();
            // the scale starts at January 14, so adjust accordingly
            float percentage = ((float)currentScene.jDate - 14) / daysInScale;
            lineGraph.drawSelectedDataRange(g, displaySize, percentage);
        }

        // draw the line graph points and legend.
        lineGraph.drawPoints(g,displaySize,valuesArray,colorsArray);
        lineGraph.drawLegend(g,displaySize,namesArray,colorsArray);
    }

    // set the default landcover
    //--------------------------
// FIXME - not sure this method has much value
    public void setDefaultLandcover()
    {
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();

        if (currSensor.hasNdviLineGraph)
            performAction();
    }
    
    // read and set all of the landcover dialog box checkboxes 
    //--------------------------------------------------------
    public void performAction()
    {
        boolean setDefault = true;

        Metadata scene = md.getCurrentScene();

        // read the ndvi data
        if ((scene == null) || ((gridCol != scene.gridCol) 
            || (gridRow != scene.gridRow))
            || (currSensor != applet.sensorMenu.getCurrentSensor()))
        {
            read();
        }

        currentScene = scene;

        // check if no checkboxes are selected to know if we need to set the
        // default landcover values.
        if (ndviGraphDialog.isLandcoverSelected()
            && (scene != null && displayYear == scene.year))
        {
            setDefault = false;
        }
        if (scene != null)
        {
            displayYear = scene.year;
        }
        
        int defaultLandcoverCount = 0;
        DecimalFormat formatNumber = new DecimalFormat("###,###.##");

        int year = firstYear;
        if (scene != null)
            year = scene.year;

        int numAvailable = availLandcover.size();

        // set and enable/disable the dialog box checkbox
        for (int i = 0; i < numAvailable; i++)
        {
            Landcover landcover = (Landcover)availLandcover.elementAt(i);
            if (landcover.year == year)
            {
                // add the count to the label
                String landcoverLabel = landcover.name + ": ("
                            + formatNumber.format(landcover.count) + ")";

                // find the landcovers that are available and if we need to 
                // set the default landcovers 
                if (setDefault && (defaultLandcoverCount <= 4))
                {
                    ndviGraphDialog.setDefault(landcoverLabel);
                }
                else if (setDefault)
                {
                    if(!ndviGraphDialog.setLandcover(landcoverLabel))
                    {
                        ndviGraphDialog.setAvailLandcover(landcoverLabel);
                    }
                }
                else
                {
                    ndviGraphDialog.setAvailLandcover(landcoverLabel);
                }
                defaultLandcoverCount++;
            }
        }
        
        // set the enable/disable of the set default button
        if (ndviGraphDialog.isLandcoverSelected())
            ndviGraphDialog.enableButtons();
        else
            ndviGraphDialog.disableButtons();
    }

    // method to return the number of days on the x-axis
    //--------------------------------------------------
    private int getDaysInScale()
    {
        int year = currentScene.year;

        int daysInScale = 351;
        if (((year % 4) != 0) || (((year % 100) == 0) && ((year % 400) != 0)))
        {
            daysInScale = 352;
        }

        return daysInScale;
    }

    // action performed event handler to display the scene that is in the
    // date range the user clicked on
    //-------------------------------------------------------------------
    public void mouseClicked(MouseEvent event)
    {
        Dimension displaySize = getSize();
        
        // get x & y coordinate of the click.
        int clickedX = event.getX();
        int clickedY = event.getY();
        
        // get min and max ranges.
        int minX = leftMargin;
        int maxX = displaySize.width - rightMargin;
        int minY = topMargin;
        int maxY = displaySize.height - bottomMargin;;
        
        // draw line if clicked in the graph area.
        if (((clickedX > minX) && (clickedX < maxX)) && 
            ((clickedY > minY) && (clickedY < maxY)))
        {
            if ((currentScene != null) && (isDataAvailable()))
            {
                float percentage = lineGraph.getXPercentage(clickedX);

                int daysInScale = getDaysInScale();
                // the scale starts at January 14, so adjust accordingly
                int targetDate = (int)(daysInScale * percentage + 0.5F) + 14;

                md.setSceneToClosestDate(currentScene, targetDate,
                                         currentScene.year);
            }
        }
    }
    
    // dummy event handlers for the unused events needed for a MouseListener
    //----------------------------------------------------------------------
    public void mousePressed(MouseEvent event){}
    public void mouseEntered(MouseEvent event){}
    public void mouseExited(MouseEvent event){}
    public void mouseReleased(MouseEvent event){} 

    // printable interface method to print the graph
    //----------------------------------------------
    public int print(Graphics g, PageFormat pf, int pageIndex)
        throws PrinterException
    {
        // only a single page is available to print, so if anything past the
        // 0 index, return that the page doesn't exist
        if (pageIndex >= 1)
            return Printable.NO_SUCH_PAGE;

        // determine how much the image needs to be scaled to fit on the page
        Dimension size = getSize();
        double scale = pf.getImageableWidth() / size.width;
        double hScale = pf.getImageableHeight() / size.height;
        if (hScale < scale)
            scale = hScale;

        // set the location to start printing on the page and the correct scale
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        g2.scale(scale, scale);

        // paint the graph on the printed page
        paintComponent(g2);

        // indicate the page exists
        return Printable.PAGE_EXISTS;
   }
}
