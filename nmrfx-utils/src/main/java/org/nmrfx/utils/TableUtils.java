package org.nmrfx.utils;

import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class TableUtils {

    private TableUtils() {}

    /**
     * Copies the contents of a table, including the headers, to the clipboard as a tab separated string.
     * If no rows are selected or copyAll is true, then all rows will be copied. Otherwise only the
     * selected rows will be copied.
     * @param tableView The TableView to copy.
     * @param forceCopyAll If true, all rows will be copied regardless of selections
     */
    public static void copyTableToClipboard(TableView<?> tableView, boolean forceCopyAll) {
        List<Integer> rowsToCopy = new ArrayList<>(tableView.getSelectionModel().getSelectedIndices());
        if (rowsToCopy.isEmpty() || forceCopyAll) {
            rowsToCopy = IntStream.range(0, tableView.getItems().size()).boxed().toList();

        }
        List<String> headerColumns = tableView.getColumns().stream().map(TableColumn::getText).toList();
        StringBuilder tabSeparatedString = new StringBuilder();
        tabSeparatedString.append(String.join("\t", headerColumns)).append(System.lineSeparator());
        int lastColumnIndex = headerColumns.size() - 1;
        for(Integer rowIndex: rowsToCopy) {
            for (int colIndex = 0; colIndex < headerColumns.size(); colIndex++) {
                tabSeparatedString.append(tableView.getColumns().get(colIndex).getCellData(rowIndex));
                if (colIndex != lastColumnIndex) {
                    tabSeparatedString.append("\t");
                }
            }
            tabSeparatedString.append(System.lineSeparator());
        }
        Clipboard clipBoard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.put(DataFormat.PLAIN_TEXT, tabSeparatedString.toString());
        clipBoard.setContent(content);
    }

    public static <T> void addColorColumnEditor(TableColumn<T, Color> posColorCol, BiConsumer<T, Color> applyColor) {
        posColorCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Color item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || (item == null)) {
                    setGraphic(null);
                } else {
                    final ColorPicker cp = new ColorPicker();
                    cp.getStyleClass().add("button");
                    cp.setStyle("-fx-color-label-visible:false;");
                    cp.setValue(item);
                    setGraphic(cp);
                    cp.setOnAction(t -> {
                        getTableView().edit(getTableRow().getIndex(), column);
                        commitEdit(cp.getValue());
                    });
                }
            }

            @Override
            public void commitEdit(Color color) {
                super.commitEdit(color);
                T item = getTableRow().getItem();
                applyColor.accept(item, color);
            }
        });
    }
    public static <T> void addCheckBoxEditor(TableColumn<T, Boolean> posColorCol, BiConsumer<T, Boolean> applyValue) {
        posColorCol.setCellFactory(column -> new CheckBoxTableCell<>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || (item == null)) {
                    setGraphic(null);
                } else {
                    final CheckBox cp = new CheckBox();
                    cp.setSelected(item);
                    setGraphic(cp);
                    cp.setOnAction(t -> {
                        getTableView().edit(getTableRow().getIndex(), column);
                        commitEdit(cp.isSelected());
                    });

                }
            }

            @Override
            public void commitEdit(Boolean value) {
                super.commitEdit(value);
                T item = getTableRow().getItem();
                applyValue.accept(item, value);
            }
        });
    }
}