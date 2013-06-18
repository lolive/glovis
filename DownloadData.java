// Name: DownloadData.java
//
// Description: provides a helper class for starting a download of the
//  inventory metadata and browse.
//
//--------------------------------------------------------------------------
import java.awt.Point;
import java.awt.Polygon;
import java.io.File;
import java.lang.String;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class DownloadData
{
    static File savedDirectory = null;  // directory previously chosen

    // method to start the download of data from the server.  It makes sure
    // there is data to download, allows the user to select where to download
    // the data to, and starts a file downloading thread to download the data
    //-----------------------------------------------------------------------
    static void downloadData
    (
        imgViewer applet,       // I: reference to the main applet class
        boolean downloadCell,   // I: flag to download the center cell
        boolean downloadSceneList // I: flag to download the data from the
                                  // current scene list
    )
    {
        TOC[] mosaicCells = applet.md.getMosaicCells();
        PolygonIntersectTester intersectTester = null;
        Sensor currSensor = applet.sensorMenu.getCurrentSensor();
        int cellsToDisplay = currSensor.getNumCellsAtResolution(
                                applet.md.pixelSize);

        // make sure there are scenes to download
        int downloadCount = 0;

        if (downloadSceneList)
        {
            // downloading data from the current scene list, so get the count
            // from the scene list
            downloadCount = currSensor.sceneList.getSceneCount();
        }
        else if (cellsToDisplay == Sensor.SINGLE_SCENE)
        {
            // only displaying a single scene, only download it
            if (applet.md.getCurrentScene() != null)
                downloadCount = 1;
        }
        else if (currSensor.isFullMosaic)
        {
            // for a full mosaic sensor, if downloading anything that
            // intersects the center cell, determine the cell extents and
            // construct a polygon intersect testing object to detect the
            // scenes that intersect the cell
            if (downloadCell)
            {
                // if the upper left corner isn't valid, return since something
                // basic is wrong
                Point ul = applet.imgArea.getUpperLeftCorner();
                if (ul == null)
                    return;

                // get the pixel size and projection currently displayed
                double pixelSize = applet.md.actualPixelSize;
                ProjectionTransformation proj = applet.md.getProjection();

                // estimate the viewport by splitting the difference between
                // the center of the center cell and the four corner cells.
                // Note this is an approximation, but it should work well
                // enough for our use.
                int mosaicWidth = applet.md.getMosaicWidth();
                int mosaicHeight = applet.md.getMosaicHeight();
                int mosaicSize = applet.md.getMosaicSize();
                int[] cells = {0, mosaicSize - mosaicWidth, mosaicSize - 1,
                        mosaicWidth - 1}; // cells used to construct viewport
                Point center = mosaicCells[applet.md.getMosaicCenterIndex()]
                    .getCenterProjCoords(proj);
                int[] tempX = new int[4];
                int[] tempY = new int[4];
                for (int i = 0; i < cells.length; i++)
                {
                    Point current 
                        = mosaicCells[cells[i]].getCenterProjCoords(proj);
                    tempX[i] = (current.x + center.x)/2;
                    tempY[i] = (current.y + center.y)/2;
                    // convert the coordinates to screen coordinates
                    tempX[i] = (int)Math.round((tempX[i] - ul.x)/pixelSize);
                    tempY[i] = (int)Math.round((ul.y - tempY[i])/pixelSize);
                }

                // create the polygon intersection testing object
                Polygon cellBoundary = new Polygon(tempX,tempY,4);
                intersectTester = new PolygonIntersectTester(cellBoundary);
                Point offset = applet.imgArea.getOffsetToCenterDisplay();
                intersectTester.translate(-offset.x, -offset.y);
            }

            // look in all the cells to count how many scenes need to be
            // downloaded
            for (int cell = 0; cell < mosaicCells.length; cell++)
            {
                TOC currentCell = mosaicCells[cell];

                // skip cells that aren't valid since a cell may have scenes
                // listed, but not be valid (like at projection area boundaries)
                if (!currentCell.valid)
                    continue;

                for (int i = 0; i < currentCell.numImg; i++)
                {
                    Metadata scene = currentCell.scenes[i];

                    // consider only visible scenes
                    if (scene.visible)
                    {
                        // if downloading any visible data, or if the scene
                        // intersects the center cell boundary, it is a scene
                        // to download
                        if (!downloadCell 
                            || intersectTester.intersects(scene.screenLocation))
                        {
                            downloadCount++;
                        }
                    }
                }
            }
        }
        else
        {
            // downloading data for a non-full mosaic (i.e. like Landsat)
            if (downloadCell)
            {
                // downloading data for the center cell, with a full temporal
                // search
                TOC currentCell = mosaicCells[applet.md.getMosaicCenterIndex()];

                if (currentCell.valid)
                {
                    // count the scenes that meet the current search limit
                    // restrictions
                    for (int i = 0; i < currentCell.numImg; i++)
                    {
                        Metadata scene = currentCell.scenes[i];
                        if (scene.visible)
                            downloadCount++;
                    }
                }
            }
            else
            {
                // downloading just the visible data shown on the display,
                // so loop through the cells, considering just the currently
                // displayed scene in each cell
                for (int cell = 0; cell < mosaicCells.length; cell++)
                {
                    TOC currentCell = mosaicCells[cell];

                    if (currentCell.valid)
                    {
                        Metadata scene =
                            currentCell.scenes[currentCell.currentDateIndex];

                        if (scene.visible)
                            downloadCount++;
                    }
                }
            }
        }

        if (downloadCount == 0)
        {
            // pop up a message box to indicate nothing to download
            JOptionPane.showMessageDialog(applet.getDialogContainer(),
                "No scenes are available for download", "No scenes available",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // get the location to download the data to
        File destDir = null;

        // fix some labels on JFileChooser
        String oldFileNameLabelText
            = UIManager.getString("FileChooser.fileNameLabelText");
        UIManager.put("FileChooser.fileNameLabelText", "Directory Name:");
        String oldSaveInLabelText
            = UIManager.getString("FileChooser.saveInLabelText");
        UIManager.put("FileChooser.saveInLabelText", "Look In:");
        try
        {
            // create a swing file chooser for selecting the destination
            // directory
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Select destination directory");

            // if there is a previously saved directory and that directory
            // exists, start the file chooser there
            if ((savedDirectory != null) && savedDirectory.exists())
                fc.setCurrentDirectory(savedDirectory);

            // show the file chooser dialog
            int result = fc.showSaveDialog(applet.getDialogContainer());
            if (result == JFileChooser.APPROVE_OPTION)
            {
                // a directory was chosen, so set it
                destDir = fc.getSelectedFile();
            }
        }
        catch (Exception e)
        {
        }

        // restore labels on JFileChooser
        UIManager.put("FileChooser.fileNameLabelText", oldFileNameLabelText);
        UIManager.put("FileChooser.saveInLabelText", oldSaveInLabelText);

        // if the user selected a destination directory, start the file
        // transfer
        if (destDir != null)
        {
            try
            {
                // make sure destination directory exists and can be written to
                if (destDir.exists())
                {
                    if (!destDir.canWrite())
                    {
                        // popup a message box on error
                        JOptionPane.showMessageDialog(
                            applet.getDialogContainer(),
                            "Error: Cannot write to " + destDir,
                            "Error downloading data",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                else
                {
                    if (!destDir.mkdirs())
                    {
                        // popup a message box on error
                        JOptionPane.showMessageDialog(
                            applet.getDialogContainer(),
                            "Error: Cannot create directory " + destDir,
                            "Error downloading data",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
            catch (Exception e)
            {
            }


            savedDirectory = destDir;

            // build a list of the scene metadata objects to download (need
            // a separate list since the download happens in its own thread 
            // and the mosaicCells array may be changed by the MosaicData
            // class)
            Metadata[] scenes = new Metadata[downloadCount];
            if (downloadSceneList)
            {
                // download the scenes from the current scene list
                SceneList list = currSensor.sceneList;
                for (int i = 0; i < downloadCount; i++)
                    scenes[i] = list.getSceneAt(i);
            }
            else if (cellsToDisplay == Sensor.SINGLE_SCENE)
                scenes[0] = applet.md.getCurrentScene();
            else if (currSensor.isFullMosaic)
            {
                // get the list of scenes for a full mosaic sensor
                int currIndex = 0;
                for (int cell = 0; cell < mosaicCells.length; cell++)
                {
                    TOC currentCell = mosaicCells[cell];

                    // skip cells that aren't valid since a cell may have
                    // scenes listed, but not be valid (like at projection area
                    // boundaries)
                    if (!currentCell.valid)
                        continue;

                    for (int i = 0; i < currentCell.numImg; i++)
                    {
                        Metadata scene = currentCell.scenes[i];

                        if (scene.visible)
                        {
                            // if downloading any visible data, or if the scene
                            // intersects the center cell boundary, it is a
                            // scene to download
                            if (!downloadCell || intersectTester.intersects(
                                    scene.screenLocation))
                            {
                                scenes[currIndex] = new Metadata(scene);
                                currIndex++;
                            }
                        }
                    }
                }
            }
            else
            {
                // get the list of scenes for a non-full mosaic sensor (i.e.
                // like Landsat)
                int currIndex = 0;
                if (downloadCell)
                {
                    // downloading data for the center cell, with a full 
                    // temporal search
                    TOC currentCell
                        = mosaicCells[applet.md.getMosaicCenterIndex()];

                    if (currentCell.valid)
                    {
                        // add the scenes that meet the current search limit
                        // restrictions
                        for (int i = 0; i < currentCell.numImg; i++)
                        {
                            Metadata scene = currentCell.scenes[i];
                            if (scene.visible)
                            {
                                scenes[currIndex] = new Metadata(scene);
                                currIndex++;
                            }
                        }
                    }
                }
                else
                {
                    // downloading just the visible data shown on the display,
                    // so just get the current scene shown in each cell
                    for (int cell = 0; cell < mosaicCells.length; cell++)
                    {
                        TOC currentCell = mosaicCells[cell];

                        if (currentCell.valid)
                        {
                            Metadata scene =
                               currentCell.scenes[currentCell.currentDateIndex];

                            if (scene.visible)
                            {
                                scenes[currIndex] = new Metadata(scene);
                                currIndex++;
                            }
                        }
                    }
                }
            }

            // create the file downloader object and start the download thread
            FileDownloader fd = new FileDownloader(applet, destDir, scenes);
            fd.startDownload();
        }
    }
}
