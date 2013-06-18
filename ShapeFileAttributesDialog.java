//---------------------------------------------------------------------------
// Name: ShapeFileAttributesDialog
//
// Description: Implements a dialog to display shapefile attributes.
//---------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;


public class ShapeFileAttributesDialog extends JDialog implements 
    WindowListener, ActionListener 
{
    private JTabbedPane tabbedPane; // tabbed pane for the shapefiles
    private AttributeTable attributeTable; // for displaying attributes

    // Constructor for the shape file attributes dialog
    //-------------------------------------------------
    public ShapeFileAttributesDialog(JFrame parent)
    {
        super(parent,"Shapefile Attributes",false);

        // Create tab pane
        tabbedPane = new JTabbedPane();

        // create a panel for displaying the attributes
        JPanel attributeDisplayPanel = new JPanel();
        attributeDisplayPanel.setLayout(new BorderLayout());

        attributeTable = new AttributeTable();
        attributeTable.setToolTipText("Attribute values");

        JScrollPane scrollPane = new JScrollPane(attributeTable);
        attributeDisplayPanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Attributes", attributeDisplayPanel);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_A);
        tabbedPane.setToolTipTextAt(0, "Display attribute values");
        
        // create the button panel
        JPanel buttonPanel = new JPanel();
        JButton closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close the shapefile attributes");
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);

        // place the tabbed pane and button panel in the main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(600,400);

        // request the window events
        addWindowListener(this);
    }

    // method to handle the windowClosing event
    //-----------------------------------------
    public void windowClosing(WindowEvent e)
    {
        setVisible(false);
    }

    // dummy window event handlers for events that do not need handling
    //-----------------------------------------------------------------
    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) { }

    // method to allow adding a new shapefile to the attributes dialog
    public ShapeFileAttributesPanel addShapeFile(String shapeFileName,
                                                 ShapeFileMapLayer mapLayer,
                                                 String[] attributeNames)
    {
        ShapeFileAttributesPanel attributePanel 
                = new ShapeFileAttributesPanel(mapLayer, attributeNames,
                                               attributeTable);

        // add the tab, including the index to the tab in the tab name
        int index = tabbedPane.getTabCount();
        tabbedPane.addTab("" + index + ":" + shapeFileName, attributePanel);

        // set the tool tip and mnemonic to select the tab
        tabbedPane.setToolTipTextAt(index, shapeFileName + " settings");
        int keyEvent = KeyEvent.VK_0 + index;
        if (index > 9)
            keyEvent = KeyEvent.VK_A + index - 10;
        tabbedPane.setMnemonicAt(index, keyEvent);

        // return the attributes panel
        return attributePanel;
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();

        if (command.equals("Close"))
        {
            // hide the dialog box when the close button is pressed
            setVisible(false);
        }
    }
}
