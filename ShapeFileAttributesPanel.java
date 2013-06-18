// ShapeFileAttributesPanel.java implements a panel for selecting the shapefile
// attributes to display and filters to apply.
//-----------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class ShapeFileAttributesPanel extends JPanel implements ActionListener 
{
    private CheckBoxPanel checkboxPanel; // checkboxes for the attributes to
                                         // display
    private JButton colorButton;         // button to change the map layer color
    private JComboBox activeAttribute;   // combobox to select the active attrib
    private AttributeFilterPanel attribFilterPanel;
    private JButton applyButton;         // button to apply filter changes
    private ShapeFileMapLayer mapLayer;  // reference to the map layer
    private AttributeTable attributeTable;// reference to the attribute table
    private String[] attributeNames;     // attributes names for this layer

    // The AttributeFilterPanel class implements the GUI panel for setting the
    // filter conditions for the map layer.
    //------------------------------------------------------------------------
    class AttributeFilterPanel extends JPanel implements ActionListener
    {
        private JComboBox filterAttribute;// combobox with available attributes
        private JTextField filterValue;   // text entry for the filter value

        // constructor for the AttributeFilterPanel
        //-----------------------------------------
        AttributeFilterPanel(String[] attributeNames)
        {
            // create the filter attributes combobox
            filterAttribute = new JComboBox();
            filterAttribute.setActionCommand("FilterAttribute");
            filterAttribute.setToolTipText("Select attribute for filtering");
            filterAttribute.addItem("None");
            for (int i = 0; i < attributeNames.length; i++)
                filterAttribute.addItem(attributeNames[i]);
            filterAttribute.addActionListener(this);

            // create the filter value text entry box
            filterValue = new JTextField("", 15);
            filterValue.setEnabled(false);
            filterValue.setToolTipText("Enter attribute value");
            filterValue.setActionCommand("Apply");
            filterValue.addActionListener(this);

            // add the components to the panel
            add(filterAttribute);
            add(new JLabel(" = "));
            add(filterValue);
        }

        // method to apply the current filter configuration to the map layer
        //------------------------------------------------------------------
        public void applyFilter()
        {
            String attribute = (String)filterAttribute.getSelectedItem();
            String value = filterValue.getText().trim();
            mapLayer.setFilter(attribute, value);
        }

        // action performed event handler
        //-------------------------------
        public void actionPerformed(ActionEvent e) 
        {
            String command = e.getActionCommand();

            if (command.equals("FilterAttribute"))
            {
                String attribute = (String)filterAttribute.getSelectedItem();
                // if the selected filter attribute is "None", clear the text
                // and disable the text entry
                if (attribute.equals("None"))
                {
                    filterValue.setText("");
                    filterValue.setEnabled(false);
                }
                else
                    filterValue.setEnabled(true);

                applyFilter();
            }
            else if (command.equals("Apply"))
            {
                applyFilter();
            }
        }
    }

    // Constructor for the shape file attributes panel
    //------------------------------------------------
    public ShapeFileAttributesPanel(ShapeFileMapLayer mapLayer,
                                    String[] attributeNames,
                                    AttributeTable attributeTable)
    {
        this.mapLayer = mapLayer;
        this.attributeTable = attributeTable;

        // create the panel for selecting which attributes to show
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        checkboxPanel = new CheckBoxPanel("Displayed Attributes:");
        add(checkboxPanel);

        JPanel optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));

        // create a button to allow changing the attribute color
        JPanel colorPanel = new JPanel();
        colorPanel.add(new JLabel("Shape Color: "));
        colorButton = new JButton("Change");
        colorButton.setActionCommand("SetColor");
        colorButton.setBackground(mapLayer.getColor());
        colorButton.setMnemonic(KeyEvent.VK_H);
        colorButton.setToolTipText("Change shape color");
        colorButton.addActionListener(this);
        colorPanel.add(colorButton);
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        optionPanel.add(colorPanel);

        // create a combo box to select the attribute to show in the statusbar
        JPanel activeAttributePanel = new JPanel();
        activeAttributePanel.add(new JLabel("Active Attribute:"));
        activeAttribute = new JComboBox();
        activeAttribute.setActionCommand("ActiveAttribute");
        activeAttribute.addItem("None");
        activeAttribute.setToolTipText("Select status bar attribute");
        activeAttribute.addActionListener(this);
        activeAttributePanel.add(activeAttribute);
        activeAttributePanel.setBorder(
                BorderFactory.createLineBorder(Color.black));
        optionPanel.add(activeAttributePanel);

        // create an attribute filter panel to filter shapes
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        JPanel tempPanel = new JPanel();
        tempPanel.add(new JLabel("Only Display:"));
        filterPanel.add(tempPanel);
        
        attribFilterPanel = new ShapeFileAttributesPanel.AttributeFilterPanel(attributeNames);
        filterPanel.add(attribFilterPanel);

        tempPanel = new JPanel();
        applyButton = new JButton("Apply");
        applyButton.setMnemonic(KeyEvent.VK_A);
        applyButton.setToolTipText("Apply current filter");
        applyButton.addActionListener(this);
        tempPanel.add(applyButton);
        filterPanel.add(tempPanel);
        filterPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        optionPanel.add(filterPanel);

        // force the other panels in the option panel to their smallest size
        // by inserting a filler box that wants as much space as it can get
        optionPanel.add(new Box.Filler(new Dimension(5,5),
                            new Dimension(5,Short.MAX_VALUE),
                            new Dimension(5,Short.MAX_VALUE)));
        add(optionPanel);

        // set the maximum width of the checkboxPanel to the minimum width to
        // prevent it from using more space than needed
        setAttributeNames(attributeNames);
        Dimension size = checkboxPanel.getMinimumSize();
        size.height = 2000;
        checkboxPanel.setMaximumSize(size);

        // allow the user to select the starting color
        selectLayerColor();
    }

    // method to update a combobox with new entries
    //---------------------------------------------
    private void updateComboBox(JComboBox comboBox, String[] names,
                                boolean autoselect)
    {
        // update the active attribute combobox
        String selectedItem = (String) comboBox.getSelectedItem();

        // remove the old entries from the combobox, leaving the first
        int count = comboBox.getItemCount();
        for (int i = count - 1; i > 0; i--)
            comboBox.removeItemAt(i);

        // add the names to the combobox
        for (int i = 0; i < names.length; i++)
            comboBox.addItem(names[i]);

        // if autoselect is requested, default the selection in the combobox
        if (autoselect)
        {
            if (!selectedItem.equals("None"))
                comboBox.setSelectedItem(selectedItem);
            if (comboBox.getSelectedIndex() <= 0)
            {
                comboBox.setSelectedItem("NAME");
                if (comboBox.getSelectedIndex() <= 0)
                    comboBox.setSelectedItem("FIRENAME");
                if (comboBox.getSelectedIndex() <= 0)
                    comboBox.setSelectedItem("None");
            }
        }
        else
            comboBox.setSelectedItem("None");
    }

    // method to set the attribute names in the panel
    //-----------------------------------------------
    private void setAttributeNames(String[] names)
    {
        attributeNames = names;

        // set the attribute names in the checkbox panel
        checkboxPanel.setCheckBoxes(names);
        checkboxPanel.addActionListener(this);

        // update the active attribute combobox
        updateComboBox(activeAttribute, names, true);

        // if this is the layer currently shown in the attribute table, update
        // the names shown there
        if (attributeTable.getCurrentLayer() == null)
        {
            boolean[] selected = getSelectedArray();
            attributeTable.setAttributeNames(mapLayer, attributeNames,
                                             selected);
        }
    }

    // method to set the attribute values in the attribute table
    //----------------------------------------------------------
    public void setAttributeValues(String[] values)
    {
        boolean[] selected = getSelectedArray();
        attributeTable.setAttributeNames(mapLayer, attributeNames, selected);
        attributeTable.setAttributeValues(mapLayer, values, selected);
    }

    // method to return an array indicating which attributes are selected in
    // the checkbox panel
    //----------------------------------------------------------------------
    private boolean[] getSelectedArray()
    {
        return checkboxPanel.getSelectedArray();
    }

    // method to allow the user to select the color to use for the map layer
    //----------------------------------------------------------------------
    private void selectLayerColor()
    {
        Color color = JColorChooser.showDialog(this,
                   "Choose color for drawing shapes", mapLayer.getColor());
        if (color != null)
        {
            mapLayer.setColor(color);
            colorButton.setBackground(color);
        }
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();

        if (command.equals("ActiveAttribute"))
        {
            // update the active attribute
            String attribute = (String)activeAttribute.getSelectedItem();
            mapLayer.setActiveAttribute(attribute);
        }
        else if (command.equals("Apply"))
        {
            // apply the current filter conditions
            attribFilterPanel.applyFilter();
        }
        else if (command.equals("SetColor"))
        {
            // select the color for the map layer
            selectLayerColor();
        }
        else
        {
            // if this map layer is currently displayed in the attribute table,
            // update the attribute table since the attributes selected for
            // display changed
            if (attributeTable.getCurrentLayer() == mapLayer)
            {
                boolean[] selected = getSelectedArray();
                attributeTable.updateVisible(selected);
            }
        }
    }
}
