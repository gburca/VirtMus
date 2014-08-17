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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that iterates through user-specified positive number ranges.
 *
 * Usage:
 * for (int i : new NumberRange("3-5,9,2-6")) print(i);
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class NumberRange implements Iterable<Integer> {
    List<Pair<Integer, Integer>> ranges = new ArrayList<>();

    public NumberRange(int start, int end) {
        if (start <= end) {
            ranges.add(new Pair<>(start, end));
        }
    }

    /**
     * Constructs a number range by parsing the input string.
     *
     * Range bounds are separated by "-", and ranges are separated by ",". Ex:
     * "3-5,7,9-10". Negative numbers can not be used. Degenerate ranges ("5") are
     * supported. Inverse ranges ("5-3") are silently ignored - they're represented
     * by the empty set. The ranges do not have to be consecutive, and may overlap.
     * For example: "5-9, 2-7".
     *
     * Usage:
     * for (int i : new NumberRange("3-5,9,2-6")) print(i);
     *
     * @param rangeStr A description of the range to generate.
     */
    public NumberRange(String rangeStr) {
        String clean = rangeStr.replaceAll("[^-,0-9]", "");
        clean = clean.replaceAll("^\\D+", "");
        clean = clean.replaceAll("\\D+$", "");

        // number, optional ("-" + another number), end is another comma or string end
        Pattern nextVal = Pattern.compile("([0-9]+)(?:-([0-9]+))?(?:,|$)");
        Matcher m = nextVal.matcher(clean);

        while (m.find()) {
            Pair<Integer, Integer> p;
            Integer i1 = Integer.parseInt(m.group(1));
            if (m.group(2) == null) {
                // Degenerate range
                p = new Pair<>(i1, i1);
            } else {
                p = new Pair<>(i1, Integer.parseInt(m.group(2)));
            }

            if (p.first <= p.second) {
                ranges.add(p);
            }
        }
    }

    @Override
    public Iterator iterator() {
        return new RangeIterator(ranges);
    }

    public class RangeIterator implements Iterator {
        List<Pair<Integer, Integer>> ranges;
        int range;
        int current;
        boolean hasNext;

        public RangeIterator(List<Pair<Integer, Integer>> ranges) {
            if (ranges == null || ranges.isEmpty()) {
                hasNext = false;
            } else {
                hasNext = true;
                this.ranges = ranges;
                range = 0;
                current = ranges.get(range).first;
            }
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public Integer next() {
            Integer val = current;
            if (current < ranges.get(range).second) {
                current++;
            } else if (range < ranges.size() - 1) {
                range++;
                current = ranges.get(range).first;
            } else {
                hasNext = false;
            }
            return val;
        }

        @Override
        public void remove() {
            // not supported
        }
    }
}
