// GridCellEntry.java implements a widget to allow entry of a grid cell
// column and row location.
//---------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.GridLayout;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GridCellEntry extends JPanel implements ActionListener, Observer
{
    private JLabel gridTypeLabel;   // label for grid cell type (i.e. WRS-2)
    private JLabel gridColRowName;  // label for col/row names
    private FocusTextField gridColEditField; // column entry text widget
    private FocusTextField gridRowEditField; // row entry text widget
    private JButton goButton;        // Go button for col/row "teleport"
    private MosaicData md;          // reference to the mosaic data
    private imgViewer applet;       // reference to the applet

    // Constructor for the grid cell entry widget
    //-------------------------------------------
    GridCellEntry(imgViewer applet, MosaicData md)
    {
        this.md = md;
        this.applet = applet;

        // use a box layout for the panel since it does a good job of sizing
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // set the font to use for the widget
        setFont(applet.normalFont);

        // build a panel for the label
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new GridLayout(2,1));

        // build the column/row navigation area
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        NavigationModel navModel = currSensor.navModel;
        gridTypeLabel = new JLabel(navModel.getModelName());
        gridTypeLabel.setFont(applet.boldFont);
        labelPanel.add(gridTypeLabel);
        gridColRowName = new JLabel(navModel.getColName() + " /" 
                         + navModel.getRowName() + ":");
        gridColRowName.setFont(applet.boldFont);
        labelPanel.add(gridColRowName);
        add(labelPanel);

        gridColEditField = new FocusTextField(navModel.getColumnDigits());
        gridColEditField.setToolTipText(navModel.getColName() + " entry");
        gridColEditField.addActionListener(this);
        add(gridColEditField);

        gridRowEditField = new FocusTextField(navModel.getRowDigits());
        gridRowEditField.setToolTipText(navModel.getRowName() + " entry");
        gridRowEditField.addActionListener(this);
        add(gridRowEditField);

        goButton = new JButton("Go");
        goButton.setToolTipText(" Go to " + gridColRowName);
        goButton.addActionListener(this);
        add(goButton);

        // set the size so that it won't grow in height (otherwise the box
        // layout will let it grow too tall)
        Dimension size = getPreferredSize();
        size.width = 100;
        setMinimumSize(size);
        size.width = 240;
        setMaximumSize(size);
    }

    // event handler for button presses and the enter key
    //---------------------------------------------------
    public void actionPerformed(ActionEvent event) 
    {
        parseInput();
    }

    // Method for the Observer interface
    //----------------------------------
    public void update(Observable ob, Object arg)
    {
        // make sure the grid type label is correct
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        NavigationModel navModel = currSensor.navModel;
        gridTypeLabel.setText(navModel.getModelName());

        String colRowName = new String(navModel.getColName() + " /" 
                            + navModel.getRowName() + ":");
        gridColRowName.setText(colRowName);

        // show/hide this panel as needed for the current sensor
        setVisible(!currSensor.hideGridEntry);
        
        gridColEditField.setToolTipText(navModel.getColName() + " entry");
        gridRowEditField.setToolTipText(navModel.getRowName() + " entry");
        gridColEditField.setColumns(navModel.getColumnDigits());
        gridRowEditField.setColumns(navModel.getRowDigits());
        goButton.setToolTipText("Go to " + colRowName);

        // set the gridCol/gridRow no matter where we are
        TOC cell = md.getCurrentCell();
        String col = navModel.getColumnString(cell.gridCol);
        String row = navModel.getRowString(cell.gridRow);
        gridColEditField.setText(col);
        gridRowEditField.setText(row);
    }

    // Method for the Event-Listeners
    // as the "Go" button and "Enter Key" 
    //------------------------------------------
    private void parseInput()
    {
        NavigationModel nm = applet.sensorMenu.getCurrentSensor().navModel;
    
        // parse the column and row text entry widgets and go to that 
        // location if the parsing is successful
        try
        {
            // parse the column and row values from the entry fields
            int col = nm.getColumnNumberFromString(gridColEditField.getText());
            int row = nm.getRowNumberFromString(gridRowEditField.getText());

            // limit the col/row values to legal bounds for the current
            // navigation model
            int boundedCol = nm.checkColumnBounds(col);
            int boundedRow = nm.checkRowBounds(row);

            // if the original col/row is not the same as the bounded 
            // version, the user entered a value that is out of range.
            // A little unusual to do it this way, but it allows this 
            // same code to work for any grid cell layout.
            if (col != boundedCol)
            {
                // column wasn't in legal bounds
                applet.statusBar.showStatus(nm.getColName() + " out of range!");
                gridColEditField.setText("");
            }
            else if (row != boundedRow)
            {
                // row wasn't in legal bounds
                applet.statusBar.showStatus(nm.getRowName() + " out of range!");
                gridRowEditField.setText("");
            }
            else if (!nm.isValidGridCell(boundedCol, boundedRow))
            {
                // the grid cell isn't a legal one
                applet.statusBar.showStatus(nm.getColName() + "=" + col
                    + ", " + nm.getRowName() + "=" + row 
                    + " does not contain data!");
                gridColEditField.setText("");
                gridRowEditField.setText("");
            }
            else
            {
                // col/row were legal, so move to that location
                if (md.canMoveToMapArea(col, row))
                {
                    md.scrollData(col, row, 0, 0, false, true, false);
                }
            }
        }
        catch (NumberFormatException e)
        {
            applet.statusBar.showStatus("Illegal " + nm.getColName() + "/" 
                         + nm.getRowName() + " number format!");
        }
    }
}


