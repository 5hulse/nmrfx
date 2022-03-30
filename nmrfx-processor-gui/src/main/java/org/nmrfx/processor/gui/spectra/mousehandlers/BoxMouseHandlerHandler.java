package org.nmrfx.processor.gui.spectra.mousehandlers;

import javafx.scene.input.MouseEvent;

import java.util.Optional;

public class BoxMouseHandlerHandler extends MouseHandler {
    MouseBindings.MOUSE_ACTION mouseAction;

    public BoxMouseHandlerHandler(MouseBindings mouseBindings) {
        super(mouseBindings);
    }

    public static Optional<BoxMouseHandlerHandler> handler(MouseBindings mouseBindings) {
        BoxMouseHandlerHandler handler = new BoxMouseHandlerHandler(mouseBindings);
        return Optional.of(handler);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (mouseEvent.isAltDown() || mouseEvent.isControlDown()) {
            mouseAction = MouseBindings.MOUSE_ACTION.DRAG_ADDREGION;
        } else if (mouseBindings.getMouseEvent().isShiftDown()) {
            mouseAction = MouseBindings.MOUSE_ACTION.DRAG_SELECTION;
        } else {
            mouseAction = MouseBindings.MOUSE_ACTION.DRAG_EXPAND;
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        mouseBindings.getChart().finishBox(mouseAction, mouseBindings.getDragStart(), mouseBindings.getMouseX(), mouseBindings.getMouseY());
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        mouseBindings.getChart().dragBox(mouseAction, mouseBindings.getDragStart(), mouseBindings.getMouseX(), mouseBindings.getMouseY());
    }
}
