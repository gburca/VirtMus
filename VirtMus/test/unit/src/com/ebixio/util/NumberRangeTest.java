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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class NumberRangeTest {

    public NumberRangeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of iterator method, of class NumberRange.
     */
    @Test
    public void testIterator() {
        System.out.println("iterator");

        iteratorHelper("5", new int[]{5});
        iteratorHelper("5-", new int[]{5});
        iteratorHelper("-5", new int[]{5});
        iteratorHelper("-5-", new int[]{5});
        iteratorHelper("5,", new int[]{5});
        iteratorHelper(",5", new int[]{5});
        iteratorHelper(",5,", new int[]{5});

        iteratorHelper("5-8", new int[]{5, 6, 7, 8});
        iteratorHelper("3-5,9,2-3", new int[]{3, 4, 5, 9, 2, 3});
        iteratorHelper("5-3", new int[]{});
    }

    private void iteratorHelper(String r, int[] expResult) {
        NumberRange instance = new NumberRange(r);
        int expIdx = 0;
        for (int i : instance) {
            assertEquals(expResult[expIdx++], i);
        }
        assertEquals(expResult.length, expIdx);
    }

}
