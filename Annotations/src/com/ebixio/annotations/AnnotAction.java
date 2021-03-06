/*
 * Copyright (C) 2006-2007  Gabriel Burca (gburca dash virtmus at ebixio dot com)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.ebixio.annotations;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 * Action which shows the Annotation window component.
 */
@ActionID(id = "com.ebixio.annotations.AnnotAction", category = "Window")
@ActionRegistration(lazy = false, displayName = "#CTL_AnnotAction")
@ActionReference(path = "Menu/Window", name = "AnnotAction", position = 310)
public class AnnotAction extends AbstractAction {
    
    public AnnotAction() {
        super(NbBundle.getMessage(AnnotAction.class, "CTL_AnnotAction"));
        putValue(SMALL_ICON, new ImageIcon(ImageUtilities.loadImage(AnnotTopComponent.ICON_PATH, true)));
    }
    
    @Override
    public void actionPerformed(ActionEvent evt) {
        TopComponent win = AnnotTopComponent.findInstance();
        win.open();
        win.requestActive();
    }
    
}
