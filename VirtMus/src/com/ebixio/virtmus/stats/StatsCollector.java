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
package com.ebixio.virtmus.stats;

import com.ebixio.util.Log;
import com.ebixio.virtmus.Utils;
import java.awt.Dimension;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Generates a LogRecord for some of the statistically meaningful properties.
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class StatsCollector {

    /**
     * Logs a bunch of configurations/settings every time VirtMus is started.
     * @param logger
     */
    public static void logStartup(Logger logger) {
        logger.log(getSystemConfig());
        logger.log(getCpuInfo());
        logger.log(getMemInfo());
        logger.log(getScreenSizeInfo());
    }

    /** Creates a LogRecord with the JRE info. */
    static LogRecord getSystemConfig() {
        LogRecord log = new LogRecord(Level.INFO, "System Config");
        String os = System.getProperty("os.name", "unknown name") + ", " + System.getProperty("os.version", "unknown version") + ", " + System.getProperty("os.arch", "unknown arch");
        String vm = System.getProperty("java.vm.name", "unknown VM name") + ", " + System.getProperty("java.vm.version", "unknown VM version") + ", " + System.getProperty("java.runtime.name", "unknown RT name") + ", " + System.getProperty("java.runtime.version", "unknown RT version");
        Object[] params = new Object[]{"OS: " + os, "JVM: " + vm};
        log.setParameters(params);
        return log;
    }

    /** Creates a LogRecord with the CPU info. */
    static LogRecord getCpuInfo() {
        LogRecord log = new LogRecord(Level.INFO, "CPU Info");
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Object[] params = new Object[]{"Cores: " + os.getAvailableProcessors(),
            "Arch: " + os.getArch()};
        log.setParameters(params);
        return log;
    }

    /** Creates a LogRecord with the amount of physical memory present. */
    static LogRecord getMemInfo() {
        LogRecord log = new LogRecord(Level.INFO, "Memory");
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Method m = osBean.getClass().getMethod("getTotalPhysicalMemorySize");
            m.setAccessible(true);
            long memSz = (Long) m.invoke(osBean);
            log.setParameters(new Object[]{memSz});
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Log.log(ex);
        }
        return log;
    }

    /** Creates a LogRecord with the number of screens and the size of each. */
    static LogRecord getScreenSizeInfo() {
        LogRecord log = new LogRecord(Level.INFO, "Screen Size");
        List<Object> params = new ArrayList<>();
        int screens = Utils.getNumberOfScreens();
        params.add(screens);
        Dimension[] sizes = Utils.getScreenSizes();
        for (Dimension d : sizes) {
            params.add(String.valueOf(d.width) + "x" + String.valueOf(d.height));
        }
        log.setParameters(params.toArray());
        return log;
    }
}
