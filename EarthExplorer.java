// This is a simple class to allow changing the version of EarthExplorer
// pointed at since the URL is built in a few places with minor variations.
//-------------------------------------------------------------------------

import java.util.Vector;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

class EarthExplorer
{
    // helper method
    // -------------
    private final static String getBaseUrl()
    {
        String host_top_dir = "http://earthexplorer.usgs.gov";
        // override the URL above for test environments

        String url = host_top_dir + "/order/process?node=GV";
        return url;
    }

    // method to build the full URL based on the input dataset and IDs
    // ---------------------------------------------------------------
    public static String buildShoppingCartUrl(String dataset, Vector orderIds)
    {
        if (orderIds.size() == 0)
        {
            return null;
        }
        String url = getBaseUrl();

        url += "&dataset_name=" + dataset + "&ordered=" + orderIds.get(0);

        // we already added the first ID, so add the rest
        for (int i = 1; i < orderIds.size(); i++)
        {
            url += "," + orderIds.get(i);
        }

        return url;
    }

    // method to build the full URL for multiple datasets
    // should use JSON for this but I didn't feel like
    // downloading another package... which approach is lazier??
    // idMap is keyed on dataset_name, each value is a Vector of IDs
    // ------------------------------------------------------------------
    public static String buildShoppingCartMultiDatasetUrl (HashMap idMap)
    {
        if (idMap.isEmpty())
        {
            return null;
        }
        String url = getBaseUrl();
        String dataset = new String();
        Vector orderIds;
        Iterator datasetIterator = idMap.keySet().iterator();

        // rather than &dataset_name=NAME&ordered=ID,ID as for a single dataset,
        // the format for multiple datasets is JSON-encoded as:
        // &ordered={"NAME":["ID","ID"],"NAME":["ID"]}&dataset_name=&type=json

        url += "&ordered={";

        while (datasetIterator.hasNext())
        {
            dataset = (String) datasetIterator.next();
            orderIds = (Vector) idMap.get(dataset);
            if (orderIds.size() > 0)
            {
                url += "\"" + dataset + "\":[\"" + orderIds.get(0) + "\"";
                // we already added the first ID, so add the rest
                for (int i = 1; i < orderIds.size(); i++)
                {
                    url += ",\"" + orderIds.get(i) + "\"";
                }
                url += "]";
                if (datasetIterator.hasNext())
                {
                    url += ",";
                }
            }
        }

        url += "}&dataset_name=&type=json";

        return url;
    }
}

