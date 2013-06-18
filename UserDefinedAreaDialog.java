// UserDefinedArea.java implements a dialog box that allows the user to 
// create an user defined area by clicking points on the screen.
//------------------------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class UserDefinedAreaDialog extends JDialog implements WindowListener, 
    ActionListener
{
    private JButton exitButton;         // accepts the polygon and closes
    private JButton clearButton;        // clears the current UDA
    private JButton undoButton;         // removes the last point added
    private JButton showButton;         // move screen to polygon
    private JCheckBox checkBox;         // checkbox for enabling UDA
    private imgViewer applet;           // pointer to applet
    private MosaicData md;              // pointer to mosaic data
    private UserDefinedArea userDefinedArea;    // object that has methods for
                                                // supporting the user defined
                                                // area

    // Constructor for the user defined area dialog
    //---------------------------------------------
    public UserDefinedAreaDialog(JFrame parent, imgViewer applet, MosaicData md)
    {
        super(parent, "User Defined Area", false);
        this.applet = applet;
        this.md = md;

        userDefinedArea = new UserDefinedArea(applet,md,this);

        getContentPane().setLayout(new BorderLayout());

        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new GridLayout(3,1));

        // build labels describing the function of this dialog box
        JLabel temp = new JLabel("Please click points to create area.");
        tempPanel.add(temp);

        temp = new JLabel("Note: Normal mouse commands are");
        tempPanel.add(temp);

        temp = new JLabel("disabled while this dialog box is open.");
        tempPanel.add(temp);

        // checkbox to activate the user defined area
        checkBox = new JCheckBox("Close Area Polygon & Apply User Defined Area",
                                 false);
        checkBox.setToolTipText("Close area polygon & apply user defined area");                         
        checkBox.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (checkBox.isSelected())
                        userDefinedArea.closeAndApplyArea();
                    else
                        userDefinedArea.uncloseArea();
                }
            });
        checkBox.setEnabled(false);
        
        // create buttons for dialog box
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,4));
        
        exitButton = new JButton("Exit");
        exitButton.setMnemonic(KeyEvent.VK_E);
        exitButton.setToolTipText("Exit user defined area");
        exitButton.addActionListener(this);

        clearButton = new JButton("Clear");
        clearButton.setMnemonic(KeyEvent.VK_L);
        clearButton.setToolTipText("Clear user defined area");
        clearButton.addActionListener(this);

        undoButton = new JButton("Undo");
        undoButton.setMnemonic(KeyEvent.VK_U);
        undoButton.setToolTipText("Undo last operation");
        undoButton.addActionListener(this);
        undoButton.setEnabled(false);

        showButton = new JButton("Show");
        showButton.setMnemonic(KeyEvent.VK_S);
        showButton.setToolTipText("Show user defined area");
        showButton.addActionListener(this);
        showButton.setEnabled(false);

        buttonPanel.add(exitButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(showButton);

        getContentPane().add(tempPanel,"North");
        getContentPane().add(checkBox, "Center");
        getContentPane().add(buttonPanel, "South");

        setSize(340,200);

        addWindowListener(this);
    }

    // method to return if dialog box is visible or not
    //-------------------------------------------------
    public boolean isDialogBoxVisible()
    {
        return isShowing();
    }

    // method to return if the polygon is closed or not
    //-------------------------------------------------
    public boolean isUserDefinedAreaClosed()
    {
        return userDefinedArea.getPolygonIsClosed();
    }

    // method to enable/disable the checkbox
    //--------------------------------------
    public void enableButtons()
    {
        if (userDefinedArea.numberOfPolygonPoints() > 0)
        {
            // closing the polygon at this time causes an invalid polygon
            if (userDefinedArea.doesCloseCauseIntersect())
            {
                checkBox.setSelected(false);
                checkBox.setEnabled(false);
            }
            // polygon able to close
            else
            {
                checkBox.setEnabled(true);
                checkBox.setSelected(userDefinedArea.getPolygonIsClosed());
            }
            showButton.setEnabled(true);
        }
        // if no points are selected, not possible to make polygon
        else
        {
            checkBox.setSelected(false);
            checkBox.setEnabled(false);
            showButton.setEnabled(false);
        }

        // enable/disable undo button
        if (userDefinedArea.isUndoStackEmpty())
        {
            undoButton.setEnabled(false);
        }
        else
        {
            undoButton.setEnabled(true);
        }
    }

    // method to return the user defined area class
    //---------------------------------------------
    public UserDefinedArea getUserDefinedArea()
    {
        return userDefinedArea;
    }
    
    // method to handle the windowClosing event
    //-----------------------------------------
    public void windowClosing(WindowEvent e)
    {
        setVisible(false);
    }

    // method to handle the windowOpened event
    //-------------------------------------------
    public void windowOpened(WindowEvent e) 
    {
        // repaint the screen to draw any partial polygon that may have been
        // made previously.
        applet.imgArea.repaint();
    }

    // dummy window event handlers for events that do not need handling
    //-----------------------------------------------------------------
    public void windowClosed(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        
        if (command.equals("Exit"))
        {
            userDefinedArea.applyUserDefinedArea();
            
            // see if the currently selected scene is in the user defined area
            // if its not, make current scene one in the user defined area
            TOC cell = md.getCurrentCell();
            if (cell.valid)
            {
                Metadata currScene = cell.scenes[cell.currentDateIndex];
                if (userDefinedArea.getPolygonIsClosed()
                        && !userDefinedArea.sceneIntersects(currScene))
                {
                    md.sceneFilter.gotoLastDate();
                }
            }
            setVisible(false);
        }
        else if (command.equals("Clear"))
        {
            userDefinedArea.clearPolygon();
        }
        else if (command.equals("Undo"))
        {
            userDefinedArea.returnToPrevState();
        }
        else if (command.equals("Show"))
        {
            userDefinedArea.moveScreenToPolygon();
        }
    }
}

