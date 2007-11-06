/*
 * VirtMusKernel.java
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
package com.ebixio.virtmus;

import javax.media.jai.KernelJAI;

/**
 *
 * @author GBURCA
 */
public class VirtMusKernel {

    private static final float[] IDENTITY = {0, 0, 0,
            0, 1, 0,
            0, 0, 0};
    private static final float[] EDGE = {0, 1, 0,
            1, 0, 1,
            0, 1, 0};
    private static final float[] CORNER = {1, 0, 1,
            0, 0, 0,
            1, 0, 1};

    public static KernelJAI getKernel(int corner,
            int edge,
            int identity) {
        float[] kernel = new float[9];
        int sum = corner * 4 + edge * 4 + identity;
        if (sum == 0) {
            sum = 1;
        } // to avoid dividing by zero
        for (int i = 0; i < 9; i++) {
            kernel[i] = (corner * CORNER[i] + edge * EDGE[i] + identity * IDENTITY[i]) / sum;
        }
        return new KernelJAI(3, 3, kernel);
    }
    
//    /**
//     * Creates a symetric Kernel of size "size*2 + 1"
//     * @param size Kernel size (see above)
//     * @return
//     */
//    public static KernelJAI getSymKernel(int size) {
//        if (size < 1) {
//            return getKernel(1, 1, 3);
//        }
//        int size2 = size * 2 + 1;
//        float[] data = new float[size2];
//        
//        for (int i = 0; i < size + 1; i++) {
//            data[i] = i+1;
//        }
//        for (int i = 0; i < size; i++) {
//            data[size + 1 + i] = data[size - 1 - i];
//        }
//        
//        // TODO: Data must be normalized
//        
//        return new KernelJAI(size2, size2, size+1, size+1, data, data);
//    }

}
