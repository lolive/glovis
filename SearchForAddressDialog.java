// SearchForAddressDialog.java implements a dialog for searching for an
// address.
//---------------------------------------------------------------------------
import java.awt.Dimension;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
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
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;  // invokeLater

public class SearchForAddressDialog extends JDialog implements WindowListener,
    ActionListener
{
    private imgViewer applet;       // reference to the main applet
    private JTextField addressEntry;// address entry field
    private JLabel warningLabel;
    private AddressTablePanel addressTablePanel;

    // class for tracking the parameters to the search thread
    private class SearchParameters
    {
        public imgViewer applet;
        public SearchForAddressDialog searchDialog;
        public StringBuffer searchResults;
        public String address;
    }

    // Constructor for the scene list dialog
    //--------------------------------------
    public SearchForAddressDialog(JFrame parent, imgViewer applet)
    {
        super(parent,"Search for Address",false);
        this.applet = applet;

        getContentPane().setLayout(new BorderLayout());

        // Set up the main panel and grid layout
        JPanel mainPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.setLayout(gridbag);

        JLabel label = new JLabel("Enter Address:");
        mainPanel.add(label,gbc);

        addressEntry = new JTextField("",35);
        Dimension size = addressEntry.getPreferredSize();
        addressEntry.setMinimumSize(size);
        addressEntry.setMaximumSize(size);
        addressEntry.setToolTipText("Enter address to search for");
        addressEntry.addActionListener(this);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.add(addressEntry,gbc);

        // reset the width to one column
        gbc.gridwidth = 1;

        warningLabel = new JLabel("",JLabel.CENTER);
        warningLabel.setForeground(Color.RED);
        mainPanel.add(warningLabel,gbc);

        // set up the address list panel
        addressTablePanel = new AddressTablePanel(applet);

        // set up the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,4));

        JButton searchButton = new JButton("Search");
        searchButton.setMnemonic(KeyEvent.VK_S);
        searchButton.setToolTipText("Search for Address");
        searchButton.addActionListener(this);

        JButton displayButton = new JButton("Display");
        displayButton.setMnemonic(KeyEvent.VK_D);
        displayButton.setToolTipText("Display Address");
        displayButton.addActionListener(this);

        JButton clearButton = new JButton("Clear");
        clearButton.setMnemonic(KeyEvent.VK_L);
        clearButton.setToolTipText("Clear search for address");
        clearButton.addActionListener(this);

        JButton closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.setToolTipText("Close search for address");
        closeButton.addActionListener(this);

        buttonPanel.add(searchButton);
        buttonPanel.add(displayButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);

        getContentPane().add(mainPanel,"North");
        getContentPane().add(addressTablePanel,"Center");
        getContentPane().add(buttonPanel,"South");

        // hack to get IE to size the dialog box - TBD find a good way that
        // sizes it correctly
        setSize(500,270);

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

        String address = addressEntry.getText().trim();

        if (command.equals("Close"))
        {
            setVisible(false);
        }
        else if (command.equals("Display"))
        {
            // display the selected address
            addressTablePanel.display();
        }
        else if (command.equals("Clear"))
        {
            addressEntry.setText("");
            warningLabel.setText(" ");
            addressTablePanel.clearAddresses();
        }
        else if (command.equals("Search") || (!address.equals("")) )
        {
            searchForAddress();
        }
    }

    // method to get the address to search for, create a new search thread
    // and start it
    //--------------------------------------------------------------------
    private void searchForAddress()
    {
        // get the address to search for, trimming off any leading or trailing
        // spaces
        String address = addressEntry.getText().trim();

        // don't bother searching if the user hasn't entered anything
        if (!address.equals(""))
        {
            // clear the previous results
            addressTablePanel.clearAddresses();

            // create parameter object for the search
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.applet = applet;
            searchParameters.searchDialog = this;
            searchParameters.address = address;
            searchParameters.searchResults = new StringBuffer("");

            // create a new search thread and start it.  If a previous
            // search thread was started, it is just forgotten and its
            // results ignored.
            SearchThread searchThread = new SearchThread(searchParameters);
            searchThread.start();
        }
        else
        {
            // popup a message box with an error message
            JOptionPane.showMessageDialog(this, "Address is invalid ",
                        "Invalid Entry", JOptionPane.ERROR_MESSAGE);
        }
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
                String inputLine = params.searchDialog.callGeocodingCgi(
                                                params.address);

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

    // method to invoke the CGI script that geocodes the address
    //----------------------------------------------------------
    private String callGeocodingCgi(String address)
    {
        String searchResult = null;

        try
        {
            // build the cgi URL for the search query.
            String cgiCommand = "geocodeGoogle.cgi?";
            cgiCommand += "address=" + URLEncoder.encode(address, "UTF-8");
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

    // class to process the results of the search.  It is a separate class
    // that implements the Runnable interface so invokeLater can be used to 
    // make sure the search results are processed in the GUI thread since
    // some actions taken may require synchronization with the GUI thread.
    // searchParameters should be in the format:
    //   Found:longitude:latitude:label::longitude:latitude:label
    // or if no results:
    //   Found:
    // or:
    //   NotFound: No matches found
    // or if error:
    //   Error: message
    // for example:
    //   Found:-95.9979883:41.2523634:Omaha, NE, USA::-94.7440964:33.1806770:Omaha, TX 75571, USA::-93.1885130:36.4522883:Omaha, AR 72662, USA::-88.3030977:37.8903243:Omaha, IL 62871, USA::-85.0133509:32.1465241:Omaha, GA 31821, USA
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

            if (searchResults.startsWith("Found:"))
            {
                // remove the "Found:" tag
                searchResults = searchResults.substring(6);

                // split the line into separate addresses
                String[] addressList = searchResults.split("::");

                if (addressList != null)
                {
                    if (addressList.length >= 100)
                        warningLabel.setText("Note: Search results are "+ 
                                      "limited to 100 records");
                    else
                        warningLabel.setText(" ");

                    addressTablePanel.setAddresses(addressList);
                }
            }
            else
            {
                // popup a message box with an error message
                JOptionPane.showMessageDialog(searchParameters.searchDialog,
                        "No matches found","Search Complete",
                        JOptionPane.WARNING_MESSAGE);
            }
            searchParameters = null;
        }
    }
}
