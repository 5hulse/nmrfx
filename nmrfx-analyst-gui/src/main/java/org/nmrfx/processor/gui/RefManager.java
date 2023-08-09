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
package org.nmrfx.processor.gui;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.controlsfx.control.textfield.CustomTextField;
import org.nmrfx.processor.datasets.DatasetType;
import org.nmrfx.processor.datasets.vendor.NMRData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * @author brucejohnson
 */
public class RefManager {

    private static final Logger log = LoggerFactory.getLogger(RefManager.class);
    TitledPane referencePane;
    ProcessorController processorController;
    Map<DataProps, ToggleButton> toggleButtons = new HashMap<>();
    Map<String, SimpleObjectProperty> objectPropertyMap = new HashMap<>();
    VendorParsGUI vendorParsGUI = new VendorParsGUI();


    public static class PositiveIntegerFilter implements UnaryOperator<TextFormatter.Change> {

        @Override
        public TextFormatter.Change apply(TextFormatter.Change change) {
            if (change.getControlNewText().matches("[0-9]*")) {
                return change;
            }
            return null;
        }
    }

    public static class PositiveIntegerStringConverter extends IntegerStringConverter {

        @Override
        public Integer fromString(String value) {
            int result = super.fromString(value);
            if (result < 0) {
                throw new RuntimeException("Negative number");
            }
            return result;
        }

        @Override
        public String toString(Integer value) {
            if (value < 0) {
                return "0";
            }
            return super.toString(value);
        }

    }

    public static class FixedDecimalFilter implements UnaryOperator<TextFormatter.Change> {

        @Override
        public TextFormatter.Change apply(TextFormatter.Change change) {
            if (change.getControlNewText().matches("-?([0-9]*)?(\\.[0-9]*)?")) {
                return change;
            }
            return null;
        }
    }

    public class FixedDecimalConverter extends DoubleStringConverter {

        private final int decimalPlaces;

        public FixedDecimalConverter(int decimalPlaces) {
            this.decimalPlaces = decimalPlaces;
        }

        @Override
        public String toString(Double value) {
            return String.format("%." + decimalPlaces + "f", value);
        }

        @Override
        public Double fromString(String valueString) {
            if (valueString.isEmpty()) {
                return 0d;
            }
            return super.fromString(valueString);
        }
    }

    enum DataProps {
        LABEL("Label", false) {
            String getDataValue(NMRData nmrData, int iDim) {
                return nmrData.getLabelNames()[iDim];
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim) {
                ((SimpleObjectProperty<String>) objectPropertyMap.get(name() + iDim)).set(nmrData.getLabelNames()[iDim]);
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim) {
                ((SimpleObjectProperty<String>) objectPropertyMap.get(name() + iDim)).set(value);
            }

            SimpleObjectProperty<String> getObjectProperty() {
                return new SimpleObjectProperty<>("");
            }
        },
        TDSIZE("TDSize", true) {
            String getDataValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSize(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim) {
                ((SimpleObjectProperty<Integer>) objectPropertyMap.get(name() + iDim)).set(nmrData.getSize(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim) {
                ((SimpleObjectProperty<Integer>) objectPropertyMap.get(name() + iDim)).set(Integer.parseInt(value));
            }

            SimpleObjectProperty<Integer> getObjectProperty() {
                return new SimpleObjectProperty<>(0);
            }
        },
        ACQSIZE("AcqSize", true) {
            String getDataValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSize(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim) {
                ((SimpleObjectProperty<Integer>) objectPropertyMap.get(name() + iDim)).set(nmrData.getSize(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim) {
                ((SimpleObjectProperty<Integer>) objectPropertyMap.get(name() + iDim)).set(Integer.parseInt(value));
            }

            SimpleObjectProperty<Integer> getObjectProperty() {
                return new SimpleObjectProperty<>(0);
            }
        },
        SF("Frequency (MHz)", true) {
            String getDataValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSF(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim) {
                ((SimpleObjectProperty<Double>) objectPropertyMap.get(name() + iDim)).set(nmrData.getSF(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim) {
                ((SimpleObjectProperty<Double>) objectPropertyMap.get(name() + iDim)).set(Double.parseDouble(value));
            }

            SimpleObjectProperty<Double> getObjectProperty() {
                return new SimpleObjectProperty<>(0.0);
            }
        },
        SW("Sweep Width", true) {
            String getDataValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSW(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim) {
                ((SimpleObjectProperty<Double>) objectPropertyMap.get(name() + iDim)).set(nmrData.getSW(iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim) {
                ((SimpleObjectProperty<Double>) objectPropertyMap.get(name() + iDim)).set(Double.parseDouble(value));
            }

            SimpleObjectProperty<Double> getObjectProperty() {
                return new SimpleObjectProperty<>(0.0);
            }
        },
        REF("Reference", false) {
            String getDataValue(NMRData nmrData, int iDim) {
                String value;
                if (iDim == 0) {
                    value = "";
                } else {
                    String tn = nmrData.getTN(iDim);
                    value = "";
                    for (int i = 0; i < tn.length(); i++) {
                        if (Character.isLetter(tn.charAt(i))) {
                            value = String.valueOf(tn.charAt(i));
                        }
                    }
                }
                return value;
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim) {
                ((SimpleObjectProperty<String>) objectPropertyMap.get(name() + iDim)).set(getDataValue(nmrData, iDim));
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim) {
                ((SimpleObjectProperty<String>) objectPropertyMap.get(name() + iDim)).set(value);
            }

            SimpleObjectProperty<String> getObjectProperty() {
                return new SimpleObjectProperty<>("");
            }
        },
        SKIP("Skip", true) {
            String getDataValue(NMRData nmrData, int iDim) {
                return "0";
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim) {
                ((SimpleObjectProperty<Boolean>) objectPropertyMap.get(name() + iDim)).set(false);
            }

            void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim) {
                ((SimpleObjectProperty<Boolean>) objectPropertyMap.get(name() + iDim)).set(Boolean.parseBoolean(value.toLowerCase()));
            }

            SimpleObjectProperty<Boolean> getObjectProperty() {
                return new SimpleObjectProperty<>(false);
            }
        };
        final String title;
        final boolean locked;

        DataProps(String title, boolean locked) {
            this.title = title;
            this.locked = locked;
        }

        abstract String getDataValue(NMRData nmrData, int iDim);

        abstract void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, NMRData nmrData, int iDim);

        abstract void setObjectValue(Map<String, SimpleObjectProperty> objectPropertyMap, String value, int iDim);

        abstract SimpleObjectProperty getObjectProperty();

        String getString(Map<String, SimpleObjectProperty> objectPropertyMap, int iDim) {
            SimpleObjectProperty field = objectPropertyMap.get(name() + iDim);
            Object value = field.getValue();
            return value == null ? "" : value.toString();
        }
    }

    static public final String[] propNames = {"skip", "label", "acqarray", "acqsize", "tdsize", "sf", "sw", "ref"};
    static final String[] bSFNames = {"SFO1,1", "SFO1,2", "SFO1,3", "SFO1,4", "SFO1,5"};
    static final String[] bSWNames = {"SW_h,1", "SW_h,2", "SW_h,3", "SW_h,4", "SW_h,5"};

    RefManager(ProcessorController processorController, TitledPane referencePane) {
        this.processorController = processorController;
        this.referencePane = referencePane;
    }

    public String getPythonString(DataProps dataProps, int nDim, String indent) {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(indent);
        sBuilder.append(dataProps.name().toLowerCase(Locale.ROOT));
        sBuilder.append("(");

        for (int dim = 0; dim < nDim; dim++) {
            if (dim > 0) {
                sBuilder.append(",");
            }
            String value = getCurrentValue(dataProps, dim);
            boolean useString = true;
            // Ending with F or D allows a string to be parsed as a number
            if ((value.length() > 0) && !Character.isLetter(value.charAt(value.length() - 1))) {
                try {
                    Double.parseDouble(value);
                    useString = false;
                } catch (NumberFormatException nFE) {
                    useString = true;

                }
            }
            if ((value.equals("False") || value.equals("True"))) {
                useString = false;
            }
            if (dataProps == DataProps.LABEL) {
                useString = true;
            }
            if (useString) {
                sBuilder.append("'");
                sBuilder.append(value);
                sBuilder.append("'");
            } else {
                sBuilder.append(value);
            }
        }
        sBuilder.append(")");
        return sBuilder.toString();
    }

    public String getCurrentValue(DataProps dataProps, int iDim) {
        return dataProps.getString(objectPropertyMap, iDim);
    }

    private void updateEditable(ToggleButton toggleButton, DataProps dataProp, NMRData nmrData, int nDim) {
        if (toggleButton.isSelected()) {
            for (int iDim = 0; iDim < nDim; iDim++) {
                dataProp.setObjectValue(objectPropertyMap, nmrData, iDim);
            }
        }
    }

    private void invalidateScript() {
        processorController.chartProcessor.setScriptValid(false);
        refresh();
    }

    private ComboBox<String> setupAcqOrder(NMRData nmrData) {
        ComboBox<String> choiceBox = new ComboBox();
        if (nmrData != null) {
            ArrayList<String> choices = new ArrayList<>();
            if (nmrData.getNDim() > 1) {
                ArrayList<Integer> dimList = new ArrayList<>();
                for (int i = 1; i <= (nmrData.getNDim() - 1); i++) {
                    dimList.add(i);
                }
                PermutationIterator permIter = new PermutationIterator(dimList);
                StringBuilder sBuilder = new StringBuilder();
                while (permIter.hasNext()) {
                    ArrayList<Integer> permDims = (ArrayList<Integer>) permIter.next();
                    sBuilder.setLength(0);
                    if (nmrData.getVendor().equals("bruker") || nmrData.getVendor().equals("rs2d")) {
                        sBuilder.append(nmrData.getNDim());
                    }
                    for (Integer iVal : permDims) {
                        sBuilder.append(iVal);
                    }
                    choices.add(sBuilder.toString());
                }
                choiceBox.getItems().addAll(choices);
                choiceBox.setEditable(true);
                choiceBox.setValue(processorController.chartProcessor.getAcqOrder());
                choiceBox.valueProperty().addListener(e -> invalidateScript());
            }
        }
        return choiceBox;
    }


    private void refresh() {
        ChartProcessor chartProcessor = processorController.chartProcessor;
        if (processorController.isViewingDataset()) {
            if (processorController.autoProcess.isSelected()) {
                processorController.processIfIdle();
            }
        } else {
            chartProcessor.execScriptList(true);
            chartProcessor.getChart().layoutPlotChildren();
        }
    }

    private void updateDatasetChoice(DatasetType dataType) {
        ChartProcessor chartProcessor = processorController.chartProcessor;
        if (dataType != chartProcessor.getDatasetType()) {
            processorController.unsetDatasetName();
        }
        chartProcessor.setDatasetType(dataType);
        processorController.updateFileButton();
    }

    HBox getParDisplay(NMRData nmrData, String field) {
        HBox hBox = new HBox();
        Label label = new Label(field);
        label.setPrefWidth(100);
        label.setAlignment(Pos.CENTER_LEFT);
        hBox.setSpacing(10);
        TextField textField = new TextField();
        textField.setPrefWidth(200);
        textField.setEditable(false);

        switch (field) {
            case "Solvent" -> textField.setText(nmrData.getSolvent());
            case "Sequence" -> textField.setText(nmrData.getSequence());
            case "Temperature" -> textField.setText(String.valueOf(nmrData.getTempK()));
            case "Date" -> textField.setText(nmrData.getZonedDate().toString());
        }
        hBox.getChildren().addAll(label, textField);
        return hBox;
    }

    public void updateReferencePane(NMRData nmrData, int nDim) {
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        String[] infoFields = {"Sequence", "Solvent", "Temperature", "Date"};
        for (String infoField: infoFields) {
            vBox.getChildren().add(getParDisplay(nmrData, infoField));
        }

        Label dataTypeLabel = new Label("Output Type");
        dataTypeLabel.setPrefWidth(100);
        dataTypeLabel.setAlignment(Pos.CENTER_LEFT);
        HBox datatypeBox = new HBox();
        datatypeBox.setSpacing(10);

        datatypeBox.setAlignment(Pos.CENTER_LEFT);
        ChoiceBox<DatasetType> dataChoice = new ChoiceBox<>();
        dataChoice.getItems().addAll(DatasetType.values());
        datatypeBox.getChildren().addAll(dataTypeLabel, dataChoice, processorController.getDatasetFileButton());
        dataChoice.setValue(processorController.chartProcessor.getDatasetType());
        dataChoice.setOnAction(e -> updateDatasetChoice(dataChoice.getValue()));

        Label acqOrderLabel = new Label("Acq. Order");
        acqOrderLabel.setPrefWidth(100);
        acqOrderLabel.setAlignment(Pos.CENTER_LEFT);
        HBox acqOrderBox = new HBox();
        acqOrderBox.setSpacing(10);
        acqOrderBox.setAlignment(Pos.CENTER_LEFT);
        acqOrderBox.getChildren().addAll(acqOrderLabel, setupAcqOrder(nmrData));


        vBox.getChildren().addAll(datatypeBox, acqOrderBox);

        if ((nmrData != null) && nmrData.getVendor().equals("bruker")) {
            CheckBox checkBox = new CheckBox("Fix DSP");
            checkBox.setSelected(processorController.chartProcessor.getFixDSP());
            vBox.getChildren().add(checkBox);
            checkBox.setOnAction(e -> {
                processorController.chartProcessor.setFixDSP(checkBox.isSelected());
                invalidateScript();
            });
        }

        ScrollPane scrollPane = new ScrollPane();
        GridPane gridPane = new GridPane();
        scrollPane.setContent(gridPane);
        vBox.getChildren().add(scrollPane);
        referencePane.setContent(vBox);
        int start = 2;
        for (int i = 0; i < nDim; i++) {
            Label label = new Label("Dim: " + String.valueOf(i + 1));
            gridPane.add(label, i + start, 0);
        }
        int row = 1;
        for (DataProps dataProp : DataProps.values()) {
            Label label = new Label(dataProp.title);
            Insets insets = new Insets(5, 5, 5, 10);
            label.setPadding(insets);
            gridPane.add(label, 1, row);
            ToggleButton toggleButton = GlyphsDude.createIconToggleButton(FontAwesomeIcon.LOCK, "", "12", ContentDisplay.GRAPHIC_ONLY);
            gridPane.add(toggleButton, 0, row);
            toggleButton.setSelected(dataProp.locked);
            toggleButton.setOnAction(e -> updateEditable(toggleButton, dataProp, nmrData, nDim));
            toggleButtons.put(dataProp, toggleButton);
            for (int i = 0; i < nDim; i++) {
                var prop = dataProp.getObjectProperty();
                if (dataProp == DataProps.SKIP) {
                    if (i > 0) {
                        CheckBox checkBox = new CheckBox();
                        checkBox.disableProperty().bind(toggleButton.selectedProperty());
                        checkBox.setOnAction(e -> invalidateScript());
                        objectPropertyMap.put(dataProp.name() + i, prop);
                        gridPane.add(checkBox, i + start, row);
                    }
                } else {
                    if (dataProp == DataProps.REF) {
                        ReferenceMenuTextField referenceMenuTextField = new ReferenceMenuTextField(processorController);
                        referenceMenuTextField.setPrefWidth(100);
                        referenceMenuTextField.setText(dataProp.getDataValue(nmrData, i));
                        referenceMenuTextField.getTextField().textProperty().addListener(e -> invalidateScript());
                        referenceMenuTextField.getTextField().textProperty().bindBidirectional(prop);
                        gridPane.add(referenceMenuTextField, i + start, row);
                        objectPropertyMap.put(dataProp.name() + i, prop);
                    } else {
                        CustomTextField textField = new CustomTextField();
                        textField.setPrefWidth(100);
                        textField.editableProperty().bind(toggleButton.selectedProperty().not());
                        textField.setOnKeyPressed(e -> invalidateScript());
                        gridPane.add(textField, i + start, row);
                        objectPropertyMap.put(dataProp.name() + i, prop);
                        if (prop.get() instanceof Double dValue) {
                            prop.set(Double.parseDouble(dataProp.getDataValue(nmrData, i)));
                            TextFormatter<Double> textFormatter = new TextFormatter<>(new FixedDecimalConverter(2), 0.0, new FixedDecimalFilter());
                            textFormatter.valueProperty().bindBidirectional(prop);
                            textField.setTextFormatter(textFormatter);
                        } else if (prop.get() instanceof Integer dValue) {
                            prop.set(Integer.parseInt(dataProp.getDataValue(nmrData, i)));
                            TextFormatter<Integer> textFormatter = new TextFormatter<>(new PositiveIntegerStringConverter(), 0, new PositiveIntegerFilter());
                            textFormatter.valueProperty().bindBidirectional(prop);
                            textField.setTextFormatter(textFormatter);
                        } else {
                            prop.set(dataProp.getDataValue(nmrData, i));
                            TextFormatter<String> textFormatter = new TextFormatter<>(TextFormatter.IDENTITY_STRING_CONVERTER);
                            textFormatter.valueProperty().bindBidirectional(prop);
                            textField.setTextFormatter(textFormatter);
                        }
                    }
                }
            }
            row++;
        }
        Button button = new Button("Vendor Pars...");
        vBox.getChildren().add(button);
        button.setOnAction(e -> showVendorPars(nmrData));
    }

    private void showVendorPars(NMRData nmrData) {
        vendorParsGUI.showStage();
        vendorParsGUI.updateParTable(nmrData);
    }


    public boolean getSkip(String iDim) {
        SimpleObjectProperty objectProp = objectPropertyMap.get(DataProps.SKIP.name() + iDim);
        return (objectProp == null) || (objectProp.get() == null) ? false : ((Boolean) objectProp.get()).booleanValue();
    }

    public String getScriptReferenceLines(int nDim, String indent) {
        ChartProcessor chartProcessor = processorController.chartProcessor;
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(indent);
        sBuilder.append("acqOrder(");
        sBuilder.append(chartProcessor.getAcqOrder(true));
        sBuilder.append(")");
        sBuilder.append(System.lineSeparator());
        sBuilder.append(indent);
        sBuilder.append("acqarray(");
        sBuilder.append(chartProcessor.getArraySizes());
        sBuilder.append(")");
        sBuilder.append(System.lineSeparator());
        sBuilder.append(indent);
        sBuilder.append("fixdsp(");
        sBuilder.append(chartProcessor.getFixDSP() ? "True" : "False");
        sBuilder.append(")");
        sBuilder.append(System.lineSeparator());
        for (DataProps dataProps : DataProps.values()) {
            if (!toggleButtons.get(dataProps).isSelected()) {
                sBuilder.append(getPythonString(dataProps, nDim, indent));
                sBuilder.append(System.lineSeparator());
            }
        }
        return sBuilder.toString();
    }

    Optional<String> getSkipString() {
        Optional<String> result = Optional.empty();
        NMRData nmrData = getNMRData();
        if (nmrData != null) {
            Optional<String> skipString = processorController.navigatorGUI.getSkipString();
            if (skipString.isPresent()) {
                String s = "markrows(" + skipString.get() + ")";
                result = Optional.of(s);
            }
        }
        return result;

    }


    public void setDataFields(List<String> headerList) {
        ChartProcessor chartProcessor = processorController.chartProcessor;
        for (String s : headerList) {
            int index = s.indexOf('(');
            boolean lastIsClosePar = s.charAt(s.length() - 1) == ')';
            if ((index != -1) && lastIsClosePar) {
                String propName = s.substring(0, index).toUpperCase();
                String args = s.substring(index + 1, s.length() - 1);
                switch (propName) {
                    case "ACQORDER" -> chartProcessor.setAcqOrder(args);
                    case "ACQARRAY" -> chartProcessor.setArraySize(args);
                    case "FIXDSP" -> chartProcessor.setFixDSP(args.equals("True"));
                    default -> {
                        DataProps dataProps = DataProps.valueOf(propName);
                        List<String> parValues = CSVLineParse.parseLine(args);
                        int dim = 0;
                        for (String parValue : parValues) {
                            dataProps.setObjectValue(objectPropertyMap, parValue, dim);
                            dim++;
                        }
                    }
                }
            }
        }
    }

    NMRData getNMRData() {
        return processorController.chartProcessor.getNMRData();
    }
}
