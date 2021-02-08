import java.io.File;
import java.io.IOException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class MainWindow extends Application {
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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            scene = new Scene(loadFXML("main"));
            //stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/icon.png")));
            stage.setTitle("Barcode Image Processor");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            setErrorLabel(e.getMessage());
        }
    }

    private void setErrorLabel(String message) {
        errorLabel.setText(message);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/gui/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public Scene getScene() {
        return scene;
    }

    public void browse(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("src"));
        File selectedDirectory = directoryChooser.showDialog(scene.getWindow());
        String selectedDirPath = selectedDirectory.getAbsolutePath();
        initFolderInput.setText(selectedDirPath);
        System.out.println(selectedDirectory.getAbsolutePath());
    }
}
