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

import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.gui.spectra.NMRAxis;
import org.nmrfx.processor.gui.spectra.DatasetAttributes;
import org.nmrfx.processor.gui.spectra.PeakDisplayParameters;
import org.nmrfx.processor.gui.spectra.PeakListAttributes;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.DoubleStringConverter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.util.converter.IntegerStringConverter;
import org.controlsfx.control.ListSelectionView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.SegmentedButton;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.processor.gui.PolyChart.DISDIM;
import static org.nmrfx.processor.gui.PolyChart.DISDIM.OneDX;
import static org.nmrfx.processor.gui.PolyChart.DISDIM.TwoD;
import org.nmrfx.processor.gui.utils.ColorSchemes;
import org.nmrfx.utilities.DictionarySort;

/**
 *
 * @author johnsonb
 */
public class SpecAttrWindowController implements Initializable {

    static final DecimalFormat formatter = new DecimalFormat();

    private Stage stage;
    private Pane pane;
    private PopOver popOver;
    @FXML
    private BorderPane attrBorderPane;
    @FXML
    private ToolBar viewToolBar;
    @FXML
    private ToolBar toolBar;
    @FXML
    private ToolBar styleToolBar;
    @FXML
    private TableView<DatasetAttributes> datasetTableView;
    @FXML
    private TableView<PeakListAttributes> peakListTableView;
    @FXML
    private GridPane viewGrid;
    @FXML
    private ComboBox<DISDIM> disDimCombo;
    @FXML
    private CheckBox integralCheckBox;
    @FXML
    private RangeSlider integralPosSlider;
    @FXML
    private Label integralLowValue;
    @FXML
    private Label integralHighValue;
    @FXML
    private CheckBox regionCheckBox;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab datasetTab;
    @FXML
    private Tab peakSelectTab;

    StringProperty[][] limitFields;
    Label[] labelFields;
    @FXML
    Slider scaleSlider;

    PolyChart chart;
    @FXML
    private CheckBox offsetTrackingCheckBox;
    @FXML
    private CheckBox useDatasetColorCheckBox;
    @FXML
    private CheckBox slice1StateCheckBox;
    @FXML
    private CheckBox slice2StateCheckBox;
    @FXML
    private ColorPicker slice1ColorPicker;
    @FXML
    private ColorPicker slice2ColorPicker;
    @FXML
    Slider xOffsetSlider;
    @FXML
    Slider yOffsetSlider;

    @FXML
    CheckBox axisColorCheckBox;
    @FXML
    CheckBox cross0ColorCheckBox;
    @FXML
    CheckBox cross1ColorCheckBox;
    @FXML
    CheckBox bgColorCheckBox;

    @FXML
    private ColorPicker axisColorPicker;
    @FXML
    private ColorPicker cross0ColorPicker;
    @FXML
    private ColorPicker cross1ColorPicker;
    @FXML
    private ColorPicker bgColorPicker;

    @FXML
    private ComboBox<Double> ticFontSizeComboBox;
    @FXML
    private ComboBox<Double> labelFontSizeComboBox;
    @FXML
    private CheckBox gridCheckBox;
    @FXML
    private ComboBox<Integer> leftBorderSizeComboBox;
    @FXML
    private ComboBox<Integer> rightBorderSizeComboBox;
    @FXML
    private ComboBox<Integer> topBorderSizeComboBox;
    @FXML
    private ComboBox<Integer> bottomBorderSizeComboBox;
    @FXML
    private CheckBox titlesCheckBox;

    SegmentedButton groupButton;
    ChoiceBox<String> showOnlyCompatibleBox = new ChoiceBox();

    private ComboBox[] dimCombos;
    Label[] axisLabels;
    Label[] dimLabels;
    ListSelectionView<String> datasetView;
    ListSelectionView<String> peakView;
    static String[] rowNames = {"X", "Y", "Z", "A"};

    ListChangeListener<String> peakTargetListener;
    ListChangeListener<String> datasetTargetListener;
    Node columnMenuNode;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tabPane.setStyle("-fx-font-size:8pt;");
        initToolBar();
        initViewToolBar();
        initStyleToolBar();
        initTable();
        initPeakListTable();
        createViewGrid();
        datasetView = new ListSelectionView<>();
        datasetTab.setContent(datasetView);
        ChangeListener<Dataset> listener = new ChangeListener<Dataset>() {
            @Override
            public void changed(ObservableValue<? extends Dataset> observable, Dataset oldValue, Dataset newValue) {
                System.out.println("datasets changed");
                PolyChart chart = PolyChart.getActiveChart();
                if (chart != null) {
                    setChart(chart);
                }
            }
        };
        peakView = new ListSelectionView<>();
        peakSelectTab.setContent(peakView);
        peakView.setSourceFooter(showOnlyCompatibleBox);
        showOnlyCompatibleBox.getItems().add("Matching");
        showOnlyCompatibleBox.getItems().add("Compatible");
        showOnlyCompatibleBox.getItems().add("All");
        showOnlyCompatibleBox.setValue("Compatible");
        showOnlyCompatibleBox.setOnAction(e -> updatePeakView());
        peakSelectTab.setOnSelectionChanged(e -> {
            if (peakSelectTab.isSelected()) {
                updatePeakView();
            }
        });
        peakTargetListener = (ListChangeListener.Change<? extends String> c) -> {
            updateChartPeakLists();
        };
        datasetTargetListener = (ListChangeListener.Change<? extends String> c) -> {
            updateChartDatasets();
        };

        datasetView.getTargetItems().addListener(datasetTargetListener);
        peakView.getTargetItems().addListener(peakTargetListener);

        ticFontSizeComboBox.getItems().addAll(5.5, 6.0, 6.5, 7.0, 8.0, 9.0,
                10.0, 11.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, 26.0,
                28.0, 32.0, 36.0);
        labelFontSizeComboBox.getItems().addAll(5.5, 6.0, 6.5, 7.0, 8.0, 9.0,
                10.0, 11.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, 26.0,
                28.0, 32.0, 36.0);
        leftBorderSizeComboBox.getItems().addAll(0, 1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 75, 100, 125, 150);
        rightBorderSizeComboBox.getItems().addAll(0, 1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 75, 100, 125, 150);
        topBorderSizeComboBox.getItems().addAll(0, 1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 75, 100, 125, 150);
        bottomBorderSizeComboBox.getItems().addAll(0, 1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 75, 100, 125, 150);

        bgColorPicker.disableProperty().bind(bgColorCheckBox.selectedProperty().not());
        axisColorPicker.disableProperty().bind(axisColorCheckBox.selectedProperty().not());
        cross0ColorPicker.disableProperty().bind(cross0ColorCheckBox.selectedProperty().not());
        cross1ColorPicker.disableProperty().bind(cross1ColorCheckBox.selectedProperty().not());

        integralPosSlider.setMin(0.0);
        integralPosSlider.setMax(1.0);
        integralPosSlider.setLowValue(0.8);
        integralPosSlider.setHighValue(0.95);

        integralPosSlider.lowValueProperty().addListener(e -> updateIntegralState());
        integralPosSlider.highValueProperty().addListener(e -> updateIntegralState());
        integralCheckBox.selectedProperty().addListener(e -> updateIntegralState());
        regionCheckBox.selectedProperty().addListener(e -> updateIntegralState());

    }

    void updateIntegralState() {
        chart.chartProps.setRegions(regionCheckBox.isSelected());
        chart.chartProps.setIntegrals(integralCheckBox.isSelected());
        double lowValue = integralPosSlider.getLowValue();
        double highValue = integralPosSlider.getHighValue();
        chart.chartProps.setIntegralLowPos(lowValue);
        chart.chartProps.setIntegralHighPos(highValue);
        integralLowValue.setText(String.format("%.2f", lowValue));
        integralHighValue.setText(String.format("%.2f", highValue));
        chart.refresh();
    }

    public boolean isShowing() {
        boolean showing = false;
        if (stage != null) {
            showing = stage.isShowing();
        } else if (popOver != null) {
            showing = popOver.isShowing() || popOver.isDetached();
        }
        return showing;
    }

    public Stage getStage() {
        return stage;
    }

    public Pane getPane() {
        return pane;
    }

    public PopOver getPopOver() {
        return popOver;
    }

    public void setPopOver(PopOver popOver) {
        this.popOver = popOver;
    }

    public void hideToolBar() {
        Button bButton = new Button("Refresh");
        bButton.setOnAction(e -> refreshAction());
        attrBorderPane.setTop(bButton);
        attrBorderPane.setPrefHeight(250);
    }

    public void showToolBar() {
        attrBorderPane.setTop(toolBar);
        attrBorderPane.setPrefHeight(300);
    }

    private void addViewRefreshButton(ListSelectionView listView) {
        Button bButton = new Button("Refresh");
        bButton.setOnAction(e -> refreshAction());
        HBox hBox = new HBox();
        hBox.getChildren().add(bButton);
        hBox.getChildren().add(new Label("Available"));
        listView.setSourceHeader(hBox);
    }

    private void dimMenuAction(ActionEvent event, int iAxis) {
        MenuItem menuItem = (MenuItem) event.getSource();
        if (menuItem.getText().equals("Full")) {
            chart.full(iAxis);
        } else if (menuItem.getText().equals("Center")) {
            chart.center(iAxis);
        } else if (menuItem.getText().equals("First")) {
            chart.firstPlane(iAxis);
        } else if (menuItem.getText().equals("Last")) {
            chart.lastPlane(iAxis);
        }
    }

    private void setDisDim() {
//        switch (disDimCombo.getValue()) {
//            case "1Dx":
//                chart.disDim = 0;
//                break;
//            case "2D":
//                chart.disDim = 2;
//                break;
//            default:
//        }
//        System.out.println("set dis dim " + disDimCombo.getValue() + " " + chart.disDim);

    }

    private void createViewGrid() {
        disDimCombo.getItems().addAll(OneDX, TwoD);
        //disDimCombo.setValue(OneDX);
        limitFields = new StringProperty[rowNames.length][2];
        labelFields = new Label[rowNames.length];
        int iRow = 1;
        dimCombos = new ComboBox[rowNames.length];
        axisLabels = new Label[rowNames.length];
        dimLabels = new Label[rowNames.length];
        for (String rowName : rowNames) {
            int iRow0 = iRow - 1;
            MenuButton mButton = new MenuButton(rowName);
            MenuItem menuItem = new MenuItem("Full");
            mButton.getItems().add(menuItem);
            menuItem.addEventHandler(ActionEvent.ACTION, event -> dimMenuAction(event, iRow0));
            if (iRow > 2) {
                menuItem = new MenuItem("First");
                mButton.getItems().add(menuItem);
                menuItem.addEventHandler(ActionEvent.ACTION, event -> dimMenuAction(event, iRow0));
                menuItem = new MenuItem("Center");
                mButton.getItems().add(menuItem);
                menuItem.addEventHandler(ActionEvent.ACTION, event -> dimMenuAction(event, iRow0));
                menuItem = new MenuItem("Last");
                mButton.getItems().add(menuItem);
                menuItem.addEventHandler(ActionEvent.ACTION, event -> dimMenuAction(event, iRow0));
            }

            viewGrid.add(mButton, 1, iRow);
            TextField minField = new TextField();
            minField.setPrefWidth(75.0);
            limitFields[iRow - 1][0] = minField.textProperty();
            viewGrid.add(minField, 2, iRow);
            TextField maxField = new TextField();
            maxField.setPrefWidth(75.0);
            viewGrid.add(maxField, 3, iRow);
            Label label = new Label("PPM");
            labelFields[iRow0] = label;
            label.setPrefWidth(40);
            viewGrid.add(label, 4, iRow);
            Label axisLabel = new Label("");
            axisLabel.setPrefWidth(40);
            viewGrid.add(axisLabel, 5, iRow);
            axisLabels[iRow - 1] = axisLabel;
            limitFields[iRow - 1][1] = maxField.textProperty();
            if (iRow < 3) {
                ComboBox comboBox = new ComboBox();
                viewGrid.add(comboBox, 6, iRow);
                dimCombos[iRow - 1] = comboBox;
                comboBox.getItems().addAll("1", "2", "3", "4");
                comboBox.getSelectionModel().select(iRow - 1);
                comboBox.setOnAction(e -> dimAction(rowName, e));
            } else {
                Label dimLabel = new Label(String.valueOf(iRow));
                dimLabel.setPrefWidth(30);
                dimLabels[iRow0] = dimLabel;
                viewGrid.add(dimLabel, 6, iRow);
            }
            iRow++;
        }
    }

    public static SpecAttrWindowController create() {
        FXMLLoader loader = new FXMLLoader(SpecAttrWindowController.class.getResource("/fxml/SpecAttrScene.fxml"));
        Stage stage = new Stage(StageStyle.DECORATED);
        try {
            Pane pane = (Pane) loader.load();
            Scene scene = new Scene(pane);
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");

            SpecAttrWindowController controller = loader.<SpecAttrWindowController>getController();
            controller.stage = stage;
            stage.setTitle("Spectrum Attributes");
            stage.setAlwaysOnTop(true);
            stage.setOnShown(e -> controller.update());
            stage.show();
            return controller;
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
            return null;
        }
    }

    public static SpecAttrWindowController createPane() {
        FXMLLoader loader = new FXMLLoader(SpecAttrWindowController.class.getResource("/fxml/SpecAttrScene.fxml"));
        SpecAttrWindowController controller = null;
        try {
            Pane pane = (Pane) loader.load();
            controller = loader.<SpecAttrWindowController>getController();
            controller.pane = pane;
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return controller;

    }

    public boolean isSceneMode() {
        RadioButton gButton = (RadioButton) groupButton.getToggleGroup().getSelectedToggle();
        boolean sceneMode = false;
        if (gButton != null) {
            sceneMode = gButton.getText().equals("Scene");
        }
        return sceneMode;

    }

    public void updateDatasetTableView() {
        boolean sceneMode = isSceneMode();
        if (datasetTableView == null) {
            System.out.println("null table");
        } else if (sceneMode) {
            ObservableList<DatasetAttributes> datasetAttrList = FXCollections.observableArrayList();
            List<PolyChart> charts = chart.getSceneMates(true);
            charts.stream().forEach(chart2
                    -> datasetAttrList.addAll(chart2.getDatasetAttributes()));
            datasetTableView.setItems(datasetAttrList);
        } else {
            datasetTableView.setItems(chart.getDatasetAttributes());
        }
    }

    public void updatePeakListTableView() {
        boolean sceneMode = false;
        if (peakListTableView == null) {
            System.out.println("null table");
        } else if (sceneMode) {
            ObservableList<PeakListAttributes> peakListAttrList = FXCollections.observableArrayList();
            List<PolyChart> charts = chart.getSceneMates(true);
            charts.stream().forEach(chart2
                    -> peakListAttrList.addAll(chart2.getPeakListAttributes()));
            peakListTableView.setItems(peakListAttrList);
        } else {
            peakListTableView.setItems(chart.getPeakListAttributes());
        }
    }

    void updatePeakView() {
        peakView.getTargetItems().removeListener(peakTargetListener);
        String showOnlyMode = showOnlyCompatibleBox.getValue();
        ObservableList<String> peaksTarget = peakView.getTargetItems();
        ObservableList<String> peaksSource = peakView.getSourceItems();
        peaksTarget.clear();
        peaksSource.clear();
        List<DatasetAttributes> dataAttrs = chart.getDatasetAttributes();
        List<PeakListAttributes> peakAttrs = chart.getPeakListAttributes();

        for (PeakListAttributes peakAttr : peakAttrs) {
            peaksTarget.add(peakAttr.getPeakListName());
        }
        for (PeakList peakList : PeakList.getLists()) {
            if (!peaksTarget.contains(peakList.getName())) {
                boolean ok = false;
                if (showOnlyMode.equals("All")) {
                    ok = true;
                } else if (showOnlyMode.equals("Compatible") && chart.isPeakListCompatible(peakList, false)) {
                    ok = true;
                } else if (showOnlyMode.equals("Matching")) {
                    String datasetName = peakList.getDatasetName();
                    ok = dataAttrs.stream().anyMatch(d -> d.getFileName().equals(datasetName));
                }
                if (ok) {
                    peaksSource.add(peakList.getName());
                }
            }
        }
        peakView.getTargetItems().addListener(peakTargetListener);
    }

    void updateDatasetView() {
        datasetView.getTargetItems().removeListener(datasetTargetListener);
        ObservableList<String> datasetsTarget = datasetView.getTargetItems();
        ObservableList<String> datasetsSource = datasetView.getSourceItems();
        datasetsTarget.clear();
        datasetsSource.clear();
        List<DatasetAttributes> dataAttrs = chart.getDatasetAttributes();
        List<PeakListAttributes> peakAttrs = chart.getPeakListAttributes();

        for (Object obj : chart.getDatasetAttributes()) {
            DatasetAttributes dataAttr = (DatasetAttributes) obj;
            datasetsTarget.add(dataAttr.getDataset().getName());
        }
        DictionarySort<Dataset> sorter = new DictionarySort<>();
        FXMLController.datasetList.stream().sorted(sorter).forEach(d -> {
            if (!datasetsTarget.contains(d.getName())) {
                datasetsSource.add(d.getName());
            }
        });
        datasetView.getTargetItems().addListener(datasetTargetListener);
    }

    public void setChart(PolyChart chart) {
        this.chart = chart;
        // disDimCombo.valueProperty().addListener(e -> setDisDim());
        update();
    }

    public void update() {
        if (isShowing()) {
            FXMLController fxmlController = FXMLController.getActiveController();
            chart = fxmlController.getActiveChart();
            updateDatasetTableView();
            updatePeakListTableView();
            clearDimActions();
            bindToChart(chart);
            setLimits();
            updateDatasetView();
            updatePeakView();
            updateDims();
            setupDimActions();
            datasetTableView.getSelectionModel().clearSelection();
        }
    }

    void initToolBar() {
        String iconSize = "16px";
        String fontSize = "7pt";
        ArrayList<Button> buttons = new ArrayList<>();
        Button bButton;
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.REFRESH, "Refresh", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> refreshAction());
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.EXPAND, "Full", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> chart.full());
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.SEARCH, "Expand", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> chart.expand());
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.SEARCH_MINUS, "In", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> chart.zoom(1.2));
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.SEARCH_PLUS, "Out", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> chart.zoom(0.8));
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.ARROWS_V, "Auto", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> autoScale());
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.ARROW_UP, "Higher", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> adjustScale(0.8));
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.ARROW_DOWN, "Lower", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> adjustScale(1.2));
        buttons.add(bButton);

        for (Button button : buttons) {
            button.getStyleClass().add("toolButton");
        }
        toolBar.getItems().addAll(buttons);

    }

    void initViewToolBar() {
        String iconSize = "16px";
        String fontSize = "7pt";
        Pane filler1 = new Pane();
        Pane filler2 = new Pane();
        HBox.setHgrow(filler1, Priority.ALWAYS);
        HBox.setHgrow(filler2, Priority.ALWAYS);
        ArrayList<ButtonBase> buttons = new ArrayList<>();
        Button bButton;
        bButton = new Button("Save");
        buttons.add(bButton);
        bButton.setOnAction(e -> saveParameters());
        ToggleGroup group = new ToggleGroup();
        RadioButton chartButton = new RadioButton("Chart");
        RadioButton sceneButton = new RadioButton("Scene");
        sceneButton.setOnAction(e -> updateDatasetTableView());
        chartButton.setOnAction(e -> updateDatasetTableView());
        chartButton.getStyleClass().add("toolButton");
        sceneButton.getStyleClass().add("toolButton");
        groupButton = new SegmentedButton(chartButton, sceneButton);
        for (ButtonBase button : buttons) {
            button.getStyleClass().add("toolButton");
            button.setMinWidth(50);
        }
        viewToolBar.getItems().addAll(buttons);
        viewToolBar.getItems().add(filler1);
        viewToolBar.getItems().add(groupButton);
        viewToolBar.getItems().add(filler2);
        chartButton.setSelected(true);

    }

    void initStyleToolBar() {
        String iconSize = "16px";
        String fontSize = "7pt";
        Pane filler1 = new Pane();
        Pane filler2 = new Pane();
        HBox.setHgrow(filler1, Priority.ALWAYS);
        HBox.setHgrow(filler2, Priority.ALWAYS);
        ArrayList<ButtonBase> buttons = new ArrayList<>();
        Button bButton;
        bButton = new Button("Apply");
        buttons.add(bButton);
        bButton.setOnAction(e -> copyProps());
        for (ButtonBase button : buttons) {
            button.getStyleClass().add("toolButton");
            button.setMinWidth(50);
        }
        styleToolBar.getItems().addAll(buttons);
    }

    void initTable() {
        DoubleStringConverter dsConverter = new DoubleStringConverter();
        IntegerStringConverter isConverter = new IntegerStringConverter();
        datasetTableView.setEditable(true);
        datasetTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableColumn<DatasetAttributes, String> fileNameCol = new TableColumn<>("dataset");
        fileNameCol.setCellValueFactory(new PropertyValueFactory("fileName"));
        fileNameCol.setEditable(false);

        TableColumn<DatasetAttributes, String> levelCol = new TableColumn<>("lvl");
        levelCol.setCellValueFactory(new PropertyValueFactory("lvl"));
        levelCol.setCellFactory(tc -> new TextFieldTableCell(dsConverter));

        ContextMenu levelMenu = new ContextMenu();
        MenuItem unifyLevelItem = new MenuItem("unify");
        unifyLevelItem.setOnAction(e -> unifyLevel());
        levelCol.setContextMenu(levelMenu);
        levelMenu.getItems().addAll(unifyLevelItem);

        TableColumn<DatasetAttributes, String> offsetCol = new TableColumn<>("offset");
        offsetCol.setCellValueFactory(new PropertyValueFactory("offset"));
        offsetCol.setCellFactory(tc -> new TextFieldTableCell(dsConverter));

        ContextMenu offsetMenu = new ContextMenu();
        MenuItem unifyOffsetItem = new MenuItem("unify");
        unifyOffsetItem.setOnAction(e -> unifyOffset());
        MenuItem rampOffsetItem = new MenuItem("ramp");
        rampOffsetItem.setOnAction(e -> rampOffset());
        offsetMenu.getItems().addAll(unifyOffsetItem, rampOffsetItem);
        offsetCol.setContextMenu(offsetMenu);
        offsetCol.setPrefWidth(50);

        TableColumn<DatasetAttributes, String> nLevelsCol = new TableColumn<>("nLvl");
        nLevelsCol.setCellValueFactory(new PropertyValueFactory("nlvls"));
        nLevelsCol.setCellFactory(tc -> new TextFieldTableCell(isConverter));
        nLevelsCol.setPrefWidth(35);

        ContextMenu nLvlMenu = new ContextMenu();
        MenuItem unifyNLvlItem = new MenuItem("unify");
        unifyNLvlItem.setOnAction(e -> unifyNLvl());
        nLevelsCol.setContextMenu(nLvlMenu);
        nLvlMenu.getItems().addAll(unifyNLvlItem);

        TableColumn<DatasetAttributes, String> clmCol = new TableColumn<>("clm");
        clmCol.setCellValueFactory(new PropertyValueFactory("clm"));
        clmCol.setCellFactory(tc -> new TextFieldTableCell(dsConverter));
        clmCol.setPrefWidth(50);

        ContextMenu clmlMenu = new ContextMenu();
        MenuItem unifyCLMItem = new MenuItem("unify");
        unifyCLMItem.setOnAction(e -> unifyCLM());
        clmCol.setContextMenu(clmlMenu);
        clmlMenu.getItems().addAll(unifyCLMItem);

        TableColumn<DatasetAttributes, Boolean> posDrawOnCol = new TableColumn<>("on");
        posDrawOnCol.setCellValueFactory(new PropertyValueFactory("pos"));
        posDrawOnCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        posDrawOnCol.setPrefWidth(25);
        posDrawOnCol.setMaxWidth(25);
        posDrawOnCol.setResizable(false);

        ContextMenu posOnMenu = new ContextMenu();
        MenuItem allPosOnItem = new MenuItem("all on");
        allPosOnItem.setOnAction(e -> setDrawStatus(true, true));
        MenuItem allPosOffItem = new MenuItem("all off");
        allPosOffItem.setOnAction(e -> setDrawStatus(true, false));
        posDrawOnCol.setContextMenu(posOnMenu);
        posOnMenu.getItems().addAll(allPosOnItem, allPosOffItem);

        TableColumn<DatasetAttributes, String> posLineWidthCol = new TableColumn<>("width");
        posLineWidthCol.setCellValueFactory(new PropertyValueFactory("posWidth"));
        posLineWidthCol.setCellFactory(tc -> new TextFieldTableCell(dsConverter));
        posLineWidthCol.setPrefWidth(50);

        ContextMenu posWidthMenu = new ContextMenu();
        MenuItem unifyPosWidthItem = new MenuItem("unify");
        unifyPosWidthItem.setOnAction(e -> unifyWidth(true));
        posLineWidthCol.setContextMenu(posWidthMenu);
        posWidthMenu.getItems().addAll(unifyPosWidthItem);

        TableColumn<DatasetAttributes, Color> posColorCol = new TableColumn<>("color");
        posColorCol.setPrefWidth(50);
        posColorCol.setCellValueFactory(new PropertyValueFactory("posColor"));
        posColorCol.setCellValueFactory(cellData -> cellData.getValue().posColorProperty());
        posColorCol.setCellFactory(column -> {
            return new TableCell<DatasetAttributes, Color>() {
                @Override

                protected void updateItem(Color item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    if (empty || (item == null)) {
                        setGraphic(null);
                    } else {
                        final ColorPicker cp = new ColorPicker();
                        cp.setValue(item);
                        setGraphic(cp);
                        cp.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                            public void
                                    handle(javafx.event.ActionEvent t) {
                                getTableView().edit(getTableRow().getIndex(), column);
                                commitEdit(cp.getValue());
                            }
                        });
                    }
                }
            };
        });

        posColorCol.setOnEditCommit(new EventHandler<CellEditEvent<DatasetAttributes, Color>>() {
            @Override
            public void handle(CellEditEvent<DatasetAttributes, Color> t) {
                ((DatasetAttributes) t.getTableView().getItems().get(t.getTablePosition().
                        getRow())).setPosColor(t.getNewValue());
            }
        });
        ContextMenu posColorMenu = new ContextMenu();
        MenuItem unifyPosColorItem = new MenuItem("unify");
        unifyPosColorItem.setOnAction(e -> unifyColor(true));
        MenuItem interpPosColor = new MenuItem("interpolate");
        interpPosColor.setOnAction(e -> interpolatePosColors());
        MenuItem schemaPosColor = new MenuItem("schema...");
        schemaPosColor.setOnAction(e -> setPosColorsToSchema());
        posColorCol.setContextMenu(posColorMenu);
        posColorMenu.getItems().addAll(unifyPosColorItem, interpPosColor, schemaPosColor);

        TableColumn<DatasetAttributes, Boolean> negDrawOnCol = new TableColumn<>("on");
        negDrawOnCol.setCellValueFactory(new PropertyValueFactory("neg"));
        negDrawOnCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        negDrawOnCol.setPrefWidth(25);
        negDrawOnCol.setMaxWidth(25);
        negDrawOnCol.setResizable(false);

        ContextMenu negOnMenu = new ContextMenu();
        MenuItem allNegOnItem = new MenuItem("all on");
        allNegOnItem.setOnAction(e -> setDrawStatus(false, true));
        MenuItem allNegOffItem = new MenuItem("all off");
        allNegOffItem.setOnAction(e -> setDrawStatus(false, false));
        negDrawOnCol.setContextMenu(negOnMenu);
        negOnMenu.getItems().addAll(allNegOnItem, allNegOffItem);

        TableColumn<DatasetAttributes, Double> negLineWidthCol = new TableColumn<>("width");
        negLineWidthCol.setCellValueFactory(new PropertyValueFactory("negWidth"));
        negLineWidthCol.setPrefWidth(50);

        ContextMenu negWidthMenu = new ContextMenu();
        MenuItem unifyNegWidthItem = new MenuItem("unify");
        unifyNegWidthItem.setOnAction(e -> unifyWidth(false));
        negLineWidthCol.setContextMenu(negWidthMenu);
        negWidthMenu.getItems().addAll(unifyNegWidthItem);

        TableColumn<DatasetAttributes, Color> negColorCol = new TableColumn<>("color");
        negColorCol.setPrefWidth(50);
        negColorCol.setCellValueFactory(cellData -> cellData.getValue().negColorProperty());
        negColorCol.setCellFactory(column -> {
            return new TableCell<DatasetAttributes, Color>() {
                @Override
                protected void updateItem(Color item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    if (empty || (item == null)) {
                        setGraphic(null);
                    } else {
                        final ColorPicker cp = new ColorPicker();
                        cp.setValue(item);
                        setGraphic(cp);
                        cp.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                            public void
                                    handle(javafx.event.ActionEvent t) {
                                getTableView().edit(getTableRow().getIndex(), column);
                                commitEdit(cp.getValue());
                            }
                        });
                    }
                }
            };
        });

        negColorCol.setOnEditCommit(new EventHandler<CellEditEvent<DatasetAttributes, Color>>() {
            @Override
            public void handle(CellEditEvent<DatasetAttributes, Color> t) {
                ((DatasetAttributes) t.getTableView().getItems().get(t.getTablePosition().
                        getRow())).setNegColor(t.getNewValue());
            }
        });
        ContextMenu negColorMenu = new ContextMenu();
        MenuItem unifyNegColorItem = new MenuItem("unify");
        unifyNegColorItem.setOnAction(e -> unifyColor(false));
        MenuItem interpNegColorItem = new MenuItem("interpolate");
        interpNegColorItem.setOnAction(e -> interpolateNegColors());
        MenuItem schemaNegColor = new MenuItem("schema...");
        schemaNegColor.setOnAction(e -> setNegColorsToSchema());
        negColorCol.setContextMenu(negColorMenu);

        negColorMenu.getItems().addAll(unifyNegColorItem, interpNegColorItem, schemaNegColor);

        TableColumn positiveColumn = new TableColumn("Positive");
        TableColumn negativeColumn = new TableColumn("Negative");
        positiveColumn.getColumns().setAll(posDrawOnCol, posColorCol, posLineWidthCol);
        negativeColumn.getColumns().setAll(negDrawOnCol, negColorCol, negLineWidthCol);
        datasetTableView.getColumns().setAll(fileNameCol, levelCol, offsetCol, nLevelsCol, clmCol, positiveColumn, negativeColumn);
    }

    void initPeakListTable() {
        DoubleStringConverter dsConverter = new DoubleStringConverter();
        IntegerStringConverter isConverter = new IntegerStringConverter();
        peakListTableView.setEditable(true);
        peakListTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<PeakListAttributes, Boolean> drawPeaksCol = new TableColumn<>("on");
        drawPeaksCol.setCellValueFactory(new PropertyValueFactory("drawPeaks"));
        drawPeaksCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        drawPeaksCol.setPrefWidth(25);
        drawPeaksCol.setMaxWidth(25);
        drawPeaksCol.setResizable(false);

        TableColumn<PeakListAttributes, String> peakListCol = new TableColumn<>("listname");
        peakListCol.setCellValueFactory(new PropertyValueFactory("peakListName"));
        peakListCol.setEditable(false);

        TableColumn<PeakListAttributes, Color> onColorCol = new TableColumn<>("oncolor");
        onColorCol.setPrefWidth(50);
        onColorCol.setCellValueFactory(new PropertyValueFactory("onColor"));
        onColorCol.setCellValueFactory(cellData -> cellData.getValue().onColorProperty());
        onColorCol.setCellFactory(column -> {
            return new TableCell<PeakListAttributes, Color>() {
                @Override

                protected void updateItem(Color item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    if (empty || (item == null)) {
                        setGraphic(null);
                    } else {
                        final ColorPicker cp = new ColorPicker();
                        cp.setValue(item);
                        setGraphic(cp);
                        cp.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                            public void
                                    handle(javafx.event.ActionEvent t) {
                                getTableView().edit(getTableRow().getIndex(), column);
                                commitEdit(cp.getValue());
                            }
                        });
                    }
                }
            };
        });
        onColorCol.setOnEditCommit(new EventHandler<CellEditEvent<PeakListAttributes, Color>>() {
            @Override
            public void handle(CellEditEvent<PeakListAttributes, Color> t) {
                ((PeakListAttributes) t.getTableView().getItems().get(t.getTablePosition().
                        getRow())).setOnColor(t.getNewValue());
            }
        });
        TableColumn<PeakListAttributes, Color> offColorCol = new TableColumn<>("offcolor");
        offColorCol.setPrefWidth(50);
        offColorCol.setCellValueFactory(new PropertyValueFactory("offColor"));
        offColorCol.setCellValueFactory(cellData -> cellData.getValue().offColorProperty());
        offColorCol.setCellFactory(column -> {
            return new TableCell<PeakListAttributes, Color>() {
                @Override

                protected void updateItem(Color item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    if (empty || (item == null)) {
                        setGraphic(null);
                    } else {
                        final ColorPicker cp = new ColorPicker();
                        cp.setValue(item);
                        setGraphic(cp);
                        cp.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                            public void
                                    handle(javafx.event.ActionEvent t) {
                                getTableView().edit(getTableRow().getIndex(), column);
                                commitEdit(cp.getValue());
                            }
                        });
                    }
                }
            };
        });
        offColorCol.setOnEditCommit(new EventHandler<CellEditEvent<PeakListAttributes, Color>>() {
            @Override
            public void handle(CellEditEvent<PeakListAttributes, Color> t) {
                ((PeakListAttributes) t.getTableView().getItems().get(t.getTablePosition().
                        getRow())).setOffColor(t.getNewValue());
            }
        });

        TableColumn<PeakListAttributes, PeakDisplayParameters.ColorTypes> peakColorTypeCol = new TableColumn<>("ColorBy");
        peakColorTypeCol.setCellValueFactory(cellData -> cellData.getValue().colorTypeProperty());

        peakColorTypeCol.setCellFactory(tc -> {
            ComboBox<PeakDisplayParameters.ColorTypes> combo = new ComboBox<>();
            combo.getItems().addAll(PeakDisplayParameters.ColorTypes.values());
            TableCell<PeakListAttributes, PeakDisplayParameters.ColorTypes> cell = new TableCell<PeakListAttributes, PeakDisplayParameters.ColorTypes>() {
                @Override
                protected void updateItem(PeakDisplayParameters.ColorTypes reason, boolean empty) {
                    super.updateItem(reason, empty);
                    setText(null);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        combo.setValue(reason);
                        setGraphic(combo);
                    }
                }
            };
            combo.setOnAction(e
                    -> peakListTableView.getItems().get(cell.getIndex()).setColorType(combo.getValue()));
            return cell;
        });
        peakColorTypeCol.setPrefWidth(75);
        peakColorTypeCol.setMaxWidth(150);

        TableColumn<PeakListAttributes, PeakDisplayParameters.DisplayTypes> peakDisTypeCol = new TableColumn<>("Type");
        peakDisTypeCol.setCellValueFactory(cellData -> cellData.getValue().displayTypeProperty());

        peakDisTypeCol.setCellFactory(tc -> {
            ComboBox<PeakDisplayParameters.DisplayTypes> combo = new ComboBox<>();
            combo.getItems().addAll(PeakDisplayParameters.DisplayTypes.values());
            TableCell<PeakListAttributes, PeakDisplayParameters.DisplayTypes> cell = new TableCell<PeakListAttributes, PeakDisplayParameters.DisplayTypes>() {
                @Override
                protected void updateItem(PeakDisplayParameters.DisplayTypes reason, boolean empty) {
                    super.updateItem(reason, empty);
                    setText(null);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        combo.setValue(reason);
                        setGraphic(combo);
                    }
                }
            };
            combo.setOnAction(e
                    -> peakListTableView.getItems().get(cell.getIndex()).setDisplayType(combo.getValue()));
            return cell;
        });
        peakDisTypeCol.setPrefWidth(75);
        peakDisTypeCol.setMaxWidth(150);

        TableColumn<PeakListAttributes, PeakDisplayParameters.LabelTypes> peakLabelTypeCol = new TableColumn<>("Label");
        peakLabelTypeCol.setCellValueFactory(cellData -> cellData.getValue().labelTypeProperty());

        peakLabelTypeCol.setCellFactory(tc -> {
            ComboBox<PeakDisplayParameters.LabelTypes> combo = new ComboBox<>();
            combo.getItems().addAll(PeakDisplayParameters.LabelTypes.values());
            TableCell<PeakListAttributes, PeakDisplayParameters.LabelTypes> cell = new TableCell<PeakListAttributes, PeakDisplayParameters.LabelTypes>() {
                @Override
                protected void updateItem(PeakDisplayParameters.LabelTypes reason, boolean empty) {
                    super.updateItem(reason, empty);
                    setText(null);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        combo.setValue(reason);
                        setGraphic(combo);
                    }
                }
            };
            combo.setOnAction(e
                    -> peakListTableView.getItems().get(cell.getIndex()).setLabelType(combo.getValue()));
            return cell;
        });
        peakLabelTypeCol.setPrefWidth(75);
        peakLabelTypeCol.setMaxWidth(150);

        TableColumn<PeakListAttributes, String> nPlanesCol = new TableColumn<>("npl");
        nPlanesCol.setCellValueFactory(new PropertyValueFactory("nplanes"));
        nPlanesCol.setCellFactory(tc -> new TextFieldTableCell(isConverter));
        nPlanesCol.setPrefWidth(35);
        nPlanesCol.setMaxWidth(50);

        ContextMenu nPlanesMenu = new ContextMenu();
        MenuItem unifyNPlanesMenu = new MenuItem("unify");
        unifyNPlanesMenu.setOnAction(e -> unifyNPlanes());
        nPlanesCol.setContextMenu(nPlanesMenu);
        nPlanesMenu.getItems().addAll(unifyNPlanesMenu);

        TableColumn<PeakListAttributes, Boolean> drawLinksCol = new TableColumn<>("lnk");
        drawLinksCol.setCellValueFactory(new PropertyValueFactory("drawLinks"));
        drawLinksCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        drawLinksCol.setPrefWidth(35);
        drawLinksCol.setMaxWidth(50);
        drawLinksCol.setResizable(false);

        TableColumn<PeakListAttributes, Boolean> simPeaksCol = new TableColumn<>("sim");
        simPeaksCol.setCellValueFactory(new PropertyValueFactory("simPeaks"));
        simPeaksCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        simPeaksCol.setPrefWidth(35);
        simPeaksCol.setMaxWidth(50);
        simPeaksCol.setResizable(false);

        peakListTableView.getColumns().setAll(peakListCol, drawPeaksCol,
                onColorCol, offColorCol, peakColorTypeCol, peakDisTypeCol, nPlanesCol,
                drawLinksCol, simPeaksCol, peakLabelTypeCol);
    }

    private void xFullAction(ActionEvent event) {
        MenuItem menuItem = (MenuItem) event.getSource();
    }

//    @FXML
//    private void scaleSliderAction(Event event) {
//        Slider slider = (Slider) event.getSource();
//        double sliderScale = slider.getValue();
//        System.out.println("action " + sliderScale);
//        chart.sliceAttributes.setScale(sliderScale);
//        chart.refreshCrossHairs();
//    }
    @FXML
    private void sliceAction(Event event) {
        chart.sliceAttributes.setSlice1Color(slice1ColorPicker.getValue());
        chart.sliceAttributes.setSlice2Color(slice2ColorPicker.getValue());
        chart.getCrossHairs().refreshCrossHairs();
    }

    private void colorAction(MouseEvent event) {
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setOnAction(new EventHandler() {
            public void handle(Event t) {
                Color c = colorPicker.getValue();
            }
        });

    }

    void doNothing() {

    }

    void setupDimActions() {
        int i = 0;
        for (ComboBox combo : dimCombos) {
            final int iDim = i;
            if ((i < 2) && (dimCombos[i] != null)) {
                combo.setOnAction(e -> dimAction(rowNames[iDim], e));
            }
            i++;
        }
    }

    void clearDimActions() {
        for (ComboBox combo : dimCombos) {
            if (combo != null) {
                combo.setOnAction(e -> doNothing());
            }
        }
    }

    private void updateChartPeakLists() {
        ObservableList<String> peakListTargets = peakView.getTargetItems();
        chart.updatePeakLists(peakListTargets);
    }

    private void updateChartDatasets() {
        ObservableList<String> datasetTargets = datasetView.getTargetItems();
        chart.updateDatasets(datasetTargets);
    }

    @FXML
    private void updateBGColor() {
        // check to see if the new background color is the same as the pos color
        // for first dataset.  If it is, change the color of the first dataset
        // so it is visible.

        if (bgColorCheckBox.isSelected()) {
            Color color = bgColorPicker.getValue();
            if (!chart.getDatasetAttributes().isEmpty()) {
                DatasetAttributes dataAttr = chart.getDatasetAttributes().get(0);
                Color posColor = dataAttr.getPosColor();
                if ((posColor != null) && (color != null)) {
                    double diff = Math.abs(posColor.getRed() - color.getRed());
                    diff += Math.abs(posColor.getGreen() - color.getGreen());
                    diff += Math.abs(posColor.getBlue() - color.getBlue());
                    if (diff < 0.05) {
                        dataAttr.setPosColor(PolyChart.chooseBlackWhite(color));
                    }
                }
            }
        }
        updateProps();
    }

    @FXML
    private void updateProps() {
        if (bgColorCheckBox.isSelected()) {
            chart.chartProps.setBgColor(bgColorPicker.getValue());
        } else {
            chart.chartProps.setBgColor(null);

        }
        if (axisColorCheckBox.isSelected()) {
            chart.chartProps.setAxesColor(axisColorPicker.getValue());
        } else {
            chart.chartProps.setAxesColor(null);
        }
        if (cross0ColorCheckBox.isSelected()) {
            chart.chartProps.setCross0Color(cross0ColorPicker.getValue());
        } else {
            chart.chartProps.setCross0Color(null);
        }
        if (cross1ColorCheckBox.isSelected()) {
            chart.chartProps.setCross1Color(cross1ColorPicker.getValue());
        } else {
            chart.chartProps.setCross1Color(null);
        }
        chart.chartProps.setTicFontSize(ticFontSizeComboBox.getValue());
        chart.chartProps.setLabelFontSize(labelFontSizeComboBox.getValue());
        chart.chartProps.setLeftBorderSize(leftBorderSizeComboBox.getValue());
        chart.chartProps.setRightBorderSize(rightBorderSizeComboBox.getValue());
        chart.chartProps.setTopBorderSize(topBorderSizeComboBox.getValue());
        chart.chartProps.setBottomBorderSize(bottomBorderSizeComboBox.getValue());
        chart.chartProps.setGrid(gridCheckBox.isSelected());
        chart.chartProps.setTitles(titlesCheckBox.isSelected());

    }

    private void refreshAction() {
        updateChartDatasets();
        updateChartPeakLists();
        updateProps();
        chart.disDimProp.set(disDimCombo.getValue());

        try {
            chart.xAxis.lowerBoundProperty().setValue(formatter.parse(limitFields[0][0].get()));
            chart.xAxis.upperBoundProperty().setValue(formatter.parse(limitFields[0][1].get()));
            if (!chart.is1D()) {
                for (int i = 1; (i < chart.getNDim()) && (i < chart.axes.length); i++) {
                    NMRAxis axis = chart.axes[i];
                    axis.lowerBoundProperty().setValue(formatter.parse(limitFields[i][0].get()));
                    axis.upperBoundProperty().setValue(formatter.parse(limitFields[i][1].get()));
                }
            }
            chart.layoutPlotChildren();
            if (isSceneMode()) {
                List<PolyChart> charts = chart.getSceneMates(false);
                charts.stream().forEach(c -> c.layoutPlotChildren());
            }
        } catch (ParseException parseE) {
        }
    }

    void updateDims() {
        // fix me is this right
        int start = 0;
        if (!chart.getDatasetAttributes().isEmpty()) {
            DatasetAttributes datasetAttr = (DatasetAttributes) chart.datasetAttributesList.get(0);
            for (int i = 0; i < chart.getNDim(); i++) {
                if ((i < 2) && (dimCombos[i] != null)) {
                    dimCombos[i].getSelectionModel().select(datasetAttr.dim[i]);
                }
                axisLabels[i].setText(chart.axModes[i].getDatasetLabel(datasetAttr, i));
            }
            start = chart.getNDim();
        }
        for (int i = start; i < limitFields.length; i++) {
            limitFields[i][0].set("");
            limitFields[i][1].set("");
            axisLabels[i].setText("");

        }
    }

    private void dimAction(String rowName, Event e) {
        DatasetAttributes datasetAttr = (DatasetAttributes) chart.datasetAttributesList.get(0);
        ComboBox cBox = (ComboBox) e.getSource();
        int iDim = cBox.getSelectionModel().getSelectedIndex();
        datasetAttr.setDim(rowName, iDim);
        for (int i = 0; i < chart.getNDim(); i++) {
            if (i < 2) {
                dimCombos[i].getSelectionModel().select(datasetAttr.dim[i]);
            } else {
                dimLabels[i].setText(String.valueOf(datasetAttr.dim[i] + 1));
            }
            axisLabels[i].setText(chart.axModes[i].getDatasetLabel(datasetAttr, i));
            // fixme  should be able to swap existing limits, not go to full
            chart.full(i);
        }
    }

    public void bindToChart(PolyChart polyChart) {
        for (int i = 0; i < 2; i++) {
            NMRAxis axis;
            if (i == 0) {
                axis = polyChart.xAxis;
            } else {
                axis = polyChart.yAxis;
            }
            //limitFields[i][0].unbind();
//            limitFields[i][0].bind(axis.lowerBoundProperty().asString());
            //limitFields[i][1].unbind();
//            limitFields[i][1].bind(axis.upperBoundProperty().asString());
        }
        polyChart.sliceAttributes.offsetTrackingProperty().bindBidirectional(offsetTrackingCheckBox.selectedProperty());
        polyChart.sliceAttributes.useDatasetColorProperty().bindBidirectional(useDatasetColorCheckBox.selectedProperty());
        polyChart.sliceAttributes.slice1StateProperty().bindBidirectional(slice1StateCheckBox.selectedProperty());
        polyChart.sliceAttributes.slice2StateProperty().bindBidirectional(slice2StateCheckBox.selectedProperty());
        polyChart.sliceAttributes.offsetXValueProperty().bindBidirectional(xOffsetSlider.valueProperty());
        polyChart.sliceAttributes.offsetYValueProperty().bindBidirectional(yOffsetSlider.valueProperty());
        polyChart.sliceAttributes.scaleValueProperty().bindBidirectional(scaleSlider.valueProperty());
        slice1ColorPicker.setValue(polyChart.sliceAttributes.slice1ColorProperty().get());
        slice2ColorPicker.setValue(polyChart.sliceAttributes.slice2ColorProperty().get());
        if (chart.chartProps.getAxesColor() == null) {
            axisColorCheckBox.setSelected(false);
        } else {
            axisColorPicker.setValue(polyChart.chartProps.axesColorProperty().get());
        }
        if (chart.chartProps.getBgColor() == null) {
            bgColorCheckBox.setSelected(false);
        } else {
            bgColorPicker.setValue(polyChart.chartProps.bgColorProperty().get());
        }
        if (chart.chartProps.getCross0Color() == null) {
            cross0ColorCheckBox.setSelected(false);
        } else {
            cross0ColorPicker.setValue(polyChart.chartProps.cross0ColorProperty().get());
        }
        if (chart.chartProps.getCross1Color() == null) {
            cross1ColorCheckBox.setSelected(false);
        } else {
            cross1ColorPicker.setValue(polyChart.chartProps.cross1ColorProperty().get());
        }
        ticFontSizeComboBox.setValue(polyChart.chartProps.getTicFontSize());
        labelFontSizeComboBox.setValue(polyChart.chartProps.getLabelFontSize());
        leftBorderSizeComboBox.setValue(polyChart.chartProps.getLeftBorderSize());
        rightBorderSizeComboBox.setValue(polyChart.chartProps.getRightBorderSize());
        topBorderSizeComboBox.setValue(polyChart.chartProps.getTopBorderSize());
        bottomBorderSizeComboBox.setValue(polyChart.chartProps.getBottomBorderSize());
        gridCheckBox.setSelected(polyChart.chartProps.getGrid());
        integralPosSlider.setLowValue(polyChart.chartProps.getIntegralLowPos());
        integralPosSlider.setHighValue(polyChart.chartProps.getIntegralHighPos());
        integralCheckBox.setSelected(chart.chartProps.getIntegrals());
        regionCheckBox.setSelected(chart.chartProps.getRegions());
        titlesCheckBox.setSelected(polyChart.chartProps.getTitles());
        DISDIM curDisDim = polyChart.disDimProp.get();
        disDimCombo.setValue(curDisDim);
        // polyChart.disDimProp.bindBidirectional(disDimCombo.valueProperty());
    }

    void unifyWidth(boolean pos) {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        items.stream().forEach((dataAttr) -> {
            if (pos) {
                dataAttr.setPosWidth(items.get(0).getPosWidth());
            } else {
                dataAttr.setNegWidth(items.get(0).getNegWidth());

            }
        });

    }

    int getSelected() {
        int selected = datasetTableView.getSelectionModel().getSelectedIndex();
        if (selected == -1) {
            selected = 0;
        }
        return selected;

    }

    void unifyLevel() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        if (!items.isEmpty()) {
            int index = datasetTableView.getSelectionModel().getSelectedIndex();
            index = index == -1 ? 0 : index;
            final double lvl = items.get(index).getLvl();
            items.stream().forEach((dataAttr) -> {
                dataAttr.setLvl(lvl);
            });
            datasetTableView.refresh();
        }
    }

    void unifyCLM() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        if (!items.isEmpty()) {
            int index = datasetTableView.getSelectionModel().getSelectedIndex();
            index = index == -1 ? 0 : index;
            final double clm = items.get(index).getClm();
            items.stream().forEach((dataAttr) -> {
                dataAttr.setClm(clm);
            });
            datasetTableView.refresh();
        }
    }

    void unifyNLvl() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        if (!items.isEmpty()) {
            int index = datasetTableView.getSelectionModel().getSelectedIndex();
            index = index == -1 ? 0 : index;
            final int nlvls = items.get(index).getNlvls();
            items.stream().forEach((dataAttr) -> {
                dataAttr.setNlvls(nlvls);
            });
            datasetTableView.refresh();
        }
    }

    void unifyOffset() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        items.stream().forEach((dataAttr) -> {
            dataAttr.setOffset(items.get(0).getOffset());
        });
        datasetTableView.refresh();

    }

    void rampOffset() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        int nItems = items.size();
        if (nItems > 0) {
            double offset = items.get(0).getOffset();
            double offsetIncr = 0.0;
            if (nItems > 1) {
                offsetIncr = 0.8 / (nItems);
            }
            for (DatasetAttributes dataAttr : items) {
                dataAttr.setOffset(offset);
                offset += offsetIncr;
            }
        }
        datasetTableView.refresh();

    }

    void setDrawStatus(boolean pos, final boolean state) {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        items.stream().forEach((dataAttr) -> {
            if (pos) {
                dataAttr.setPos(state);
            } else {
                dataAttr.setNeg(state);

            }
        });
        datasetTableView.refresh();
    }

    void unifyColor(boolean pos) {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        items.stream().forEach((dataAttr) -> {
            if (pos) {
                dataAttr.setPosColor(items.get(0).getPosColor());
            } else {
                dataAttr.setNegColor(items.get(0).getNegColor());

            }
        });
        datasetTableView.refresh();
        chart.refresh();

    }

    void setLimits() {
        int i = 0;
        for (NMRAxis axis : chart.axes) {
            double lower = axis.getLowerBound();
            double upper = axis.getUpperBound();
            limitFields[i][0].setValue(formatter.format(lower));
            limitFields[i][1].setValue(formatter.format(upper));
            labelFields[i].setText(chart.axModes[i].name());
            if (i > 1) {
                if (!chart.getDatasetAttributes().isEmpty()) {
                    DatasetAttributes dataAttr = chart.getDatasetAttributes().get(0);
                    int lowPt = chart.axModes[i].getIndex(dataAttr, i, lower);
                    int upPt = chart.axModes[i].getIndex(dataAttr, i, upper);

                    int center = ((lowPt + upPt) / 2);
                    chart.controller.getStatusBar().updatePlaneSpinner(center, i);
                }
            }
            i++;
        }
    }

    void unifyNPlanes() {
        ObservableList<PeakListAttributes> items = peakListTableView.getItems();
        if (!items.isEmpty()) {
            int index = peakListTableView.getSelectionModel().getSelectedIndex();
            index = index == -1 ? 0 : index;
            final int nlvls = items.get(index).getNplanes();
            items.stream().forEach((peakAttr) -> {
                peakAttr.setNplanes(nlvls);
            });
            peakListTableView.refresh();
        }
    }

    void attachDatasets() {

    }

    void removeDatasets() {

    }

    Color interpolateColor(Color color1, Color color2, double f) {
        double hue1 = color1.getHue();
        double hue2 = color2.getHue();
        double sat1 = color1.getSaturation();
        double sat2 = color2.getSaturation();
        double bright1 = color1.getBrightness();
        double bright2 = color2.getBrightness();
        double hue = (1.0 - f) * hue1 + f * hue2;
        double sat = (1.0 - f) * sat1 + f * sat2;
        double bright = (1.0 - f) * bright1 + f * bright2;
        Color color = Color.hsb(hue, sat, bright);
        return color;

    }

    void interpolatePosColors() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        if (items.size() < 2) {
            return;
        }
        Color color1 = items.get(0).getPosColor();
        Color color2 = items.get(items.size() - 1).getPosColor();
        double delta = 1.0 / (items.size() - 1);
        double f = 0.0;
        for (DatasetAttributes dataAttr : items) {
            Color color = interpolateColor(color1, color2, f);
            f += delta;
            dataAttr.setPosColor(color);
        }
        datasetTableView.refresh();
        chart.refresh();
    }

    void interpolateNegColors() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        if (items.size() < 2) {
            return;
        }
        Color color1 = items.get(0).getNegColor();
        Color color2 = items.get(items.size() - 1).getNegColor();
        double delta = 1.0 / (items.size() - 1);
        double f = 0.0;
        for (DatasetAttributes dataAttr : items) {
            Color color = interpolateColor(color1, color2, f);
            f += delta;
            dataAttr.setNegColor(color);
        }
        datasetTableView.refresh();
        chart.refresh();

    }

    void setPosColorsToSchema() {
        double x = stage.getX();
        double y = stage.getY() + stage.getHeight() + 10;
        ColorSchemes.showSchemaChooser(s -> updatePosColorsWithSchema(s), x, y);
    }

    void setNegColorsToSchema() {
        double x = stage.getX();
        double y = stage.getY() + stage.getHeight() + 10;
        ColorSchemes.showSchemaChooser(s -> updateNegColorsWithSchema(s), x, y);
    }

    public void updateNegColorsWithSchema(String colorName) {
        updateColorsWithSchema(colorName, false);

    }

    public void updatePosColorsWithSchema(String colorName) {
        updateColorsWithSchema(colorName, true);
    }

    public void updateColorsWithSchema(String colorName, boolean posColors) {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        if (items.size() < 2) {
            return;
        }
        int i = 0;
        List<Color> colors = ColorSchemes.getColors(colorName, items.size());
        for (DatasetAttributes dataAttr : items) {
            Color color = colors.get(i++);
            if (posColors) {
                dataAttr.setPosColor(color);
            } else {
                dataAttr.setNegColor(color);
            }
        }
        datasetTableView.refresh();
        chart.refresh();
    }

    void saveParameters() {
        ObservableList<DatasetAttributes> items = datasetTableView.getItems();
        items.stream().forEach((dataAttr) -> {
            Dataset dataset = dataAttr.getDataset();
            double lvl = dataAttr.getLvl();
            double scale = dataset.getScale();
            double tlvl = scale * lvl;
            if (tlvl > 1.0e5) {
                scale = 1.0e6;
                lvl = Math.round(tlvl / scale * 1.0e3) / 1.0e3;
            } else if (tlvl > 1.0e2) {
                scale = 1.0e3;
                lvl = Math.round(tlvl / scale * 1.0e3) / 1.0e3;
            } else {
                scale = 1.0;
                lvl = tlvl;
            }
            dataset.setLvl(lvl);
            dataset.setScale(scale);
            dataset.setPosColor(dataAttr.getPosColor().toString());
            dataset.setNegColor(dataAttr.getNegColor().toString());
            boolean posOn = dataAttr.getPos();
            boolean negOn = dataAttr.getNeg();
            int posNeg = 0;
            if (posOn) {
                posNeg += 1;
            }
            if (negOn) {
                posNeg += 2;
            }
            dataset.setPosneg(posNeg);
            dataset.writeParFile();
        });
    }

    void autoScale() {
        if (isSceneMode()) {
            List<PolyChart> charts = chart.getSceneMates(true);
            charts.stream().forEach(c -> {
                c.autoScale();
                c.refresh();
            });
        } else {
            ObservableList<DatasetAttributes> items = datasetTableView.getSelectionModel().getSelectedItems();
            if (items.isEmpty()) {
                items = datasetTableView.getItems();
            }
            // fixme  kluge because scaling 1D chart multiple times keeps changing scale
            // not so bad for autoScale, but bad for adjustScale
            int limit = chart.is1D() ? 1 : 1000;
            items.stream().limit(limit).forEach(dataAttr -> {
                chart.autoScale(dataAttr);
            });
            chart.refresh();
        }
    }

    void adjustScale(double factor) {
        if (isSceneMode()) {
            List<PolyChart> charts = chart.getSceneMates(true);
            charts.stream().forEach(c -> {
                c.adjustScale(factor);
                c.refresh();
            });
        } else {
            if (chart.is1D()) {

            }
            ObservableList<DatasetAttributes> items = datasetTableView.getSelectionModel().getSelectedItems();
            if (items.isEmpty()) {
                items = datasetTableView.getItems();
            }
            // fixme  kluge because scaling 1D chart multiple times keeps changing scale
            int limit = chart.is1D() ? 1 : 1000;
            items.stream().limit(limit).forEach(dataAttr -> {
                chart.adjustScale(dataAttr, factor);
            });
            chart.refresh();
        }
    }

    public Node makeColumnIcon() {
        StackPane stackPane = new StackPane();
        Polygon polygon = new Polygon();
        polygon.getPoints().addAll(new Double[]{
            4.0, 9.0,
            0.0, 0.0,
            9.0, 0.0});
        polygon.setFill(Color.GREEN);
        stackPane.getChildren().add(polygon);
        return stackPane;
    }

    private void copyProps() {
        List<PolyChart> charts = chart.getController().getCharts();
        for (PolyChart polyChart : charts) {
            if (chart != polyChart) {
                chart.chartProps.copyTo(polyChart);
            }
        }
        chart.getController().draw();
    }

}
