/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.peaks.Analyzer;
import org.nmrfx.processor.datasets.peaks.ComplexCoupling;
import org.nmrfx.processor.datasets.peaks.Coupling;
import org.nmrfx.processor.datasets.peaks.CouplingPattern;
import org.nmrfx.processor.datasets.peaks.Multiplet;
import org.nmrfx.processor.datasets.peaks.Multiplets;
import org.nmrfx.processor.datasets.peaks.Peak;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.processor.datasets.peaks.Singlet;
import org.nmrfx.processor.gui.spectra.CrossHairs;
import org.nmrfx.processor.gui.spectra.DatasetAttributes;
import org.nmrfx.processor.gui.spectra.MultipletSelection;
import org.nmrfx.processor.gui.spectra.PeakListAttributes;
import static org.nmrfx.utils.GUIUtils.affirm;
import static org.nmrfx.utils.GUIUtils.warn;

/**
 *
 * @author brucejohnson
 */
public class MultipletController implements Initializable, SetChangeListener<MultipletSelection> {

    Stage stage = null;
    HBox navigatorToolBar;
    TextField multipletIdField;
    @FXML
    HBox menuBar;
    @FXML
    HBox toolBar;
    @FXML
    HBox regionToolBar;
    @FXML
    HBox peakToolBar;
    @FXML
    HBox multipletToolBar;
    @FXML
    HBox fittingToolBar;
    @FXML
    HBox integralToolBar;
    @FXML
    HBox typeToolBar;
    @FXML
    GridPane gridPane;
    @FXML
    Button splitButton;
    @FXML
    Button splitRegionButton;
    @FXML
    ChoiceBox<String> peakTypeChoice;
    ChoiceBox<String>[] patternChoices;
    TextField integralField;
    TextField[] couplingFields;
    TextField[] slopeFields;
    private PolyChart chart;
    Optional<Multiplet> activeMultiplet = Optional.empty();
    boolean ignoreCouplingChanges = false;
    ChangeListener<String> patternListener;
    Analyzer analyzer = null;

    public MultipletController() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String[] patterns = {"d", "t", "q", "p", "h", "dd", "ddd", "dddd"};
        int nCouplings = 5;
        double width1 = 30;
        double width2 = 80;
        double width3 = 60;
        patternChoices = new ChoiceBox[nCouplings];
        couplingFields = new TextField[nCouplings];
        slopeFields = new TextField[nCouplings];
        Button doubletButton = new Button("To Doublets");
        doubletButton.setOnAction(e -> toDoublets());
        gridPane.add(doubletButton, 0, 0, 2, 1);
        for (int iRow = 0; iRow < nCouplings; iRow++) {
            patternChoices[iRow] = new ChoiceBox<>();
            patternChoices[iRow].setPrefWidth(width2);
            if (iRow == 0) {
                patternChoices[iRow].getItems().add("");
                patternChoices[iRow].getItems().add("m");
                patternChoices[iRow].getItems().add("s");
            } else {
                patternChoices[iRow].getItems().add("");
            }
            patternChoices[iRow].getItems().addAll(patterns);
            patternChoices[iRow].setValue(patternChoices[iRow].getItems().get(0));
            couplingFields[iRow] = new TextField();
            slopeFields[iRow] = new TextField();
            couplingFields[iRow].setPrefWidth(width3);
            slopeFields[iRow].setPrefWidth(width3);
            gridPane.add(patternChoices[iRow], 0, iRow + 1);
            gridPane.add(couplingFields[iRow], 1, iRow + 1);
            gridPane.add(slopeFields[iRow], 2, iRow + 1);
        }
        initMenus();
        initNavigator(toolBar);
        initTools();
        patternListener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                couplingChanged();
            }
        };
        addPatternListener();
        FXMLController controller = FXMLController.getActiveController();
        controller.selPeaks.addListener(e -> setActivePeaks(controller.selPeaks.get()));

    }

    public void initMenus() {
        MenuButton menu = new MenuButton("Actions");
        menuBar.getChildren().add(menu);
        MenuItem analyzeMenuItem = new MenuItem("Analyze");
        analyzeMenuItem.setOnAction(e -> analyze1D());

        MenuItem pickRegionsMenuItem = new MenuItem("Pick Regions");
        pickRegionsMenuItem.setOnAction(e -> pickRegions());

        MenuItem clearMenuItem = new MenuItem("Clear");
        clearMenuItem.setOnAction(e -> clearAnalysis());

        MenuItem thresholdMenuItem = new MenuItem("Set Threshold");
        thresholdMenuItem.setOnAction(e -> setThreshold());

        MenuItem clearThresholdMenuItem = new MenuItem("Clear Threshold");
        clearThresholdMenuItem.setOnAction(e -> clearThreshold());

        menu.getItems().addAll(analyzeMenuItem, pickRegionsMenuItem, clearMenuItem, thresholdMenuItem, clearThresholdMenuItem);
    }

    public void initNavigator(HBox toolBar) {
        this.navigatorToolBar = toolBar;
        multipletIdField = new TextField();
        multipletIdField.setMinWidth(35);
        multipletIdField.setMaxWidth(35);
        multipletIdField.setPrefWidth(35);

        String iconSize = "12px";
        String fontSize = "7pt";
        ArrayList<Button> buttons = new ArrayList<>();
        Button bButton;

        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.FAST_BACKWARD, "", iconSize, fontSize, ContentDisplay.GRAPHIC_ONLY);
        bButton.setOnAction(e -> firstMultiplet(e));
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.BACKWARD, "", iconSize, fontSize, ContentDisplay.GRAPHIC_ONLY);
        bButton.setOnAction(e -> previousMultiplet(e));
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.FORWARD, "", iconSize, fontSize, ContentDisplay.GRAPHIC_ONLY);
        bButton.setOnAction(e -> nextMultiplet(e));
        buttons.add(bButton);
        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.FAST_FORWARD, "", iconSize, fontSize, ContentDisplay.GRAPHIC_ONLY);
        bButton.setOnAction(e -> lastMultiplet(e));
        buttons.add(bButton);
        Button deleteButton = GlyphsDude.createIconButton(FontAwesomeIcon.BAN, "", fontSize, iconSize, ContentDisplay.GRAPHIC_ONLY);

        // prevent accidental activation when inspector gets focus after hitting space bar on peak in spectrum
        // a second space bar hit would activate
        deleteButton.setOnKeyPressed(e -> e.consume());
        deleteButton.setOnAction(e -> deleteMultiplet());

        toolBar.getChildren().addAll(buttons);
        toolBar.getChildren().add(multipletIdField);
        toolBar.getChildren().add(deleteButton);
        HBox spacer = new HBox();
        toolBar.getChildren().add(spacer);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        multipletIdField.setOnKeyReleased(kE -> {
            if (null != kE.getCode()) {
                switch (kE.getCode()) {
                    case ENTER:
                        gotoPeakId(multipletIdField);
                        break;
                    default:
                        break;
                }
            }
        });

    }

    ImageView getIcon(String name) {
        Image imageIcon = new Image("/images/" + name + ".png", true);
        ImageView imageView = new ImageView(imageIcon);
        return imageView;
    }

    void initTools() {
        Font font = new Font(7);
        List<Button> peakButtons = new ArrayList<>();
        List<Button> regionButtons = new ArrayList<>();
        List<Button> multipletButtons = new ArrayList<>();
        List<Button> fitButtons = new ArrayList<>();
        Button button;

        button = new Button("Add", getIcon("region_add"));
        button.setOnAction(e -> addRegion());
        regionButtons.add(button);

        button = new Button("Adjust", getIcon("region_adjust"));
        button.setOnAction(e -> adjustRegion());
        regionButtons.add(button);

        button = new Button("Split", getIcon("region_split"));
        button.setOnAction(e -> splitRegion());
        regionButtons.add(button);

        button = new Button("Delete", getIcon("region_delete"));
        button.setOnAction(e -> removeRegion());
        regionButtons.add(button);

        button = new Button("Add 1", getIcon("peak_add1"));
        button.setOnAction(e -> addPeak());
        peakButtons.add(button);

        button = new Button("Add 2", getIcon("peak_add2"));
        button.setOnAction(e -> addTwoPeaks());
        peakButtons.add(button);

        button = new Button("AutoAdd", getIcon("peak_auto"));
        button.setOnAction(e -> addAuto());
        peakButtons.add(button);

        button = new Button("Delete", getIcon("editdelete"));
        button.setOnAction(e -> removeWeakPeak());
        peakButtons.add(button);

        button = new Button("Extract", getIcon("extract"));
        button.setOnAction(e -> extractMultiplet());
        multipletButtons.add(button);

        button = new Button("Merge", getIcon("merge"));
        button.setOnAction(e -> mergePeaks());
        multipletButtons.add(button);

        button = new Button("Transfer", getIcon("transfer"));
        button.setOnAction(e -> transferPeaks());
        multipletButtons.add(button);

        button = new Button("Fit", getIcon("reload"));
        button.setOnAction(e -> fitSelected());
        fitButtons.add(button);

        button = new Button("BICFit", getIcon("reload"));
        button.setOnAction(e -> objectiveDeconvolution());
        fitButtons.add(button);

        for (Button button1 : regionButtons) {
            button1.setContentDisplay(ContentDisplay.TOP);
            button1.setFont(font);
            button1.getStyleClass().add("toolButton");
            regionToolBar.getChildren().add(button1);
        }
        for (Button button1 : peakButtons) {
            button1.setContentDisplay(ContentDisplay.TOP);
            button1.setFont(font);
            button1.getStyleClass().add("toolButton");
            peakToolBar.getChildren().add(button1);
        }
        for (Button button1 : multipletButtons) {
            button1.setContentDisplay(ContentDisplay.TOP);
            button1.setFont(font);
            button1.getStyleClass().add("toolButton");
            multipletToolBar.getChildren().add(button1);
        }
        for (Button button1 : fitButtons) {
            button1.setContentDisplay(ContentDisplay.TOP);
            button1.setFont(font);
            button1.getStyleClass().add("toolButton");
            fittingToolBar.getChildren().add(button1);
        }
        Label integralLabel = new Label("N:");
        integralLabel.setPrefWidth(80);
        integralField = new TextField();
        integralField.setPrefWidth(120);
        integralToolBar.getChildren().addAll(integralLabel, integralField);

        integralField.setOnKeyReleased(k -> {
            if (k.getCode() == KeyCode.ENTER) {
                try {
                    double value = Double.parseDouble(integralField.getText().trim());
                    activeMultiplet.ifPresent(m -> {
                        double volume = m.getVolume();
                        PeakList peakList = m.getOrigin().getPeakList();
                        peakList.scale = volume / value;
                        refresh();
                    });
                } catch (NumberFormatException nfE) {

                }

            }
        });

        Label peakTypeLabel = new Label("Type: ");
        peakTypeLabel.setPrefWidth(80);
        peakTypeChoice = new ChoiceBox();
        typeToolBar.getChildren().addAll(peakTypeLabel, peakTypeChoice);
        peakTypeChoice.getItems().addAll(Peak.getPeakTypes());
        peakTypeChoice.valueProperty().addListener(e -> setPeakType());
        peakTypeChoice.setPrefWidth(120);

        /*
extract.png				region_add.png		wizard
merge.png				region_adjust.png
		region_delete.png


         */
    }

    public Analyzer getAnalyzer() {
        if (analyzer == null) {
            chart = PolyChart.getActiveChart();
            Dataset dataset = chart.getDataset();
            if ((dataset == null) || (dataset.getNDim() > 1)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Chart must have a 1D dataset");
                alert.showAndWait();
                return null;
            }
            analyzer = new Analyzer(dataset);
        }
        return analyzer;
    }

    private void analyze1D() {
        Analyzer analyzer = getAnalyzer();
        if (analyzer != null) {
            try {
                analyzer.analyze();
                PeakList peakList = analyzer.getPeakList();
                List<String> peakListNames = new ArrayList<>();
                peakListNames.add(peakList.getName());
                chart.chartProps.setRegions(false);
                chart.chartProps.setIntegrals(true);
                chart.updatePeakLists(peakListNames);
            } catch (IOException ex) {
                Logger.getLogger(AnalystApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void pickRegions() {
        Analyzer analyzer = getAnalyzer();
        if (analyzer != null) {
            analyzer.peakPickRegions();
            PeakList peakList = analyzer.getPeakList();
            List<String> peakListNames = new ArrayList<>();
            peakListNames.add(peakList.getName());
            chart.chartProps.setRegions(false);
            chart.chartProps.setIntegrals(true);
            chart.updatePeakLists(peakListNames);
        }
    }

    private void clearAnalysis() {
        Analyzer analyzer = getAnalyzer();
        if (analyzer != null) {
            if (affirm("Clear Analysis")) {
                PeakList peakList = analyzer.getPeakList();
                PeakList.remove(peakList.getName());
                chart.chartProps.setRegions(false);
                chart.chartProps.setIntegrals(false);
                chart.refresh();
            }
        }
    }

    private void clearThreshold() {
        if (analyzer != null) {
            analyzer.clearThreshold();
        }
    }

    private void setThreshold() {
        Analyzer analyzer = getAnalyzer();
        if (analyzer != null) {
            CrossHairs crossHairs = chart.getCrossHairs();
            if (!crossHairs.hasCrosshairState("h0")) {
                warn("Threshold", "Must have horizontal crosshair");
                return;
            }
            Double[] pos = crossHairs.getCrossHairPositions(0);
            System.out.println(pos[0] + " " + pos[1]);
            analyzer.setThreshold(pos[1]);
        }
    }

    void deleteMultiplet() {

    }

    public void initMultiplet() {
        if (!gotoSelectedMultiplet()) {
            List<Peak> peaks = getPeaks();
            if (!peaks.isEmpty()) {
                Multiplet m = peaks.get(0).getPeakDim(0).getMultiplet();
                activeMultiplet = Optional.of(m);
                updateMultipletField(false);
            }
        }
    }

    public boolean gotoSelectedMultiplet() {
        boolean result = false;
        List<MultipletSelection> multiplets = chart.getSelectedMultiplets();
        if (!multiplets.isEmpty()) {
            Multiplet m = multiplets.get(0).getMultiplet();
            activeMultiplet = Optional.of(m);
            updateMultipletField(false);
            result = true;
        }
        return result;
    }

    List<Peak> getPeaks() {
        List<Peak> peaks = Collections.EMPTY_LIST;
        Optional<PeakList> peakListOpt = getPeakList();
        if (peakListOpt.isPresent()) {
            PeakList peakList = peakListOpt.get();
            peaks = peakList.peaks();
        }
        return peaks;
    }

    void updateMultipletField() {
        updateMultipletField(true);
    }

    void updateMultipletField(boolean resetView) {
        if (activeMultiplet.isPresent()) {
            Multiplet multiplet = activeMultiplet.get();
            multipletIdField.setText(String.valueOf(multiplet.getIDNum()));
            if (resetView) {
                refreshPeakView(multiplet);
            }
            String mult = multiplet.getMultiplicity();
            Coupling coup = multiplet.getCoupling();
            updateCouplingChoices(coup);
            String peakType = Peak.typeToString(multiplet.getOrigin().getType());
            peakTypeChoice.setValue(peakType);
            double scale = multiplet.getOrigin().getPeakList().scale;
            double value = multiplet.getVolume() / scale;
            integralField.setText(String.format("%.2f", value));
//            if (multiplet.isGenericMultiplet()) {
//                splitButton.setDisable(true);
//            } else {
//                splitButton.setDisable(false);
//            }
        } else {
            multipletIdField.setText("");
        }
    }

    void setPeakType() {
        activeMultiplet.ifPresent(m -> {
            String peakType = peakTypeChoice.getValue();
            m.getOrigin().setType(Peak.getType(peakType));
        });

    }

    void clearPatternListener() {
        for (ChoiceBox<String> cBox : patternChoices) {
            cBox.valueProperty().removeListener(patternListener);
        }
    }

    void addPatternListener() {
        for (ChoiceBox<String> cBox : patternChoices) {
            cBox.valueProperty().addListener(patternListener);
        }
    }

    void clearCouplingChoices() {
        for (int i = 0; i < patternChoices.length; i++) {
            patternChoices[i].setValue("");
            couplingFields[i].setText("");
            slopeFields[i].setText("");
        }
        ignoreCouplingChanges = false;
    }

    void updateCouplingChoices(Coupling coup) {
        clearPatternListener();
        String[] couplingNames = {"", "s", "d", "t", "q", "p", "h"};
        clearCouplingChoices();
        if (coup instanceof ComplexCoupling) {
            patternChoices[0].setValue("m");
        } else if (coup instanceof CouplingPattern) {
            CouplingPattern couplingPattern = (CouplingPattern) coup;
            double[] values = couplingPattern.getValues();
            double[] slopes = couplingPattern.getSin2Thetas();
            int[] nCoup = couplingPattern.getNValues();
            for (int i = 0; i < values.length; i++) {
                couplingFields[i].setText(String.format("%.2f", values[i]));
                slopeFields[i].setText(String.format("%.2f", slopes[i]));
                patternChoices[i].setValue(couplingNames[nCoup[i]]);
            }
        } else if (coup instanceof Singlet) {
            patternChoices[0].setValue("s");
        }
        addPatternListener();
    }

    int getCurrentIndex() {
        int id = activeMultiplet.get().getPeakDim().getPeak().getIndex();
        return id;
    }

    void firstMultiplet(ActionEvent e) {
        List<Peak> peaks = getPeaks();
        if (!peaks.isEmpty()) {
            activeMultiplet = Optional.of(peaks.get(0).getPeakDim(0).getMultiplet());
        } else {
            activeMultiplet = Optional.empty();
        }
        updateMultipletField();
    }

    void previousMultiplet(ActionEvent e) {
        if (activeMultiplet.isPresent()) {
            int id = getCurrentIndex();
            id--;
            if (id < 0) {
                id = 0;
            }
            List<Peak> peaks = getPeaks();
            activeMultiplet = Optional.of(peaks.get(id).getPeakDim(0).getMultiplet());
            updateMultipletField();
        } else {
            firstMultiplet(e);
        }

    }

    void gotoPrevious(int id) {
        id--;
        if (id < 0) {
            id = 0;
        }
        List<Peak> peaks = getPeaks();
        activeMultiplet = Optional.of(peaks.get(id).getPeakDim(0).getMultiplet());
        updateMultipletField(false);

    }

    void nextMultiplet(ActionEvent e) {
        if (activeMultiplet.isPresent()) {
            List<Peak> peaks = getPeaks();
            int id = getCurrentIndex();
            int last = peaks.size() - 1;
            id++;
            if (id > last) {
                id = last;
            }
            activeMultiplet = Optional.of(peaks.get(id).getPeakDim(0).getMultiplet());
            updateMultipletField();
        } else {
            firstMultiplet(e);
        }
    }

    void lastMultiplet(ActionEvent e) {
        List<Peak> peaks = getPeaks();
        if (!peaks.isEmpty()) {
            activeMultiplet = Optional.of(peaks.get(peaks.size() - 1).getPeakDim(0).getMultiplet());
        }
        updateMultipletField();
    }

    void gotoPeakId(TextField textField) {

    }

    public static MultipletController create() {
        FXMLLoader loader = new FXMLLoader(MinerController.class.getResource("/fxml/MultipletScene.fxml"));
        MultipletController controller = null;
        Stage stage = new Stage(StageStyle.DECORATED);
        try {
            Scene scene = new Scene((BorderPane) loader.load());
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");

            controller = loader.<MultipletController>getController();
            controller.stage = stage;
            stage.setTitle("Multiplets");
            stage.setScene(scene);
            stage.setMinWidth(200);
            stage.setMinHeight(475);
            stage.show();
            stage.toFront();
            controller.chart = controller.getChart();
            controller.chart.addMultipletListener(controller);
            controller.initMultiplet();

        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }
        return controller;
    }

    public Stage getStage() {
        return stage;
    }

    Optional<PeakList> getPeakList() {
        Optional<PeakList> peakListOpt = Optional.empty();
        List<PeakListAttributes> attrs = chart.getPeakListAttributes();
        if (!attrs.isEmpty()) {
            peakListOpt = Optional.of(attrs.get(0).getPeakList());
        } else {
            Analyzer analyzer = getAnalyzer();
            PeakList peakList = analyzer.getPeakList();
            if (peakList != null) {
                List<String> peakListNames = new ArrayList<>();
                peakListNames.add(peakList.getName());
                chart.updatePeakLists(peakListNames);
                attrs = chart.getPeakListAttributes();
                peakListOpt = Optional.of(attrs.get(0).getPeakList());
            }
        }
        return peakListOpt;

    }

    PolyChart getChart() {
        FXMLController controller = FXMLController.getActiveController();
        PolyChart activeChart = controller.getActiveChart();
        return activeChart;
    }

    void refresh() {
        chart.refresh();
        updateMultipletField(false);

    }

    List<MultipletSelection> getMultipletSelection() {
        FXMLController controller = FXMLController.getActiveController();
        List<MultipletSelection> multiplets = chart.getSelectedMultiplets();
        return multiplets;
    }

    public void fitSelected() {
        Analyzer analyzer = getAnalyzer();
        activeMultiplet.ifPresent(m -> {
            Optional<Double> result = analyzer.fitMultiplet(m);
        });
        refresh();
    }

    public void splitSelected() {
        activeMultiplet.ifPresent(m -> {
            if (m.isGenericMultiplet()) {
            } else {
                Multiplets.splitToMultiplicity(m, "d");
                Multiplets.updateAfterMultipletConversion(m);
            }
        });
        refresh();
    }

    public void splitRegion() {
        double ppm = chart.getVerticalCrosshairPositions()[0];
        Analyzer analyzer = getAnalyzer();
        try {
            List<Multiplet> multiplets = analyzer.splitRegion(ppm);
            if (!multiplets.isEmpty()) {
                activeMultiplet = Optional.of(multiplets.get(0));
            } else {
                activeMultiplet = Optional.empty();
            }
            updateMultipletField(false);
        } catch (IOException ex) {
        }
        chart.refresh();
    }

    public void adjustRegion() {
        Analyzer analyzer = getAnalyzer();
        double ppm0 = chart.getVerticalCrosshairPositions()[0];
        double ppm1 = chart.getVerticalCrosshairPositions()[1];
        analyzer.removeRegion((ppm0 + ppm1) / 2);
        analyzer.addRegion(ppm0, ppm1);
        try {
            activeMultiplet = analyzer.analyzeRegion((ppm0 + ppm1) / 2);
            updateMultipletField(false);
            chart.refresh();
        } catch (IOException ex) {
        }
    }

    public void addRegion() {
        Analyzer analyzer = getAnalyzer();
        double ppm0 = chart.getVerticalCrosshairPositions()[0];
        double ppm1 = chart.getVerticalCrosshairPositions()[1];
        analyzer.addRegion(ppm0, ppm1);
        try {
            activeMultiplet = analyzer.analyzeRegion((ppm0 + ppm1) / 2);
            // this will force updating peaklist and adding to chart if not there
            Optional<PeakList> peakListOpt = getPeakList();
            if (peakListOpt.isPresent()) {
                PeakList peakList = peakListOpt.get();
                if (peakList.peaks().size() == 1) {
                    double volume = activeMultiplet.get().getVolume();
                    peakList.scale = volume;
                }
            }
            updateMultipletField(false);
            chart.refresh();

        } catch (IOException ex) {
        }
    }

    public void removeRegion() {
        activeMultiplet.ifPresent(m -> {
            int id = m.getIDNum();
            double ppm = m.getCenter();
            Analyzer analyzer = getAnalyzer();
            analyzer.removeRegion(ppm);
            gotoPrevious(id);
            chart.refresh();
        });
    }

    public void rms() {
        activeMultiplet.ifPresent(m -> {
            Optional<Double> result = Multiplets.rms(m);
            if (result.isPresent()) {
                System.out.println("rms " + result.get());
            }
        });
    }

    public void objectiveDeconvolution() {
        activeMultiplet.ifPresent(m -> {
            analyzer.objectiveDeconvolution(m);
            chart.refresh();
            refresh();
        });
    }

    public void addAuto() {
        activeMultiplet.ifPresent(m -> {
            Optional<Double> result = Multiplets.deviation(m);
            if (result.isPresent()) {
                System.out.println("dev pos " + result.get());
                Multiplets.addPeaksToMultiplet(m, result.get());
                analyzer.fitMultiplet(m);
                chart.refresh();
                refresh();

            }

        });

    }

    public void addPeak() {
        addPeaks(false);
    }

    public void addTwoPeaks() {
        addPeaks(true);
    }

    public void addPeaks(boolean both) {
        activeMultiplet.ifPresent(m -> {
            double ppm1 = chart.getVerticalCrosshairPositions()[0];
            double ppm2 = chart.getVerticalCrosshairPositions()[1];
            if (both) {
                Multiplets.addPeaksToMultiplet(m, ppm1, ppm2);
            } else {
                Multiplets.addPeaksToMultiplet(m, ppm1);

            }
            analyzer.fitMultiplet(m);
            chart.refresh();
            refresh();
        });
    }

    void removeWeakPeak() {
        activeMultiplet.ifPresent(m -> {
            Multiplets.removeWeakPeaksInMultiplet(m, 1);
            refresh();
        });
    }

    public void toDoublets() {
        activeMultiplet.ifPresent(m -> {
            Multiplets.toDoublets(m);
        });
        refresh();
    }

    public void guessGeneric() {
        activeMultiplet.ifPresent(m -> {
            Multiplets.guessMultiplicityFromGeneric(m);
        });
        refresh();
    }

    public void extractMultiplet() {
        // fixme
//        List<Peak> peaks = chart.getSelectedPeaks();
//        if (peaks.size() > 0) {
//            Peak peak0 = peaks.get(0);
//            List<PeakDim> peakDims = peak0.getPeakDim(0).getCoupledPeakDims();
//            if (peaks.size() == peakDims.size()) {
//                Alert alert = new Alert(Alert.AlertType.ERROR);
//                alert.setContentText("Can't extract all peaks in multiplet");
//                alert.showAndWait();
//                return;
//            }
//            activeMultiplet = Multiplets.extractMultiplet(peaks);
//            refresh();
//        }
    }

    public void transferPeaks() {
        List<Peak> peaks = chart.getSelectedPeaks();
        if (peaks.size() > 0) {
            activeMultiplet.ifPresent(m -> {
                activeMultiplet = Multiplets.transferPeaks(m, peaks);
            });
            refresh();
        }
    }

    public void mergePeaks() {
        List<Peak> peaks = chart.getSelectedPeaks();
        if (peaks.size() > 0) {
            // fixme  activeMultiplet = Multiplets.mergePeaks(peaks);
            refresh();
        }
    }

    public void refreshPeakView(Multiplet multiplet) {
        boolean resize = false;
        if (multiplet != null) {
            double bounds = multiplet.getBoundsValue();
            double center = multiplet.getCenter();
            double widthScale = 2.5;
            if ((chart != null) && !chart.getDatasetAttributes().isEmpty()) {
                DatasetAttributes dataAttr = (DatasetAttributes) chart.getDatasetAttributes().get(0);
                Double[] ppms = {center};
                Double[] widths = {bounds * widthScale};
                if (resize && (widthScale > 0.0)) {
                    chart.moveTo(ppms, widths);
                } else {
                    chart.moveTo(ppms);
                }
            }
        }
    }

    private void couplingChanged() {
        activeMultiplet.ifPresent(m -> {
            StringBuilder sBuilder = new StringBuilder();
            for (ChoiceBox<String> choice : patternChoices) {
                String value = choice.getValue().trim();
                if (value.length() > 0) {
                    sBuilder.append(value);
                }
            }
            String multNew = sBuilder.toString();

            String multOrig = m.getMultiplicity();
            System.out.println("convert " + multOrig + " " + multNew);
            if (!multNew.equals(multOrig)) {
                Analyzer analyzer = getAnalyzer();
                Multiplets.convertMultiplicity(m, multOrig, multNew);
                analyzer.fitMultiplet(m);
                refresh();
            }
        });
    }

    @Override
    public void onChanged(Change<? extends MultipletSelection> change) {
        ObservableSet<MultipletSelection> mSet = (ObservableSet<MultipletSelection>) change.getSet();
        boolean allreadyPresent = false;
        if (!mSet.isEmpty()) {
            if (activeMultiplet.isPresent()) {
                for (MultipletSelection mSel : mSet) {
                    if (mSel.getMultiplet() == activeMultiplet.get()) {
                        // current active multiplet in selection so don't change anything
                        allreadyPresent = true;
                        break;
                    }
                }

            }
            if (!allreadyPresent) {
                for (MultipletSelection mSel : mSet) {
                    activeMultiplet = Optional.of(mSel.getMultiplet());
                    updateMultipletField(false);
                    break;
                }
            }
        }
    }

    public void setActivePeaks(List<Peak> peaks) {
        if ((peaks != null) && !peaks.isEmpty()) {
            Peak peak = peaks.get(0);
            activeMultiplet = Optional.of(peak.getPeakDim(0).getMultiplet());
            updateMultipletField(false);
        }
    }
}
