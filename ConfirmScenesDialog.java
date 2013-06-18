// ConfirmScenesDialog.java implements a dialog that warns the user of scenes
// of questionable quality and allows them to specify which scenes to order
// anyway.
//
//---------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ConfirmScenesDialog extends JDialog implements WindowListener, 
    ActionListener
{
    private Metadata[] scenes;    // scenes that need to be confirmed
    private boolean cancelled;    // flag to indicate the dialog was cancelled
    private JCheckBox[] checkBoxes; // checkbox for each scene
    private int goodSceneCount;   // count of good scenes that can be ordered
    private JButton orderButton;  // button for ordering the scenes
    
    // Constructor for the scene list dialog
    //--------------------------------------
    public ConfirmScenesDialog(Metadata[] badScenes, 
                               int goodSceneCount)
    {
        super(new JFrame(), "Include Low Quality Scenes?", true);

        // save the list of poor quality scenes
        this.scenes = badScenes;
        this.goodSceneCount = goodSceneCount;

        cancelled = false;

        // use a border layout for the dialog box
        getContentPane().setLayout(new BorderLayout());
        
        // set up the panel for the scenes to confirm
        JPanel scenePanel = new JPanel();
        scenePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;

        int displayLine = 0;    // track the current line for the layout

        // add an explanation message to the dialog
        String[] message =
            {"The following scenes have excessive cloud cover or poor quality."
             + "  Do you really",
             "want to include them?",
             " ",
             "Select the scenes you want to include and press the "
                 + "Continue button to proceed ",
             "with the selected scenes plus any scenes from your "
                 + "original list that are not",
             "listed below.  Unselected scenes will remain in the "
                 + "scene list.  Pressing the",
             "Cancel button will cancel all scenes and leave all "
                 + "scenes in the scene list.", 
             " "};
        for (int i = 0; i < message.length; i++)
        {
            JLabel line = new JLabel(message[i]);
            gbc.gridy = displayLine;
            scenePanel.add(line, gbc);
            displayLine++;
        }

        // create a checkbox for each scene
        boolean hasCloudCover = scenes[0].getSensor().hasCloudCover;
        checkBoxes = new JCheckBox[scenes.length];
        for (int i = 0; i < scenes.length; i++)
        {
            Metadata scene = scenes[i];
            String entry = "Scene ID: " + scene.entityID;

            // include cloud cover if the sensor has it
            if (hasCloudCover)
                entry += ", " + scene.cloudCover + "% Cloud Cover";

            // include the quality values if present
            if (scene.quality != null)
            {
                int numQuality = scene.quality.length;
                for (int qual = 0; qual < numQuality; qual++)
                {
                    if (numQuality > 1)
                    {
                        entry += ", Quality " + (qual + 1) + ": " 
                                 + scene.quality[qual];
                    }
                    else
                        entry += ", Quality: " + scene.quality[qual];
                }
            }

            // create and add the checkbox
            checkBoxes[i] = new JCheckBox(entry);
            checkBoxes[i].setToolTipText("Confirm "+ scene.entityID);
            gbc.gridy = displayLine;
            scenePanel.add(checkBoxes[i], gbc);
            displayLine++;

            // if no good scenes exist, sign up for action events on the 
            // checkboxes so the order button can be properly enabled only
            // when scenes will be ordered
            if (goodSceneCount == 0)
                checkBoxes[i].addActionListener(this);
        }

        // put the scene panel in a scrollpane
        JScrollPane scrollArea = new JScrollPane(scenePanel);

        // set up the buttons
        JPanel buttonPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        buttonPanel.setLayout(gridbag);

        orderButton = new JButton("Continue");
        orderButton.setMnemonic(KeyEvent.VK_O);
        orderButton.setToolTipText("Continue with selected scenes");
        orderButton.addActionListener(this);
        if (goodSceneCount == 0)
            orderButton.setEnabled(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setToolTipText("Cancel all scenes");
        cancelButton.addActionListener(this);

        gbc = new GridBagConstraints();
        gbc.weighty = 0;
        gbc.weightx = 20;
        gbc.fill = GridBagConstraints.BOTH;

        buttonPanel.add(orderButton,gbc);
        // reset the width to one column
        gbc.gridwidth = 1;
        // make the close button the last one in this row
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(cancelButton,gbc);

        // add the scene list and button panels to the dialog
        getContentPane().add(scrollArea,"Center");
        getContentPane().add(buttonPanel,"South");

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(520,350);

        // request the window events
        addWindowListener(this);
    }

    // method to handle the windowClosing event
    //-----------------------------------------
    public void windowClosing(WindowEvent e)
    {
        // if the window is closed without hitting the order/cancel buttons,
        // consider the operation cancelled
        cancelled = true;
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

    // method to indicate if the dialog box was cancelled
    //---------------------------------------------------
    public boolean wasCancelled()
    {
        return cancelled;
    }

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();

        if (command.equals("Continue"))
        {
            // update the visible state of the scenes based on which ones the
            // user checked
            for (int i = 0; i < scenes.length; i++)
            {
                if (checkBoxes[i].isSelected())
                    scenes[i].visible = true;
            }

            // hide the dialog box
            setVisible(false);
        }
        else if (command.equals("Cancel"))
        {
            cancelled = true;

            // hide the dialog box
            setVisible(false);
        }
        else
        {
            // a checkbox must have changed state, so decide whether to 
            // enable the order button
            if (goodSceneCount == 0)
            {
                boolean sceneSelected = false;
                for (int i = 0; i < scenes.length; i++)
                {
                    if (checkBoxes[i].isSelected())
                    {
                        sceneSelected = true;
                        break;
                    }
                }
                orderButton.setEnabled(sceneSelected);
            }
        }
    }
}
