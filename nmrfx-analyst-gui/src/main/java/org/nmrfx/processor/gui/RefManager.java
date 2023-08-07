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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.textfield.CustomTextField;
import org.nmrfx.processor.datasets.DatasetType;
import org.nmrfx.processor.datasets.vendor.NMRData;
import org.nmrfx.utils.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author brucejohnson
 */
public class RefManager {

    private static final Logger log = LoggerFactory.getLogger(RefManager.class);
    ChangeListener<Number> doubleListener;
    ChangeListener<Number> intListener;
    ChangeListener<String> stringListener;
    ChangeListener<Boolean> boolListener;
    ChangeListener<String> complexListener;
    ChangeListener<String> listListener;
    PropertySheet refSheet;
    TitledPane referencePane;
    ProcessorController processorController;
    Map<String, Object> refMap = new HashMap<>();
    Map<String, Object> controlsMap = new HashMap<>();
    Map<DataProps, ToggleButton> toggleButtons = new HashMap<>();

    enum DataProps {
        LABEL("Label", false) {
            String getValue(NMRData nmrData, int iDim) {
                return nmrData.getLabelNames()[iDim];
            }
        },
        TDSIZE("TDSize", true) {
            String getValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSize(iDim));
            }
        },
        ACQSIZE("AcqSize", true) {
            String getValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSize(iDim));
            }
        },
        SF("Frequency (MHz)", true) {
            String getValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSF(iDim));
            }
        },
        SW("Sweep Width", true) {
            String getValue(NMRData nmrData, int iDim) {
                return String.valueOf(nmrData.getSW(iDim));
            }
        },
        REF("Reference", false) {
            String getValue(NMRData nmrData, int iDim) {
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
        },
        SKIP("Skip", true) {
            String getValue(NMRData nmrData, int iDim) {
                return "0";
            }
        };
        String title;
        boolean locked;

        DataProps(String title, boolean locked) {
            this.title = title;
            this.locked = locked;
        }

        abstract String getValue(NMRData nmrData, int iDim);

        String getString(Map<String, Object> textFieldMap, int iDim) {
            Object field = textFieldMap.get(name() + iDim);
            if (field instanceof TextField textField) {
                return textField.getText().trim();
            } else if (field instanceof ReferenceMenuTextField referenceMenuTextField) {
                return referenceMenuTextField.getTextField().getText();
            } else if (field instanceof CheckBox checkBox) {
                return checkBox.isSelected() ? "True" : "False";
            } else {
                return "";
            }
        }
    }

    static public final String[] propNames = {"skip", "label", "acqarray", "acqsize", "tdsize", "sf", "sw", "ref"};
    static final String[] bSFNames = {"SFO1,1", "SFO1,2", "SFO1,3", "SFO1,4", "SFO1,5"};
    static final String[] bSWNames = {"SW_h,1", "SW_h,2", "SW_h,3", "SW_h,4", "SW_h,5"};

    RefManager(ProcessorController processorController, PropertySheet refSheet, TitledPane referencePane) {
        this.processorController = processorController;
        this.refSheet = refSheet;
        this.referencePane = referencePane;
        doubleListener = (ObservableValue<? extends Number> observableValue, Number number, Number number2)
                -> updateOp((PropertySheet.Item) observableValue);
        intListener = (ObservableValue<? extends Number> observableValue, Number number, Number number2)
                -> updateOp((PropertySheet.Item) observableValue);
        boolListener = (ObservableValue<? extends Boolean> observableValue, Boolean bool, Boolean bool2)
                -> updateOp((PropertySheet.Item) observableValue);
        stringListener = (ObservableValue<? extends String> observableValue, String string, String string2)
                -> updateOp((PropertySheet.Item) observableValue);
        complexListener = (ObservableValue<? extends String> observableValue, String c1, String c2)
                -> updateOp((PropertySheet.Item) observableValue);
        listListener = (ObservableValue<? extends String> observableValue, String c1, String c2)
                -> updateOp((PropertySheet.Item) observableValue);

        refSheet.setPropertyEditorFactory(new NvFxPropertyEditorFactory(processorController));
        refSheet.setMode(PropertySheet.Mode.NAME);
        refSheet.setModeSwitcherVisible(false);
        refSheet.setSearchBoxVisible(false);

    }

    public String getCurrentValues(DataProps dataProps, int nDim, String indent) {
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
        return dataProps.getString(controlsMap, iDim);
    }

    private void updateEditable(ToggleButton toggleButton, DataProps dataProp, NMRData nmrData, int nDim) {
        if (toggleButton.isSelected()) {
            for (int iDim = 0; iDim < nDim; iDim++) {
                Object field = controlsMap.get(dataProp.name() + iDim);
                if (field instanceof TextField textField) {
                    textField.setText(dataProp.getValue(nmrData, iDim));
                } else if (field instanceof ReferenceMenuTextField referenceMenuTextField) {
                    referenceMenuTextField.setText(dataProp.getValue(nmrData, iDim));
                } else if (field instanceof CheckBox checkBox) {
                } else {
                }
            }
        }
    }

    private void setControlValue(Object control, String value) {
        if (control instanceof TextField textField) {
            textField.setText(value);
        } else if (control instanceof ReferenceMenuTextField referenceMenuTextField) {
            referenceMenuTextField.setText(value);
        } else if (control instanceof CheckBox checkBox) {
            checkBox.setSelected(Boolean.parseBoolean(value.toLowerCase()));
        } else {
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

    public void updateReferencePane(NMRData nmrData, int nDim) {
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        Label dataTypeLabel = new Label("Output Type");
        dataTypeLabel.setPrefWidth(100);
        dataTypeLabel.setAlignment(Pos.CENTER_LEFT);
        HBox datatypeBox = new HBox();
        datatypeBox.setAlignment(Pos.CENTER_LEFT);
        ChoiceBox<DatasetType> dataChoice = new ChoiceBox<>();
        dataChoice.getItems().addAll(DatasetType.values());
        datatypeBox.getChildren().addAll(dataTypeLabel, dataChoice);
        dataChoice.setValue(processorController.chartProcessor.getDatasetType());
        dataChoice.setOnAction(e -> updateDatasetChoice(dataChoice.getValue()));

        Label acqOrderLabel = new Label("Acq. Order");
        acqOrderLabel.setPrefWidth(100);
        acqOrderLabel.setAlignment(Pos.CENTER_LEFT);
        HBox acqOrderBox = new HBox();
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
            Label label = new Label(String.valueOf(i + 1));
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
                if (dataProp == DataProps.SKIP) {
                    if (i > 0) {
                        CheckBox checkBox = new CheckBox();
                        gridPane.add(checkBox, i + start, row);
                        controlsMap.put(dataProp.name() + i, checkBox);
                        checkBox.setDisable(dataProp.locked);
                        checkBox.setOnAction(e -> invalidateScript());
                    }
                } else {
                    if (dataProp == DataProps.REF) {
                        ReferenceMenuTextField referenceMenuTextField = new ReferenceMenuTextField(processorController);
                        referenceMenuTextField.setPrefWidth(100);
                        referenceMenuTextField.setText(dataProp.getValue(nmrData, i));
                        referenceMenuTextField.getTextField().textProperty().addListener(e -> invalidateScript());
                        gridPane.add(referenceMenuTextField, i + start, row);
                        controlsMap.put(dataProp.name() + i, referenceMenuTextField);
                    } else {
                        CustomTextField textField = new CustomTextField();
                        textField.setPrefWidth(100);
                        textField.setText(dataProp.getValue(nmrData, i));
                        textField.editableProperty().bind(toggleButton.selectedProperty().not());
                        textField.setOnKeyPressed(e -> invalidateScript());
                        gridPane.add(textField, i + start, row);
                        controlsMap.put(dataProp.name() + i, textField);
                    }
                }
            }
            row++;
        }
    }


    private void updateOp(PropertySheet.Item updateItem) {
        ChartProcessor chartProcessor = processorController.chartProcessor;
        String propName = updateItem.getName();
        String dimName = updateItem.getCategory();
        String nameWithDim = propName + dimName;
        String value = updateItem.getValue().toString();
        if (propName.equals("skip")) {
            if (value.equals("true")) {
                refMap.put(nameWithDim, "1");
            } else {
                refMap.put(nameWithDim, "0");
            }
        } else {
            refMap.put(nameWithDim, value);
        }
        boolean refresh = true;
        switch (propName) {
            case "fixdsp":
                chartProcessor.setFixDSP((Boolean) updateItem.getValue());
                refresh = true;
                break;
            case "dataset":
                break;
            case "datatype":
                String dataType = updateItem.getValue().toString();
                if (DatasetType.valueOf(dataType) != chartProcessor.getDatasetType()) {
                    processorController.unsetDatasetName();
                }
                chartProcessor.setDatasetType(DatasetType.valueOf(dataType));
                processorController.updateFileButton();
                break;
            case "acqOrder":
                String acqOrder = updateItem.getValue().toString();
                chartProcessor.setAcqOrder(acqOrder);
                break;
            case "acqarray":
                String acqArray = updateItem.getValue().toString();
                try {
                    int dim = Integer.parseInt(dimName);
                    dim--;
                    int arraySize = Integer.parseInt(acqArray);
                    chartProcessor.setArraySize(dim, arraySize);
                } catch (NumberFormatException nFE) {
                    System.out.println("set array size error " + nFE.getMessage());
                }
                break;
            default:
                break;
        }
        chartProcessor.setScriptValid(false);
        if (refresh) {
            ProcessorController pController = processorController;
            if (pController.isViewingDataset()) {
                if (pController.autoProcess.isSelected()) {
                    processorController.processIfIdle();
                }
            } else {
                chartProcessor.execScriptList(true);
                chartProcessor.getChart().layoutPlotChildren();
            }
        }

    }

    public String getParString(int nDim, String indent) {
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
                sBuilder.append(getCurrentValues(dataProps, nDim, indent));
                sBuilder.append(System.lineSeparator());
            }
        }
        return sBuilder.toString();
    }

    public String getParStringOld(int nDim, String indent) {
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
        for (String propName : propNames) {
            if (propName.equals("acqarray")) {
                continue;
            }
            sBuilder.append(indent);
            sBuilder.append(propName);
            sBuilder.append("(");
            for (int dim = 0; dim < nDim; dim++) {
                if (dim > 0) {
                    sBuilder.append(",");
                }
                String value = getPropValue(dim, propName, false);
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
                if (propName.equals("label")) {
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
            sBuilder.append(System.lineSeparator());
        }
        getSkipString().ifPresent(skipString -> {
            sBuilder.append(skipString);
            sBuilder.append(System.lineSeparator());
        });
        return sBuilder.toString();
    }

    Optional<String> getSkipString() {
        Optional<String> result = Optional.empty();
        NMRData nmrData = getNMRData();
        if (nmrData != null) {
            Optional<String> skipString = processorController.getSkipString();
            if (skipString.isPresent()) {
                String s = "markrows(" + skipString.get() + ")";
                result = Optional.of(s);
            }
        }
        return result;

    }

    String getPropValue(int dim, String propName, boolean getDefault) {
        String dimName = "" + (dim + 1);
        String nameWithDim = propName + dimName;
        NMRData nmrData = getNMRData();
        if (nmrData == null) {
            return "";
        }
        String value;
        if (!getDefault && refMap.containsKey(nameWithDim)) {
            value = refMap.get(nameWithDim).toString();
        } else {
            switch (propName) {
                case "sw":
                    value = nmrData.getSWNames()[dim];
                    break;
                case "skip":
                    if (nmrData.getSize(dim) > 1) {
                        value = "0";
                    } else {
                        value = "1";
                    }
                    break;
                case "tdsize":
                    if (getDefault) {
                        value = String.valueOf(nmrData.getSize(dim));
                    } else {
                        value = "0";
                    }
                    break;
                case "acqsize":
                    if (getDefault) {
                        value = String.valueOf(nmrData.getSize(dim));
                    } else {
                        value = "0";
                    }
                    break;
                case "acqarray":
                    value = "0";
                    break;
                case "sf":
                    value = nmrData.getSFNames()[dim];
                    break;
                case "label":
                    value = nmrData.getLabelNames()[dim];
                    break;
                case "ref":
                    if (dim == 0) {
                        value = "";
                    } else {
                        String tn = nmrData.getTN(dim);
                        value = "";
                        for (int i = 0; i < tn.length(); i++) {
                            if (Character.isLetter(tn.charAt(i))) {
                                value = String.valueOf(tn.charAt(i));
                            }
                        }
                    }
                    break;
                default:
                    value = "";
            }

        }
        return value;
    }

    void resetData() {
        refMap.clear();
    }

    void clearItems() {
        refSheet.getItems().clear();
    }

    void setupItems(int dim) {
        ChartProcessor chartProcessor = processorController.chartProcessor;
        NMRData nmrData = getNMRData();
        ObservableList<PropertySheet.Item> newItems = FXCollections.observableArrayList();
        String dimName = "" + (dim + 1);
        if (dim == 0) {
            newItems.add(new ChoiceOperationItem(refSheet, stringListener, chartProcessor.getDatasetType().toString(),
                    DatasetType.names(), dimName, "datatype", "Dataset type"));
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
                    newItems.add(new EditableChoiceOperationItem(refSheet, stringListener, chartProcessor.getAcqOrder(), choices, dimName, "acqOrder", "Enter the acquisition order of the dataset"));
                }
            } else {
                newItems.add(new TextOperationItem(refSheet, stringListener, chartProcessor.getAcqOrder(), dimName, "acqOrder", "Enter the acquisition order of the dataset"));
            }
            if ((nmrData != null) && nmrData.getVendor().equals("bruker")) {
                newItems.add(new BooleanOperationItem(refSheet, boolListener, chartProcessor.getFixDSP(), dimName, "fixdsp", "Fix DSP buildup before FT"));
            }
        }

        for (String propName : propNames) {
            if (propName.equals("skip")) {
                if (dim > 0) {
                    String value = getPropValue(dim, propName, false);
                    boolean boolValue = false;
                    if (value.equals("1")) {
                        boolValue = true;
                    }
                    newItems.add(new BooleanOperationItem(refSheet, boolListener, boolValue, dimName, propName, "Skip this dimension?"));
                }
            } else if (propName.equals("ref")) {
                String value = getPropValue(dim, propName, false);
                String defaultValue = getPropValue(dim, propName, true);
                if (nmrData != null) {
                    if (dim == 0) {
                        defaultValue = nmrData.getRef(0) + "";
                    }
                }
                String comment = " (default is " + defaultValue + ")";
                newItems.add(new MenuTextOperationItem(refSheet, stringListener, value, dimName, propName, "Select the " + propName + comment));
            } else if (propName.contains("size")) {
                String value = getPropValue(dim, propName, false);
                int iValue = 0;
                try {
                    // Set empty strings to have a value of 0
                    if (!value.isEmpty()) {
                        iValue = Integer.parseInt(value);
                    }
                } catch (NumberFormatException nFe) {
                    log.warn("Unable to parse value.", nFe);
                }
                String defaultValue = getPropValue(dim, propName, true);
                String comment = " (default is " + defaultValue + ")";
                newItems.add(new IntOperationItem(refSheet, intListener, iValue, 0, 1000000, dimName, propName, "Enter the " + propName + comment));
            } else if (propName.equals("acqarray")) {
                int arraySize = 0;
                if (nmrData != null) {
                    arraySize = nmrData.getArraySize(dim);
                }
                String comment = " (default is " + 0 + ")";
                newItems.add(new IntOperationItem(refSheet, intListener, arraySize, 0, 1000000, dimName, propName, "Enter the " + propName + comment));
            } else {
                String value = getPropValue(dim, propName, false);
                String defaultValue = getPropValue(dim, propName, true);
                String comment = " (default is " + defaultValue + ")";
                newItems.add(new TextOperationItem(refSheet, stringListener, value, dimName, propName, "Enter the " + propName + comment));
            }
        }
        refSheet.getItems().setAll(newItems);
    }

    public void setDataFields(List<String> headerList) {
        ChartProcessor chartProcessor = processorController.chartProcessor;
        for (String s : headerList) {
            int index = s.indexOf('(');
            boolean lastIsClosePar = s.charAt(s.length() - 1) == ')';
            if ((index != -1) && lastIsClosePar) {
                String propName = s.substring(0, index);
                String args = s.substring(index + 1, s.length() - 1);
                List<String> parValues = CSVLineParse.parseLine(args);
                switch (propName) {
                    case "acqOrder":
                        chartProcessor.setAcqOrder(args);
                        break;
                    case "acqarray":
                        chartProcessor.setArraySize(args);
                        break;
                    case "fixdsp":
                        chartProcessor.setFixDSP(args.equals("True"));
                        break;
                    default:
                        int dim = 0;
                        for (String parValue : parValues) {
                            String dimName = "" + (dim + 1);
                            String nameWithDim = propName + dimName;
                            DataProps dataProp = DataProps.valueOf(propName.toUpperCase());
                            Object control = controlsMap.get(nameWithDim);
                            setControlValue(control, parValue);
                            dim++;
                        }
                        break;
                }
            }
        }
    }

    NMRData getNMRData() {
        return processorController.chartProcessor.getNMRData();
    }

    public boolean getSkip(String dimName) {
        if (!StringUtils.isNumeric(dimName)) {
            return false;
        }
        String propValue = "0";
        try {
            int dim = Integer.parseInt(dimName);
            dim--;
            propValue = getPropValue(dim, "skip", false);
        } catch (NumberFormatException nFE) {
            log.warn("Unable to parse skip.", nFE);
        }
        return propValue.equals("1");
    }
}
