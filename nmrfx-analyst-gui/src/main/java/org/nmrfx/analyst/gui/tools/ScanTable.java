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
package org.nmrfx.analyst.gui.tools;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.table.ColumnFilter;
import org.controlsfx.control.table.TableFilter;
import org.controlsfx.dialog.ExceptionDialog;
import org.nmrfx.datasets.DatasetBase;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.DatasetException;
import org.nmrfx.processor.datasets.DatasetMerger;
import org.nmrfx.processor.datasets.vendor.NMRData;
import org.nmrfx.processor.datasets.vendor.NMRDataUtil;
import org.nmrfx.processor.gui.ChartProcessor;
import org.nmrfx.processor.gui.FXMLController;
import org.nmrfx.processor.gui.PolyChart;
import org.nmrfx.processor.gui.ProcessorController;
import org.nmrfx.processor.gui.controls.FileTableItem;
import org.nmrfx.processor.gui.spectra.DatasetAttributes;
import org.nmrfx.processor.gui.utils.ColorSchemes;
import org.nmrfx.processor.gui.utils.TableColors;
import org.nmrfx.processor.processing.Processor;
import org.nmrfx.utils.FormatUtils;
import org.nmrfx.utils.GUIUtils;
import org.nmrfx.utils.TableUtils;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Bruce Johnson
 */
public class ScanTable {

    private static final Logger log = LoggerFactory.getLogger(ScanTable.class);
    static final String PATH_COLUMN_NAME = "path";
    static final String SEQUENCE_COLUMN_NAME = "sequence";
    static final String ROW_COLUMN_NAME = "row";
    static final String ETIME_COLUMN_NAME = "etime";
    static final String NDIM_COLUMN_NAME = "ndim";
    static final String DATASET_COLUMN_NAME = "dataset";
    static final String GROUP_COLUMN_NAME = "group";
    static final String COLOR_COLUMN_NAME = "Color";
    static final String POSITIVE_COLUMN_NAME = "Positive";
    static final String NEGATIVE_COLUMN_NAME = "Negative";
    static final String SCANNER_ERROR = "Scanner Error";

    static final List<String> standardHeaders = List.of(PATH_COLUMN_NAME, SEQUENCE_COLUMN_NAME, ROW_COLUMN_NAME, ETIME_COLUMN_NAME, NDIM_COLUMN_NAME);
    static final Color[] COLORS = new Color[17];
    static final double[] hues = {0.0, 0.5, 0.25, 0.75, 0.125, 0.375, 0.625, 0.875, 0.0625, 0.1875, 0.3125, 0.4375, 0.5625, 0.6875, 0.8125, 0.9375};
    static {
        COLORS[0] = Color.BLACK;
        int i = 1;
        double brightness = 0.8;
        for (double hue : hues) {
            if (i > 8) {
                brightness = 0.5;
            } else if (i > 4) {
                brightness = 0.65;
            }
            COLORS[i++] = Color.hsb(hue * 360.0, 0.9, brightness);
        }

    }

    ScannerTool scannerTool;
    TableView<FileTableItem> tableView;
    TableFilter<FileTableItem> fileTableFilter;
    TableFilter.Builder<FileTableItem> builder = null;
    File scanDir = null;

    PopOver popOver = new PopOver();
    ObservableList<FileTableItem> fileListItems = FXCollections.observableArrayList();
    Map<String, DatasetAttributes> activeDatasetAttributes = new HashMap<>();
    HashMap<String, String> columnTypes = new HashMap<>();
    HashMap<String, String> columnDescriptors = new HashMap<>();
    boolean processingTable = false;
    Set<String> groupNames = new TreeSet<>();
    Map<String, Map<String, Integer>> groupMap = new HashMap<>();
    int groupSize = 1;
    ListChangeListener<FileTableItem> filterItemListener = c -> {
        getGroups();
        ensureAllDatasetsAdded();
        selectionChanged();
    };
    ListChangeListener<Integer> selectionListener;

    TableColumn<FileTableItem, Color> posColorCol = new TableColumn<>(COLOR_COLUMN_NAME);
    TableColumn<FileTableItem, Color> negColorCol = new TableColumn<>(COLOR_COLUMN_NAME);
    TableColumn<FileTableItem, Boolean> posDrawOnCol;
    TableColumn<FileTableItem, Boolean> negDrawOnCol;

    public ScanTable(ScannerTool controller, TableView<FileTableItem> tableView) {
        this.scannerTool = controller;
        this.tableView = tableView;
        init();
    }

    private void init() {
        TableColumn<FileTableItem, String> fileColumn = new TableColumn<>("FileName");
        TableColumn<FileTableItem, String> seqColumn = new TableColumn<>(SEQUENCE_COLUMN_NAME);
        TableColumn<FileTableItem, String> nDimColumn = new TableColumn<>(NDIM_COLUMN_NAME);
        TableColumn<FileTableItem, Long> dateColumn = new TableColumn<>("Date");

        fileColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getFileName()));
        seqColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getSeqName()));
        nDimColumn.setCellValueFactory(e -> new SimpleStringProperty(String.valueOf(e.getValue().getNDim())));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("Date"));

        tableView.getColumns().addAll(fileColumn, seqColumn, nDimColumn, dateColumn);
        setDragHandlers(tableView);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectionListener = c -> selectionChanged();
        tableView.getSelectionModel().getSelectedIndices().addListener(selectionListener);
        columnTypes.put(PATH_COLUMN_NAME, "S");
        columnTypes.put(SEQUENCE_COLUMN_NAME, "S");
        columnTypes.put(NDIM_COLUMN_NAME, "I");
        columnTypes.put(ROW_COLUMN_NAME, "I");
        columnTypes.put(DATASET_COLUMN_NAME, "S");
        columnTypes.put(ETIME_COLUMN_NAME, "I");
        columnTypes.put(GROUP_COLUMN_NAME, "I");

    }

    public void refresh() {
        tableView.refresh();
    }

    private void ensureAllDatasetsAdded() {
        PolyChart chart = scannerTool.getChart();
        List<String> datasetNames = new ArrayList<>();
        for (var fileTableItem : getItems()) {
            String datasetColumnValue = fileTableItem.getDatasetName();
            if (datasetColumnValue.isEmpty()) {
                continue;
            }

            String datasetPath = fileTableItem.getDatasetName();
            File file = new File(datasetPath);
            String datasetName = file.getName();
            if (!datasetNames.contains(datasetName)) {
                datasetNames.add(datasetName);
                Dataset dataset = Dataset.getDataset(datasetName);
                if (dataset == null) {
                    File datasetFile = new File(scanDir, datasetPath);
                    FXMLController.getActiveController().openDataset(datasetFile, false, true);
                }
            }
        }
        chart.updateDatasets(datasetNames);
    }

    private void setDatasetAttributes() {
        PolyChart chart = scannerTool.getChart();
        List<DatasetAttributes> datasetAttributesList = chart.getDatasetAttributes();
        activeDatasetAttributes.clear();
        for (var datasetAttributes : datasetAttributesList) {
            activeDatasetAttributes.put(datasetAttributes.getDataset().getName(), datasetAttributes);
        }
        for (var fileTableItem : getItems()) {
            String datasetPath = fileTableItem.getDatasetName();
            if (datasetPath == null) {
                continue;
            }
            File file = new File(datasetPath);
            String datasetName = file.getName();
            DatasetAttributes datasetAttributes = activeDatasetAttributes.get(datasetName);
            fileTableItem.setDatasetAttributes(datasetAttributes);
        }
    }

    private void setDatasetVisibility(List<FileTableItem> showRows, Double curLvl) {
        if (activeDatasetAttributes.isEmpty()) {
            ensureAllDatasetsAdded();
            setDatasetAttributes();
        }
        PolyChart chart = scannerTool.getChart();
        List<DatasetAttributes> datasetAttributesList = chart.getDatasetAttributes();
        List<Integer> rows = new ArrayList<>();
        Map<Integer, Double> offsetMap = new HashMap<>();
        Set<Integer> groupSet = new HashSet<>();
        datasetAttributesList.forEach(d -> d.setPos(false));
        boolean singleData = datasetAttributesList.size() == 1;
        if (singleData) {
            DatasetAttributes dataAttr = datasetAttributesList.get(0);
            dataAttr.setMapColor(0, dataAttr.getMapColor(0)); // ensure colorMap is not empty
        }
        showRows.forEach(fileTableItem -> {
                    var dataAttr = fileTableItem.getDatasetAttributes();
                    if (dataAttr != null) {
                        dataAttr.setPos(true);
                        if (curLvl != null) {
                            dataAttr.setLvl(curLvl);
                        }
                        Integer row = fileTableItem.getRow();
                        if (singleData && (row != null)) {
                            int iGroup = fileTableItem.getGroup();
                            groupSet.add(iGroup);
                            double offset = iGroup * 1.0 / groupSize * 0.8;
                            offsetMap.put(row - 1, offset);
                            rows.add(row - 1); // rows index from 1
                        } else {
                            dataAttr.clearColors();
                        }
                    }
                }
        );

        if (singleData) {
            DatasetAttributes dataAttr = datasetAttributesList.get(0);
            int nDim = dataAttr.nDim;
            chart.full(nDim - 1);
            if ((nDim - dataAttr.getDataset().getNFreqDims()) == 1) {
                chart.setDrawlist(rows);
            } else {
                chart.clearDrawlist();
            }
            if (groupSet.size() > 1) {
                dataAttr.setMapOffsets(offsetMap);
            } else {
                dataAttr.clearOffsets();
            }
        }
    }

    private void colorByGroup() {
        getItems().forEach(fileTableItem -> {
            var dataAttr = fileTableItem.getDatasetAttributes();
            if (dataAttr != null) {
                Integer row = fileTableItem.getRow();
                if (row != null) {
                    int iGroup = fileTableItem.getGroup();
                    Color color = getGroupColor(iGroup);
                    dataAttr.setMapColor(row - 1, color);
                } else {
                    dataAttr.clearColors();
                }
            }
        });
        PolyChart chart = scannerTool.getChart();
        chart.refresh();
    }

     protected final void selectionChanged() {
        if (processingTable) {
            return;
        }
        List<FileTableItem> selected = tableView.getSelectionModel().getSelectedItems();
        PolyChart chart = scannerTool.getChart();
        ProcessorController processorController = chart.getProcessorController(false);
        if ((processorController == null)
                || processorController.isViewingDataset()
                || !processorController.isVisible()) {
            List<FileTableItem> showRows = new ArrayList<>();
            if (selected.isEmpty()) {
                showRows.addAll(tableView.getItems());
            } else {
                showRows.addAll(selected);
            }
            Double curLvl = null;
            if (!chart.getDatasetAttributes().isEmpty()) {
                DatasetAttributes dataAttr = chart.getDatasetAttributes().get(0);
                curLvl = dataAttr.getLvl();
            }

            setDatasetVisibility(showRows, curLvl);
            refresh();
            chart.refresh();
        }
    }

    protected final void setDragHandlers(Node mouseNode) {
        mouseNode.setOnDragOver(this::mouseDragOver);
        mouseNode.setOnDragDropped(this::mouseDragDropped);
        mouseNode.setOnDragExited((DragEvent event) -> mouseNode.setStyle("-fx-border-color: #C6C6C6;"));
    }

    private void mouseDragDropped(final DragEvent e) {
        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;

            // Only get the first file from the list
            final File file = db.getFiles().get(0);
            if (file.isDirectory()) {
                scanDir = file;
                Platform.runLater(() -> {
                    ArrayList<String> nmrFiles = NMRDataUtil.findNMRDirectories(scanDir.getAbsolutePath());
                    initTable();
                    loadScanFiles(nmrFiles);
                });
            } else {
                Platform.runLater(() -> loadScanTable(file));

            }
        }
        e.setDropCompleted(success);
        e.consume();
    }

    private void mouseDragOver(final DragEvent e) {
        final Dragboard db = e.getDragboard();

        List<File> files = db.getFiles();
        if (db.hasFiles()) {
            if (!files.isEmpty() && (files.get(0).isDirectory() || files.get(0).toString().endsWith(".txt"))) {
                tableView.setStyle("-fx-border-color: green;"
                        + "-fx-border-width: 1;");
                e.acceptTransferModes(TransferMode.COPY);
            }
        } else {
            e.consume();
        }
    }

    public void setScanDirectory(File selectedDir) {
        scanDir = selectedDir;
    }

    public void loadScanFiles() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File scanDirFile = dirChooser.showDialog(null);
        if (scanDirFile == null) {
            return;
        }
        scanDir = scanDirFile;
        ArrayList<String> nmrFiles = NMRDataUtil.findNMRDirectories(scanDir.getAbsolutePath());
        processingTable = true;
        try {
            initTable();
            fileListItems.clear();
            loadScanFiles(nmrFiles);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            processingTable = false;
        }
    }

    public void processScanDir(ChartProcessor chartProcessor, boolean combineFileMode) {
        if ((chartProcessor == null) || !chartProcessor.hasCommands()) {
            GUIUtils.warn(SCANNER_ERROR, "Processing Script Not Configured");
            return;
        }

        if ((scanDir == null) || scanDir.toString().equals("")) {
            GUIUtils.warn(SCANNER_ERROR, "No scan directory");
            return;
        }
        String outDirName = GUIUtils.input("Output directory name", "output");
        Path outDirPath = Paths.get(scanDir.toString(), outDirName);
        File scanOutputDir = outDirPath.toFile();
        if (!scanOutputDir.exists() && !scanOutputDir.mkdir()) {
            GUIUtils.warn(SCANNER_ERROR, "Could not create output dir");
            return;
        }

        String combineFileName = GUIUtils.input("Output file name", "process");

        if (!scanOutputDir.exists() || !scanOutputDir.isDirectory() || !scanOutputDir.canWrite()) {
            GUIUtils.warn(SCANNER_ERROR, "Output dir is not a writable directory");
            return;
        }
        ObservableList<FileTableItem> fileTableItems = tableView.getItems();
        if (fileTableItems.isEmpty()) {
            return;
        }

        if (!combineFileName.contains(".")) {
            combineFileName += ".nv";
        }

        String fileRoot = combineFileName;
        if (fileRoot.contains(".")) {
            fileRoot = fileRoot.substring(0, fileRoot.lastIndexOf("."));
        }

        PolyChart chart = scannerTool.getChart();
        processingTable = true;
        try (PythonInterpreter processInterp = new PythonInterpreter()) {
            List<String> fileNames = new ArrayList<>();

            String initScript = ChartProcessor.buildInitScript();
            processInterp.exec(initScript);

            int nDim = fileTableItems.get(0).getNDim();
            String processScript = chartProcessor.buildScript(nDim);
            Processor processor = Processor.getProcessor();
            processor.keepDatasetOpen(false);

            int rowNum = 1;
            for (FileTableItem fileTableItem : fileTableItems) {
                File fidFile = new File(scanDir, fileTableItem.getFileName());
                String fidFilePath = fidFile.getAbsolutePath();
                File datasetFile = new File(scanOutputDir, fileRoot + rowNum + ".nv");
                String datasetFilePath = datasetFile.getAbsolutePath();
                String fileScript = ChartProcessor.buildFileScriptPart(fidFilePath, datasetFilePath);
                processInterp.exec(FormatUtils.formatStringForPythonInterpreter(fileScript));
                processInterp.exec(processScript);
                fileNames.add(datasetFilePath);
                fileTableItem.setRow(rowNum++);
                if (combineFileMode) {
                    fileTableItem.setDatasetName(outDirName + "/" + combineFileName);
                } else {
                    fileTableItem.setDatasetName(outDirName + "/" + datasetFile.getName());
                }
            }
            updateFilter();
            if (combineFileMode) {
                // merge datasets into single pseudo-nd dataset
                DatasetMerger merger = new DatasetMerger();
                File mergedFile = new File(scanOutputDir, combineFileName);
                String mergedFilepath = mergedFile.getAbsolutePath();
                try {
                    // merge all the 1D files into a pseudo 2D file
                    merger.merge(fileNames, mergedFilepath);
                    // After merging, remove the 1D files
                    for (String fileName : fileNames) {
                        File file = new File(fileName);
                        FXMLController.getActiveController().closeFile(file);
                        Files.deleteIfExists(file.toPath());
                        String parFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".par";
                        File parFile = new File(parFileName);
                        Files.deleteIfExists(parFile.toPath());
                    }

                    // load merged dataset
                    FXMLController.getActiveController().openDataset(mergedFile, false, true);
                    List<Integer> rows = new ArrayList<>();
                    rows.add(0);
                    chart.setDrawlist(rows);
                } catch (IOException | DatasetException ex) {
                    ExceptionDialog eDialog = new ExceptionDialog(ex);
                    eDialog.showAndWait();
                }
            } else {
                // load first output dataset
                File datasetFile = new File(scanOutputDir, fileRoot + 1 + ".nv");
                FXMLController.getActiveController().openDataset(datasetFile, false, true);
            }
            chart.full();
            chart.autoScale();

            File saveTableFile = new File(scanDir, "scntbl.txt");
            saveScanTable(saveTableFile);
            scannerTool.miner.setDisableSubMenus(!combineFileMode);

        } finally {
            processingTable = false;
        }
    }

    public void openSelectedListFile() {
        int selItem = tableView.getSelectionModel().getSelectedIndex();
        if (selItem >= 0) {
            if ((scanDir == null) || scanDir.toString().isBlank()) {
                return;
            }
            ProcessorController processorController = scannerTool.getChart().getProcessorController(true);
            if (processorController != null) {
                FileTableItem fileTableItem = tableView.getItems().get(selItem);
                String fileName = fileTableItem.getFileName();
                String filePath = Paths.get(scanDir.getAbsolutePath(), fileName).toString();

                scannerTool.getChart().getFXMLController().openFile(filePath, false, false);
            }

        }
    }

    private void loadScanFiles(ArrayList<String> nmrFiles) {
        fileListItems.clear();
        long firstDate = Long.MAX_VALUE;
        List<FileTableItem> items = new ArrayList<>();
        for (String filePath : nmrFiles) {
            File file = new File(filePath);
            NMRData nmrData = null;
            try {
                nmrData = NMRDataUtil.getFID(filePath);
            } catch (IOException ioE) {
                log.warn(ioE.getMessage(), ioE);

            }
            if (nmrData != null) {
                long date = nmrData.getDate();
                if (date < firstDate) {
                    firstDate = date;
                }
                Path relativePath = scanDir.toPath().relativize(file.toPath());
                items.add(new FileTableItem(relativePath.toString(), nmrData.getSequence(), nmrData.getNDim(), nmrData.getDate(), 0, ""));
            }
        }
        items.sort(Comparator.comparingLong(FileTableItem::getDate));
        final long firstDate2 = firstDate;
        items.forEach((FileTableItem item) -> {
            item.setDate(item.getDate() - firstDate2);
            fileListItems.add(item);
        });

        fileTableFilter.resetFilter();
    }

    public void loadScanTable() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Open Table File"));
        File file = fileChooser.showOpenDialog(popOver);
        if (file != null) {
            loadScanTable(file);
        }
    }

    public File getScanDir() {
        return scanDir;
    }

    public void loadFromDataset() {
        PolyChart chart = scannerTool.getChart();
        DatasetBase dataset = chart.getDataset();
        if (dataset == null) {
            log.warn("Unable to load dataset, dataset is null.");
            return;
        }
        if (dataset.getNDim() < 2) {
            log.warn("Unable to load dataset, dataset only has 1 dimension.");
            return;
        }
        scanDir = null;
        // Need to disconnect listeners before updating fileListItem or the selectionListener and filterItemListeners
        // will be triggered during every iteration of the loop, greatly reducing performance
        tableView.getSelectionModel().getSelectedIndices().removeListener(selectionListener);
        tableView.getItems().removeListener(filterItemListener);
        fileListItems.clear();
        int nRows = dataset.getSizeTotal(1);
        HashMap<String, String> fieldMap = new HashMap<>();
        double[] values = dataset.getValues(1);
        for (int iRow = 0; iRow < nRows; iRow++) {
            double value = 0;
            if ((values != null) && (iRow < values.length)) {
                value = values[iRow];
            }
            long eTime = (long) (value * 1000);
            fileListItems.add(new FileTableItem(dataset.getName(), "", 1, eTime, iRow + 1, dataset.getName(), fieldMap));
        }
        tableView.getSelectionModel().getSelectedIndices().addListener(selectionListener);
        tableView.getItems().addListener(filterItemListener);
        columnTypes.put(PATH_COLUMN_NAME, "S");
        columnTypes.put(SEQUENCE_COLUMN_NAME, "S");
        columnTypes.put(NDIM_COLUMN_NAME, "I");
        columnTypes.put(ROW_COLUMN_NAME, "I");
        columnTypes.put(DATASET_COLUMN_NAME, "S");
        columnTypes.put(ETIME_COLUMN_NAME, "I");
        columnTypes.put(GROUP_COLUMN_NAME, "I");
        Long firstDate = 0L;
        for (FileTableItem item : fileListItems) {
            item.setDate(item.getDate() - firstDate);
        }
        initTable();
        fileTableFilter.resetFilter();
        List<Integer> rows = new ArrayList<>();
        rows.add(0);
        // Load from Dataset assumes an arrayed dataset
        dataset.setNFreqDims(dataset.getNDim() - 1);
        if (dataset.getNDim() > 2) {
            chart.getDisDimProperty().set(PolyChart.DISDIM.TwoD);
        } else {
            chart.getDisDimProperty().set(PolyChart.DISDIM.OneDX);
        }
        chart.setDrawlist(rows);
        chart.full();
        chart.autoScale();
    }

    private void loadScanTable(File file) {
        long firstDate = Long.MAX_VALUE;
        int iLine = 0;
        String[] headers = new String[0];
        HashMap<String, String> fieldMap = new HashMap<>();
        boolean[] notDouble = null;
        boolean[] notInteger = null;
        String firstDatasetName = "";
        if ((scanDir == null) || scanDir.toString().isBlank()) {
            setScanDirectory(file.getParentFile());
        }

        processingTable = true;
        boolean combineFileMode = true;
        try {
            fileListItems.clear();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (iLine == 0) {
                        headers = line.split("\t");
                        notDouble = new boolean[headers.length];
                        notInteger = new boolean[headers.length];
                    } else {
                        String[] fields = line.split("\t");
                        for (int iField = 0; iField < fields.length; iField++) {
                            fields[iField] = fields[iField].trim();
                            try {
                                 Integer.parseInt(fields[iField]);
                            } catch (NumberFormatException nfE) {
                                notInteger[iField] = true;
                                try {
                                     Double.parseDouble(fields[iField]);
                                } catch (NumberFormatException nfE2) {
                                    notDouble[iField] = true;
                                }
                            }
                            fieldMap.put(headers[iField].toLowerCase(), fields[iField]);
                        }
                        boolean hasAll = true;
                        int nDim = 1;
                        long eTime = 0;
                        String sequence = "";
                        int row = 0;
                        for (String standardHeader : standardHeaders) {
                            if (!fieldMap.containsKey(standardHeader)) {
                                hasAll = false;
                            } else {
                                switch (standardHeader) {
                                    case NDIM_COLUMN_NAME -> nDim = Integer.parseInt(fieldMap.get(standardHeader));
                                    case ROW_COLUMN_NAME -> row = Integer.parseInt(fieldMap.get(standardHeader));
                                    case ETIME_COLUMN_NAME -> eTime = Long.parseLong(fieldMap.get(standardHeader));
                                    case SEQUENCE_COLUMN_NAME -> sequence = fieldMap.get(standardHeader);
                                }
                            }
                        }
                        String fileName = fieldMap.get(PATH_COLUMN_NAME);
                        String datasetName = "";
                        if (fieldMap.containsKey(DATASET_COLUMN_NAME)) {
                            datasetName = fieldMap.get(DATASET_COLUMN_NAME);
                            if (firstDatasetName.equals("")) {
                                firstDatasetName = datasetName;
                            } else if (!firstDatasetName.equals(datasetName)) {
                                combineFileMode = false;
                            }
                        }

                        if (!hasAll) {
                            if ((fileName == null) || fileName.isBlank()) {
                                GUIUtils.warn("Load scan table", "No value in path field");
                                return;
                            }
                            if ((scanDir == null) || scanDir.toString().isBlank()) {
                                GUIUtils.warn("Load scan table", "No scan directory");
                                return;
                            }
                            Path filePath = FileSystems.getDefault().getPath(scanDir.toString(), fileName);

                            NMRData nmrData;
                            try {
                                nmrData = NMRDataUtil.getFID(filePath.toString());
                            } catch (IOException ioE) {
                                GUIUtils.warn("Load scan table", "Couldn't load this file: " + filePath.toString());
                                return;
                            }

                            if (!fieldMap.containsKey(ETIME_COLUMN_NAME)) {
                                eTime = nmrData.getDate();
                            }
                            if (!fieldMap.containsKey(SEQUENCE_COLUMN_NAME)) {
                                sequence = nmrData.getSequence();
                            }
                            if (!fieldMap.containsKey(NDIM_COLUMN_NAME)) {
                                nDim = nmrData.getNDim();
                            }
                        }
                        if (eTime < firstDate) {
                            firstDate = eTime;
                        }
                       var item = new FileTableItem(fileName, sequence, nDim, eTime, row, datasetName, fieldMap);
                        fileListItems.add(item);
                    }

                    iLine++;
                }
            } catch (IOException ioE) {
                log.warn(ioE.getMessage(), ioE);
            }
            for (int i = 0; i < headers.length; i++) {
                if (!notInteger[i]) {
                    columnTypes.put(headers[i], "I");
                } else if (!notDouble[i]) {
                    columnTypes.put(headers[i], "D");
                } else {
                    columnTypes.put(headers[i], "S");
                }
            }
            columnTypes.put(PATH_COLUMN_NAME, "S");
            columnTypes.put(SEQUENCE_COLUMN_NAME, "S");
            columnTypes.put(NDIM_COLUMN_NAME, "I");
            columnTypes.put(ROW_COLUMN_NAME, "I");
            columnTypes.put(DATASET_COLUMN_NAME, "S");
            columnTypes.put(ETIME_COLUMN_NAME, "I");

            for (FileTableItem item : fileListItems) {
                item.setDate(item.getDate() - firstDate);
                item.setTypes(headers, notDouble, notInteger);
            }
            initTable();
            addHeaders(headers);
            fileTableFilter.resetFilter();
            if (firstDatasetName.length() > 0) {
                File parentDir = file.getParentFile();
                Path path = FileSystems.getDefault().getPath(parentDir.toString(), firstDatasetName);
                if (path.toFile().exists()) {
                    Dataset firstDataset = FXMLController.getActiveController().openDataset(path.toFile(), false, true);
                    // If there is only one unique dataset name, assume an arrayed experiment
                    List<String> uniqueDatasetNames = new ArrayList<>(fileListItems.stream().map(FileTableItem::getDatasetName).collect(Collectors.toSet()));
                    if (uniqueDatasetNames.size() == 1 && uniqueDatasetNames.get(0) != null && !uniqueDatasetNames.get(0).equals("")) {
                        firstDataset.setNFreqDims(firstDataset.getNDim() - 1);
                    }
                    PolyChart chart = scannerTool.getChart();
                    List<Integer> rows = new ArrayList<>();
                    rows.add(0);
                    chart.setDrawlist(rows);
                    chart.full();
                    chart.autoScale();
                }
            }
            addGroupColumn();
            scannerTool.miner.setDisableSubMenus(!combineFileMode);

        } catch (NumberFormatException e) {
            log.warn(e.getMessage(), e);
        } finally {
            processingTable = false;
        }
    }

    public void saveScanTable() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Table File");
        if (scanDir != null) {
            fileChooser.setInitialDirectory(scanDir);
        }
        File file = fileChooser.showSaveDialog(popOver);
        if (file != null) {
            saveScanTable(file);
        }
    }

    public TableView<FileTableItem> getTableView() {
        return tableView;
    }

    public List<String> getHeaders() {
        ObservableList<TableColumn<FileTableItem, ?>> columns = tableView.getColumns();
        List<String> headers = new ArrayList<>();
        for (TableColumn<FileTableItem, ?> column : columns) {
            String name = column.getText();
            headers.add(name);
        }
        return headers;
    }

    private void saveScanTable(File file) {
        Charset charset = StandardCharsets.US_ASCII;
        List<String> headers = getHeaders();

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), charset)) {
            boolean first = true;
            for (String header : headers) {
                if (!first) {
                    writer.write('\t');
                } else {
                    first = false;
                }
                writer.write(header, 0, header.length());
            }
            for (FileTableItem item : tableView.getItems()) {
                writer.write('\n');
                String s = item.toString(headers, columnTypes);
                writer.write(s, 0, s.length());
            }
        } catch (IOException x) {
            log.warn(x.getMessage(), x);
        }
    }

    public String getNextColumnName(String name, String columnDescriptor) {
        String columnName = columnDescriptors.get(columnDescriptor);
        int maxColumn = -1;
        if (columnName == null) {
            if (!name.equals("")) {
                columnName = name;
            } else {
                for (String columnType : columnTypes.keySet()) {
                    int columnNum;
                    if (columnType.startsWith("V.")) {
                        try {
                            int colonPos = columnType.indexOf(":");
                            columnNum = Integer.parseInt(columnType.substring(2, colonPos));
                            if (columnNum > maxColumn) {
                                maxColumn = columnNum;
                            }
                        } catch (NumberFormatException nfE) {
                            log.warn("Unable to parse column number.", nfE);
                        }
                    }
                }
                columnName = "V." + (maxColumn + 1);
            }
            columnDescriptors.put(columnDescriptor, columnName);
        }
        return columnName;
    }

    public void addGroupColumn() {
        addTableColumn(GROUP_COLUMN_NAME, "I");
    }

    private List<String> headersMissing(String[] headerNames) {
        List<String> missing = new ArrayList<>();
        for (var headerName:headerNames) {
            if (headerAbsent(headerName)) {
                missing.add(headerName);
            }
        }
        return missing;
    }

    private boolean headerAbsent(String headerName) {
        ObservableList<TableColumn<FileTableItem, ?>> columns = tableView.getColumns();
        boolean present = false;
        for (TableColumn<FileTableItem, ?> column : columns) {
            String name = column.getText();
            if (name.equals(headerName)) {
                present = true;
                break;
            }
        }
        return !present;
    }

    public void addTableColumn(String newName, String type) {
        if (headerAbsent(newName)) {
            columnTypes.put(newName, type);
            addColumn(newName);
        }
    }

    private void initTable() {
        tableView.setEditable(true);
        TableColumn<FileTableItem, String> fileColumn = new TableColumn<>(PATH_COLUMN_NAME);
        TableColumn<FileTableItem, String> seqColumn = new TableColumn<>(SEQUENCE_COLUMN_NAME);
        TableColumn<FileTableItem, Number> nDimColumn = new TableColumn<>(NDIM_COLUMN_NAME);
        TableColumn<FileTableItem, Long> dateColumn = new TableColumn<>(ETIME_COLUMN_NAME);
        TableColumn<FileTableItem, Number> rowColumn = new TableColumn<>(ROW_COLUMN_NAME);
        TableColumn<FileTableItem, String> datasetColumn = new TableColumn<>(DATASET_COLUMN_NAME);

        fileColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getFileName()));
        seqColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getSeqName()));
        nDimColumn.setCellValueFactory(e -> new SimpleIntegerProperty(e.getValue().getNDim()));
        rowColumn.setCellValueFactory(e -> new SimpleIntegerProperty(e.getValue().getRow()));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("Date"));
        datasetColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getDatasetName()));

        posColorCol.setCellValueFactory(e -> new SimpleObjectProperty<>(e.getValue().getPosColor()));
        posColorCol.setEditable(true);
        posColorCol.setSortable(false);
        TableUtils.addColorColumnEditor(posColorCol, (item, color) -> {
            item.setPosColor(color);
            scannerTool.getChart().refresh();
        });

        negColorCol.setCellValueFactory(e -> new SimpleObjectProperty<>(e.getValue().getNegColor()));
        negColorCol.setEditable(true);
        TableUtils.addColorColumnEditor(negColorCol, (item, color) -> {
            item.setNegColor(color);
            scannerTool.getChart().refresh();
        });
        negColorCol.setSortable(false);

        posDrawOnCol = new TableColumn<>("on");
        posDrawOnCol.setSortable(false);
        posDrawOnCol.setEditable(true);
        posDrawOnCol.setCellValueFactory(e -> new SimpleBooleanProperty(e.getValue().getPos()));
        TableUtils.addCheckBoxEditor(posDrawOnCol, (item, b) -> {
            item.setPos(b);
            scannerTool.getChart().refresh();
        });

        posDrawOnCol.setPrefWidth(25);
        posDrawOnCol.setMaxWidth(25);
        posDrawOnCol.setResizable(false);

        negDrawOnCol = new TableColumn<>("on");
        negDrawOnCol.setSortable(false);
        negDrawOnCol.setEditable(true);
        negDrawOnCol.setCellValueFactory(e -> new SimpleBooleanProperty(e.getValue().getNeg()));
        TableUtils.addCheckBoxEditor(negDrawOnCol, (item, b) -> {
            item.setNeg(b);
            scannerTool.getChart().refresh();
        });
        negDrawOnCol.setPrefWidth(25);
        negDrawOnCol.setMaxWidth(25);
        negDrawOnCol.setResizable(false);


        TableColumn<FileTableItem, Number> groupColumn = new TableColumn<>(GROUP_COLUMN_NAME);
        groupColumn.setCellValueFactory(e -> new SimpleIntegerProperty(e.getValue().getGroup()));

        groupColumn.setCellFactory(column -> {
            return new TableCell<>() {
                @Override
                protected void updateItem(Number group, boolean empty) {
                    super.updateItem(group, empty);
                    if (group == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        // Format date.
                        setText(String.valueOf(group));
                        setTextFill(getGroupColor(group.intValue()));
                    }
                }
            };
        });

        tableView.getColumns().clear();
        TableColumn<FileTableItem, TableColumn> posColumn = new TableColumn<>(POSITIVE_COLUMN_NAME);
        TableColumn<FileTableItem, TableColumn> negColumn = new TableColumn<>(NEGATIVE_COLUMN_NAME);
        posColumn.getColumns().addAll(posDrawOnCol, posColorCol);
        negColumn.getColumns().addAll(negDrawOnCol, negColorCol);
        tableView.getColumns().addAll(fileColumn, seqColumn, nDimColumn, dateColumn, rowColumn, datasetColumn, groupColumn, posColumn, negColumn);
        updateFilter();

        for (TableColumn<FileTableItem, ?> column : tableView.getColumns()) {
            if (!column.getText().equals(COLOR_COLUMN_NAME) && !column.getText().equals(POSITIVE_COLUMN_NAME) && !column.getText().equals(NEGATIVE_COLUMN_NAME)) {
                if (setColumnGraphic(column)) {
                    column.graphicProperty().addListener(e -> graphicChanged(column));
                }
            }
        }
    }

    private void addHeaders(String[] headers) {
        var missingHeaders = headersMissing(headers);
        for (var header:missingHeaders) {
            addColumn(header);
        }
    }

    private void addColumn(String header) {
        if (headerAbsent(header)) {
            String type = columnTypes.get(header);
            final TableColumn<FileTableItem, ?> newColumn;
            if (type == null) {
                type = "S";
                log.info("No type for {}", header);
            }
            switch (type) {
                case "D":
                    TableColumn<FileTableItem, Number> doubleExtraColumn = new TableColumn<>(header);
                    newColumn = doubleExtraColumn;
                    doubleExtraColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getDoubleExtra(header)));
                    doubleExtraColumn.setCellFactory(col
                            -> new TableCell<>() {
                        @Override
                        public void updateItem(Number value, boolean empty) {
                            super.updateItem(value, empty);
                            if (empty) {
                                setText(null);
                            } else {
                                setText(String.format("%.4f", value.doubleValue()));
                            }
                        }
                    });
                    tableView.getColumns().add(doubleExtraColumn);
                    break;
                case "I":
                    TableColumn<FileTableItem, Number> intExtraColumn = new TableColumn<>(header);
                    newColumn = intExtraColumn;
                    intExtraColumn.setCellValueFactory(e -> new SimpleIntegerProperty(e.getValue().getIntegerExtra(header)));
                    tableView.getColumns().add(intExtraColumn);
                    break;
                default:
                    TableColumn<FileTableItem, String> extraColumn = new TableColumn<>(header);
                    newColumn = extraColumn;
                    extraColumn.setCellValueFactory(e -> new SimpleStringProperty(String.valueOf(e.getValue().getExtra(header))));
                    tableView.getColumns().add(extraColumn);
                    break;
            }

            updateFilter();
            setColumnGraphic(newColumn);
            newColumn.graphicProperty().addListener(e -> graphicChanged(newColumn));
        }
    }

    private void graphicChanged(TableColumn<FileTableItem, ?> column) {
        Node node = column.getGraphic();
        boolean isFiltered = (node != null) && !(node instanceof StackPane);
        if ((node == null) || isFiltered) {
            setColumnGraphic(column);
        }
    }

    private ContextMenu createColorContextMenu(boolean posColorMode) {
        ContextMenu colorMenu = new ContextMenu();
        MenuItem unifyColorItem = new MenuItem("unify");
        unifyColorItem.setOnAction(e -> unifyColors(posColorMode));
        MenuItem interpColor = new MenuItem("interpolate");
        interpColor.setOnAction(e -> interpolateColors(posColorMode));
        MenuItem schemaPosColor = new MenuItem("schema...");
        schemaPosColor.setOnAction(e -> setColorsToSchema(posColorMode));
        colorMenu.getItems().addAll(unifyColorItem, interpColor, schemaPosColor);
        return colorMenu;
    }

    private void unifyColors(boolean posColorMode) {
        if (!getItems().isEmpty()) {
            var selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                selectedItem = getItems().get(0);
            }
            TableColors.unifyColor(getItems(), selectedItem.getColor(posColorMode), (item, color) -> item.setColor(color, posColorMode));
            scannerTool.getChart().refresh();
            refresh();
        }
    }

    private void interpolateColors(boolean posColorMode) {
        int size = getItems().size();
        if (size > 1) {
            Color color1 = getItems().get(0).getColor(posColorMode);
            Color color2 = getItems().get(size - 1).getColor(posColorMode);
            TableColors.interpolateColors(getItems(), color1, color2,
                    (item, color) -> item.setColor(color, posColorMode));
            scannerTool.getChart().refresh();
            refresh();
        }
    }

    void setColorsToSchema(boolean posColorMode) {
        double x = tableView.getLayoutX();
        double y = tableView.getLayoutY() + tableView.getHeight() + 10;
        ColorSchemes.showSchemaChooser(s -> updateColorsWithSchema(s, posColorMode), x, y);
    }

    public void updateColorsWithSchema(String colorName, boolean posColors) {
        var items = getItems();
        if (items.size() < 2) {
            return;
        }
        int i = 0;
        List<Color> colors = ColorSchemes.getColors(colorName, items.size());
        for (var item : items) {
            Color color = colors.get(i++);
            item.setColor(color, posColors);
        }
        refresh();
        scannerTool.getChart().refresh();
    }

    private void setMenuGraphics(TableColumn<FileTableItem, ?> column, boolean posColorMode) {
        Text text = GlyphsDude.createIcon(FontAwesomeIcon.BARS);
        text.setMouseTransparent(true);
        column.setGraphic(text);
        column.setContextMenu(createColorContextMenu(posColorMode));
    }

    private boolean setColumnGraphic(TableColumn<FileTableItem, ?> column) {
        String text = column.getText().toLowerCase();
        if (text.equalsIgnoreCase(COLOR_COLUMN_NAME) || text.equalsIgnoreCase(POSITIVE_COLUMN_NAME) || text.equalsIgnoreCase(NEGATIVE_COLUMN_NAME)) {
            return false;
        }
        String type = columnTypes.get(column.getText());
        if (!"D".equals(type) && isGroupable(text) && !text.equalsIgnoreCase(COLOR_COLUMN_NAME)) {
            boolean isGrouped = groupNames.contains(text);
            boolean isFiltered = isFiltered(column);
            StackPane stackPane = new StackPane();
            Rectangle rect = new Rectangle(10, 10);
            stackPane.getChildren().add(rect);
            Color color;
            if (isGrouped) {
                color = isFiltered ? Color.RED : Color.BLUE;
            } else {
                color = isFiltered ? Color.ORANGE : Color.WHITE;
            }
            rect.setFill(color);
            rect.setStroke(Color.BLACK);
            rect.setOnMousePressed(e -> hitColumnGrouper(e, rect, text));
            rect.setOnMouseReleased(Event::consume);
            rect.setOnMouseClicked(Event::consume);
            column.setGraphic(stackPane);
            return true;
        } else if ("D".equals(type) || isData(text)) {
            StackPane stackPane = new StackPane();
            Rectangle rect = new Rectangle(10, 10);
            Line line1 = new Line(1, 1, 10, 10);
            Line line2 = new Line(1, 10, 10, 1);

            line1.setStroke(Color.BLACK);
            line2.setStroke(Color.BLACK);
            line1.setMouseTransparent(true);
            line2.setMouseTransparent(true);
            stackPane.getChildren().addAll(rect, line1, line2);
            rect.setFill(Color.WHITE);
            rect.setStroke(Color.BLACK);
            rect.setOnMousePressed(e -> hitDataDelete(column));
            rect.setOnMouseReleased(Event::consume);
            rect.setOnMouseClicked(Event::consume);
            column.setGraphic(stackPane);
            return true;
        } else {
            return false;
        }
    }

    private boolean isFiltered(TableColumn column) {
        boolean filtered = false;
        Optional<ColumnFilter<FileTableItem, ?>> opt = fileTableFilter.getColumnFilter(column);
        if (opt.isPresent()) {
            filtered = opt.get().isFiltered();
        }
        return filtered;
    }

    private boolean isGroupable(String text) {
        return !standardHeaders.contains(text) && !text.equalsIgnoreCase(GROUP_COLUMN_NAME)
                && !text.contains(":") && !text.equalsIgnoreCase(DATASET_COLUMN_NAME) &&
                !text.contains(COLOR_COLUMN_NAME) && !text.equalsIgnoreCase(POSITIVE_COLUMN_NAME)
                && !text.equalsIgnoreCase(NEGATIVE_COLUMN_NAME);
    }

    public boolean isData(String text) {
        return !standardHeaders.contains(text) && !text.equals(GROUP_COLUMN_NAME)
                && text.contains(":") && !text.equals(DATASET_COLUMN_NAME);
    }

    private void hitDataDelete(TableColumn<FileTableItem, ?> column) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete column " + column.getText());
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                tableView.getColumns().remove(column);
            }
        });
    }

    private void hitColumnGrouper(MouseEvent e, Rectangle rect, String text) {
        e.consume();

        text = text.toLowerCase();
        if (groupNames.contains(text)) {
            groupNames.remove(text);
            rect.setFill(Color.WHITE);
        } else {
            groupNames.add(text);
            rect.setFill(Color.GREEN);
        }
        getGroups();
        colorByGroup();
        selectionChanged();
        tableView.refresh();
    }

    public void updateFilter() {
        // Old listener must be removed before setting the items!
        tableView.getItems().removeListener(filterItemListener);
        tableView.setItems(fileListItems);
        builder = TableFilter.forTableView(tableView);
        fileTableFilter = builder.apply();
        fileTableFilter.resetFilter();
        tableView.getItems().addListener(filterItemListener);
        getGroups();
        setMenuGraphics(posColorCol, true);
        setMenuGraphics(negColorCol, false);
    }

    public ObservableList<FileTableItem> getItems() {
        return fileListItems;
    }

    public void makeGroupMap() {
        groupMap.clear();
        for (String groupName : groupNames) {
            Set<String> group = new TreeSet<>();
            for (FileTableItem item : tableView.getItems()) {
                String value = item.getExtraAsString(groupName);
                group.add(value);
            }
            Map<String, Integer> map = new HashMap<>();
            for (String value : group) {
                map.put(value, map.size());

            }
            groupMap.put(groupName, map);
        }
    }

    public static Color getGroupColor(int index) {
        index = Math.min(index, COLORS.length - 1);
        return COLORS[index];
    }

    public void getGroups() {
        for (var column : tableView.getColumns()) {
            setColumnGraphic(column);
        }
        makeGroupMap();
        int maxValue = 0;
        for (FileTableItem item : tableView.getItems()) {
            int mul = 1;
            int iValue = 0;
            for (String groupName : groupNames) {
                Map<String, Integer> map = groupMap.get(groupName);
                if (!map.isEmpty()) {
                    String value = item.getExtraAsString(groupName);
                    int index = map.get(value);
                    iValue += index * mul;
                    mul *= map.size();
                }
            }
            item.setGroup(iValue);
            maxValue = Math.max(maxValue, iValue);
        }
        groupSize = maxValue + 1;
    }
}
