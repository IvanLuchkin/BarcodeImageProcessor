package controllers;

import static filesystem.FilesLoader.loadFiles;
import static filesystem.FolderCreator.createResFolder;
import static scanner.ParametersInitializer.initParams;

import java.io.File;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import scanner.ExitCode;
import scanner.ParametersInitializer;
import tasks.ScanFilesTask;

public class MainWindowController extends Application {
    private static Scene scene;
    @FXML
    private TextField initFolderInput;
    @FXML
    private Button browseButton;
    @FXML
    private DynamicScrollPane imageScrollPane;
    @FXML
    private Button scanButton;
    @FXML
    private TextField maxChainSizeDefaultInput;
    @FXML
    private TextField maxChainSizeExclusiveInput;
    @FXML
    private Label errorLabel;
    @FXML
    private Button helpButton;
    @FXML
    private ProgressBar progressBar;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            scene = new Scene(loadFxml("main"));
            stage.setTitle("Barcode Image Processor");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Could not load GUI", e);
        }
    }

    private static Parent loadFxml(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                MainWindowController.class.getResource("/gui/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFxml(fxml));
    }

    public Scene getScene() {
        return scene;
    }

    public void getPathFromBrowseDialog() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = directoryChooser.showDialog(scene.getWindow());
        String selectedDirPath = selectedDirectory.getAbsolutePath();
        initFolderInput.setText(selectedDirPath);
    }

    public void onScanAction() {
        ParametersInitializer.setInitFolderInput(initFolderInput);
        ParametersInitializer.setMaxChainSizeDefaultInput(maxChainSizeDefaultInput);
        ParametersInitializer.setMaxChainSizeExclusiveInput(maxChainSizeExclusiveInput);
        ExitCode.setErrorLabel(errorLabel);
        ScanFilesTask.setImageScrollPane(imageScrollPane);
        imageScrollPane.setVisible(false);
        ExitCode initParamsResult = initParams();
        if (initParamsResult.isAbortive()) {
            initParamsResult.showMessage();
            return;
        }
        if (ScanFilesTask.getTotalFiles() == 0) {
            ScanFilesTask.setTotalFiles(loadFiles());
        }
        loadFiles();
        ExitCode folderCreationResult = createResFolder();
        if (folderCreationResult.isAbortive()) {
            folderCreationResult.showMessage();
            return;
        }
        ScanFilesTask scanFilesTask = new ScanFilesTask();
        progressBar.progressProperty().bind(scanFilesTask.progressProperty());
        Thread thread = new Thread(scanFilesTask);
        thread.setDaemon(true);
        thread.start();
    }
}
