package tasks;

import static filesystem.FilesLoader.loadFiles;

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
import controllers.MainWindowController;
import java.io.File;
import java.io.IOException;
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
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import scanner.ExitCode;

public class ScanFilesTask extends Task<ExitCode> {

    private static Path RES_FOLDER;
    private static Path INIT_FOLDER;
    private static File[] FILES;
    private static double TOTAL_FILES;
    private static double FILES_PROCESSED;
    private static int MAX_CHAIN_SIZE_DEFAULT;
    private static int MAX_CHAIN_SIZE_EXCLUSIVE;
    private static final Map<File, String> TRANSACTION = new HashMap<>();
    private static final Map<ExitCode, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put(ExitCode.TOO_MANY_FAILED_SCANS, "Too many failed scans");
        MESSAGES.put(ExitCode.INVALID_SCAN_RESULT, "Invalid scan result");
        MESSAGES.put(ExitCode.SUCCESS, "Scanning finished successfully");
        MESSAGES.put(ExitCode.INVALID_FIRST_FILE, "The first image is not barcode image");
        MESSAGES.put(ExitCode.MORE_ANGLES_REQUIRED, "First product has more angles than specified");
    }

    private final MainWindowController mainWindowController;

    public ScanFilesTask(MainWindowController mainWindowController) {
        this.mainWindowController = mainWindowController;
    }

    @Override
    public ExitCode call() {
        ExitCode result =
                processFiles(0, 0, "NO_RES_YET", "NONE");
        Platform.runLater(() -> mainWindowController.getErrorLabel().setText(MESSAGES.get(result)));
        return result;
    }

    public ExitCode processFiles(int fileIndex, int counter,
                                 String lastResult, String prevFileType) {
        ExitCode res = analyzeAngleCounter(counter);
        if (!res.equals(ExitCode.SUCCESS)) {
            return res;
        }
        if (fileIndex >= FILES.length) {
            executeTransaction();
            return ExitCode.SUCCESS;
        }
        File file = FILES[fileIndex];
        return analyzeScanResult(fileIndex, counter, lastResult,
                getResult(file), prevFileType, file);
    }

    private void executeTransaction() {
        for (Map.Entry<File, String> entry : TRANSACTION.entrySet()) {
            entry.getKey().renameTo(new File(entry.getValue()));
        }
        FILES_PROCESSED += TRANSACTION.size();
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
                    ExitCode res1 =
                            analyzePreviousFile(prevFileType, lastBarcodeResult, file, counter);
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
                    TRANSACTION.clear();
                    TRANSACTION.put(file, RES_FOLDER.toString() + File.separator
                            + "_" + lastBarcodeResult + "_" + file.getName());
                    return processFiles(++fileIndex, 0, lastBarcodeResult, "BARCODE");
                }
            default:
                return ExitCode.INVALID_SCAN_RESULT;
        }
    }

    public ExitCode analyzePreviousFile(
            String prevFileType, String lastBarcodeResult, File file, int counter) {
        switch (prevFileType) {
            case "ANGLE" -> {
                TRANSACTION.put(file, RES_FOLDER.toString() + File.separator
                        + lastBarcodeResult + "_" + counter + ".jpg");
                return ExitCode.SUCCESS;
            }
            case "BARCODE" -> {
                TRANSACTION.put(file, RES_FOLDER.toString() + File.separator
                        + lastBarcodeResult + ".jpg");
                return ExitCode.SUCCESS;
            }
            case "NONE" -> {
                return ExitCode.INVALID_FIRST_FILE;
            }
            default -> {
                return ExitCode.INVALID_SCAN_RESULT;
            }
        }
    }

    public ExitCode analyzeAngleCounter(int counter) {
        if (MAX_CHAIN_SIZE_EXCLUSIVE == -1) {
            if (counter > MAX_CHAIN_SIZE_DEFAULT) {
                loadFiles();
                fillScrollPaneWithImages();
                TRANSACTION.clear();
                return ExitCode.TOO_MANY_FAILED_SCANS;
            }
        } else {
            if (counter > MAX_CHAIN_SIZE_EXCLUSIVE) {
                TRANSACTION.clear();
                return ExitCode.MORE_ANGLES_REQUIRED;
            }
        }
        return ExitCode.SUCCESS;
    }

    private String getResult(File file) {
        try {
            String name = file.getName();
            if (name.contains("!")) {
                return name.substring(name.indexOf('!') + 1, 6);
            }
            ArrayList<Result> results = new ArrayList<>(1);

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            Map<DecodeHintType, Object> pureHints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.CODE_39));
            pureHints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
            
            BinaryBitmap imageBitmap = new BinaryBitmap(
                    new HybridBinarizer(
                            new BufferedImageLuminanceSource(Imaging.getBufferedImage(file))));
            Reader reader = new MultiFormatReader();
            MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
            Result[] theResults = multiReader.decodeMultiple(imageBitmap, hints);

            if (theResults != null) {
                results.addAll(Arrays.asList(theResults));
            }
            if (results.isEmpty()) {
                try {
                    Result theResult = reader.decode(imageBitmap, pureHints);
                    if (theResult != null) {
                        results.add(theResult);
                    }
                } catch (ReaderException re) {
                    re.printStackTrace();
                }
            }
            if (results.isEmpty()) {
                try {
                    Result theResult = reader.decode(imageBitmap, hints);
                    if (theResult != null) {
                        results.add(theResult);
                    }
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
        mainWindowController.getImageScrollPane().setVisible(true);
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
                mainWindowController.getImageScrollPane().setContent(grid));
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
            mainWindowController.getErrorLabel().setText(barcodeValue);
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

    public static double getTotalFiles() {
        return TOTAL_FILES;
    }

    public static void setTotalFiles(double totalFiles) {
        TOTAL_FILES = totalFiles;
    }

    public static Path getResFolder() {
        return RES_FOLDER;
    }

    public static void setResFolder(Path resFolder) {
        RES_FOLDER = resFolder;
    }

    public static Path getInitFolder() {
        return INIT_FOLDER;
    }

    public static void setInitFolder(Path initFolder) {
        INIT_FOLDER = initFolder;
    }

    public static File[] getFiles() {
        return FILES;
    }

    public static void setFiles(File[] files) {
        ScanFilesTask.FILES = files;
    }

    public static void setMaxChainSizeDefault(int maxChainSizeDefault) {
        MAX_CHAIN_SIZE_DEFAULT = maxChainSizeDefault;
    }

    public static void setMaxChainSizeExclusive(int maxChainSizeExclusive) {
        MAX_CHAIN_SIZE_EXCLUSIVE = maxChainSizeExclusive;
    }
}
