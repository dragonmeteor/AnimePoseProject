package yumyai.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/**
 * Class that handles the GUI events
 *
 * @author arbree Oct 21, 2005 Copyright 2005 Program of Computer Graphics,
 *         Cornell University
 */
public class BasicAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    protected ActionListener listener;

    public BasicAction(String newName, ActionListener newListener) {
        super(newName);
        listener = newListener;
    }

    public void setShortDescription(String s) {
        putValue(AbstractAction.SHORT_DESCRIPTION, s);
    }

    public void setMnemonicKey(int i) {

        putValue(AbstractAction.MNEMONIC_KEY, new Integer(i));
    }

    public void setAcceleratorKey(int key, int masks) {
        putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, masks));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        listener.actionPerformed(e);
    }
}