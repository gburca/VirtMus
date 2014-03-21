/*
 * Copyright (C) 2006-2012  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Gabriel Burca <gburca dash virtmus at ebixio dot com>
 */
public class VirtMusLookup extends AbstractLookup {
    
    private InstanceContent ic;
    
    private VirtMusLookup(InstanceContent ic) {
        super(ic);
        this.ic = ic;
    }
    
    public synchronized void add(Object inst) {
        ic.add(inst);
    }
    
    public synchronized void remove(Object inst) {
        ic.remove(inst);
    }
    
    public static synchronized VirtMusLookup getInstance() {
        return VirtMusLookupHolder.INSTANCE;
    }
    
    private static class VirtMusLookupHolder {
        private static final VirtMusLookup INSTANCE = new VirtMusLookup(new InstanceContent());
    }
}
