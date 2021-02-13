import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

public class MainWindow extends Application {
    private static Scene scene;
    private static Path RES_FOLDER;
    private static Path INIT_FOLDER;
    private static File[] FILES;
    private static double TOTAL_FILES;
    private static double FILES_PROCESSED;
    private static int MAX_CHAIN_SIZE_DEFAULT;
    private static int MAX_CHAIN_SIZE_EXCLUSIVE;
    private static final Map<File, String> toBeRenamed = new HashMap<>();
    private static final Map<ExitCode, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put(ExitCode.TOO_MANY_FAILED_SCANS, "Too many failed scans");
        MESSAGES.put(ExitCode.INVALID_SCAN_RESULT, "Invalid scan result");
        MESSAGES.put(ExitCode.SUCCESS, "Scanning finished successfully");
        MESSAGES.put(ExitCode.INVALID_FIRST_FILE, "The first image is not barcode image");
        MESSAGES.put(ExitCode.MORE_ANGLES_REQUIRED, "First product has more angles than specified");
    }

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
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Could not load GUI", e);
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/gui/" + fxml + ".fxml"));
        System.out.println(MainWindow.class.getResource("/gui/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public Scene getScene() {
        return scene;
    }

    public void browse() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = directoryChooser.showDialog(scene.getWindow());
        String selectedDirPath = selectedDirectory.getAbsolutePath();
        initFolderInput.setText(selectedDirPath);
        System.out.println(selectedDirectory.getAbsolutePath());
    }

    public void scan() {
        imageScrollPane.setVisible(false);
        if (!initParams()) {
            return;
        }
        if (TOTAL_FILES == 0) {
            TOTAL_FILES = loadFiles();
        }
        loadFiles();
        if (!checkForIncorrectlyRenamed()) {
            return;
        }
        if (!createResFolder()) {
            return;
        }
        Task<ExitCode> scanFilesTask = new Task<ExitCode>() {
            @Override
            public ExitCode call() throws Exception {
                ExitCode result =
                        processFiles(0, 0, "NO_RES_YET", "NONE");
                Platform.runLater(() -> errorLabel.setText(MESSAGES.get(result)));
                return result;
            }

            public ExitCode processFiles(int fileIndex, int counter, String lastResult, String prevFileType) {
                ExitCode res = analyzeAngleCounter(counter);
                if (!res.equals(ExitCode.SUCCESS)) {
                    return res;
                }
                if (fileIndex >= FILES.length) {
                    executeTransaction();
                    return ExitCode.SUCCESS;
                }
                File file = FILES[fileIndex];
                return analyzeScanResult(fileIndex, counter, lastResult, getResult(file), prevFileType, file);
            }

            private void executeTransaction() {
                for (Map.Entry<File, String> entry : toBeRenamed.entrySet()) {
                    entry.getKey().renameTo(new File(entry.getValue()));
                }
                FILES_PROCESSED += toBeRenamed.size();
                updateProgress(FILES_PROCESSED, TOTAL_FILES);
            }

            public ExitCode analyzeScanResult(int fileIndex, int counter, String lastBarcodeResult,
                                              String newResult, String prevFileType, File file) {
                switch (newResult.length()) {
                    case (38):
                        ExitCode res = analyzePreviousFile(prevFileType, lastBarcodeResult, file, counter);
                        if (!res.equals(ExitCode.SUCCESS)) {
                            return res;
                        }
                        return processFiles(++fileIndex, ++counter, lastBarcodeResult, "ANGLE");
                    case (5):
                        if (lastBarcodeResult.equals(newResult)) {
                            ExitCode res1 = analyzePreviousFile(prevFileType, lastBarcodeResult, file, counter);
                            if (!res1.equals(ExitCode.SUCCESS)) {
                                return res1;
                            }
                            return processFiles(++fileIndex, ++counter, lastBarcodeResult, "ANGLE");
                        } else {
                            if (!lastBarcodeResult.equals("NO_RES_YET")) {
                                MAX_CHAIN_SIZE_EXCLUSIVE = -1;
                            }
                            lastBarcodeResult = newResult;
                            executeTransaction();
                            toBeRenamed.clear();
                            toBeRenamed.put(file, RES_FOLDER.toString() + File.separator + "_" + lastBarcodeResult + "_" + file.getName());
                            return processFiles(++fileIndex, 0, lastBarcodeResult, "BARCODE");
                        }
                    default:
                        return ExitCode.INVALID_SCAN_RESULT;
                }
            }

            public ExitCode analyzePreviousFile(String prevFileType, String lastBarcodeResult, File file, int counter) {
                switch (prevFileType) {
                    case "ANGLE" -> {
                        toBeRenamed.put(file, RES_FOLDER.toString() + File.separator + lastBarcodeResult + "_" + counter + ".jpg");
                        return ExitCode.SUCCESS;
                    }
                    case "BARCODE" -> {
                        toBeRenamed.put(file, RES_FOLDER.toString() + File.separator + lastBarcodeResult + ".jpg");
                        return ExitCode.SUCCESS;
                    }
                    case "NONE" -> {
                        return ExitCode.INVALID_FIRST_FILE;
                    }
                }
                return ExitCode.INVALID_FIRST_FILE;
            }

            public ExitCode analyzeAngleCounter(int counter) {
                if (MAX_CHAIN_SIZE_EXCLUSIVE == -1) {
                    if (counter > MAX_CHAIN_SIZE_DEFAULT) {
                        loadFiles();
                        fillScrollPaneWithImages();
                        toBeRenamed.clear();
                        return ExitCode.TOO_MANY_FAILED_SCANS;
                    }
                } else {
                    if (counter > MAX_CHAIN_SIZE_EXCLUSIVE) {
                        toBeRenamed.clear();
                        return ExitCode.MORE_ANGLES_REQUIRED;
                    }
                }
                return ExitCode.SUCCESS;
            }

            private String getResult(File file) {
                try {
                    String name = file.getName();
                    if (name.contains("!")) return name.substring(name.indexOf('!') + 1, 6);
                    BinaryBitmap imageBitmap = new BinaryBitmap(
                            new HybridBinarizer(
                                    new BufferedImageLuminanceSource(Imaging.getBufferedImage(file))));
                    ArrayList<Result> results = new ArrayList<>(1);
                    Reader reader = new MultiFormatReader();

                    final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);
                    final Map<DecodeHintType, Object> HINTS_PURE = new EnumMap<>(DecodeHintType.class);
                    HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                    HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.CODE_39));
                    HINTS_PURE.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);

                    MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
                    Result[] theResults = multiReader.decodeMultiple(imageBitmap, HINTS);

                    if (theResults != null) results.addAll(Arrays.asList(theResults));
                    if (results.isEmpty()) {
                        try {
                            Result theResult = reader.decode(imageBitmap, HINTS_PURE);
                            if (theResult != null) results.add(theResult);
                        } catch (ReaderException re) {
                            re.printStackTrace();
                        }
                    }
                    if (results.isEmpty()) {
                        try {
                            Result theResult = reader.decode(imageBitmap, HINTS);
                            if (theResult != null) results.add(theResult);
                        } catch (ReaderException re) {
                            re.printStackTrace();
                        }
                    }
                    for (Result result : results) {
                        return result.getText();
                    }

                } catch (NotFoundException e) {
                    return "Could not find a barcode on the image.";
                } catch (IOException | ImageReadException e) {
                    return "File reading issue.";
                }
                return "NO_TRACE";
            }

            public void fillScrollPaneWithImages() {
                imageScrollPane.setVisible(true);
                GridPane grid = new GridPane();
                List<File> files = Arrays.stream(FILES).limit(7).collect(Collectors.toList());
                List<ImageView> imageViews = files
                        .stream()
                        .sorted()
                        .map(this::createImageView)
                        .collect(Collectors.toList());
                for (int i = 0; i < imageViews.size(); i++) {
                    grid.addColumn(i, imageViews.get(i));
                }
                Platform.runLater(() ->
                        imageScrollPane.setContent(grid));
            }

            private ImageView createImageView(File file) {
                ImageView imageView = new ImageView();
                Image image = new Image(Paths.get(file.getAbsolutePath()).toUri().toString());
                imageView.setImage(image);
                imageView.setFitWidth(330);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setCache(true);
                imageView.setOnMouseClicked(event ->
                        renameFileOnMouseClicked(file));
                return imageView;
            }

            public void renameFileOnMouseClicked(File file) {
                String oldFilePath = file.getAbsolutePath();
                String barcodeValue = renameFileViaDialog();
                if (barcodeValue.contains("Empty")) {
                    errorLabel.setText(barcodeValue);
                    return;
                }
                String newFilePath = oldFilePath.substring(0, oldFilePath.lastIndexOf(File.separator) + 1)
                        + "!" + barcodeValue + oldFilePath.substring(oldFilePath.lastIndexOf("_"));
                file.renameTo(new File(newFilePath));
            }

            public String renameFileViaDialog() {
                Dialog<String> renameFileDialog = new Dialog<>();
                renameFileDialog.setTitle("Rename file");
                ButtonType buttonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                renameFileDialog.getDialogPane().getButtonTypes().add(buttonType);
                GridPane gridPane = new GridPane();
                gridPane.setHgap(10);
                gridPane.setVgap(10);
                gridPane.setPadding(new Insets(20, 150, 10, 10));
                TextField newFileNameField = new TextField();
                gridPane.add(new Label("Barcode value:"), 0, 0);
                gridPane.add(newFileNameField, 1, 0);
                renameFileDialog.getDialogPane().setContent(gridPane);
                renameFileDialog.setResultConverter(dialogButton -> {
                    if (dialogButton == buttonType) {
                        return newFileNameField.getText();
                    }
                    return null;
                });
                Optional<String> result = renameFileDialog.showAndWait();
                return result.orElse("Empty file name");
            }
        };
        progressBar.progressProperty().bind(scanFilesTask.progressProperty());
        Thread thread = new Thread(scanFilesTask);
        thread.setDaemon(true);
        thread.start();
    }

    public boolean initParams() {
        String initFolderParam = initFolderInput.getText();
        if (!initFolderParam.trim().equals("")) {
            INIT_FOLDER = Paths.get(initFolderParam);
        } else {
            errorLabel.setText("Invalid path");
            return false;
        }
        String defaultMaxAngleSizeParam = maxChainSizeDefaultInput.getText();
        if (!defaultMaxAngleSizeParam.isEmpty()) {
            System.out.println(defaultMaxAngleSizeParam);
            try {
                int defMaxChainSize = Integer.parseInt(defaultMaxAngleSizeParam);
                if (defMaxChainSize < 20 && defMaxChainSize > 0) {
                    MAX_CHAIN_SIZE_DEFAULT = defMaxChainSize;
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                errorLabel.setText("Wrong parameter for default max angle count");
                return false;
            }
        } else {
            MAX_CHAIN_SIZE_DEFAULT = 2;
        }
        String exclusiveMaxAngleCountParam = maxChainSizeExclusiveInput.getText();
        if (!exclusiveMaxAngleCountParam.isEmpty()) {
            try {
                int exclMaxChainSize = Integer.parseInt(exclusiveMaxAngleCountParam);
                if (exclMaxChainSize < 20 && exclMaxChainSize > 0) {
                    MAX_CHAIN_SIZE_EXCLUSIVE = exclMaxChainSize;
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Wrong parameter for first product angle count");
                return false;
            }
        } else {
            MAX_CHAIN_SIZE_EXCLUSIVE = -1;
        }
        return true;
    }

    public void filterFiles() {
        FILES = Arrays.stream(FILES).filter(V -> (V.getName().contains(".jpg") ||
                V.getName().contains(".JPG") ||
                V.getName().contains(".jpeg") ||
                V.getName().contains(".JPEG") ||
                V.getName().contains(".png") ||
                V.getName().contains(".tif"))).toArray(File[]::new);
    }

    public int loadFiles() {
        File[] files = new File(INIT_FOLDER.toString()).listFiles();
        if (files == null) {
            System.out.println("Folder does not exist or does not contain any files.");
            System.exit(1);
        }
        FILES = files;
        filterFiles();
        sortFiles();
        return FILES.length;
    }

    private boolean checkForIncorrectlyRenamed() {
        for (File file : FILES) {
            String name = file.getName();
            if (name.contains("!") && name.length() < 11) {
                errorLabel.setText("File " + name + " was renamed incorrectly.");
                return false;
            }
        }
        return true;
    }

    public void sortFiles() {
        Arrays.sort(FILES, (prev, next) -> {
            String prevName = prev.getName();
            String nextName = next.getName();
            String prevID = prevName.substring(prevName.length() - 8, prevName.length() - 4);
            String nextID = nextName.substring(nextName.length() - 8, nextName.length() - 4);
            if (prevID.equals("") || nextID.equals("")) {
                return 0;
            }
            return Integer.parseInt(prevID) - Integer.parseInt(nextID);
        });
    }

    public boolean createResFolder() {
        try {
            RES_FOLDER = Paths.get(INIT_FOLDER + File.separator + "res");
            Files.createDirectory(RES_FOLDER);
            return true;
        } catch (FileAlreadyExistsException e) {
            return true;
        } catch (IOException e) {
            errorLabel.setText("Could not create result folder.");
            return false;
        }
    }
}
