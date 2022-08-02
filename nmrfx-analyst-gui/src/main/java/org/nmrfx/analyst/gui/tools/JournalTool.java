package org.nmrfx.analyst.gui.tools;

import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;
import org.nmrfx.analyst.gui.annotations.AnnoJournalFormat;
import org.nmrfx.analyst.peaks.JournalFormatPeaks;
import org.nmrfx.processor.gui.FXMLController;
import org.nmrfx.processor.gui.PolyChart;

public class JournalTool {
    static String formatName = "JMedCh";
    VBox vBox;
    AnnoJournalFormat annoJournalFormat;

    public JournalTool() {
    }

    public VBox getBox() {
        return vBox;
    }

    public boolean popoverInitialized() {
        return vBox != null;
    }

    public static JournalTool getTool(PolyChart chart) {
        JournalTool journalTool = (JournalTool) chart.getPopoverTool(JournalTool.class.getName());
        if (journalTool == null) {
            journalTool = new JournalTool();
            chart.setPopoverTool(JournalTool.class.getName(), journalTool);
        }
        return journalTool;
    }

    public void initializePopover(PopOver popOver) {
        this.vBox = new VBox();
        HBox hBox = new HBox();
        hBox.setMinHeight(10);
        HBox.setHgrow(hBox, Priority.ALWAYS);

        ComboBox<String> comboBox = new ComboBox<>();
        for (var format : JournalFormatPeaks.getFormatNames()) {
            comboBox.getItems().add(format);
        }
        comboBox.setValue(formatName);
        comboBox.setOnAction(e -> updateFormat(comboBox.getValue()));
        vBox.getChildren().addAll(hBox, comboBox);
        popOver.setContentNode(vBox);
    }

    public void setAnnoJournalFormat(AnnoJournalFormat annoJournalFormat) {
        this.annoJournalFormat = annoJournalFormat;
    }

    private void updateFormat(String journalName) {
        formatName = journalName;
        if (annoJournalFormat != null) {
            annoJournalFormat.setJournalName(journalName);
        }
        FXMLController.getActiveController().getActiveChart().refresh();
    }
}
