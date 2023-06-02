package org.nmrfx.analyst.gui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.nmrfx.processor.gui.PreferencesController;

public class FileMenuActions extends MenuActions {
    private DatasetBrowserController browserController = null;

    public FileMenuActions(AnalystApp app, Menu menu) {
        super(app, menu);
    }

    @Override
    public void basic() {
        MenuItem openMenuItem = new MenuItem("Open...");
        openMenuItem.setOnAction(e -> AnalystApp.getFXMLControllerManager().getOrCreateActiveController().openAction(e));

        Menu recentFilesMenuItem = new Menu("Recent Files");
        menu.setOnShowing(e -> {
            recentFilesMenuItem.getItems().clear();
            PreferencesController.setupRecentMenus(recentFilesMenuItem);
        });

        Menu graphicsMenu = new Menu("Export Graphics");

        MenuItem pdfMenuItem = new MenuItem("Export PDF...");
        pdfMenuItem.disableProperty().bind(AnalystApp.getFXMLControllerManager().activeControllerProperty().isNull());
        pdfMenuItem.setOnAction(e -> AnalystApp.getFXMLControllerManager().getOrCreateActiveController().exportPDFAction(e));

        MenuItem svgMenuItem = new MenuItem("Export SVG...");
        svgMenuItem.setOnAction(e -> AnalystApp.getFXMLControllerManager().getOrCreateActiveController().exportSVGAction(e));
        svgMenuItem.disableProperty().bind(AnalystApp.getFXMLControllerManager().activeControllerProperty().isNull());

        MenuItem pngMenuItem = new MenuItem("Export PNG...");
        pngMenuItem.setOnAction(e -> AnalystApp.getFXMLControllerManager().getOrCreateActiveController().exportPNG(e));
        pngMenuItem.disableProperty().bind(AnalystApp.getFXMLControllerManager().activeControllerProperty().isNull());

        graphicsMenu.getItems().addAll(svgMenuItem, pdfMenuItem, pngMenuItem);

        MenuItem datasetBrowserMenuItem = new MenuItem("Dataset Browser...");
        datasetBrowserMenuItem.setOnAction(e -> showDataBrowser());


        menu.getItems().addAll(openMenuItem,
                recentFilesMenuItem, datasetBrowserMenuItem,
                graphicsMenu);
    }

    @Override
    protected void advanced() {
        MenuItem addMenuItem = new MenuItem("Open Dataset (No Display) ...");
        addMenuItem.setOnAction(e -> AnalystApp.getFXMLControllerManager().getOrCreateActiveController().addNoDrawAction(e));
        menu.getItems().addAll(addMenuItem);

    }

    public void addAdvancedMenuItem() {
        MenuItem startAdvancedItem = new MenuItem("Start Advanced");
        startAdvancedItem.setOnAction(e -> app.advanced(startAdvancedItem));
        menu.getItems().add(startAdvancedItem);
    }

    void showDataBrowser() {
        if (browserController == null) {
            browserController = DatasetBrowserController.create();
        }
        Stage browserStage = browserController.getStage();
        browserStage.toFront();
        browserStage.show();
    }
}
