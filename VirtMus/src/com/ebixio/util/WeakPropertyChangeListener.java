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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class WeakPropertyChangeListener implements PropertyChangeListener {
    WeakReference<PropertyChangeListener> listenerRef;
    Object src;

    /**
     *
     * @param listener The listener that should be notified.
     * @param src The source from which we should remove the listener when the
     * weak reference is gone.
     */
    public WeakPropertyChangeListener(PropertyChangeListener listener, Object src) {
        listenerRef = new WeakReference<>(listener);
        this.src = src;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt){
        PropertyChangeListener listener = (PropertyChangeListener)listenerRef.get();
        if (listener == null){
            removeListener();
        } else {
            listener.propertyChange(evt);
        }
    }

    private void removeListener(){
        try{
            Method method = src.getClass().getMethod("removePropertyChangeListener",
                    new Class[] {PropertyChangeListener.class});
            method.invoke(src, new Object[]{ this });
        } catch(NoSuchMethodException | SecurityException |
                IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e){
            Log.log(e);
        }
    }

    public boolean isSameListener(PropertyChangeListener other) {
        if (other == null) return false;
        
        if (other instanceof WeakPropertyChangeListener) {
            WeakPropertyChangeListener wpcl = (WeakPropertyChangeListener)other;
            return listenerRef.get() == wpcl.listenerRef.get()
                    && src == wpcl.src;            
        } else {
            return other == listenerRef.get();
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[" + listenerRef.get() + "]";
    }
}
