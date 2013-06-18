//---------------------------------------------------------------------------
// Name: CheckBoxPanel
//
// Description: Provides a panel with a list of checkboxes.  The panel is
//      originally built without any checkboxes and they can be set later.
//      An optional title can be placed at the top of the panel and buttons
//      to set and clear all the checkboxes are provided.  The checkboxes
//      are placed in a scrolling panel in case they don't fit in the
//      available space.
//---------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class CheckBoxPanel extends JPanel implements ActionListener
{
    JPanel checkboxPanel;       // panel for the checkboxes
    JCheckBox[] checkboxes;     // array of checkboxes

    // constructor for the panel.  Set the title parameter to null if no title
    // is wanted.
    //------------------------------------------------------------------------
    public CheckBoxPanel(String title)
    {
        setLayout(new BorderLayout());

        // set the title on the panel if one is provided
        if (title != null)
        {
            add(new JLabel(title), BorderLayout.NORTH);
        }

        // create a scrolling panel to hold the checkboxes 
        checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        add(new JScrollPane(checkboxPanel), BorderLayout.CENTER);

        // create buttons to set/clear all the checkboxes
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,2));

        JButton setButton = new JButton("Set All");
        setButton.setMnemonic(KeyEvent.VK_S);
        setButton.setToolTipText("Select all checkboxes");
        setButton.addActionListener(this);
        buttonPanel.add(setButton);

        JButton clearButton = new JButton("Clear All");
        clearButton.setMnemonic(KeyEvent.VK_L);
        clearButton.setToolTipText("Deselect all checkboxes");
        clearButton.addActionListener(this);
        buttonPanel.add(clearButton);

        // put the buttons at the bottom of the panel
        add(buttonPanel, BorderLayout.SOUTH);

        // put a border around the panel
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    // method to set the checkboxes in the panel.  Note that after this method
    // is called, the addActionListener method needs to be called to receive
    // ActionEvents when a checkbox is clicked.
    //------------------------------------------------------------------------
    public void setCheckBoxes(String[] names)
    {
        // if the checkboxes have been set previously, remove the ones that
        // are there so they can be replaced with the new list
        if (checkboxes != null)
        {
            checkboxPanel.removeAll();
            for (int i = 0; i < checkboxes.length; i++)
                checkboxes[i] = null;
            checkboxes = null;
        }

        // add the new checkboxes to the panel
        if (names.length > 0)
        {
            checkboxes = new JCheckBox[names.length];
            for (int i = 0; i < names.length; i++)
            {
                checkboxes[i] = new JCheckBox(names[i], true);
                checkboxPanel.add(checkboxes[i]);
            }
        }
    }

    // method to add an action listener to all the checkboxes.  Note that
    // any time an action event is received from any checkbox, the state
    // of all the checkboxes should be checked since the set/clear all only
    // sends one event, no matter how many buttons changed state.
    //---------------------------------------------------------------------
    public void addActionListener(ActionListener listener)
    {
        if (checkboxes != null)
        {
            for (int i = 0; i < checkboxes.length; i++)
                checkboxes[i].addActionListener(listener);
        }
    }

    // method to return an array of the selected state of all the checkboxes.
    // The order of the array is the same as the list of names originally
    // provided.
    //-----------------------------------------------------------------------
    public boolean[] getSelectedArray()
    {
        boolean[] selected = null;

        if (checkboxes != null)
        {
            selected = new boolean[checkboxes.length];

            for (int i = 0; i < checkboxes.length; i++)
                selected[i] = checkboxes[i].isSelected();
        }
        return selected;
    }

    // action listener for the set/clear all buttons
    //----------------------------------------------
    public void actionPerformed(ActionEvent e)
    {
        if (checkboxes != null)
        {
            String command = e.getActionCommand();

            if (command.equals("Set All"))
            {
                // set all checkboxes, except the first, to selected
                for (int i = 1; i < checkboxes.length; i++)
                    checkboxes[i].setSelected(true);
                // simulate a click on the first checkbox to set it to selected
                // so an action event is generated
                checkboxes[0].setSelected(false);
                checkboxes[0].doClick();
            }
            else if (command.equals("Clear All"))
            {
                // set all checkboxes, except the first, to not selected
                for (int i = 1; i < checkboxes.length; i++)
                    checkboxes[i].setSelected(false);
                // simulate a click on the first checkbox to set it to not
                // selected so an action event is generated
                checkboxes[0].setSelected(true);
                checkboxes[0].doClick();
            }
        }
    }
}
