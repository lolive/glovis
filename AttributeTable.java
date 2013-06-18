//---------------------------------------------------------------------------
// Name: AttributeTable
//
// Description: AttributeTable implements a class to display a table with
//      attribute names and values.
//---------------------------------------------------------------------------
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

public class AttributeTable extends JTable
{
    private AttributeModel tableModel;  // model for the data in the table
    private ShapeFileMapLayer layer;    // map layer that is currently displayed

    // AttributeModel defines the model to use for the AttributeTable
    class AttributeModel extends AbstractTableModel
    {
        String[] columnNames = {"Attribute", "Value"};
        String[] attributeNames;        // full list of attribute names
        String[] visibleAttributeNames; // list of visible attribute names
        String[] attributeValues;       // full list of attribute values
        String[] visibleAttributeValues;// list of visible attribute values

        // method to return the name of the requested column
        //--------------------------------------------------
        public String getColumnName(int col)
        {
            return columnNames[col];
        }

        // method to set the attribute names, along with an indication of
        // which ones are currently visible for display
        //---------------------------------------------------------------
        public void setAttributeNames(String[] names, boolean[] visible)
        {
            // save the list of names and allocate space for the values
            attributeNames = names;
            attributeValues = new String[names.length];

            // initialize the attribute values to an empty string
            for (int i = 0; i < attributeValues.length; i++)
                attributeValues[i] = "";

            // update the visible names and values arrays
            updateVisible(visible);
        }

        // method to set the attribute values, along with an indication of 
        // which ones are visible
        //----------------------------------------------------------------
        public void setAttributeValues(String[] values, boolean[] visible)
        {
            // save the full list of values
            attributeValues = values;

            // copy the visible values to the visible values array
            int index = 0;
            for (int i = 0; i < values.length; i++)
            {
                if (visible[i])
                {
                    visibleAttributeValues[index] = values[i];
                    index++;
                }
            }

            // notify the table that things have changed
            fireTableDataChanged();
        }

        // method to update which attributes are visible
        //----------------------------------------------
        public void updateVisible(boolean[] visible)
        {
            // count the visible rows
            int visibleRows = 0;
            for (int i = 0; i < visible.length; i++)
            {
                if (visible[i])
                    visibleRows++;
            }

            // create new visible arrays of the correct size
            visibleAttributeNames = new String[visibleRows];
            visibleAttributeValues = new String[visibleRows];

            // copy the attributes that are visible to the visible arrays
            int index = 0;
            for (int i = 0; i < visible.length; i++)
            {
                if (visible[i])
                {
                    visibleAttributeNames[index] = attributeNames[i];
                    visibleAttributeValues[index] = attributeValues[i];
                    index++;
                }
            }

            // notify the table that there has been a change
            fireTableDataChanged();
        }

        // method to return the number of rows in the table
        //-------------------------------------------------
        public int getRowCount()
        {
            if (visibleAttributeNames != null)
                return visibleAttributeNames.length;
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
            if (column == 0)
                return visibleAttributeNames[row];
            else
                return visibleAttributeValues[row];
        }

        // enabled editing of the cells so that the values can be copied
        // from the table.  If the user actually edits the contents, it won't
        // actually be changed since setValueAt is not implemented.
        //-------------------------------------------------------------------
        public boolean isCellEditable(int rowIndex, int columnIndex) 
        {
            return true;
        }
    }

    // Constructor for the attribute table
    //------------------------------------
    public AttributeTable()
    {
        tableModel = new AttributeModel();
        setModel(tableModel);

        TableColumn column = getColumnModel().getColumn(0);
        column.setPreferredWidth(150);
        column.setMinWidth(100);
        column.setMaxWidth(150);
        column = getColumnModel().getColumn(1);
        column.setPreferredWidth(450);
    }

    // method to set the attribute names to display in the table
    //----------------------------------------------------------
    public void setAttributeNames(ShapeFileMapLayer layer, String[] names,
                                  boolean[] selected)
    {
        this.layer = layer;
        tableModel.setAttributeNames(names, selected);
    }

    // method to set the attribute values to display in the table
    //-----------------------------------------------------------
    public void setAttributeValues(ShapeFileMapLayer layer, String[] values,
                                   boolean[] selected)
    {
        this.layer = layer;
        tableModel.setAttributeValues(values, selected);
    }

    // method to update which attributes are visible in the table
    //-----------------------------------------------------------
    public void updateVisible(boolean[] selected)
    {
        tableModel.updateVisible(selected);
    }

    // method to return which map layer is currently being displayed
    //--------------------------------------------------------------
    public ShapeFileMapLayer getCurrentLayer()
    {
        return layer;
    }
}
