// StatusBar.java implements a simple application status bar
//----------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;
import javax.swing.JPanel;

class StatusBar extends JPanel
{
    private JTextField resolution;  // text field for the current resolution
    private JTextField limits;      // text field for the search limits
    private JTextField status;      // text field for the status bar
    public ProgressIndicator progress; // progress indicator for loading data

    // constructor for the status bar
    StatusBar(imgViewer applet)
    {
        // make sure the background is light gray since some Java
        // implementations get it wrong
        setBackground(Color.LIGHT_GRAY);

        // create an uneditable text field for the current resolution
        resolution = new JTextField("",6);
        resolution.setEditable(false);
        resolution.setFocusable(false);
        Dimension size = resolution.getPreferredSize();
        size.width = 60;
        resolution.setMinimumSize(size);
        resolution.setMaximumSize(size);
        
        // create an uneditable text field for the search limits display
        limits = new JTextField("",20);
        limits.setEditable(false);
        limits.setFocusable(false);
        size = limits.getPreferredSize();
        size.width = 180;
        limits.setMinimumSize(size);
        limits.setMaximumSize(size);
        
        // create an uneditable text field for the status message
        status = new JTextField("", 70);
        status.setEditable(false);
        status.setFocusable(false);

        // create the progress indicator
        progress = new ProgressIndicator(applet);

        // use a grid bag layout for the status bar panel
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        // add the resolution display to the status bar
        gbc.weightx = 0;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        add(resolution,gbc);

        // add the search limits display
        add(limits,gbc);

        // make the status field completely fill the panel
        gbc.weightx = 100;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        add(status, gbc);

        gbc.weightx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(progress, gbc);
    }

    // method to update the status bar message
    public void showStatus(String message)
    {
        status.setText(message);
        // make sure the beginning of the status message is visible in case
        // the field is too short to display everything
        status.setCaretPosition(0);
    }

    // method to update the search limits status
    public void showSearchLimits(String limitsMessage)
    {
        limits.setText(limitsMessage);
    }

    // method to update the resolution status
    public void showResolution(String resolutionString)
    {
        resolution.setText(resolutionString);
    }
}
