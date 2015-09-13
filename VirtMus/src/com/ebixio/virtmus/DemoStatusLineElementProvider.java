/*
 * Copyright (C) 2006-2014  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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
package com.ebixio.virtmus;


/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
import java.awt.Component;
import javax.swing.JLabel;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * This is how a component is added to the status line to override existing
 * functionality.
 *
 * Uncomment the annotation to activate. May not want to supersede both
 * notifications and progress.
 *
 * From a blog by Peter Stomenhoff
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
//@ServiceProvider(
//    service = StatusLineElementProvider.class,
//    supersedes = {
//        "org.netbeans.progress.module.ProgressVisualizerProvider",
//        "org.netbeans.core.ui.notifications.StatusLineElement"
//        "org.netbeans.modules.editor.impl.StatusLineFactories$LineColumn",
//        "org.netbeans.modules.editor.impl.StatusLineFactories$TypingMode"
//    }
//)
public class DemoStatusLineElementProvider implements StatusLineElementProvider {

    @Override
    public Component getStatusLineElement() {
        return new JLabel("hello");
    }

}