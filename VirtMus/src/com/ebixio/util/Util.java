/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.util;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author GBURCA1
 */
public class Util {

    public static String join(Collection<String> s, String delimiter) {
        if (s.isEmpty()) {
            return "";
        }
        Iterator<String> iter = s.iterator();
        StringBuffer buffer = new StringBuffer(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        return buffer.toString();
    }
}
