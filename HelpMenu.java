//  HelpMenu.java implements the menu for help options in the applet.
//  
//-------------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.net.URL;
import java.io.BufferedInputStream;

public class HelpMenu extends JMenu implements ActionListener
{
    private imgViewer applet;
    private JMenuItem dataAcqRequestItem;
    private String glovisTitle;
    private String glovisVersion;

    // constructor
    public HelpMenu(imgViewer applet, String title)
    {
        // call the parent constructor, setting the dialog to be modal
        super(title);
        setMnemonic(KeyEvent.VK_H);

        // save the applet object
        this.applet = applet;

        this.glovisTitle = "USGS Global Visualization Viewer (GloVis)";
        this.glovisVersion = "Version: 8.16";

        // add the quick start selection
        JMenuItem item = new JMenuItem("Quick Start Guide", KeyEvent.VK_Q);
        item.addActionListener(this);
        add(item);

        // add the viewer help selection
        item = new JMenuItem("User Guide", KeyEvent.VK_U);
        item.addActionListener(this);
        add(item);

        // add the about browse selection
        item = new JMenuItem("About Browse Images", KeyEvent.VK_B);
        item.addActionListener(this);
        add(item);

        // add the GloVis Brochure selection
        item = new JMenuItem("GloVis Brochure", KeyEvent.VK_L);
        item.addActionListener(this);
        add(item);

        // add the about GloVis selection
        item = new JMenuItem("About GloVis", KeyEvent.VK_G);
        item.addActionListener(this);
        add(item);

        // divide between static menu options and sensor-specific
        addSeparator();

        // add the product info selection
        item = new JMenuItem("Product Information", KeyEvent.VK_P);
        item.addActionListener(this);
        add(item);

        // add the acquisition schedule selection
        item = new JMenuItem("Data Acquisition Schedule", KeyEvent.VK_D);
        item.addActionListener(this);
        add(item);

        // create the menu item for the data acquisition request menu item
        // but don't add it to the menu since it is only visible for some
        // sensors
        dataAcqRequestItem = new JMenuItem("Data Acquisition Request",
                                           KeyEvent.VK_R);
        dataAcqRequestItem.addActionListener(this);

//        item = new JMenuItem("Home", KeyEvent.VK_H);
//        item.addActionListener(this);
//        add(item);

        // configure the menu for the current sensor
        setSensor(applet.sensorMenu.getCurrentSensor());
    }

    // method to configure the menu for the current sensor
    //----------------------------------------------------
    public void setSensor(Sensor currentSensor)
    {
        if (currentSensor.dataAcqRequestURL != null)
        {
            // add the data request menu item
            int insertLocation = 5;
            insert(dataAcqRequestItem,insertLocation);
        }
        else
            remove(dataAcqRequestItem);
    }

    // show "About GloVis" dialog
    // --------------------------
    public void aboutGloVis()
    {
        List<String> about = new ArrayList<String>();
        about.add(this.glovisTitle);
        about.add(this.glovisVersion);

        JOptionPane.showMessageDialog(applet.getDialogContainer(),
            about.toArray(), "About GloVis",
            JOptionPane.INFORMATION_MESSAGE);

        return;
    }

    // event handler for the menu selections
    //--------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        String page = null;
        String targetWindow = null;

        if (command.equals("Quick Start Guide"))
        {
            page = new String("../QuickStart.shtml");   
            targetWindow = new String("glovishelp");
        }
        else if (command.equals("User Guide"))
        {
            page = new String("../ImgViewerHelp.shtml");   
            targetWindow = new String("glovishelp");
        }
        else if (command.equals("About Browse Images"))
        {
            page = new String("../AboutBrowse.shtml");   
            targetWindow = new String("glovishelp");
        }
        else if (command.equals("GloVis Brochure"))
        {
            page = new String("http://pubs.usgs.gov/gip/137/");   
            targetWindow = new String("brochure");
        }
        else if (command.equals("About GloVis"))
        {
            aboutGloVis();
        }
        else if (command.equals("Product Information"))
        {
            page = applet.sensorMenu.getCurrentSensor().productInfoURL;
            targetWindow = new String("_blank");
        }
        else if (command.equals("Data Acquisition Schedule"))
        {
            page = applet.sensorMenu.getCurrentSensor().acquisitionScheduleURL;
            targetWindow = new String("_blank");
        }
        else if (command.equals("Data Acquisition Request"))
        {
            page = applet.sensorMenu.getCurrentSensor().dataAcqRequestURL;
            targetWindow = new String("_blank");
        }
        else if (command.equals("Home"))
        {
            page = new String("../index.shtml");
            targetWindow = new String("_blank");
        }

        if (page != null)
        {
            try
            {
                URL linkURL = new URL(applet.getCodeBase(),page);
                applet.getAppletContext().showDocument(linkURL,targetWindow);
            }
            catch (Exception e){}
        }
    }
}
