package controllers;

import filesystem.FilesLoader;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import scanner.ImageProcessor;
import scanner.ParametersInitializer;

public class MainWindowController extends Application {
    private static Scene scene;
    @FXML
    private TextField initFolderInput;
    @FXML
    private Button browseButton;
    @FXML
    private ScrollPane imageScrollPane;
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
    private ParametersInitializer parametersInitializer;
    private FilesLoader filesLoader;

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
        this.parametersInitializer = new ParametersInitializer(this);
        this.filesLoader = new FilesLoader(this);
        new ImageProcessor(this).scan();
    }

    public ScrollPane getImageScrollPane() {
        return imageScrollPane;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public TextField getInitFolderInput() {
        return initFolderInput;
    }

    public TextField getMaxChainSizeDefaultInput() {
        return maxChainSizeDefaultInput;
    }

    public TextField getMaxChainSizeExclusiveInput() {
        return maxChainSizeExclusiveInput;
    }

    public Label getErrorLabel() {
        return errorLabel;
    }
}
