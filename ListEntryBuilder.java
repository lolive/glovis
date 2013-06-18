// ListEntryBuilder.java defines the interface for converting a scene reference
// into a line for the SceneListList class.  This allows the scene list
// contents to be different for the main applet scene list and the scene list
// dialog.
//
// Note: it is assumed that the first thing displayed by the entry is the 
// entity id.
//
//-----------------------------------------------------------------------------

interface ListEntryBuilder
{
    public String getEntry(Metadata scene);
}
