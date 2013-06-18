//---------------------------------------------------------------------------
// Name: DBFReader
//
// Description: Provides a class for reading a DBF database file (the kind
//      that is used to store attributes for a shapefile).
//---------------------------------------------------------------------------
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class DBFReader
{
    private FileChannel dbfChannel;     // IO channel for the .dbf file
    private FileInputStream dbfStream;  // IO stream for the .dbf file

    // ColumnInfo defines the info that describes each of the columns in the
    // dbf file
    class ColumnInfo
    {
        String name;    // name of the column
        int offset;     // offset to the column in the record
        int size;       // size of the column in bytes
        int decimals;   // decimal places in column (if a number column)
        char type;      // type of column
                        //   N or F = number (int, float, or scientific format)
                        //   C = character string
                        //   D = date in YYYYMMDD format
    }

    private ColumnInfo[] columnInfo;    // info for the columns in the file
    private int numRecords;             // number of records in the file
    private int headerLength;           // length of the header in bytes
    private int recordLength;           // length of a record in bytes
    private int numColumns;             // number of columns in the file

    private ByteBuffer recordBuffer;    // most recently read record buffer
    private int recordBufferIndex;      // most recently read record index

    private final int FILE_INFO_SIZE = 32; // bytes at the start of the file
                                           // that provide size info
    private final int COLUMN_INFO_SIZE = 32; // bytes in file for each column
                                             // info record

    // open the indicated shape file.  Throws an IOException if there is an
    // error opening the file.
    //---------------------------------------------------------------------
    public void open
    (
        String prefix   // I: name of shapefile without the extension
    ) throws IOException
    {
        // open the DBF file
        File dbfFile = new File(prefix + ".dbf");
        dbfStream = new FileInputStream(dbfFile);
        dbfChannel = dbfStream.getChannel();

        // get the basic information about the file layout
        ByteBuffer headerBuffer = ByteBuffer.allocate(FILE_INFO_SIZE);
        if (dbfChannel.read(headerBuffer, 0) != FILE_INFO_SIZE)
            throw new IOException("Error reading " + prefix + ".dbf header");

        // get the info from the buffer
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
        numRecords = headerBuffer.getInt(4);
        headerLength = headerBuffer.getShort(8);
        recordLength = headerBuffer.getShort(10);
        // the number of columns 
        numColumns = (headerLength - FILE_INFO_SIZE) / COLUMN_INFO_SIZE;
        headerBuffer = null;

        // allocate a buffer for reading records
        recordBuffer = ByteBuffer.allocate(recordLength);
        recordBufferIndex = -1;

        // allocate the buffer for the column info
        ByteBuffer colInfoBuffer 
                = ByteBuffer.allocate(headerLength - FILE_INFO_SIZE);
        if (dbfChannel.read(colInfoBuffer, FILE_INFO_SIZE) 
            != (headerLength - FILE_INFO_SIZE))
        {
            throw new IOException("Error reading " + prefix
                                  + ".dbf column info");
        }
        colInfoBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // allocate a buffer with 11 bytes for the column names
        byte[] nameBuffer = new byte[11];

        // allocate an array for the column info
        columnInfo = new DBFReader.ColumnInfo[numColumns];

        // read the info for all the columns
        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++)
        {
            ColumnInfo column = new DBFReader.ColumnInfo();
            columnInfo[columnIndex] = column;
            int index = COLUMN_INFO_SIZE * columnIndex;

            column.type = (char)colInfoBuffer.get(index + 11);
            if ((column.type == 'N') || (column.type == 'F'))
            {
                // get the size for the number columns
                column.size = (int)colInfoBuffer.get(index + 16);
                column.decimals = colInfoBuffer.get(index + 17);
            }
            else
            {
                // get the size for the other types of columns
                column.size = colInfoBuffer.getShort(index + 16);
                column.decimals = 0;
            }

            // calculate the offset in the record to this column.  The first
            // column starts at an offset of 1 and the columns after that are
            // calculated using the previous column offset and size.
            if (columnIndex == 0)
                column.offset = 1;
            else
            {
                column.offset = columnInfo[columnIndex - 1].offset 
                             + columnInfo[columnIndex - 1].size;
            }

            // get the column name
            for (int i = 0; i < nameBuffer.length; i++)
                nameBuffer[i] = colInfoBuffer.get(index + i);
            column.name = new String(nameBuffer).trim();
        }
        colInfoBuffer = null;
    }

    // method to return an array with the column names
    //------------------------------------------------
    public String[] getColumnNames()
    {
        String[] names = new String[numColumns];
        for (int i = 0; i < numColumns; i++)
            names[i] = columnInfo[i].name;
        return names;
    }

    // return the number of records in the file
    //-----------------------------------------
    public int getRecordCount()
    {
        return numRecords;
    }

    // method to close the DBF file.  Throws an IOException on an error.
    //------------------------------------------------------------------
    public void close() throws IOException
    {
        dbfChannel.close();
        dbfStream.close();
    }

    // helper method to return the offset in a file to a particular record.
    // Throws an IOException on error.
    //---------------------------------------------------------------------
    private int getRecordPosition(int recordIndex) throws IOException
    {
        int position = recordIndex * recordLength + headerLength;
        return position;
    }

    // method to return the index to the column with the provided column name.
    // Returns -1 if the column name is not found.
    //------------------------------------------------------------------------
    public int getColumnIndex(String columnName)
    {
        int foundIndex = -1;
        for (int index = 0; index < numColumns; index++)
        {
            if (columnName.equals(columnInfo[index].name))
            {
                foundIndex = index;
                break;
            }
        }

        return foundIndex;
    }

    // method to read the data from the indicated record and column.  Throws an
    // IOException if a file I/O error occurs.
    //-------------------------------------------------------------------------
    public String readColumn(int recordIndex, int columnIndex)
        throws IOException
    {
        // if the requested record is not in the record buffer, read it
        if (recordBufferIndex != recordIndex)
        {
            recordBuffer.clear();

            // read the record from the file
            int position = getRecordPosition(recordIndex);
            if (dbfChannel.read(recordBuffer, position) != recordLength)
            {
                throw new IOException("Error reading record at index "
                                      + recordIndex);
            }
            recordBufferIndex = recordIndex;
        }

        // extract the bytes for the column
        int offset = columnInfo[columnIndex].offset;
        int size = columnInfo[columnIndex].size;
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; i++)
            buffer[i] = recordBuffer.get(offset + i);

        // convert the bytes read into a string
        String value = new String(buffer, 0, size).trim();

        return value;
    }

    // method to read all the columns for a record and return an array of
    // strings  with the data.  Throws an IOException if a read error occurs.
    //-----------------------------------------------------------------------
    public String[] readRecord(int recordIndex) throws IOException
    {
        String[] values = new String[numColumns];

        for (int i = 0; i < numColumns; i++)
            values[i] = readColumn(recordIndex, i);

        return values;
    }
}
