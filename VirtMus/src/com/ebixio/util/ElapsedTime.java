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
package com.ebixio.util;

import java.util.Date;

/**
 * Measures elapsed time. Used for quick and coarse profiling.
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class ElapsedTime {
    private Date lastTime = new Date();

    public String getElapsedTime() {
        StringBuilder res = new StringBuilder();
        Date thisTime = new Date();
        long elapsed = thisTime.getTime() - lastTime.getTime();

        res.append("Last time ").append(lastTime.toString());
        res.append(" now ").append(thisTime.toString());
        res.append(" Elapsed ").append((new Long(elapsed)).toString()).append("ms");

        lastTime = thisTime;
        return res.toString();
    }
}
