// ArrowPane.java implements a class for displaying the four directional arrow
// buttons on the display and detecting when the arrows are clicked on.
//
//----------------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * ArrowPane is a GUI widget to present up, down, left, and right arrow buttons
 * for navigating around the world.
 */
public class ArrowPane extends JPanel implements ActionListener
{
    private imgViewer applet;
    private JButton rightButton;
    private JButton leftButton;
    private JButton upButton;
    private JButton downButton;
    private ImageIcon upIcon;
    private ImageIcon downIcon;
    private ImageIcon leftIcon;
    private ImageIcon rightIcon;

    /**
     * Constructor for the ArrowPane that loads the needed resources and 
     * initializes the state of the object.
     * @param applet The applet that contains the arrow pane.  Used for 
     *               obtaining the applet code base for loading images
     *               and for invoking methods on other objects.
     */
    public ArrowPane(imgViewer applet)
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        this.applet = applet;

        // create the panels for laying out the buttons
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // create the buttons
        upButton = new JButton();
        downButton = new JButton();
        leftButton = new JButton();
        rightButton = new JButton();

        try 
        {
            // read the images for the buttons and set them in the buttons
            URL iconUrl;

            iconUrl = new URL(applet.getCodeBase(), "graphics/upbutton.gif");
            upIcon = new ImageIcon(iconUrl);

            iconUrl = new URL(applet.getCodeBase(), "graphics/downbutton.gif");
            downIcon = new ImageIcon(iconUrl);

            iconUrl = new URL(applet.getCodeBase(), "graphics/leftbutton.gif");
            leftIcon = new ImageIcon(iconUrl);

            iconUrl = new URL(applet.getCodeBase(), "graphics/rightbutton.gif");
            rightIcon = new ImageIcon(iconUrl);

            upButton.setIcon(upIcon);
            downButton.setIcon(downIcon);
            leftButton.setIcon(leftIcon);
            rightButton.setIcon(rightIcon);

            // give a size hint to the layout manager
            Dimension buttonSize = new Dimension(32,35);
            upButton.setPreferredSize(buttonSize);
            downButton.setPreferredSize(buttonSize);
            leftButton.setPreferredSize(buttonSize);
            rightButton.setPreferredSize(buttonSize);
        }
        catch (MalformedURLException e)
        {
            // note, there should never be a MalformedURLException, but just
            // in case there is, use text in the buttons as a fallback
            upButton.setText("U");
            downButton.setText("D");
            leftButton.setText("L");
            rightButton.setText("R");
        }

        // add the buttons to the correct panels
        leftPanel.add(leftButton);
        rightPanel.add(rightButton);
        centerPanel.add(upButton);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(downButton);

        // add the button panels to the main panel, with spacing struts to get
        // the distance between the buttons correct
        add(leftPanel);
        add(Box.createHorizontalStrut(3));
        add(centerPanel);
        add(Box.createHorizontalStrut(3));
        add(rightPanel);
        
        // set the tool tips
        rightButton.setToolTipText("Move right");
        leftButton.setToolTipText("Move left");
        upButton.setToolTipText("Move up");
        downButton.setToolTipText("Move down");

        // register for the action events on the buttons
        rightButton.addActionListener(this);
        leftButton.addActionListener(this);
        upButton.addActionListener(this);
        downButton.addActionListener(this);
    }

    // action performed listener
    //--------------------------
    public void actionPerformed(ActionEvent event)
    {
        int raState = 0;
        int laState = 0;
        int uaState = 0;
        int daState = 0;

        JButton source = (JButton)event.getSource();

        if (source == rightButton)
            raState = 1;
        else if (source == leftButton)
            laState = 1;
        else if (source == upButton)
            uaState = 1;
        else if (source == downButton)
            daState = 1;

        // update the applet scroll position
        applet.imgArea.md.scrollInDirection(raState,laState,uaState,daState);
    }   

    // method to cleanup any resources when the applet is stopped
    //-----------------------------------------------------------
    public void cleanup()
    {
        if (upIcon != null)
            upIcon.getImage().flush();
        if (downIcon != null)
            downIcon.getImage().flush();
        if (leftIcon != null)
            leftIcon.getImage().flush();
        if (rightIcon != null)
            rightIcon.getImage().flush();
        upIcon = null;
        downIcon = null;
        leftIcon = null;
        rightIcon = null;
    }
}
