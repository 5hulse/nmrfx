package org.nmrfx.processor.gui.annotations;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import org.nmrfx.graphicsio.GraphicsContextInterface;
import org.nmrfx.processor.gui.spectra.mousehandlers.AnnotationMouseHandlerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

public class AnnoRectangle extends AnnoShape {
    private static final Logger log = LoggerFactory.getLogger(AnnoRectangle.class);

    double x1;
    double y1;
    double x2;
    double y2;
    double xp1;
    double yp1;
    double xp2;
    double yp2;
    double width;
    double height;
    int activeHandle = -1;
    double startX1;
    double startY1;
    double startX2;
    double startY2;

    public AnnoRectangle(){

    }
    public AnnoRectangle(double x1, double y1, double x2, double y2,
                         POSTYPE xPosType, POSTYPE yPosType) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.xPosType = xPosType;
        this.yPosType = yPosType;
    }

    public double getX1() {
        return x1;
    }

    public void setX1(double x1) {
        this.x1 = x1;
    }

    public double getY1() {
        return y1;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public double getX2() {
        return x2;
    }

    public void setX2(double x2) {
        this.x2 = x2;
    }

    public double getY2() {
        return y2;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    @Override
    public boolean hit(double x, double y, boolean selectMode) {
        double width = xp2-xp1;
        double height = yp2-yp1;
        Rectangle2D bounds2D = new Rectangle2D(xp1, yp1, width, height);
        boolean hit = false;
        if (bounds2D.contains(x, y)) {
            hit = true;
        }
        if (hit) {
            startX1 = x1;
            startX2 = x2;
            startY1 = y1;
            startY2 = y2;
        }
        if (selectMode && selectable) {
            selected = hit;
        }

        return hit;
    }

    @Override
    public void draw(GraphicsContextInterface gC, double[][] bounds, double[][] world) {
        try {
            gC.setLineWidth(lineWidth);
            xp1 = xPosType.transform(x1, bounds[0], world[0]);
            yp1 = yPosType.transform(y1, bounds[1], world[1]);
            xp2 = xPosType.transform(x2, bounds[0], world[0]);
            yp2 = yPosType.transform(y2, bounds[1], world[1]);
            width = Math.abs(xp2 - xp1);
            height = Math.abs(yp2 - yp1);
            if (xp1 > xp2) {
                double hold = xp1;
                xp1 = xp2;
                xp2 = hold;
            }
            if (yp1 > yp2) {
                double hold = yp1;
                yp1 = yp2;
                yp2 = hold;
            }
            if (stroke != null) {
                gC.setStroke(stroke);
                gC.strokeRect(xp1, yp1, width, height);
            }
            if (fill != null) {
                gC.setFill(fill);
                gC.fillRect(xp1, yp1, width, height);
            }
            if (isSelected()) {
                drawHandles(gC);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void drawHandles(GraphicsContextInterface gC) {
        drawHandle(gC, xp1, yp1, Pos.CENTER);
        drawHandle(gC, xp1, yp2, Pos.CENTER);
        drawHandle(gC, xp2, yp1, Pos.CENTER);
        drawHandle(gC, xp2, yp2, Pos.CENTER);
    }

    @Override
    public int hitHandle(double x, double y) {
        if (hitHandle(x, y, Pos.CENTER, xp1, yp1)) {
            activeHandle = 0;
        } else if (hitHandle(x, y, Pos.CENTER, xp1, yp2)) {
            activeHandle = 1;
        } else if (hitHandle(x, y, Pos.CENTER, xp2, yp2)) {
            activeHandle = 2;
        } else if (hitHandle(x, y, Pos.CENTER, xp2, yp1)) {
            activeHandle = 3;
        } else {
            activeHandle = -1;
        }
        return activeHandle;
    }

    public void move(double[][] bounds, double[][] world, double[] start, double[] pos) {
        double dx = pos[0] - start[0];
        double dy = pos[1] - start[1];
        if (activeHandle < 0) {
            x1 = xPosType.move(startX1, dx, bounds[0], world[0]);
            x2 = xPosType.move(startX2, dx, bounds[0], world[0]);
            y1 = yPosType.move(startY1, dy, bounds[1], world[1]);
            y2 = yPosType.move(startY2, dy, bounds[1], world[1]);
        } else if (activeHandle == 0) {
            x1 = xPosType.move(startX1, dx, bounds[0], world[0]);
            y1 = yPosType.move(startY1, dy, bounds[1], world[1]);
        } else if (activeHandle == 1) {
            x1 = xPosType.move(startX1, dx, bounds[0], world[0]);
            y2 = yPosType.move(startY2, dy, bounds[1], world[1]);
        } else if (activeHandle == 2) {
            x2 = xPosType.move(startX2, dx, bounds[0], world[0]);
            y2 = yPosType.move(startY2, dy, bounds[1], world[1]);
        } else if (activeHandle == 3) {
            x2 = xPosType.move(startX2, dx, bounds[0], world[0]);
            y1 = yPosType.move(startY1, dy, bounds[1], world[1]);
        }

    }

}
