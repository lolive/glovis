// LineGraph.java implements a line graph class the draws a line graph
// of the data that is passed to it.
//------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.awt.RenderingHints;

public class LineGraph
{
    private int padding; // spacing between x & y values
    private int topMargin; 
    private int bottomMargin;
    private int leftMargin;
    private int rightMargin;
    private double yScaleMax; // largest number on y axis
    private double yScaleMin; // smallest number on y axis
    private int numXSteps;    // number of steps on the x axis
    private double yAxisStep; // step between y axis values
    private int yRange; // number of values on the y axis
    public Color[] spectrum; // selected colors to draw lines
    private DecimalFormat formatter; // formats the axis numbers
    private Dimension savedDisplaySize;
               
    // Constructor for a Line graph
    //--------------------------------------
    public LineGraph(Color [] spectrum,int topMargin,int bottomMargin, 
                    int leftMargin, int rightMargin, int padding,
                    double yScaleMax, double yScaleMin, int numXSteps,
                    double yAxisStep, int yRange, boolean decimalFormat)
    {
        this.spectrum = spectrum; 
        this.topMargin = topMargin;
        this.bottomMargin = bottomMargin; 
        this.leftMargin = leftMargin; 
        this.rightMargin = rightMargin; 
        this.padding = padding; 
        this.yScaleMax = yScaleMax;
        this.yScaleMin = yScaleMin;
        this.numXSteps = numXSteps;
        this.yAxisStep = yAxisStep;
        this.yRange = yRange;
                
        // format the y axis values
        if (decimalFormat)
        {
            formatter = new DecimalFormat("##0.0");
        }
        else
        {
            formatter = new DecimalFormat("###,###.##");
        }
    }
    
    // method to draw the points and connecting lines from the 
    // data that is passed in.
    //---------------------------------------------------------
    public void drawPoints(Graphics g, Dimension displaySize,
                           double[][] values, Integer[] colors)
    {
        Graphics2D g2 = (Graphics2D)g;
        savedDisplaySize = displaySize;

        // get the height for the plotting portion of the graph.
        int endy = displaySize.height - bottomMargin;
        int endx = displaySize.width - rightMargin;
        double starty = yScaleMax; // yaxis total number of values.
        int originX = 0;
        int originY = 0;
        int colorCount = 0;

        // detect when no data is available for display
        boolean noData = true;
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] != null)
            {
                noData = false;
                break;
            }
        }

        // if no data is available print Information message and return.
        if (noData)
        {
            g2.setColor(Color.RED);
            Font font = new Font("Arial", Font.PLAIN, 20);
            g2.setFont(font);
            int x = (endx/2) - 40;
            int y = endy/2;
            g2.drawString("No Data Available",x,y);
            return;
        }
        
        // loop through the data and plot the points.
        for (int i = 0; i < values.length; i++)
        {
            if(values[i] != null)
            {
                // set color
                g2.setColor( spectrum[colors[colorCount].intValue()] );
                double value = 0;
                int ymargin = endy;
                int ystep = (endy - topMargin)/yRange;
                int xstep = (endx - leftMargin)/numXSteps;
                int count = 0;
                int plotX;
                double plotY;
                double originValue = 0;
           
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                // plot the data.
                double[] currValues = values[i];
                for (int j = 0 ; j < currValues.length ; j++)
                {
                    value = currValues[j];
                    count++; 
                    plotX = leftMargin + (xstep*count);
                    plotY = endy - topMargin;
                    plotY = endy - (((plotY)/starty)* value);
                    int ploty = (int)Math.round(plotY);
                
                    // only plot values that are greater zero 
                    if (value >= 0)
                    {
                        g2.fillOval(plotX-2,ploty-2,5,5);
                        if ((originY != 0) && (originX != 0) && 
                            (originValue >= 0))
                        {
                            g2.drawLine(originX,originY,plotX,ploty);
                        }
                    }
                    // save the values to be able to draw the line
                    // between points.
                    originValue = value;
                    originX = plotX;
                    originY = ploty;
                }
                originX = 0;
                originY = 0;
                colorCount++;
            }
        } 
    }
    
    // method to draw the line graph legend in the bottom margin area
    // Legend names are passed in.
    //--------------------------------------------------------------------------
    public void drawLegend(Graphics g,Dimension displaySize,
                           String[] names, Integer[] colors)
    {
        Graphics2D g2 = (Graphics2D)g;
        savedDisplaySize = displaySize;

        int step = 20;
        // give all of the words the same amount of space alignment
        int averageWordSize = 90;
        int numberWordsPerLine = 4; // 4 words to a line
        int space = 10; // space between words
        // location to start drawing the first legend item
        int startY = displaySize.height - (bottomMargin - 60);
        
        int count = 0;
        int colorCount = 0;
        int center = displaySize.width/2; // center of display
        // depending on the size ofthe display find the x axis starting
        // point and the average space for each x axis value
        int startX = ((center - (space * (numberWordsPerLine - 1))) - 
                ((averageWordSize + space)* numberWordsPerLine)/2);
        int average = (averageWordSize + space);

        // loop through the names and draw them.
        for (int i = 0; i < names.length; i++)
        {
            if(names[i] != null)
            {
                // get the name for the xaxis 
                String name = names[i];
            
                // make the first letter capital
                String firstLetter = name.substring(0,1);
                String remainder = name.substring(1);
                name = firstLetter + remainder.toLowerCase();
            
                // draw the colored rect.
                g2.setColor(spectrum[colors[colorCount].intValue()]);
                g2.fillRect(startX,startY,5,5);
                g2.setColor(Color.BLACK);
            
                // Draw the the name on the xaxis
                g2.drawString(name,startX+7,startY+5);
                count ++;
                startX = startX + (average + space);
                // draw only 4 to a line
                if (count == numberWordsPerLine)
                {
                    startY = startY + step;
                    startX = ((center - (space * (numberWordsPerLine - 1))) -
                           ((averageWordSize + space)* numberWordsPerLine)/2);
                                            
                    count = 0;
                }
                colorCount++;
            }
        }
    }

    // method to draw a vertical from the xaxis value that was clicked on
    //-------------------------------------------------------------------
    public void drawSelectedDataRange(Graphics g, Dimension displaySize,
                                      float percentage)
    {
        Graphics2D g2 = (Graphics2D)g;
        savedDisplaySize = displaySize;

        // get min and max area of the plotting area
        int minX = leftMargin;
        int maxX = displaySize.width - rightMargin;
        int minY = topMargin;
        int maxY = displaySize.height - bottomMargin;;
        int xstep = (maxX - leftMargin)/numXSteps;
        minX += xstep;
        maxX = minX + xstep * (numXSteps - 1);

        int X = minX + (int)((maxX - minX) * percentage + 0.5F);

        g2.drawLine(X, maxY, X, minY);
    }

    public float getXPercentage(int x)
    {
        int minX = leftMargin;
        int maxX = savedDisplaySize.width - rightMargin;
        int xstep = (maxX - leftMargin)/numXSteps;
        int xmargin = maxX;
        minX += xstep;
        maxX = minX + xstep * numXSteps;

        if (x < minX)
            x = minX;
        else if (x > maxX)
            x = maxX;
        maxX -= xstep;

        float percentage = ((float)(x - minX)) / (maxX - minX);
        return percentage;
    }

    // draw the layout of the graph
    //-----------------------------
    public void drawGraphLayout(Graphics g, String[] XaxisTopLabels,
                                String[] XaxisBottomLabels,
                                Dimension displaySize,String title,
                                String xAxisLabel, String yAxisLabel)
    {
        Graphics2D g2 = (Graphics2D)g;
        savedDisplaySize = displaySize;
        
        // get the plotting portion of the graph.
        int endy = displaySize.height - bottomMargin;
        int endx = displaySize.width - rightMargin;
        double starty = yScaleMax; // yaxis total number of values.

        // draw graph title
        int xtitleLocation = (displaySize.width/2) - 
                            Math.round(title.length()*3);
        int ytitleLocation = 15;
        g2.drawString(title,xtitleLocation,ytitleLocation);
        
        // draw XAxis vertical line.
        g2.drawLine(leftMargin,topMargin,leftMargin,endy);
        
        // draw XAxis label
        int xAxisXLocation = (displaySize.width/2) - 
                            Math.round(xAxisLabel.length()*3);
        int xAxisYLocation = displaySize.height - padding;
        g2.drawString(xAxisLabel,xAxisXLocation,xAxisYLocation);
        
        // draw YAxis label
        int yAxisLocation = (displaySize.height/2) - 
                    Math.round(yAxisLabel.length()*3);
        
        //rotate the YAxis label
        g2.rotate(300,padding,yAxisLocation);
        g2.drawString(yAxisLabel,padding,yAxisLocation);
        g2.rotate(-300,padding,yAxisLocation);
    
        int ymargin = endy;
        int ystep = (endy - topMargin)/yRange;
        int count = 0;
        
        // draw YAxis values & lines
        for (double i = yScaleMin; i <= starty; i += yAxisStep) 
        {
            count++;
            String axisValues = formatter.format(i); 
            
            int YMargin = leftMargin - (axisValues.length()*3) - 12;
        
            // draw YAxis values.
            g2.drawString(axisValues,YMargin,ymargin);
            
            // draw YAxis values horizontal lines.
            g2.drawLine(leftMargin,ymargin,endx,ymargin);
            ymargin = endy - (ystep*count);
        }
       
        int xstep = (endx - leftMargin)/numXSteps;
        int xmargin = endx;
        count = 0;
        
        // Draw XAxis values and lines. 
        for (int i = 0; i < numXSteps; i++) 
        {
            count++;
            xmargin = leftMargin + (xstep*count);
            int topy = topMargin - 10;

            // draw top XAxis values vertical lines.
            g2.drawLine(xmargin,topMargin,xmargin,topy);
        
            // draw bottom XAxis values vertical lines.
            g2.drawLine(xmargin,endy,xmargin,endy+10);
        
            // draw bottom XAxis values, rotating the labels
            int Ystep = (XaxisBottomLabels[i].length()*3) + 25 + endy;
            int Xstep = xmargin - (XaxisBottomLabels[i].length()*3);
            g2.rotate(-Math.PI/4,Xstep,Ystep);
            g2.drawString(XaxisBottomLabels[i],Xstep,Ystep);
            g2.rotate(Math.PI/4,Xstep,Ystep);

            // draw top XAxis values, rotating the labels
            // since these start at the tic marks, don't adjust for label length
            g2.rotate(-Math.PI/4,xmargin,topy);
            g2.drawString(XaxisTopLabels[i],xmargin,topy);
            g2.rotate(Math.PI/4,xmargin,topy);
        }
    }
}
