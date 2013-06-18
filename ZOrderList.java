// ZOrderList.java implements a class for tracking the Z-order of scenes in 
// the mosaic display.
//
// The list stores a reference to the scene's metadata.  It can operate in
// single scene mode for sensors like Landsat, or multiple scene mode for
// sensors like ASTER.
//
// Notes:
//  - This class can not be used to iterate the list by more than one thread
//    since it is not thread safe.
//  - The class is implemented as a double-linked list.  No limits on the 
//    number of items in the list exists.
//
//-----------------------------------------------------------------------------

public class ZOrderList
{
    
    // define an inner class for tracking the nodes in the linked list
    class Node
    {
        Node next;      // reference to the next node in the list
        Node prev;      // reference to the previous node in the list
        Metadata scene; // actual scene for this node
        // constructor for the node class
        Node()
        {
            next = null;
            prev = null;
            scene = null;
        }
    }

    private Node head;      // head node of the linked list
    private Node tail;      // tail node of the linked list
    private Node cursor;    // current node when stepping through the list

    private Node freelist;  // list of unused nodes.  When a node becomes 
                            // unused, it is put on the free list to attempt
                            // minimizing allocating and garbage collecting
                            // nodes all the time.  The freelist only 
                            // maintains the next link in the nodes.

    private int numItems;   // number of items currently in the list
    private boolean ssMode; // single scene mode flag

    // ZOrderList constructor
    //-----------------------
    ZOrderList()
    {
        head = null;
        tail = null;
        cursor = null;
        freelist = null;
        numItems = 0;
    }
    
    // method to produce an array containing the scene references in z-order.
    // This provides a method to get a snapshot of the z-order as it exists
    // at any given time.  The primary reason for having this is to allow
    // threading without using synchronization in this class.  A full copy
    // of the linked list is not made since the users don't need all the 
    // functionality of the list.
    //----------------------------------------------------------------------
    public Metadata[] getSnapshot()
    {
        // if no items exist in the list, just return null
        if (numItems == 0)
            return null;

        // allocate an array for the scene references
        Metadata[] list = new Metadata[numItems];

        // walk the linked list from top to bottom, copying the scene
        // references to the list
        Node currentNode = head;
        int i = 0;
        while (currentNode != null)
        {
            list[i] = currentNode.scene;
            i++;
            currentNode = currentNode.next;
        }

        // return the snapshot of the order
        return list;
    }

    // method to set the z-Order so that only a single scene is tracked for
    // each grid cell
    //---------------------------------------------------------------------
    public void setSingleSceneMode()
    {
        ssMode = true;
    }

    // method to set the z-Order so that each scene is tracked separatedly.
    // This is to allow mosaics of sensors like ASTER to be tracked.
    //---------------------------------------------------------------------
    public void setMultipleSceneMode()
    {
        ssMode = false;
    }

    // helper method to search the linked list to find the node that contains 
    // a particular scene.  Returns the reference of the node where the scene 
    // was found, or null if the scene is not found.
    //-----------------------------------------------------------------------
    private Node findSceneNode(Metadata scene)
    {
        Node foundNode = null;

        // look for value in the list
        if (ssMode)
        {
            // single scene mode, so look for a scene in the same column and
            // row for a match
            Node currentNode = head;

            while (currentNode != null)
            {
                Metadata temp = currentNode.scene;

                if ((temp.gridCol == scene.gridCol) && 
                    (temp.gridRow == scene.gridRow))
                {
                    // found the scene, so return it
                    foundNode = currentNode;
                    break;
                }

                currentNode = currentNode.next;
            }
        }
        else
        {
            // not single scene mode, so look for an exact match on the 
            // scene (Note the reference match is okay here instead of 
            // checking the scene id)
            Node currentNode = head;

            while (currentNode != null)
            {
                if (scene == currentNode.scene)
                {
                    // found the scene, so return it
                    foundNode = currentNode;
                    break;
                }

                currentNode = currentNode.next;
            }
        }
        
        return foundNode;
    }

    // routine to get a free linked list node from the free list, or allocate
    // a new one if none remain on the free list
    //-----------------------------------------------------------------------
    private Node getFreeNode()
    {
        Node freeNode = null;

        if (freelist != null)
        {
            // take the first node from the list and return it
            freeNode = freelist;
            freelist = freelist.next;
        }
        else
        {
            // the free list is empty, so allocate a new node
            freeNode = new ZOrderList.Node();
        }

        return freeNode;
    }

    // putOnTop puts the scene on the top in the Z-order 
    //--------------------------------------------------
    public void putOnTop(Metadata scene)
    {
        // find the scene in the list
        Node foundNode = findSceneNode(scene);

        if (foundNode != null)
        {
            // make sure the scene in the node is the one passed in since in 
            // single scene mode, a node can be found that contains a different
            // scene
            foundNode.scene = scene;

            // nothing to do if the found node is alreay on top
            if (head == foundNode)
                return;

            // the node was found, so remove it from the current location
            if (foundNode.prev != null)
                foundNode.prev.next = foundNode.next;
            else
                head = foundNode.next;
            if (foundNode.next != null)
                foundNode.next.prev = foundNode.prev;
            else
                tail = foundNode.prev;

            // put the scene at the head of the list
            head.prev = foundNode;
            foundNode.next = head;
            foundNode.prev = null;
            if (foundNode.next != null)
                foundNode.next.prev = foundNode;
            head = foundNode;
        }
        else
        {
            // scene not found, so get a free node
            Node newNode = getFreeNode();
            newNode.scene = scene;

            // put the new scene at the head of the list
            newNode.next = head;
            newNode.prev = null;
            if (newNode.next != null)
                newNode.next.prev = newNode;
            if (tail == null)
                tail = newNode;
            head = newNode;

            // increment the count of scenes in the list
            numItems++;
        }
    }

    // putOnBottom puts the scene at the bottom of the Z-order 
    //--------------------------------------------------------
    public void putOnBottom(Metadata scene)
    {
        // find the scene in the list
        Node foundNode = findSceneNode(scene);
            
        // put the scene at the tail of the list
        if (foundNode != null)
        {
            // make sure the scene in the node is the one passed in since in 
            // single scene mode, a node can be found that contains a different
            // scene
            foundNode.scene = scene;

            // nothing to do if the found node is already at the tail
            if (tail == foundNode)
                return;

            // remove the node from the current location
            if (foundNode.prev != null)
                foundNode.prev.next = foundNode.next;
            else
                head = foundNode.next;
            if (foundNode.next != null)
                foundNode.next.prev = foundNode.prev;
            else
                tail = foundNode.prev;

            // put the scene at the tail of the list
            tail.next = foundNode;
            foundNode.prev = tail;
            foundNode.next = null;
            if (foundNode.prev != null)
                foundNode.prev.next = foundNode;
            tail = foundNode;
        }
        else
        {
            // scene  not found, so get a free node
            Node newNode = getFreeNode();
            newNode.scene = scene;

            // put the new scene at the tail of the list
            newNode.next = null;
            newNode.prev = tail;
            if (newNode.prev != null)
                newNode.prev.next = newNode;
            if (head == null)
                head = newNode;
            tail = newNode;

            // increment the count of scenes in the list
            numItems++;
        }
    }

    // changeScene allows changing a scene reference in the z-order without
    // actually changing the z-order.  This is useful for changing the
    // displayed scene in single scene mode.
    //---------------------------------------------------------------------
    public void changeScene(Metadata scene)
    {
        if (ssMode)
        {
            Node foundNode = findSceneNode(scene);
            if (foundNode != null)
                foundNode.scene = scene;
        }
    }

    // method to insert a scene into the z-order in cloud cover order.  
    // The assumption is made that all scenes are inserted using this
    // mechanism and the existing z-order is already sorted in order of 
    // cloud cover.  It also assumed that the scene is not already in 
    // the z-order.
    //-----------------------------------------------------------------
    public void insertByCloudCover(Metadata scene)
    {
        // this should only be used when not in single scene mode
        if (ssMode)
            System.out.println("Bug! Inserting by cloud cover in ssMode");

        int cc = scene.cloudCover;

        Node insertNode = null;

        // find the position to insert the scene
        Node currentNode = head;
        while (currentNode != null)
        {
            if (cc >= 0 && cc <= currentNode.scene.cloudCover)
            {
                insertNode = currentNode;
                break;
            }

            currentNode = currentNode.next;
        }

        // get a free node for adding the scene
        Node newNode = getFreeNode();
        newNode.scene = scene;

        // if no node was found for the insertion point, put the scene at the 
        // tail of the list
        if (insertNode == null)
        {
            if (tail == null)
            {
                // the list is empty
                newNode.next = null;
                newNode.prev = null;
                head = newNode;
                tail = newNode;
            }
            else
            {
                // put the node at the tail of the list
                newNode.next = null;
                newNode.prev = tail;
                tail.next = newNode;
                tail = newNode;
            }
        }
        else
        {
            // insert the new scene
            if (head == insertNode)
            {
                // the insertion point is the head node, so update the head
                // refrence
                head = newNode;
            }
            newNode.prev = insertNode.prev;
            newNode.next = insertNode;
            insertNode.prev = newNode;
            if (newNode.prev != null)
                newNode.prev.next = newNode;
        }

        numItems++;
    }

    // clear the contents of the list
    //-------------------------------
    public void empty()
    {
        // clear all the scene references in the list to allow garbage 
        // collection of stale scenes.  Also clear the prev links since
        // they are not used in the free list.
        Node currentNode = head;
        while (currentNode != null)
        {
            currentNode.scene = null;
            currentNode.prev = null;
            currentNode = currentNode.next;
        }

        // move the entire list to the free list
        if (tail != null)
            tail.next = freelist;
        freelist = head;

        // empty the list
        tail = null;
        head = null;
        cursor = null;
        numItems = 0;
    }

    // start iterating at the top of the Z-order
    //------------------------------------------
    public void top()
    {
        cursor = head;
    }

    // Return true if the top of the Z-order has been reached
    //-------------------------------------------------------
    public boolean isTop()
    {
        if (cursor == null)
            return true;
        else
            return false;
    }

    // method to move down one location in the Z-order.  Before starting a
    // top to bottom transversal, the top method should be called to set the 
    // current location to the top of the list.  A reference to the current
    // scene's metadata is returned (or null if the end has been reached).
    //----------------------------------------------------------------------
    public Metadata down()
    {
        if (cursor != null)
        {
            Metadata ret = cursor.scene;
            cursor = cursor.next;
            return ret;
        }
        // end of list reached
        return null;
    }

    // start iterating at the bottom of the Z-order
    //---------------------------------------------
    public void bottom()
    {
        cursor = tail;
    }

    // method to move up one location in the Z-order.  Before starting a
    // bottom to top transversal, the bottom method should be called to set the 
    // current location to the bottom of the list.  A reference to the current
    // scene's metadata is returned (or null if the end has been reached).
    //-------------------------------------------------------------------------
    public Metadata up()
    {
        if (cursor != null)
        {
            Metadata ret = cursor.scene;
            cursor = cursor.prev;
            return ret;
        }
        // top of the list reached
        return null;
    }

    // debugging method that allows easy display of entries on the list
/*
    private void dump()
    {
        System.out.println("dumping zorder");
        Node currentNode = head;

        while (currentNode != null)
        {
            Metadata scene = currentNode.scene;
            System.out.println("dump " + scene.gridCol + " " 
                               + scene.gridRow + " " + scene.entityID);
            currentNode = currentNode.next;
        }
        System.out.println("done dumping zorder");
    }
*/
}
