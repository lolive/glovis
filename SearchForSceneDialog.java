// SearchForSceneDialog.java implements a dialog for searching for a scene
// by its scene ID.
//
//---------------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;  // invokeLater

public class SearchForSceneDialog extends JDialog implements WindowListener,
    ActionListener
{
    private JLabel hintMessage;     // area for providing the user a hint for
                                    // the format of the scene id
    private JTextField idEntry;     // scene id entry field
    private JTextArea statusDisplay;// text area for displaying status info
    private JPanel buttonPanel;     // panel for action buttons
    private JButton searchButton;   // button to search for the scene ID
    private JButton closeButton;    // button to close the dialog box
    private imgViewer applet;       // reference to the main applet

    // class for tracking the parameters to the search thread
    private class SearchParameters
    {
        public imgViewer applet;
        public SearchForSceneDialog searchDialog;
        public StringBuffer searchResults;
        public String searchId;
        public Sensor sensor;
    }

    // Constructor for the scene list dialog
    //--------------------------------------
    public SearchForSceneDialog(JFrame parent, imgViewer applet)
    {
        super(parent,"Search for Scene",false);
        this.applet = applet;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // make an area for providing a hint to the user on the scene id
        JPanel hintPanel = new JPanel();
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        hintMessage = new JLabel(currSensor.sceneIdHint);
        hintPanel.add(hintMessage);

        // make the scene id entry field
        idEntry = new JTextField("",50);
        Dimension size = idEntry.getPreferredSize();
        idEntry.setMinimumSize(size);
        size.width *= 3;
        idEntry.setMaximumSize(size);
        idEntry.setToolTipText("Enter scene ID");
        idEntry.addActionListener(this);

        // make the status display area
        statusDisplay = new JTextArea("",3,50);
        statusDisplay.setToolTipText("Search results");
        statusDisplay.setEditable(false);
        statusDisplay.setLineWrap(true);
        statusDisplay.setWrapStyleWord(true);

        // make the buttons for the dialog
        searchButton = new JButton("Search");
        searchButton.setMnemonic(KeyEvent.VK_S);
        searchButton.setToolTipText("Search for scene");
        searchButton.addActionListener(this);

        closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close search for scene");
        closeButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2));
        buttonPanel.add(searchButton);
        buttonPanel.add(closeButton);

        panel.add(hintPanel);
        panel.add(idEntry);
        panel.add(statusDisplay);
        panel.add(buttonPanel);

        getContentPane().add(panel);

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(420,200);

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

    // action performed event handler
    //-------------------------------
    public void actionPerformed(ActionEvent e) 
    {
        String command = e.getActionCommand();

        if (command.equals("Close"))
        {
            idEntry.setText("");
            statusDisplay.setText("");
            // hide the dialog box
            setVisible(false);
        }
        else
        {
            searchById();
        }
    }

    // method to get the id to search for, create a new search thread
    // and start it
    //---------------------------------------------------------------
    private void searchById()
    {
        // get the id to search for, trimming off any leading or trailing
        // spaces
        String id = idEntry.getText().trim();

        // don't bother searching if the user hasn't entered anything
        if (!id.equals(""))
        {
            // issue a message on which ID is being searched for
            statusDisplay.setText("Searching for " + id + "\n");
            idEntry.setText("");

            // create parameter object for the search
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.applet = applet;
            searchParameters.searchDialog = this;
            searchParameters.searchId = id;
            searchParameters.searchResults = new StringBuffer("");
            searchParameters.sensor = applet.sensorMenu.getCurrentSensor();

            // create a new search thread and start it.  If a previous
            // search thread was started, it is just forgotten and its
            // results ignored.
            SearchThread searchThread = new SearchThread(searchParameters);
            searchThread.start();
        }
    }

    // method to update the hint for the currently displayed sensor
    //-------------------------------------------------------------
    public void setSensor(Sensor newSensor)
    {
        hintMessage.setText(newSensor.sceneIdHint);

        // clear the status display since a sensor switch makes it obsolete
        statusDisplay.setText("");
    }

    // This private class implements a thread to perform the scene ID searches.
    // It is implemented as a separate thread so that the search does not 
    // make the GUI thread stop waiting for the results.
    //------------------------------------------------------------------------
    private class SearchThread extends Thread
    {
        SearchParameters params;    // search parameters for this thread

        // constructor for the search thread
        //----------------------------------
        SearchThread(SearchParameters params)
        {
            super();
            this.params = params;
        }

        // actual routine to do the search
        //--------------------------------
        public void run()
        {
            try
            {
                String inputLine = params.searchDialog.
                    callSearchCgi(params.sensor, params.searchId);

                // save the results of the search
                params.searchResults.append(inputLine);

                // process the results in the GUI thread since the results
                // could result in actions that need to take place in the GUI
                // thread
                SwingUtilities.invokeLater(new ProcessResults(params));
                params = null;
            }
            catch (Exception e)
            {
                System.out.println("search thread exception: " + e.toString());
            }
        }
    }

    // method to call the search cgi script and return the results
    //------------------------------------------------------------
    private String callSearchCgi(Sensor sensor, String sceneId)
    {
        String searchResult = null;

        // get the list of sensors for this sensor in case it is a combined
        // dataset
        Sensor[] sensors = sensor.getSensorList();

        try
        {
            // build the cgi URL for the search query.  Use the cgiDatasetName
            // if the sensor has one, otherwise use the normal datasetName
            String cgiCommand = "searchForScene.cgi?";
            for (int i = 0; i < sensors.length; i++)
            {
                if (sensors[i].cgiDatasetName == null)
                    cgiCommand += "sensor=" + sensors[i].datasetName + "&";
                else
                    cgiCommand += "sensor=" + sensors[i].cgiDatasetName + "&";
            }
            // *sigh* in Sensor.java when invoking show<whatever>Metadata.cgi
            // or show<whatever>Browse.cgi, the parameter is called "dataset"
            cgiCommand += "scene_id=" + URLEncoder.encode(sceneId, "UTF-8");

            URL searchURL = new URL(applet.getCodeBase(), cgiCommand);

            // open the connection to the web server
            URLConnection cgiConnection = searchURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                        cgiConnection.getInputStream()));

            // read the single line of output from the web server and close
            // the connection
            String inputLine = in.readLine();
            in.close();

            searchResult = inputLine;
        }
        catch (Exception e)
        {
            System.out.println("search exception: " + e.toString());
        }

        return searchResult;
    }

    // method to parse the search CGI script results and build a metadata
    // object
    //-------------------------------------------------------------------
    private Metadata parseSearchResult(Sensor sensor, String searchResults)
    {
        Metadata scene = null;

        // if a scene was found, build a Metadata object for it
        if (searchResults.startsWith("Found:"))
        {
            // the cgi script reported finding a matching scene, so 
            // try to build the metadata object for it
            try
            {
                int index = searchResults.indexOf(',');
                int index2 = searchResults.indexOf(',',index+1);
                int index3 = searchResults.indexOf(',',index2+1);
                int gridCol = sensor.navModel.getColumnNumberFromString(
                                searchResults.substring(6, index));
                int gridRow = sensor.navModel.getRowNumberFromString(
                                searchResults.substring(index+1,index2));
                int projCode = Integer.parseInt(searchResults.substring(
                            index2+1, index3));

                scene = sensor.createMetadata(searchResults.substring(
                            index3+1), gridCol, gridRow);

                // get the projection tranformation and set the corners for
                // the scene
                ProjectionTransformation proj
                        = CreateProjection.fromProjectionNumber(projCode);
                scene.setSceneCorners(proj);
            }
            catch (Exception e)
            {
                System.out.println("exception: " + e.toString());
            }
        }

        return scene;
    }

    // public method to allow a non-threaded search to be performed (meant
    // to be used for providing a scene ID when starting the applet)
    //--------------------------------------------------------------------
    public Metadata searchForScene(Sensor sensor, String sceneId)
    {
        Metadata scene = null;

        String searchResult = callSearchCgi(sensor, sceneId);
        if (searchResult != null)
        {
            scene = parseSearchResult(sensor, searchResult);
        }

        return scene;
    }

    // class to process the results of the search.  It is a separate class
    // that implements the Runnable interface so invokeLater can be used to 
    // make sure the search results are processed in the GUI thread since
    // some actions taken may require synchronization with the GUI thread.
    //---------------------------------------------------------------------
    private class ProcessResults implements Runnable
    {
        private SearchParameters searchParameters;

        // constructor for the class
        //--------------------------
        ProcessResults(SearchParameters searchParameters)
        {
            this.searchParameters = searchParameters;
        }

        // method run by the invokeLater call.  It processes the search results
        // and updates the state of the applet as needed.
        //---------------------------------------------------------------------
        public void run()
        {
            // get the search results
            String searchResults = searchParameters.searchResults.toString();

            // if the sensor hasn't changed since the search was started,
            // process the search results
            Sensor currSensor = applet.sensorMenu.getCurrentSensor();
            if (searchParameters.sensor == currSensor)
            {
                if (searchResults.startsWith("Found:"))
                {
                    // if a scene was found, build a Metadata object for it
                    Metadata scene = parseSearchResult(searchParameters.sensor,
                                                       searchResults);

                    if (scene != null)
                    {
                        // verify the scene can be displayed with the current
                        // search limits
                        applet.searchLimitDialog.applySearchLimits(scene);
                        if (scene.visible)
                        {
                            applet.md.showScene(scene);
                            statusDisplay.append(
                                    "Search Complete: Found and displayed "
                                    + scene.getEntityIDForDisplay());
                        }
                        else
                        {
                            statusDisplay.append("Search Complete: Found "
                                + scene.getEntityIDForDisplay() 
                                + " but not displayed because it is"
                                + " filtered out by the current search limits");
                        }
                    }
                    else
                    {
                        statusDisplay.append(
                            "Error: an error occurred during the search");
                    }
                }
                else if (searchResults.startsWith("Invalid:"))
                {
                    // invalid scene ID entered, so tell user
                    statusDisplay.append("Error: Invalid scene ID");
                }
                else if (searchResults.startsWith("Not Found:"))
                {
                    // the scene ID wasn't found
                    statusDisplay.append(
                            "Search Complete: no matching scene found");
                }
                else
                {
                    // some other error happened, so give a generic message to
                    // the user and dump out a more detailed message to the 
                    // console for debugging
                    statusDisplay.append(
                            "Error: an error occurred during the search");
                    System.out.println("Search error: " + searchResults);
                }
            }
            else
            {
                // the selected sensor changed since the search started, so
                // the results don't really apply anymore
                statusDisplay.append("Search cancelled due to sensor change");
            }

            // clear the reference to the search parameters to allow them to be
            // cleaned up by the garbage collector
            searchParameters = null;
        }
    }
}
