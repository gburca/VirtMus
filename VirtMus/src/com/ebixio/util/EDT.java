/*
 * Copyright (C) 2006-2015  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class EDT {

    /**
     * Wrapper around {@link javax.swing.SwingUtilities#invokeLater(java.lang.Runnable)} that allows us to
     * return a result {@link FutureTask} to the caller.
     *
     * Example:
     * <pre>
     * {@code
     * FutureTask<String> task = invokeLater(new Callable<String>() {
     *      public String call() throws Exception {
     *          return new String("hello");
     *      }});
     * String result = task.get();
     * }
     * </pre>
     *
     * @param <T> Result type
     * @param callable A task to execute on the EDT.
     * @return The task future result.
     */
    public static <T> FutureTask<T> invokeLater(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        SwingUtilities.invokeLater(task);
        return task;
    }

    /**
     * Wrapper around {@link javax.swing.SwingUtilities#invokeAndWait(java.lang.Runnable)} that allows us to
     * return a result to the caller.
     *
     * @param <T> Result type
     * @param callable A task to execute on the EDT
     * @return The task return value.
     * @throws InterruptedException Pass through from the invokeLater call.
     * @throws InvocationTargetException Pass through from the invokeLater call.
     */
    public static <T> T invokeAndWait(Callable<T> callable)
            throws InterruptedException, InvocationTargetException {
        try {
            //blocks until future returns
            return invokeLater(callable).get();
        } catch (ExecutionException e) {
            // We get a wrapped exception. Unwrap and pass on as appropriate.
            Throwable t = e.getCause();

            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof InvocationTargetException) {
                throw (InvocationTargetException) t;
            } else {
                throw new InvocationTargetException(t);
            }
        }
    }
}
