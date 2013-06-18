// MapLayerFileCache.java implements a class to cache file contents for 
// map layer files.  One MapLayerFileCache can be used for each type of 
// map layer (as needed).
//---------------------------------------------------------------------
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class MapLayerFileCache
{
    class CacheEntry
    {
        Vector data;
        int age;
        boolean loaded;
    }

    private Hashtable cache;

    // constructor for the MapLayerFileCache
    //--------------------------------------
    public MapLayerFileCache()
    {
        cache = new Hashtable(4);
    }

    // method to age the cache contents
    //---------------------------------
    public void age()
    {
        // for each entry in the hash table, increase the age
        Enumeration keys = cache.keys();
        while (keys.hasMoreElements())
        {
            String filename = (String)keys.nextElement();
            CacheEntry entry = (CacheEntry)cache.get(filename);
            // ages don't mean much after 100, so limit the aging to 100
            if (entry.age < 100)
                entry.age++;
        }
    }

    // method to add a filename to the cache.  If the file is already in the
    // cache, the age will be reset.  Returns "true" if the file will need
    // to be loaded.
    //----------------------------------------------------------------------
    public boolean addFile(String filename)
    {
        CacheEntry entry = (CacheEntry)cache.get(filename);
        if (entry != null)
        {
            // the file is in the cache, so reset its age (don't need to 
            // put it back in the cache since the reference to it is already
            // in the cache)
            entry.age = 0;
        }
        else
        {
            // file isn't in cache at all, so add it
            entry = new MapLayerFileCache.CacheEntry();
            entry.data = null;
            entry.age = 0;
            entry.loaded = false;
            cache.put(filename, entry);
        }
        return !entry.loaded;
    }

    // method to purge old cache data
    //-------------------------------
    public void purge()
    {
        while (cache.size() > 4)
        {
            int oldest = -1;
            String oldestKey = null;

            // look for the oldest cache entry to purge
            Enumeration keys = cache.keys();
            while (keys.hasMoreElements())
            {
                String filename = (String)keys.nextElement();
                CacheEntry entry = (CacheEntry)cache.get(filename);
                if (entry.age >= oldest)
                {
                    oldest = entry.age;
                    oldestKey = filename;
                }
            }

            // if the oldest entry is current, don't remove it since more
            // than 4 files need to be cached to keep the active set available
            if (oldest == 0)
                break;

            cache.remove(oldestKey);
        }
    }

    // method to return the files that need to be loaded
    //--------------------------------------------------
    String[] getFilesToLoad()
    {
        String[] files = new String[cache.size()];
        int i = 0;

        // look for the cache entries from the current generation that
        // have not been loaded yet
        Enumeration keys = cache.keys();
        while (keys.hasMoreElements())
        {
            String filename = (String)keys.nextElement();
            CacheEntry entry = (CacheEntry)cache.get(filename);
            if ((entry.age == 0) && !entry.loaded)
            {
                // needed and not yet loaded
                files[i] = filename;
                i++;
            }
        }

        if (i == 0)
        {
            // nothing to load, so just return null
            return null;
        }
        else
        {
            // files were found that need loading
            String[] returnList = new String[i];
            for (int j = 0; j < i; j++)
                returnList[j] = files[j];
            return returnList;
        }
    }

    // set the data for a cache element
    //---------------------------------
    void setCacheContents(String filename, Vector data)
    {
        CacheEntry entry = (CacheEntry)cache.get(filename);
        entry.data = data;
        entry.loaded = true;
    }

    // method to return the data for the current cache contents
    //---------------------------------------------------------
    Vector getCachedData()
    {
        Vector data = new Vector();

        // sort the keys in the cache alphabeticallys so that all the order
        // of the data returned is consistent (this fixes problems with the
        // displayed cities sometimes changing around based on the order of
        // the data given to it)
        int size = cache.size();
        int count = 0;
        String[] filenames = new String[size];

        Enumeration keys = cache.keys();
        while (keys.hasMoreElements())
        {
            String filename = (String)keys.nextElement();
            int insert_index;
            for (insert_index = 0; insert_index < count; insert_index++)
            {
                if (filename.compareTo(filenames[insert_index]) > 0)
                    break;
            }
            for (int i = count; i > insert_index; i--)
            {
                filenames[i] = filenames[i-1];
            }
            filenames[insert_index] = filename;
            count++;
        }

        // look for the cache entries from the current generation
        for (int i = 0; i < count; i++)
        {
            String filename = filenames[i];
            CacheEntry entry = (CacheEntry)cache.get(filename);
            if ((entry.age == 0) && entry.loaded && (entry.data != null))
            {
                // needed and not yet loaded
                data.addElement(entry.data);
            }
        }

        return data;
    }
}
