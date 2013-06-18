// CloudCoverLimit.java implements panel to allow setting the maximum cloud 
// cover the user wants to see.
//-------------------------------------------------------------------------
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CloudCoverLimit extends JPanel implements ActionListener
{
    private CloudCoverChoice ccPercent; // maximum cloud cover selection widget
    private MosaicData md;      // mosaic data object

    // Constructor for the CloudCoverLimit widget
    //-------------------------------------------
    CloudCoverLimit(imgViewer parent, MosaicData mdIn)
    {
        md = mdIn;

        // do not use a layout manager since none of them seem to do well
        // with choice widgets on all platforms
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // create the label and add it to the panel
        JLabel label = new JLabel("Max Cloud:");
        label.setFont(parent.boldFont);
        label.setDisplayedMnemonic('x');
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 10;
        gbc.weighty = 0;
        gbc.gridheight = 1;
        add(label,gbc);

        // create a percent dropdown list
        ccPercent = new CloudCoverChoice();
        ccPercent.setFont(parent.normalFont);
        ccPercent.setToolTipText("Set cloud limit");
        ccPercent.addActionListener(this);
        label.setLabelFor(ccPercent);
        add(ccPercent,gbc);
    }

    // method to detect when the cloud cover limit is changed
    //-------------------------------------------------------
    public void actionPerformed(ActionEvent e)
    {
        md.setCCLimit(ccPercent.getCloudCover());
    }

    // method to allow setting the cloud cover limit when it is changed 
    // by another mechanism
    //-----------------------------------------------------------------
    public void setCloudCover(int cc)
    {
        ccPercent.setCloudCover(cc);
    }
}
