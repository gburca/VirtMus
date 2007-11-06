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

package com.ebixio.virtmus.options;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.spi.options.OptionsCategory;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

public final class VirtmusOptionsCategory extends OptionsCategory {
    
    @Override
    public Icon getIcon() {
        return new ImageIcon(Utilities.loadImage("com/ebixio/virtmus/resources/VirtMus32x32.png"));
    }
    
    public String getCategoryName() {
        return NbBundle.getMessage(VirtmusOptionsCategory.class, "OptionsCategory_Name_VirtMusOpt");
    }
    
    public String getTitle() {
        return NbBundle.getMessage(VirtmusOptionsCategory.class, "OptionsCategory_Title_VirtMusOpt");
    }
    
    public OptionsPanelController create() {
        return new VirtmusOptionsPanelController();
    }
    
}
