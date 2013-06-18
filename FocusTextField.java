// FocusTextField.java is a simple class to allow automatically selecting
// the contents of a TextField when it gets the focus.
//-----------------------------------------------------------------------
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.JTextField;

class FocusTextField extends JTextField implements FocusListener
{
    // constructor to request focus events
    //------------------------------------
    FocusTextField(int columns)
    {
        super(columns);
        addFocusListener(this);
    }
    
    // method to select the entire contents of the text field when the focus
    // is gained
    //----------------------------------------------------------------------
    public void focusGained(FocusEvent e)
    {
        selectAll();
    }

    // method to unselect the contents of the text field when the focus is lost
    //-------------------------------------------------------------------------
    public void focusLost(FocusEvent e)
    {
        select(0,0);
    }
}
