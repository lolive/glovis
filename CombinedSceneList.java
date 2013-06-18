// CombinedSceneList.java implements a class to track the list of scenes for
// a dataset that is composed of multiple other datasets.
//
// Implementation Notes:
//  - This class keeps synchronized with the included scene lists by 
//    becoming a list data listener of those scene lists.  When a scene is
//    added or removed from a related list, this class receives a notification
//    and does the proper thing to keep the combined list in agreement.
//  - Even when the combined dataset is shown, the add, remove, and order
//    methods in this class operate by manipulating the correct related 
//    scene list and then update the list for the combined scene list in
//    the list data listener methods.
//--------------------------------------------------------------------------
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class CombinedSceneList extends SceneList implements ListDataListener
{
    private imgViewer applet;              // reference to main applet
    private SceneList[] relatedSceneLists; // array of scene lists from the
                                           // the contained datasets
    private boolean ignoreRemoves;         // flag to ignore remove messages in
                                           // the intervalRemoved method

    // Constructor for the CombinedSceneList
    //--------------------------------------
    CombinedSceneList(imgViewer applet, Sensor sensor, 
                      SceneList[] relatedSceneLists)
    {
        super(applet, sensor);

        this.applet = applet;
        this.relatedSceneLists = relatedSceneLists;

        ignoreRemoves = false;

        // request to be notified of changes in related scene lists
        for (int i = 0; i < relatedSceneLists.length; i++)
            relatedSceneLists[i].addListDataListener(this);
    }

    // helper method to return the scene list that a scene belongs to
    //---------------------------------------------------------------
    private SceneList findSceneList(Metadata scene)
    {
        Sensor sceneSensor = scene.getSensor();

        // find the correct related list
        SceneList relatedList = null;
        for (int i = 0; i < relatedSceneLists.length; i++)
        {
            if (sceneSensor == relatedSceneLists[i].sensor)
            {
                relatedList = relatedSceneLists[i];
                break;
            }
        }

        return relatedList;
    }

    // Add a specific scene to the scene list
    //---------------------------------------
    public void add(Metadata scene)
    {
        // find the correct related scene list
        SceneList relatedList = findSceneList(scene);
        if (relatedList == null)
            return;

        // add the scene to the related list, which will notify this class to
        // add it to this list
        relatedList.add(scene);

        // make sure the search limits are kept in sync
        if (applet.searchLimitDialog.isSceneListFilterEnabled())
            applet.searchLimitDialog.applyFilter();
    }

    // method to remove the indicated scene from the scene list
    //---------------------------------------------------------
    public void remove(Metadata scene)
    {
        // find the correct related scene list
        SceneList relatedList = findSceneList(scene);

        // remove the scene from the related list, which will notify this 
        // class to remove it from this list
        relatedList.remove(scene);

        // make sure the search limits are kept in sync
        if (applet.searchLimitDialog.isSceneListFilterEnabled())
            applet.searchLimitDialog.applyFilter();
    }

    // method to remove ALL scenes from the scene list
    //------------------------------------------------
    public void clear()
    {
        // similar to the order function but we know everything was removed
        super.clear();

        for (int i = 0; i < relatedSceneLists.length; i++)
            relatedSceneLists[i].list.removeAllElements();
    }

    // Order the contents of the scene list
    //-------------------------------------
    public void order()
    {
        super.order();

        if (list.size() == 0)
        {
            // no scenes left in the list, so remove all the scenes from the
            // related lists
            for (int i = 0; i < relatedSceneLists.length; i++)
                relatedSceneLists[i].list.removeAllElements();
        }
        else
        {
            // flag that the intervalRemoved method should ignore messages.
            // This is needed to prevent things from getting out of sync if
            // things are ordering results in only some of the scenes being
            // ordered
            ignoreRemoves = true;

            // only part of the scenes could be ordered, so remove the scenes
            // from the related lists that no longer are in the combined list
            for (int i = 0; i < relatedSceneLists.length; i++)
            {
                DefaultListModel relatedList = relatedSceneLists[i].list;
                for (int j = relatedList.size() - 1; j >= 0; j--)
                {
                    Metadata scene = (Metadata)relatedList.elementAt(j);
                    int index = find(scene);
                    if (index == -1)
                    {
                        relatedList.removeElementAt(j);
                    }
                }
            }

            // the intervalRemoved method should no longer ignore removes
            ignoreRemoves = false;
        }

        // update filter if needed
        if (applet.searchLimitDialog.isSceneListFilterEnabled())
            applet.searchLimitDialog.applyFilter();
    }

    // method to return whether the scene list contains scenes with gaps
    // in the data (i.e. Landsat 7 SLC-off)
    //------------------------------------------------------------------
    public boolean containsGapData()
    {
        // if any of the scenes in the list have gaps, return true
        for (int i = list.size() - 1; i >= 0; i--)
        {
            Metadata scene = (Metadata)list.elementAt(i);
            if (scene.getSensor().dataHasGaps)
                return true;
        }
        return false;
    }

    // method to handle the event of data being added to a related list
    //-----------------------------------------------------------------
    public void intervalAdded(ListDataEvent event)
    {
        DefaultListModel sourceList = (DefaultListModel)event.getSource();
        int startIndex = event.getIndex0();
        int endIndex = event.getIndex1();

        // any time something is added, the restore option is no longer
        // available (Note make sure to set this before manipulating the list)
        restoreEnabledFlag = false;

        // add all the scenes added to the related list to the combined list
        for (int i = startIndex; i <= endIndex; i++)
        {
            Metadata scene = (Metadata)sourceList.elementAt(i);
            list.addElement(new Metadata(scene));
        }
    }

    // method to handle the event of data being removed from a related list
    //---------------------------------------------------------------------
    public void intervalRemoved(ListDataEvent event)
    {
        // do not process removes if they are currently being ignored since
        // the data has already been removed by the order method
        if (ignoreRemoves)
            return;

        DefaultListModel sourceList = (DefaultListModel)event.getSource();
        Sensor sensor = null;
        SceneList relatedList = null;

        // find the correct scene list
        for (int i = 0; i < relatedSceneLists.length; i++)
        {
            if (sourceList == relatedSceneLists[i].list)
            {
                sensor = relatedSceneLists[i].sensor;
                relatedList = relatedSceneLists[i];
                break;
            }
        }

        if (sensor != null)
        {
            // detect when a related list is ordered and enable restore on the
            // combined list
            if (!restoreEnabledFlag && relatedList.restoreEnabledFlag)
            {
                // this is the first remove to be received, so save the scene
                // list so it can be restored
                savedList.removeAllElements();
                Object[] listContents = list.toArray();
                for (int i = 0; i < listContents.length; i++)
                    savedList.add(listContents[i]);
                restoreEnabledFlag = true;
            }

            // delete the correct scenes for the sensor from the combined list
            int startIndex = event.getIndex0();
            int endIndex = event.getIndex1();
            int count = 0;

            int i = 0;
            while (i < list.size())
            {
                Metadata scene = (Metadata)list.elementAt(i);
                if (scene.getSensor() == sensor)
                {
                    if ((count >= startIndex) && (count <= endIndex))
                        list.removeElementAt(i);
                    else
                        i++;
                    count++;

                    if (count > endIndex)
                        break;
                }
                else
                    i++;
            }
        }
    }

    // method to handle the event of data being changed in a related list
    //---------------------------------------------------------------------
    public void contentsChanged(ListDataEvent event)
    {
        /* nothing to do for this event */
    }
}
