/*
 * Copyright (C) 2006-2010  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

package com.ebixio.annotations.tools;

import com.ebixio.annotations.AnnotCanvas;
import com.ebixio.virtmus.shapes.ShapePoint;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 *
 * @author GBURCA
 */
public class ToolDot extends DrawingTool {
    public ToolDot(AnnotCanvas canvas) {
        super(canvas);
    }

    @Override
    public String getName() {
        return "Dot";
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point p = getAbsolutePoint(e.getPoint());

            if (canvas.musicPage != null) {
                canvas.musicPage.addAnnotation(new ShapePoint(canvas.paint, canvas.alpha, Math.round(canvas.diam / canvas.scale), p));
            }
            canvas.repaint();
        }
    }
}
