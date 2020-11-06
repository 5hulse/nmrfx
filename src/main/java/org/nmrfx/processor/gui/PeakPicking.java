/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javafx.collections.ObservableList;
import org.controlsfx.dialog.ExceptionDialog;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.peaks.InvalidPeakException;
import org.nmrfx.processor.datasets.peaks.Peak;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.processor.datasets.peaks.PeakPickParameters;
import org.nmrfx.processor.datasets.peaks.PeakPicker;
import org.nmrfx.processor.datasets.peaks.io.PeakWriter;
import org.nmrfx.processor.gui.spectra.DatasetAttributes;
import org.nmrfx.processor.gui.spectra.PeakListAttributes;

/**
 *
 * @author Bruce Johnson
 */
public class PeakPicking {

    static Consumer singlePickAction = null;

    public static void registerSinglePickAction(Consumer func) {
        singlePickAction = func;
    }

    public static void peakPickActive(FXMLController fxmlController, boolean refineLS) {
        PolyChart chart = fxmlController.getActiveChart();
        ObservableList<DatasetAttributes> dataList = chart.getDatasetAttributes();
        dataList.stream().forEach((DatasetAttributes dataAttr) -> {
            peakPickActive(chart, dataAttr, chart.getCrossHairs().hasCrosshairRegion(), refineLS, false, null);
        });
        chart.refresh();
    }

    private static String getListName(PolyChart chart, DatasetAttributes dataAttr) {
        String listName = null;
        List<PeakListAttributes> peakAttrs = chart.getPeakListAttributes();
        for (PeakListAttributes peakAttr : peakAttrs) {
            if (peakAttr.getDatasetAttributes() == dataAttr) {
                listName = peakAttr.getPeakListName();
                break;
            }
        }
        if (listName == null) {
            listName = PeakList.getNameForDataset(dataAttr.getFileName());
        }
        return listName;
    }

    public static PeakList peakPickActive(PolyChart chart, DatasetAttributes dataAttr, boolean useCrossHairs, boolean refineLS, boolean saveFile, String listName) {
        Dataset dataset = dataAttr.getDataset();
        int nDim = dataset.getNDim();

        if (listName == null) {
            listName = getListName(chart, dataAttr);
        }
        double level = dataAttr.getLvl();
        if (nDim == 1) {
            level = chart.crossHairPositions[0][PolyChart.HORIZONTAL];
        }
        PeakPickParameters peakPickPar = (new PeakPickParameters(dataset, listName)).level(level).mode("appendregion");
        peakPickPar.pos(dataAttr.getPos()).neg(dataAttr.getNeg());
        peakPickPar.calcRange();
        for (int iDim = 0; iDim < nDim; iDim++) {
            int jDim = dataAttr.getDim(iDim);
            if (iDim < 2) {
                if (useCrossHairs) {
                    int orientation = iDim == 0 ? PolyChart.VERTICAL : PolyChart.HORIZONTAL;
                    peakPickPar.limit(jDim,
                            chart.crossHairPositions[0][orientation],
                            chart.crossHairPositions[1][orientation]);
                } else {
                    peakPickPar.limit(jDim, chart.axes[iDim].getLowerBound(), chart.axes[iDim].getUpperBound());
                }
            } else {
                int p1 = chart.axModes[iDim].getIndex(dataAttr, iDim, chart.axes[iDim].getLowerBound());
                int p2 = chart.axModes[iDim].getIndex(dataAttr, iDim, chart.axes[iDim].getUpperBound());
                peakPickPar.limit(jDim, p1, p2);
            }
        }
        PeakPicker picker = new PeakPicker(peakPickPar);
        String canonFileName = dataset.getCanonicalFile();
        String listFileName = canonFileName.substring(0, canonFileName.lastIndexOf(".")) + ".xpk2";
        PeakList peakList = null;
        try {
            if (refineLS) {
                peakList = picker.refinePickWithLSCat();
            } else {
                peakList = picker.peakPick();
            }
            if (peakList != null) {
                chart.setupPeakListAttributes(peakList);
                if (saveFile) {
                    try (final FileWriter writer = new FileWriter(listFileName)) {
                        PeakWriter peakWriter = new PeakWriter();
                        peakWriter.writePeaksXPK2(writer, peakList);
                    }
                }
            }
        } catch (IOException | InvalidPeakException ioE) {
            ExceptionDialog dialog = new ExceptionDialog(ioE);
            dialog.showAndWait();
        }
        chart.peakStatus.set(true);
        return peakList;
    }

    public static PeakList pickAtPosition(PolyChart chart, DatasetAttributes dataAttr, double x, double y, boolean fixed, boolean saveFile) {
        Dataset dataset = dataAttr.getDataset();
        int nDim = dataset.getNDim();
        String listName = getListName(chart, dataAttr);
        double level = dataAttr.getLvl();
        if (nDim == 1) {
            level = chart.crossHairPositions[0][PolyChart.HORIZONTAL];
        }
        PeakPickParameters peakPickPar = (new PeakPickParameters(dataset, listName)).level(level).mode("appendif");
        peakPickPar.pos(dataAttr.getPos()).neg(dataAttr.getNeg());
        peakPickPar.region("point").fixed(fixed);
        peakPickPar.calcRange();
        for (int iDim = 0; iDim < nDim; iDim++) {
            int jDim = dataAttr.getDim(iDim);
            if (iDim < 2) {
                double pos = iDim == 0 ? x : y;
                peakPickPar.limit(jDim, pos, pos);
            } else {
                if (chart.axModes[iDim] == DatasetAttributes.AXMODE.PTS) {
                    peakPickPar.limit(jDim, (int) chart.axes[iDim].getLowerBound(), (int) chart.axes[iDim].getUpperBound());
                } else {
                    peakPickPar.limit(jDim, chart.axes[iDim].getLowerBound(), chart.axes[iDim].getUpperBound());
                }
            }
        }
        PeakPicker picker = new PeakPicker(peakPickPar);
        String canonFileName = dataset.getCanonicalFile();
        String listFileName = canonFileName.substring(0, canonFileName.lastIndexOf(".")) + ".xpk2";
        PeakList peakList = null;
        Peak peak = null;
        try {
            peakList = picker.peakPick();
            if (peakList != null) {
                chart.setupPeakListAttributes(peakList);
                if (saveFile) {
                    try (final FileWriter writer = new FileWriter(listFileName)) {
                        PeakWriter peakWriter = new PeakWriter();
                        peakWriter.writePeaksXPK2(writer, peakList);
                    }
                }
                peak = picker.getLastPick();
            }
        } catch (IOException | InvalidPeakException ioE) {
            ExceptionDialog dialog = new ExceptionDialog(ioE);
            dialog.showAndWait();
        }
        chart.peakStatus.set(true);
        if ((peak != null) && FXMLController.getActiveController().isPeakAttrControllerShowing()) {
            PeakAttrController controller = FXMLController.getActiveController().getPeakAttrController();
            controller.gotoPeak(peak);
        }
        if ((peak != null) && (singlePickAction != null)) {
            singlePickAction.accept(peak);
        }
        return peakList;
    }

}
