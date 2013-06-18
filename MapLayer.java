// MapLayer.java implements the base class for all the different types of
// map layers.
//------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

public abstract class MapLayer
{
    private String name;      // name of the map layer
    private Component parent; // component the map layer is drawn in
    private boolean layerOn;  // flag to indicate the map layer is drawn
    private Color originalColor; // color originally used for the map layer
    private int menuShortcut; // menu shortcut for the layer in the menu
    private boolean lowResAvailable; // flag to indicate layer is available
                              // for low resolution datasets
    private boolean isHidden; // flag to indicate the layer should be hidden
                              // from the user.  Some layers, like the address
                              // search results layer might not be available in
                              // some installations.
    protected Color color;    // color to draw the map layer in

    // constructor for the MapLayer class
    //-----------------------------------
    public MapLayer(Component parent, String layerName, Color layerColor,
                    int menuShortcut, boolean lowResAvailable)
    {
        this.parent = parent;
        name = layerName;
        originalColor = layerColor;
        color = layerColor;
        this.menuShortcut = menuShortcut;
        this.lowResAvailable = lowResAvailable;
        layerOn = false;
        isHidden = false;
    }

    // method to return the map layer name
    //------------------------------------
    public String getName()
    {
        return name;
    }

    // method to return the menu shortcut to use
    //------------------------------------------
    int getMenuShortcut()
    {
        return menuShortcut;
    }

    // method to return the original color for the map layer
    //------------------------------------------------------
    public Color getOriginalColor()
    {
        return originalColor;
    }

    // method to return the color for the map layer
    //---------------------------------------------
    public Color getColor()
    {
        return color;
    }

    // method to set the color for the map layer
    //------------------------------------------
    public void setColor(Color color)
    {
        this.color = color;
        if (layerOn)
            parent.repaint();
    }

    // method to set the layer on/off
    //-------------------------------
    public void setLayerOn(boolean on)
    {
        layerOn = on;
    }

    // method to return true if the layer is on
    //-----------------------------------------
    public boolean isLayerOn()
    {
        return layerOn;
    }

    // method to return the state of the map layer enabled/disable
    //------------------------------------------------------------
    public boolean isEnabled()
    {
        return true;
    }

    // method to return true if the map layer is available for low resolution
    // datasets
    //-----------------------------------------------------------------------
    public boolean isLowResAvailable()
    {
        return lowResAvailable;
    }

    // method to allow setting the isHidden flag.  Note that this should be 
    // set immediately after the layer is created and not changed later.
    //---------------------------------------------------------------------
    public void hide()
    {
        isHidden = true;
    }

    // method to return true if the map layer should be hidden from view
    //------------------------------------------------------------------
    public boolean isHidden()
    {
        return isHidden;
    }
    
    // method to configure the map layer for the displayed area.
    //   Returns: number of files that will need to be loaded
    //----------------------------------------------------------
    public abstract int setDisplayAreaUsingLatLong(
        LatLong minboxULdeg, LatLong minboxLRdeg, int projCode);
    public abstract int setDisplayAreaUsingProjCoords(
        Point ulCoord, Point lrCoord, int projCode);

    // method to read the map layer file
    //----------------------------------
    public abstract void read(CancelLoad isLoadCancelled, Point ulMeters, 
                              int projCode,
                              MapLayerLoadingCallback fileReadCallback);

    // method to clip the map layer components to the display
    //-------------------------------------------------------
    public abstract void clip(Point upperLeft, int pixelSize, 
                              Dimension dispSize,
                              ProjectionTransformation proj);

    // method to return a the name of a feature at a particular x/y location
    // on the screen. This default routine returns null to indicate the 
    // layer doesn't support feature names.
    //----------------------------------------------------------------------
    public MapLayerFeatureInfo findFeatureName(int x, int y)
    {
        return null;
    }

    // method to update an attribute window
    //-------------------------------------
    public void updateAttributeWindow()
    {
    }

    // method to draw the map layer on the display
    //--------------------------------------------
    public abstract void draw(Graphics g);
}
