package controllers;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import scanner.ExitCode;
import tasks.ScanFilesTask;

public class DynamicScrollPane extends ScrollPane {
    public static void fillScrollPaneWithImages(ScrollPane imageScrollPane) {
        imageScrollPane.setVisible(true);
        GridPane grid = new GridPane();
        List<ImageView> imageViews = Arrays.stream(ScanFilesTask.getFiles())
                .sorted()
                .limit(7)
                .map(DynamicScrollPane::createImageView)
                .collect(Collectors.toList());
        for (int i = 0; i < imageViews.size(); i++) {
            grid.addColumn(i, imageViews.get(i));
        }
        imageScrollPane.setContent(grid);
    }

    private static ImageView createImageView(File file) {
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void renameFileOnMouseClicked(File file) {
        String oldFilePath = file.getAbsolutePath();
        String barcodeValue = renameFileViaDialog();
        if (barcodeValue.contains("Empty")) {
            ExitCode.INCORRECT_BARCODE_VALUE.showMessage();
            return;
        }
        String newFilePath = oldFilePath.substring(0, oldFilePath.lastIndexOf(File.separator) + 1)
                + "!" + barcodeValue + oldFilePath.substring(oldFilePath.lastIndexOf("_"));
        file.renameTo(new File(newFilePath));
    }

    public static String renameFileViaDialog() {
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
}
