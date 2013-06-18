// Simple cache for visited dates indexed by inventory grid column/row
//--------------------------------------------------------------------

public class DateCache
{
    private int size;       // size of cache
    private int[] cache;  
    // cache array, contains column, row, and scene date in a packed array.
    // Note: a separate class to spell out column/row/scene date was not 
    // created to cut down on the number of separate classes to download
    // by the user.

    private int[] age;
    // age of each entry in the cache (same index).  The lower age values are
    // the oldest values in the cache that haven't had a hit

    private int currentAge; 
    // counter of the current age.  This is incremented each time a cache hit
    // occurs for either read or write access

    // Constructor
    //------------
    DateCache(int cacheSize)
    {
        size = cacheSize;
        // allocate the cache. "* 3" since column, row, and date are packed in
        cache = new int[size * 3];
        age = new int[size];
        // initialize the age tracking information
        currentAge = 0;
        for (int i = 0; i < size; i++)
            age[i] = 0;
    }

    // age the cache values and return the next age value
    //---------------------------------------------------
    private int ageCache()
    {
        // normalize age occassionally.  This will probably never be used since
        // it is ridiculous that anyone would use the application this long.
        // But it is important to handle since otherwise the same cache entry
        // would be considered the oldest if the currentAge value overflowed.
        // It is okay if the age values go negative.
        if (currentAge > 500000)
        {
            for (int i = 0; i < size; i++)
                age[i] -= 500000;
            currentAge = 0;
        }

        // update the age counter and return the age for the hit entry
        currentAge++;
        return currentAge;
    }

    // Find the index to the column/row.  If there is a cache hit, the hit
    // item is made the most recent by updating it's age.
    // Returns the index in the cache if there is a hit, or -1 if no hit
    //------------------------------------------------------------------
    private int findCachedIndex(int column, int row)
    {
        // Brute force linear search for an entry with the same column/row.
        // The cache size should be kept relatively small to prevent the
        // search from taking too much time.
        for (int index = 0; index < size; index++)
        {
            // calculate the index for packing
            int packed_index = index * 3;
            if ((cache[packed_index] == column) && 
                (cache[packed_index+1] == row))
            {
                age[index] = ageCache();
                return index;
            }
        }
        // not found
        return -1;
    }

    // Add a new entry or update an existing entry
    //--------------------------------------------
    public void add(int column, int row, int sceneDate)
    {
        // look for the column/row in the cache
        int index = findCachedIndex(column,row);
        // if column/row not found, throw the oldest away and use it
        if (index == -1)
        {
            index = 0;
            int oldAge = age[0];
            for (int i = 1; i < size; i++)
            {
                // if the current one is older (smaller age) make it the 
                // candidate for replacement
                if (age[i] < oldAge)
                {
                    oldAge = age[i];
                    index = i;
                }
            }

            // set the age for the new entry   
            age[index] = ageCache();
        }
        // save the cache entry column/row tag and date index
        cache[index * 3] = column;
        cache[index * 3 + 1] = row;
        cache[index * 3 + 2] = sceneDate;
    }
    
    // Remove entry from cache 
    //----------------------------------------
    public void remove(int column, int row)
    {
        int index = findCachedIndex(column,row);
        if (index != -1)
        {
            cache[index * 3] = 0;
            cache[index * 3 + 1] = 0;
            age[index] = 0;
        }
    }
    // Look up the date for the column/row.
    // Returns the date index if there is a hit, otherwise it returns -1.
    //-------------------------------------------------------------------
    public int lookupDate(TOC cell)
    {
        // look for the column/row in the cache
        int index = findCachedIndex(cell.gridCol,cell.gridRow);
        // match the date with the correct date index
        if (index != -1)
        {
            for (int dateIndex = 0; dateIndex < cell.numImg; dateIndex++)
            {
                if (cell.scenes[dateIndex].date == cache[index*3+2])
                {
                    // cache hit, so return the date index.
                    return (dateIndex);
                }
            }
        }

        // cache miss, so return -1
        return -1;
    }

    // Flush the contents of the cache
    //--------------------------------
    public void flush()
    {
        // set the column element of the cache to an illegal column to 
        // cause it to miss for everything
        for (int i = 0; i < size; i++)
            cache[i * 3] = 0;
    }
}

