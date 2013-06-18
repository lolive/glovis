// ProgressIndicator -- implements GUI component to display progress while 
// data is loading in the applet.
//
//--------------------------------------------------------------------------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

public class ProgressIndicator extends JPanel implements ActionListener
{
    private imgViewer applet;       // main applet for the fonts
    private Timer updateTimer;      // timer to animate the busy indicator
    private JProgressBar progressBar; // progress bar component
    private boolean isActive;       // flag to indicate the progress bar
                                    // updating is active
    private WorkMonitor[] workers;  // array of components doing work

    // Constructor for the ProgressIndicator
    //--------------------------------------
    ProgressIndicator(imgViewer applet)
    {
        this.applet = applet;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // create the progress bar
        progressBar = new JProgressBar();
        Dimension size = new Dimension(50, 18);
        progressBar.setPreferredSize(size);
        progressBar.setMinimumSize(size);
        progressBar.setMaximumSize(size);
        progressBar.setFont(applet.normalFont);
        progressBar.setForeground(Color.BLUE);
        add(progressBar);

        size.width = 130;
        setPreferredSize(size);
        setMaximumSize(size);

        isActive = false;

        // create the animation timer with an interval of 200 milliseconds
        updateTimer = new Timer(200, this);
    }

    // method to add working for monitoring
    //-------------------------------------
    public void addWorker(WorkMonitor worker)
    {
        int numWorkers = 1;

        if (workers != null)
            numWorkers += workers.length;
        WorkMonitor[] newWorkers = new WorkMonitor[numWorkers];

        for (int i = 0; i < (numWorkers - 1); i++)
            newWorkers[i] = workers[i];
        newWorkers[numWorkers - 1] = worker;
        workers = newWorkers;
    }

    // actionPerformed method for the animation timer
    //-----------------------------------------------
    public void actionPerformed(ActionEvent event)
    {
        // if the active flag has been cleared, make the progress indicator
        // invisible and stop the timer
        if (!isActive)
        {
            applet.statusBar.progress.setVisible(false);
            updateTimer.stop();
            return;
        }

        WorkMonitor wm = null;

        // get the highest priority worker
        for (int i = 0; i < workers.length; i++)
        {
            if (workers[i].isWorking())
            {
                wm = workers[i];
                break;
            }
        }

        // if a busy worker was found, update the display
        if (wm != null)
        {
            progressBar.setString(wm.getWorkLabel());
            progressBar.setStringPainted(true);
            progressBar.setMaximum(wm.getTotalWork());
            progressBar.setValue(wm.getWorkComplete());

            // if the active flag is set, make the progress indicator visible
            if (isActive)
                applet.statusBar.progress.setVisible(true);
        }
    }

    // method to set the indicator to the busy state
    //----------------------------------------------
    public void setBusy()
    {
        // set the active flag and make sure the update time is running
        isActive = true;
        updateTimer.start();
    }

    // method to set the indicator to the not busy state
    //--------------------------------------------------
    public void clearBusy()
    {
        // clear the active flag.  If the indicator was previously active,
        // the next timer message will clean up things (cannot do it here since
        // this can be called from threads besides the GUI thread)
        isActive = false;
    }
}
