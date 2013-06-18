// SceneList.java implements a class to track the list of scenes the user 
// has built.
//  
//-----------------------------------------------------------------------
import java.awt.Point;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataListener;
import javax.swing.JOptionPane;
import javax.swing.ListModel;

public class SceneList implements ListModel
{
    protected DefaultListModel list;      // main scene list
    protected Vector savedList; // scene list for restore
    protected boolean restoreEnabledFlag; // flag to show restore option
    protected Sensor sensor;    // sensor this list belongs to
    private imgViewer applet;   // reference to main applet
    private int maxOrderSize;   // max order URL size before Explorer error
    private int maxUrlLength;   // max length of a URL

    // Constructor for the SceneList
    //------------------------------
    SceneList(imgViewer applet, Sensor sensor)
    {
        this.applet = applet;
        this.sensor = sensor;

        maxOrderSize = -1;
        list = new DefaultListModel();
        savedList = new Vector();

        // determine the maximum length of a URL
        if (applet.usingIE)
        {
            // IE limits the URL to 2083 characters
            maxUrlLength = 2083;
        }
        else
        {
            // the actual server imposes a limit of 8190 characters
            maxUrlLength = 8190;
        }
    }

    // Search list for a given scene entry and return the index to it in the
    // list.  Returns -1 if the scene isn't in the list
    //----------------------------------------------------------------------
    public int find(Metadata scene)
    {
        // find the scene in the list
        int listIndex = -1;
        int count = list.size();
        for (int index = 0; index < count; index++)
        {
            Metadata listScene = (Metadata)list.elementAt(index);
            if (scene.entityID.equals(listScene.entityID))
            {
                listIndex = index;
                break;
            }
        }
        return listIndex;

    }
    
    // Add a specific scene to the scene list
    //---------------------------------------
    public void add(Metadata scene)
    {
        // if this scene isn't for the sensor this list holds, don't add it
        // (this routine can get called with scenes from the wrong sensor
        // when in the middle of switching sensors)
        if (scene.getSensor() != sensor)
            return;

        int indexInList = find(scene);
        
        // if the scene isn't in the list, add it
        if (indexInList < 0)
        {
            // if new scene added, restore no longer available
            restoreEnabledFlag = false;

            // copy the scene (so the list owns its copy)
            Metadata listScene = new Metadata(scene);

            // add the scene to the list
            list.addElement(listScene);

            // update visible if needed (after restore)
            if (applet.searchLimitDialog.isSceneListFilterEnabled())
                applet.searchLimitDialog.applyFilter();
        }
        else
        {
            // since the scene is already in the list, replace it with itself
            // so a contentsChanged event is generated.  That way the displayed
            // list can select the scene if it is already in the list.
            list.setElementAt(list.elementAt(indexInList), indexInList);
        }
    }
    
    // method to return the number of scenes in the scene list
    //--------------------------------------------------------
    public int getSceneCount()
    {
        return list.size();
    }
    public int getSize()
    {
        return list.getSize();
    }

    // method to return the number of downloadable scenes in the scene list
    // --------------------------------------------------------------------
    public int getDownloadableSceneCount()
    {
        int listCount = list.size();
        int downloadableCount = 0;
        for (int index = 0; index < listCount; index++)
        {
            Metadata listScene = (Metadata)list.elementAt(index);
            if (listScene.isDownloadable)
            {
                downloadableCount++;
            }
        }
        return downloadableCount;
    }

    // method to return a reference to the scene at an index
    //------------------------------------------------------
    public Metadata getSceneAt(int index)
    {
        return (Metadata)list.elementAt(index);
    }

    // method to remove the indicated scene from the scene list
    //---------------------------------------------------------
    public void remove(Metadata scene)
    {
        int index = find(scene);

        if (index >= 0)
        {
            // if a scene is removed, restore no longer available
            restoreEnabledFlag = false;

            // the scene is in the list, so remove it
            list.removeElementAt(index);

            // update filter if needed
            if (applet.searchLimitDialog.isSceneListFilterEnabled())
                applet.searchLimitDialog.applyFilter();
        }
    }

    // method to remove ALL scenes from the scene list
    //------------------------------------------------
    public void clear()
    {
        // similar to the order function but without verifying stuff
        // and without sending the list anywhere else

        int origNumScenes = list.size();
        if (origNumScenes < 1)
            return;

        // if the list has not already been saved for potentially restoring,
        // save it now
        if (!restoreEnabledFlag)
        {
            // set the savedList to the original list
            savedList.removeAllElements();
            Object[] listContents = list.toArray();
            for (int i = 0; i < listContents.length; i++)
                savedList.add(listContents[i]);
            restoreEnabledFlag = true;
        }
        list.removeAllElements();
        
        // update filter if needed
        if (applet.searchLimitDialog.isSceneListFilterEnabled())
            applet.searchLimitDialog.applyFilter();
    }

    // Method to confirm that the user really wants to order scenes of
    // questionable quality.  Returns the number of scenes to order, or -1
    // if the confirm dialog cancel button was pressed.
    //--------------------------------------------------------------------
    private int confirmQuestionableScenes(Metadata[] listToConfirm)
    {
        int numElements = listToConfirm.length; // number of scenes in full list
        int confirmedSceneCount = 0;      // number of scenes confirmed to order

        // don't bother checking sensors where this check isn't needed
        if (!sensor.warnWhenOrderingPoorQuality)
        {
            // make sure all the scenes are marked as visible
            for (int i = 0; i < numElements; i++)
            {
                Metadata scene = (Metadata)listToConfirm[i];
                scene.visible = true;
            }
            return numElements;
        }

        // check each scene in the list to verify it meets the quality and
        // cloud cover limits.  Update the visible flag for each scene based on
        // whether it meets the limits
        for (int i = 0; i < numElements; i++)
        {
            boolean keep = true;
            Metadata scene = (Metadata)listToConfirm[i];
            // eliminate scenes that have more than 50% cloud cover
            if (scene.cloudCover > 50)
                keep = false;
            else if (scene.quality != null)
            {
                // eliminate scenes that have a quality less than the limit
                int qualityLimit = scene.getSensor().qualityLimit;

                for (int qualityIndex = 0; qualityIndex < scene.quality.length;
                     qualityIndex++)
                {
                    if (scene.quality[qualityIndex] < qualityLimit)
                    {
                        keep = false;
                        break;
                    }
                }
            }

            scene.visible = keep;

            // count the scenes that will be ordered
            if (keep)
                confirmedSceneCount++;
        }

        // if there are no bad scenes, return now
        if (confirmedSceneCount == numElements)
            return numElements;

        // build a list of bad scenes for the dialog box
        int index = 0;
        Metadata[] poorScenes = new Metadata[numElements - confirmedSceneCount];
        for (int i = 0; i < numElements; i++)
        {
            Metadata scene = (Metadata)listToConfirm[i];
            if (!scene.visible)
            {
                poorScenes[index] = scene;
                index++;
            }
        }

        // create and show the dialog box with the bad scenes
        ConfirmScenesDialog dialog = new ConfirmScenesDialog(
                                             poorScenes, confirmedSceneCount);
        dialog.setLocationRelativeTo(applet);
        dialog.setVisible(true);

        // remember whether the dialog was cancelled and dispose of the dialog
        boolean wasCancelled = dialog.wasCancelled();
        dialog.dispose();
        dialog = null;

        // if the cancel button was pressed, return -1 to indicate it
        if (wasCancelled)
            return -1;

        // count the number of scenes that have been marked as visible now
        // and add them to the count
        for (int i = 0; i < poorScenes.length; i++)
        {
            if (poorScenes[i].visible)
                confirmedSceneCount++;
        }

        // return the number of scenes to order
        return confirmedSceneCount;
    }

    // Download the given scene
    //----------------------------------------
    public void download(Metadata scene)
    {
        // build the download URL
        String URLname;
        String windowName;
        URLname = sensor.buildDownloadURL(scene);
        System.out.println("downloading scene with URL: " + URLname);
        windowName = "Download ";
        windowName += scene.entityID;
                
        // open a new browser window with download
        try 
        {
            URL shoppingCartURL = new URL(URLname);
            applet.getAppletContext().showDocument(shoppingCartURL,
                    sensor.orderWindowName);
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e.toString());
        }
    }
 
    // Order the contents of the scene list
    // if appropriate
    //-------------------------------------
    public void order()
    {
        int origNumScenes = list.size();
        if (origNumScenes < 1)
            return;

        // origNumScenes is the number of scenes initially in this->list
        //     The number of scenes actually ordered may be smaller:
        // - numConfirmedScenes is scenes left after user gets a warning about
        //   poor quality scenes and has to confirm ordering them
        // - maxScenesPerOrder is sensor-specific max number of scenes to order
        //   or 0 if no limit
        // - maxOrderSize is maximum number of scenes that will fit in the URL
        // - numScenesOrdered is number of scenes sent to shopping cart

        // calculate the max order size if it hasn't been done
        if (maxOrderSize == -1)
            findMaxOrderSize((Metadata)list.elementAt(0));
        if (maxOrderSize < 1) // failed to determine the size
            return;
        
        int numElements = list.size();
       
        boolean fullListOrdered = true;
        boolean orderLimitedByUrlLimit = false;
        boolean orderLimitedByScenesPerOrder = false;
        
        int startElement = 0;
        int lastElement = numElements;

        int maxScenesPerOrder = sensor.maxScenesPerOrder;
        int numOrderableScenes = origNumScenes;
        int numConfirmedScenes = 0;
        int numScenesOrdered = 0;

        if (numOrderableScenes < 1)
            return;

        // build a list of scenes eligible to send to the shopping cart
        Metadata[] orderableScenes = new Metadata[numOrderableScenes];
        int origListIndex = 0;
        int orderableListIndex = 0;
        while (orderableListIndex < numOrderableScenes)
        {
            Metadata scene = (Metadata)list.elementAt(origListIndex++);
            // (all scenes are eligible, including downloadable scenes)
            orderableScenes[orderableListIndex++] = scene;
        }

        // confirm the scenes that are of questionable quality and get a count
        // of the scenes the user wishes to order.
        // This also sets the visible flag for each scene
        numConfirmedScenes = confirmQuestionableScenes(orderableScenes);

        // if the order was cancelled or no scenes to order, then return
        if (numConfirmedScenes <= 0)
            return;

        // flag whether the full list is being ordered 
        if (numConfirmedScenes != numElements)
            fullListOrdered = false;

        numScenesOrdered = numConfirmedScenes;

        // limit the number of scenes that can be submitted to the shopping
        // cart page at once based on this sensor's limitations
//        if ((maxScenesPerOrder > 0) && (numScenesOrdered > maxScenesPerOrder))
//        {
//            numScenesOrdered = maxScenesPerOrder;
//            fullListOrdered = false;
//            orderLimitedByScenesPerOrder = true;
//        }

        // limit the number of scenes that can be submitted to the shopping
        // cart page at once since there is a limit imposed on the length
        // of the URL
        if (numScenesOrdered > maxOrderSize)
        {
            numScenesOrdered = maxOrderSize;
            fullListOrdered = false;
            orderLimitedByUrlLimit = true;
        }

        Metadata[] confirmedAllowableScenes = new Metadata[numScenesOrdered];

        // build an array of the entity IDs in the orderable scene list that
        // were confirmed (set to visible), up to numScenesOrdered
        // this also sets startElement to the first scene actually ordered
        int sceneIndex = numOrderableScenes - 1;
        int addIndex = 0;
        while (sceneIndex >= 0)
        {
            startElement = sceneIndex;
            Metadata scene = (Metadata)orderableScenes[sceneIndex];
            if (scene.visible)
            {
                confirmedAllowableScenes[addIndex] = scene;
                addIndex++;
                if (addIndex >= numScenesOrdered)
                    break;
            }
            sceneIndex--;
        }

        // build the order URL
        String URLname = sensor.buildOrderURL(confirmedAllowableScenes);
        System.out.println("ordering " + numScenesOrdered
            + " scenes with URL: " + URLname);
        
        // open a new browser window with the shopping cart
        try 
        {
            URL shoppingCartURL = new URL(URLname);
            applet.getAppletContext().showDocument(shoppingCartURL,
                sensor.orderWindowName);
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e);
        }

        // if the list has not already been saved for potentially restoring,
        // save it now
        if (!restoreEnabledFlag)
        {
            // set the savedList to the original list
            savedList.removeAllElements();
            Object[] listContents = list.toArray();
            for (int i = 0; i < listContents.length; i++)
                savedList.add(listContents[i]);
            restoreEnabledFlag = true;
        }

        if (fullListOrdered)
        {
            // the full list was ordered, so clear out the 
            // scene list
            list.removeAllElements();
        }
        else
        {
            // only part of the list was ordered, so remove 
            // the scenes that were ordered.

            for (int index = numScenesOrdered - 1; index >= 0; index--)
            {
                Metadata scene
                    = (Metadata)confirmedAllowableScenes[index];
                origListIndex = find(scene);
                if (origListIndex != -1)
                {
                    list.removeElementAt(origListIndex);
                }
                // else there should be an error - why wasn't the scene found??
            }

            // pop up a message box indicating that not all the scenes were
            // able to be submitted to the shopping cart page and explain why
            if (orderLimitedByScenesPerOrder)
            {
                String [] messages = {"Added " + numScenesOrdered + " of " 
                    + origNumScenes + " scenes from your scene list "
                    + "to the shopping cart.  "+ list.size() + " scene" 
                    + (list.size() > 1 ? "s remain." : " remains."),
                    "Note: There is a limit to the number of scenes that "
                    + "can be passed to the shopping cart",
                    "for on-demand "
                    + "processing and your scene list exceeded that limit.",
                    " ",
                    "Please DO NOT press the Send to Cart button again "
                    + "to submit more scenes to the shopping",
                    "cart until your shopping cart is empty."};

                JOptionPane.showMessageDialog(applet.getDialogContainer(),
                       messages,
                       "Partial scene list submitted to shopping cart",
                        JOptionPane.WARNING_MESSAGE);
            }
            else if (orderLimitedByUrlLimit)
            {
                String [] messages = {"Added " + numScenesOrdered + " of " 
                    + origNumScenes + " scenes from your scene list "
                    + "to the shopping cart.  "+ list.size() + " scene" 
                    + (list.size() > 1 ? "s remain." : " remains."),
                    "Note: There is a limit to the length of a URL that "
                    + "can be passed to the shopping "
                    + "cart and your scene list has exceeded that limit.",
                    "Please press the Send to Cart button again "
                    + "to submit more scenes to the shopping cart."};

                JOptionPane.showMessageDialog(applet.getDialogContainer(),
                       messages,
                       "Partial scene list submitted to shopping cart",
                        JOptionPane.WARNING_MESSAGE);
            }

        }
        
        // update filter if needed
        if (applet.searchLimitDialog.isSceneListFilterEnabled())
            applet.searchLimitDialog.applyFilter();
    }
    
    // Restore scene list to saved scene list after a order  
    // SceneListList doesn't seem to like having a bunch of new elements
    // added at once, so add the saved elements through the add function
    // instead of copying the vector.
    //------------------------------------------------------------------
    public void restore()
    {
        // only restore if restore option is valid
        if (restoreEnabledFlag)
        {
            for (int index = 0; index < savedList.size(); index++)
                add((Metadata)savedList.elementAt(index));

            restoreEnabledFlag = false;
            savedList.removeAllElements();
        }
    }

    // Method for determining if restore feature should be enabled
    //----------------------------------------------------------------
    public boolean isRestoreEnabled()
    {
        return restoreEnabledFlag; 
    }

    // If a scene from the given cell is in the scene list, return the TOC
    // date index to the scene.  Otherwise return -1.
    //--------------------------------------------------------------------
    public int contains(TOC cell) 
    {
        int numElements = list.size();

        for (int i = 0; i < numElements; i++) 
        {
            // get the scene from the list
            Metadata scene = (Metadata)list.elementAt(i);

            // if the col/row and sceneID are all a match, return the
            // index of the matching scene
            if ((scene.gridCol == cell.gridCol) 
                && (scene.gridRow == cell.gridRow)) 
            {
                for (int j = 0; j < cell.numImg; j++) 
                {
                    if (scene.entityID.equals(cell.scenes[j].entityID))
                    {
                        // match found
                        return j;
                    }
                }
            }
        }

        // no match found
        return -1;
    }

    // method to calculate the max number of scenes in an order.
    //---------------------------------------------------------
    protected void findMaxOrderSize(Metadata scene)
    {
        Metadata[] scenes = new Metadata[1];
        String urlEncodingString = new String("US-ASCII");
        String singleIdString;
        String doubleIdString;
        int startUrlLength;
        int sceneIdLength;
        int loginRedirectLength = 82;
        // 82 is a "magic number" - if the user is not already logged in, the
        // orderUrl which GloVis built gets appended in the RET_ADDR parameter
        // as part of a redirect to the login page, adding 82 characters
        
        scenes[0] = scene;
        
        // build the order URL
        try
        {
            singleIdString = URLEncoder.encode(sensor.buildOrderURL(scenes),
                                               urlEncodingString);
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e.toString());
            return;
        }
        
        scenes = new Metadata[2];
        scenes[0] = scene;
        scenes[1] = scene;
        
        // build the order URL
        try
        {
            doubleIdString = URLEncoder.encode(sensor.buildOrderURL(scenes),
                                               urlEncodingString);
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e.toString());
            return;
        }

        // find length of each scene ID and the start URL
        sceneIdLength = doubleIdString.length() - singleIdString.length();
        startUrlLength = singleIdString.length() - sceneIdLength
            + loginRedirectLength;

        // find max URL size
        maxOrderSize = (maxUrlLength - startUrlLength) / sceneIdLength;
        // round down to lower multiple of 10
        maxOrderSize -= maxOrderSize % 10;
    }
    
    // method to return whether the scene list contains scenes with gaps
    // in the data (i.e. Landsat 7 SLC-off)
    //------------------------------------------------------------------
    public boolean containsGapData()
    {
        return sensor.dataHasGaps;
    }

    // methods to forward some method calls to the correct method in the
    // SceneList
    //------------------------------------------------------------------
    public Object getElementAt(int index)
    {
        return list.getElementAt(index);
    }
    public void addListDataListener(ListDataListener l)
    {
        list.addListDataListener(l);
    }
    public void removeListDataListener(ListDataListener l)
    {
        list.removeListDataListener(l);
    }
    public ListModel getModel()
    {
        return list;
    }
}
