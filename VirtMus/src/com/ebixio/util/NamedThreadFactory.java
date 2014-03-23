/*
 * MusicPage.java
 *
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

package com.ebixio.util;

import java.util.concurrent.ThreadFactory;

/**
 *
 * @author Gabriel Burca
 * 
 */
public class NamedThreadFactory implements ThreadFactory {
    private final String poolName;
    public NamedThreadFactory(String poolName) {
        this.poolName = poolName;
    }
    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable, poolName);
    }
}