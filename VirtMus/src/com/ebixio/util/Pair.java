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

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 * @param <T1> Type of the first element
 * @param <T2> Type of the second element
 */
public class Pair<T1, T2> {
    public T1 first;
    public T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return super.toString() +
                " [" + first.toString() + "," + second.toString() + "]";
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Pair)) {
            return false;
        }
        final Pair<?,?> otherPair = (Pair<?,?>) other;
        return (first == null ? otherPair.first == null : first.equals(otherPair.first)) &&
            (second == null ? otherPair.second == null : second.equals(otherPair.second));
    }

    @Override
    public int hashCode() {
        int res = 17;
        res = res * 31 + (first == null ? 0 : first.hashCode());
        res = res * 31 + (second == null ? 0 : second.hashCode());
        return res;
    }

    /**
     * Creates a new Pair.
     * @param <First>   the type of the first element
     * @param <Second>  the type of the second element
     * @param first     the first element
     * @param second    the second element
     * @return  the new {@link Pair} of the first and second elements.
     */
    public static <First,Second> Pair<First,Second> of (final First first, final Second second) {
        return new Pair<>(first, second);
    }
}
