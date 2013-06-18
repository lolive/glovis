//---------------------------------------------------------------------------
// Name: AddressTable
//
// Description: AddressTable implements a class to display a table with
//  rows containing an address, latitude, and longitude
//---------------------------------------------------------------------------
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

public class AddressTable extends JTable
{
    private AddressModel tableModel;  // model for the data in the table

    // AddressModel defines the model to use for the AttributeTable
    class AddressModel extends AbstractTableModel
    {
        String[] columnNames = {"Address", "Latitude", "Longitude"};
        String[][] addressData;        // full list of addresses

        // method to return the name of the requested column
        //--------------------------------------------------
        public String getColumnName(int col)
        {
            return columnNames[col];
        }

        // method to set the addresses in the table.  Each address in the 
        // input array should hold the longitude, latitude, then address
        // separated by a ':'.
        //---------------------------------------------------------------
        public void setAddresses(String[] addresses)
        {
            // allocate space for the table data
            addressData = new String[addresses.length][3];

            for (int index = 0; index < addresses.length; index++)
            {
                // split each address apart and save it in the table
                String[] addressList = addresses[index].split(":");
                addressData[index][0] = addressList[2];
                addressData[index][1] =  addressList[1]; 
                addressData[index][2] = addressList[0]; 
            }

            // notify the table that things have changed
            fireTableDataChanged();

            // if only one row in the table, select it automatically
            if (getRowCount() == 1)
                addRowSelectionInterval(0,0);
        }

        // method to return the number of rows in the table
        //-------------------------------------------------
        public int getRowCount()
        {
            if (addressData != null)
                return addressData.length;
            else
                return 0;
        }

        // method to return the number of columns in the table
        //----------------------------------------------------
        public int getColumnCount()
        {
            return columnNames.length;
        }

        // method to return the value in a table cell
        //-------------------------------------------
        public Object getValueAt(int row, int column)
        {
            return addressData[row][column];
        }
        
        // get the latitude and longitude from a row
        //------------------------------------------
        public LatLong getLatLong(int row)
        {
            // convert the latitude to a double
            Double lat = new Double(addressData[row][1]);
            Double lon = new Double(addressData[row][2]);
            LatLong latLong = new LatLong(lat.doubleValue(),lon.doubleValue());
            return latLong; 
        }

        // get the address name from a row
        //--------------------------------
        public String getAddress(int row)
        {
            return new String(addressData[row][0]);
        }

        // clear the table contents
        //-------------------------
        public void clearAddresses()
        {
           addressData = null;

           // notify the table that things have changed
           fireTableDataChanged();
        }
    }

    // Constructor for the attribute table
    //------------------------------------
    public AddressTable()
    {
        tableModel = new AddressModel();
        setModel(tableModel);

        TableColumn column = getColumnModel().getColumn(1);
        column.setMinWidth(50);
        column.setPreferredWidth(100);
        column.setMaxWidth(100);
        column = getColumnModel().getColumn(2);
        column.setMinWidth(50);
        column.setPreferredWidth(100);
        column.setMaxWidth(100);
    }

    // method to set the addresses to display in the table
    //----------------------------------------------------
    public void setAddresses(String[] addresses)
    {
        tableModel.setAddresses(addresses);
    }

    // clear the contents from the table
    //----------------------------------
    public void clearAddresses()
    {
        tableModel.clearAddresses();
    }

    // get the latitude and longitude for a particular row
    //----------------------------------------------------
    public LatLong getLatLong(int row)
    {
        return tableModel.getLatLong(row);
    }

    // get the address for a particular row
    //-------------------------------------
    public String getAddress(int row)
    {
        return tableModel.getAddress(row);
    }
}
