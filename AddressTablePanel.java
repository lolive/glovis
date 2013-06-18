// AddressTablePanel.java implements a List GUI component for displaying
// the addresses and their associated latitude and longitude
//----------------------------------------------------------------------

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class AddressTablePanel extends JPanel implements MouseListener
{
    private imgViewer applet;
    private AddressTable addressTable;

    // Constructor for the AddressTablePanel
    //--------------------------------------
    AddressTablePanel
    (
        imgViewer applet    // I: reference to the main applet
    )
    {
        this.applet = applet;

        // use the gridbag layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 100;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel titlePanel = new JPanel();
        JLabel title = new JLabel("Address Search Results");
        title.setFont(applet.boldFont);
        titlePanel.add(title);
        add(titlePanel, gbc);

        addressTable = new AddressTable();
        addressTable.setToolTipText("Address Location Table");
        addressTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(addressTable);
        addressTable.addMouseListener(this);

        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weighty = 100;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;

        add(scrollPane, gbc);
    }

    // set the addresses in the table
    //-------------------------------
    public void setAddresses(String[] addresses)
    {
        addressTable.setAddresses(addresses);
    }

    // clear the addresses from the table
    public void clearAddresses()
    {
        addressTable.clearAddresses();
    }

    // return the number of rows in the table
    //---------------------------------------
    public int getRowCount()
    {
        return addressTable.getRowCount();
    }

    // action performed event handler to display the location when it is
    // double-clicked
    //------------------------------------------------------------------
    public void mouseClicked(MouseEvent event)
    {
        if (event.getClickCount() == 2)
            display();
    }

    // dummy event handlers for the unused events needed for a MouseListener
    //----------------------------------------------------------------------
    public void mousePressed(MouseEvent event){}
    public void mouseEntered(MouseEvent event){}
    public void mouseExited(MouseEvent event){}
    public void mouseReleased(MouseEvent event){}

    // display the currently select address
    //-------------------------------------
    public void display()
    {
        // get the selected row.
        int rowIndex = addressTable.getSelectedRow();

        if (rowIndex >= 0)
        {
            LatLong latLong = addressTable.getLatLong(rowIndex);
            String address = addressTable.getAddress(rowIndex);

            // display this lat/long
            AddressSearchMapLayer layer = 
                applet.md.mapLayers.getAddressSearchMapLayer();

            layer.setPoint(latLong, address);
        }
    }
}
