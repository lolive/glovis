// PointOfInterestDialog.java implements a dialog for setting a point of 
// interest by a lat/long value.
//---------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PointOfInterestDialog extends JDialog implements 
    ActionListener
{
    private JTextField latitude;  // Latitude entry field
    private JTextField longitude; // Longitude entry field
    private imgViewer applet;     // reference to the main applet
    private MosaicData md;        // pointer to mosaic data
    private PointOfInterestMapLayer pointOfInterestMapLayer; 
                                  // object that has methods for
                                  // supporting the set point of
                                  // interest

    // Constructor for the Point of Interest dialog
    //---------------------------------------------
    public PointOfInterestDialog(JFrame parent, imgViewer applet, MosaicData md, 
           PointOfInterestMapLayer pointOfInterestMapLayer)
    {
        super(parent,"Set Point of Interest",false);
        this.applet = applet;
        this.md = md;

        // remember the map layers object
        this.pointOfInterestMapLayer = pointOfInterestMapLayer; 
        
        getContentPane().setLayout(new BorderLayout());
        
        // Set up the main panel and grid layout
        JPanel mainPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.setLayout(gridbag);

        // Set up the title.
        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new GridLayout(2,2));
        
        JLabel label = new JLabel("Enter a Latitude and Longitude");
        tempPanel.add(label);
        mainPanel.add(tempPanel, gbc);
        
        // set up the Latitude & longitude labels & textfields.
        tempPanel = new JPanel();
        tempPanel.setLayout(new GridLayout(2,2));

        label = new JLabel("Latitude:");
        tempPanel.add(label);
        latitude = new JTextField("",5);
        Dimension size = latitude.getPreferredSize();
        latitude.setMinimumSize(size);
        latitude.setMaximumSize(size);
        latitude.setToolTipText("Latitude entry");

        latitude.addActionListener(this);
        tempPanel.add(latitude);

        label = new JLabel("Longitude:");
        tempPanel.add(label);
        longitude = new JTextField("",5);
        longitude.setMinimumSize(size);
        longitude.setMaximumSize(size);
        longitude.setToolTipText("Longitude entry");
        longitude.addActionListener(this);
        tempPanel.add(longitude);

        mainPanel.add(tempPanel, gbc);

        // set up the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,3));
        JButton setButton = new JButton("Set");
        setButton.setMnemonic(KeyEvent.VK_S);
        setButton.setToolTipText("Set point of interest");
        setButton.addActionListener(this);
        JButton clearButton = new JButton("Clear");
        clearButton.setMnemonic(KeyEvent.VK_L);
        clearButton.setToolTipText("Clear point of interest");
        clearButton.addActionListener(this);
        JButton closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close point of interest");
        closeButton.addActionListener(this);

        buttonPanel.add(setButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);
        
        // add the year, month, and button panels to the dialog
        getContentPane().add(mainPanel,"North");
        getContentPane().add(buttonPanel,"South");

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(220,140);

    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();
        
        
        if (command.equals("Clear"))
        {
            // clear the entry boxes
            clearEntry(); 
            
            // clear point of interest
            pointOfInterestMapLayer.clearPoint();
        }
        else if (command.equals("Close"))
        {
            // hide the dialog box
            setVisible(false);
        }
        else
        {
            // action handler for "Set" button and "Enter key" action
            LatLong latLong = parseInput();
            if (latLong != null)
            {
               pointOfInterestMapLayer.setPoint(latLong);
            }
        }
    }
    
    // Method to Clear the dialog box entrys
    //--------------------------------------
    private void clearEntry()
    {
        latitude.setText("");
        longitude.setText("");
    }
   
    // Method to Format the lat/long value to a 3 digits after the
    // decimal place and update the entry box
    //------------------------------------------------------------
    public void setLatLong(LatLong latLong)
    {
        // convert the lat/long to 3 digit after the decimal place
        // and update the entry box
        double temp = latLong.latitude * 1000;
        if (temp > 0)
            temp += 0.5;
        else
            temp -= 0.5;
        temp = (double)((int)temp) / 1000.0;
        latitude.setText("" + temp);

        temp = latLong.longitude * 1000;
        if (temp > 0)
            temp += 0.5;
        else
            temp -= 0.5;
        temp = (double)((int)temp) / 1000.0;
        longitude.setText("" + temp);
    }
    
    // Method to parse the text box entries and validate them
    //-------------------------------------------------------
    private LatLong parseInput()
    {
        double lat;     // latitude read from the entry field
        double lon;     // longitude read from the entry field
        boolean doingLong = false; // flag to track what is being converted
        boolean polygonChanged = false; // 

        try
        {
            // get the latitude, trim any leading or trailing spaces,
            // and make sure it contains something
            String temp = latitude.getText();
            temp.trim();
            if (temp.equals(""))
            {
                // popup a message box with an error message
                JOptionPane.showMessageDialog(this,
                        "Latitude value invalid ","Invalid Entry", 
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            // convert the latitude to a double
            Double d = new Double(temp);
            lat = d.doubleValue();

            // flag longitude is being converted for the catch statement
            doingLong = true;

            // get the longitude, trim any leading or trailing spaces, 
            // and make sure it contains something
            temp = longitude.getText();
            temp.trim();
            if (temp.equals(""))
            {
                // popup a message box with an error message
                JOptionPane.showMessageDialog(this,
                        "Longitude value invalid ","Invalid Entry", 
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            // convert the longitude to a double
            d = new Double(temp);
            lon = d.doubleValue();

            // check the range on the latitude and longitude
            if ((lat < -90.0) || (lat > 90.0))
            {
                // popup a message box with an error message
                JOptionPane.showMessageDialog(this,
                        "Latitude " + lat + " out of range!","Invalid Entry", 
                        JOptionPane.ERROR_MESSAGE);
                latitude.setText("");
                return null;
            }
            else if ((lon < -180.0) || (lon > 180.0))
            {
                // popup a message box with an error message
                JOptionPane.showMessageDialog(this,
                        "Longitude " + lon + " out of range!","Invalid Entry", 
                        JOptionPane.ERROR_MESSAGE);
                longitude.setText("");
                return null;
            }
            else
            {
                // make sure the lat/long entered is visible on the display
                md.gotoLatLong(lat, lon);
                LatLong latLong = new LatLong(lat, lon);
                return latLong;
            }
        }
        catch (NumberFormatException e)
        {
            // error converting string to a Double, so display a message
            // in the browser status bar
            String badValue;
            String latLong;
            if (doingLong)
            {
                badValue = longitude.getText();
                longitude.setText("");
                latLong = "Longitude";
            }
            else
            {
                badValue = latitude.getText();
                latitude.setText("");
                latLong = "Latitude";
            }
             // popup a message box with an error message
             JOptionPane.showMessageDialog(this,
                     "Illegal "+latLong+" value of \"" + badValue + "\"",
                     "Invalid Entry", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
