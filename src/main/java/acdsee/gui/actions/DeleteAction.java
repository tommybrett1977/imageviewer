/*
 * DeleteAction.java
 *
 * Created on 4. Dezember 2006, 19:56
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package acdsee.gui.actions;

import acdsee.gui.components.thumbnail.FileThumbnail;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 *
 * @author Tommy Brettschneider
 */
public class DeleteAction extends AbstractAction {

    /**
     * Creates a new instance of DeleteAction
     */
    public DeleteAction() {
        super();
    }

    public DeleteAction(String text) {
        super(text, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = null;
        if (source instanceof FileThumbnail) {
            FileThumbnail thumb = (FileThumbnail) source;
            //thumb.delete();
        }
    }
}
