package org.nmrfx.analyst.gui.datasetbrowser;

import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


public abstract class DatasetBrowserTab extends Tab {
    protected final BorderPane borderPane = new BorderPane();
    protected final VBox vBox = new VBox();
    protected final ToolBar toolBar = new ToolBar();
    protected final HBox hBox = new HBox();
    protected final TextField directoryTextField = new TextField();
    protected DatasetBrowserTableView tableView;
    protected Button datasetButton;
    protected Button fetchButton;

    public DatasetBrowserTab (String tabName) {
        super(tabName);
        setContent(borderPane);
        hBox.getChildren().add(directoryTextField);
        vBox.getChildren().addAll(toolBar, hBox);
        borderPane.setTop(vBox);
        borderPane.setCenter(tableView);
        HBox.setHgrow(directoryTextField, Priority.ALWAYS);
        directoryTextField.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updatePreferences();
            }
        });

    }

    protected void initToolbar(boolean includeFetch) {
        Button retrieveIndexButton = new Button("Index");
        retrieveIndexButton.setOnAction(e -> retrieveIndex());
        Button fidButton = new Button("FID");
        fidButton.setOnAction(e -> openFile(true));
        datasetButton = new Button("Dataset");
        datasetButton.setOnAction(e -> openFile(false));

        toolBar.getItems().addAll(retrieveIndexButton, fidButton, datasetButton);
        if (includeFetch) {
            fetchButton = new Button("Fetch");
            fetchButton.setOnAction(e -> cacheDatasets());
            toolBar.getItems().add(fetchButton);
        }
    }

    /**
     * Retrieves a list of the data files present in the directory specified by the directoryTextField and displays
     * the list in the tableview.
     */
    protected void retrieveIndex() {}

    /**
     * Opens the selected file.
     * @param isFid
     */
    protected void openFile(boolean isFid) {}

    protected void cacheDatasets() {}

    protected void loadIndex() {}


    protected void updatePreferences() {}
}
