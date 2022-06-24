/*
 * NMRFx Processor : A Program for Processing NMR Data 
 * Copyright (C) 2004-2018 One Moon Scientific, Inc., Westfield, N.J., USA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nmrfx.processor.gui.annotations;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.nmrfx.graphicsio.GraphicsContextInterface;
import org.nmrfx.processor.gui.CanvasAnnotation;
import org.nmrfx.utils.GUIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brucejohnson
 */
public class AnnoText implements CanvasAnnotation {

    double x1;
    double y1;
    double x2;
    double y2;
    double startX1;
    double startY1;
    double startX2;
    double startY2;
    private static final Logger log = LoggerFactory.getLogger(AnnoText.class);

    POSTYPE xPosType;
    POSTYPE yPosType;
    Bounds bounds2D;
    protected String text;
    Font font = Font.font("Liberation Sans", 12);

    Color fill = Color.BLACK;

    public AnnoText(double x1, double y1, double x2, double y2,
            POSTYPE xPosType, POSTYPE yPosType, String text) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.xPosType = xPosType;
        this.yPosType = yPosType;
        this.text = text;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * @return the fill
     */
    public Color getFill() {
        return fill;
    }

    /**
     * @param fill the fill to set
     */
    public void setFill(Color fill) {
        this.fill = fill;
    }

    public boolean hit(double x, double y) {
        boolean hit = bounds2D.contains(x, y);
        if (hit) {
            startX1 = x1;
            startX2 = x2;
            startY1 = y1;
            startY2 = y2;
        }
        return hit;
    }

    public Bounds getBounds() {
        return bounds2D;
    }

    @Override
    public void move(double[][] bounds, double[][] world, double[] start, double[] pos) {
        double dx = pos[0] - start[0];
        double dy = pos[1] - start[1];
        x1 = xPosType.move(startX1, dx, bounds[0], world[0]);
        x2 = xPosType.move(startX2, dx, bounds[0], world[0]);
        y1 = yPosType.move(startY1, dy, bounds[1], world[1]);
        y2 = yPosType.move(startY2, dy, bounds[1], world[1]);
    }

    @Override
    public void draw(GraphicsContextInterface gC, double[][] bounds, double[][] world) {
        try {
            gC.setFill(fill);
            gC.setFont(font);
            gC.setTextAlign(TextAlignment.LEFT);
            gC.setTextBaseline(VPos.BASELINE);
            double width = GUIUtils.getTextWidth(text, font);

            double xp1 = xPosType.transform(x1, bounds[0], world[0]);
            double yp1 = yPosType.transform(y1, bounds[1], world[1]);
            double xp2 = xPosType.transform(x2, bounds[0], world[0]);
            double regionWidth = xp2 - xp1;
            if (width > regionWidth) {
                double charWidth = width / text.length();
                int start = 0;
                int end;
                double yOffset = 0.0;
                do {
                    end = start + (int) (regionWidth / charWidth);
                    if (end > text.length()) {
                        end = text.length();
                    }
                    String subStr = text.substring(start, end);
                    gC.fillText(subStr, xp1, yp1 + yOffset);
                    start = end;
                    yOffset += font.getSize() + 3;
                } while (start < text.length());
                bounds2D = new BoundingBox(xp1, yp1 - font.getSize(), regionWidth, yOffset);
            } else {
                bounds2D = new BoundingBox(xp1, yp1 - font.getSize(), width, font.getSize());
                gC.fillText(text, xp1, yp1);
            }
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
    }

    @Override
    public POSTYPE getXPosType() {
        return xPosType;
    }

    @Override
    public POSTYPE getYPosType() {
        return yPosType;
    }

}
