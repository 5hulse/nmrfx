/*
 * NMRFx Processor : A Program for Processing NMR Data
 * Copyright (C) 2004-2017 One Moon Scientific, Inc., Westfield, N.J., USA
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.utils.properties;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

/**
 * @author brucejohnson
 */
//TODO add annotations once core and utils are merged
// @PluginAPI("ring")
public class NvFxPropertyEditorFactory extends DefaultPropertyEditorFactory {

    private static final Logger log = LoggerFactory.getLogger(NvFxPropertyEditorFactory.class);
    private static Class<?>[] numericTypes = new Class[]{
            byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            BigInteger.class, BigDecimal.class
    };

    public NvFxPropertyEditorFactory() {
        super();
    }

    @Override
    public PropertyEditor<?> call(Item item) {
        Class<?> type = item.getType();

        //TODO: add support for char and collection editors
        if (type == DoubleRangeOperationItem.class) {
            Slider slider = new Slider();
            DoubleRangeOperationItem dItem = (DoubleRangeOperationItem) item;
            slider.setMin(dItem.getMin());
            slider.setMax(dItem.getMax());
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setBlockIncrement((dItem.getMax() - dItem.getMin()) / 100.0);
            slider.setMajorTickUnit((dItem.getMax() - dItem.getMin()) / 4);
            ZoomSlider zoomSlider = new ZoomSlider(slider, dItem.getAmin(), dItem.getAmax(), dItem.getZoomable());
            return new PropertySliderEditor(dItem, zoomSlider);
        } else if (type == IntOperationItem.class) {
            IntOperationItem iItem = (IntOperationItem) item;
            PropertyEditor propEditor = createIntegerEditor(item);
            CustomIntegerTextField textField = (CustomIntegerTextField) propEditor.getEditor();
            textField.setMin(iItem.getMin());
            textField.setMax(iItem.getMax());
            return propEditor;
        } else if (type == FileOperationItem.class) {
            return createFileEditor(item, false);
        } else if (type == DirectoryOperationItem.class) {
            return createFileEditor(item, true);
        } else if (type == TextOperationItem.class) {
            return Editors.createTextEditor(item);
        } else if (type == TextWaitingOperationItem.class) {
            TextWaitingOperationItem tItem = (TextWaitingOperationItem) item;
            PropertyEditor editor = Editors.createTextEditor(item);
            TextField textField = (TextField) editor.getEditor();
            textField.setOnKeyReleased(e -> tItem.keyReleased(textField, e));
            return editor;
        } else if (type == MenuTextOperationItem.class) {
            MenuTextOperationItem tItem = (MenuTextOperationItem) item;
            MenuTextField menuTextField;
            menuTextField = new MenuTextField();
            return new MenuTextFieldEditor(tItem, menuTextField);
        } else if (type == IntRangeOperationItem.class) {
            Slider slider = new Slider();
            IntRangeOperationItem iItem = (IntRangeOperationItem) item;
            slider.setMin(iItem.getMin());
            slider.setMax(iItem.getMax());
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(false);
            slider.setBlockIncrement(1);
            int delta = iItem.getMax() - iItem.getMin();
            if (delta < 6) {
                slider.setMajorTickUnit(1);
                slider.setMinorTickCount(0);
            } else {
                slider.setMajorTickUnit(delta / 4);
                slider.setMinorTickCount(delta / 4 - 1);
            }
            slider.setSnapToTicks(true);
            IntSlider zoomSlider = new IntSlider(slider, iItem.getMin(), iItem.getMax());
            return new IntPropertySliderEditor(iItem, zoomSlider);

        } else if (type == CheckComboOperationItem.class) {
            CheckComboBox comboBox = new CheckComboBox();
            CheckComboOperationItem cItem = (CheckComboOperationItem) item;
            Collection<String> values = (Collection<String>) cItem.getChoices();
            comboBox.getItems().addAll(values);
            comboBox.getCheckModel().check(cItem.defaultValue);
            return new CheckComboEditor(cItem, comboBox);

        } else if (type == BooleanOperationItem.class) {
            BooleanOperationItem bItem = (BooleanOperationItem) item;
            return Editors.createCheckEditor(bItem);
        } else if (type == EditableChoiceOperationItem.class) {
            EditableChoiceOperationItem cItem = (EditableChoiceOperationItem) item;
            PropertyEditor editor = Editors.createChoiceEditor(item, cItem.getChoices());
            ComboBox comboBox = (ComboBox) editor.getEditor();
            comboBox.setEditable(true);
            return editor;
        } else if (type == ChoiceOperationItem.class) {
            ChoiceOperationItem cItem = (ChoiceOperationItem) item;
            return Editors.createChoiceEditor(item, cItem.getChoices());
        } else if (type == IntChoiceOperationItem.class) {
            IntChoiceOperationItem cItem = (IntChoiceOperationItem) item;
            return Editors.createChoiceEditor(item, cItem.getChoices());
        } else if (type == ListOperationItem.class) {

            ListOperationItem loi = (ListOperationItem) item;
            return Editors.createTextEditor(loi);
        }
        return super.call(item);
    }

    // there should be better ways to do this
    private static boolean isNumber(Class<?> type) {
        if (type == null) {
            return false;
        }
        for (Class<?> cls : numericTypes) {
            if (type == cls) {
                return true;
            }
        }
        return false;
    }

    public static final PropertyEditor<?> createNumericEditor(Item property) {

        return new AbstractPropertyEditor<Number, CustomNumberTextField>(property, new CustomNumberTextField()) {

            private Class<? extends Number> sourceClass = (Class<? extends Number>) property.getType(); //Double.class;

            @Override
            protected ObservableValue<Number> getObservableValue() {
                return getEditor().numberProperty();
            }

            @Override
            public Number getValue() {
                try {
                    return sourceClass.getConstructor(String.class).newInstance(getEditor().getText());
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    log.warn(e.getMessage(), e);
                    return null;
                }
            }

            @Override
            public void setValue(Number value) {
                sourceClass = (Class<? extends Number>) value.getClass();
                getEditor().setText(value.toString());
            }

        };
    }

    public static final PropertyEditor<?> createIntegerEditor(Item property) {

        return new AbstractPropertyEditor<Number, CustomIntegerTextField>(property, new CustomIntegerTextField()) {
            @Override
            protected ObservableValue<Number> getObservableValue() {
                return getEditor().numberProperty();
            }

            @Override
            public Number getValue() {
                try {
                    return Integer.parseInt(getEditor().getText());
                } catch (NumberFormatException nfE) {
                    return null;
                }
            }

            @Override
            public void setValue(Number value) {
                getEditor().setText(value.toString());
            }
        };
    }

    public static final PropertyEditor<?> createFileEditor(Item property, final boolean directoryMode) {
        CustomTextField textField = new CustomTextField();
        Button button = GlyphsDude.createIconButton(FontAwesomeIcon.FOLDER_OPEN);
        textField.setRight(button);
        AbstractPropertyEditor<String, CustomTextField> editor = new AbstractPropertyEditor<String, CustomTextField>(property, textField) {
            @Override
            protected ObservableValue<String> getObservableValue() {
                return getEditor().textProperty();
            }

            @Override
            public String getValue() {
                return getEditor().getText();
            }

            @Override
            public void setValue(String value) {
                getEditor().setText(value);
            }
        };
        button.setOnAction(e -> {
            File file = getFile(e, directoryMode);
            if (file != null) {
                String path = file.getPath();
                editor.setValue(path);
            }

        });
        return editor;
    }

    static File getFile(ActionEvent e, boolean directoryMode) {
        File file;
        if (directoryMode) {
            DirectoryChooser dirChooser = new DirectoryChooser();
            file = dirChooser.showDialog(null);
        } else {
            FileChooser fileChooser = new FileChooser();
            file = fileChooser.showOpenDialog(null);
        }
        return file;
    }
}
