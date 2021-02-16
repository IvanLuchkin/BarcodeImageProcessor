package scanner;

import javafx.scene.control.Label;

public enum ExitCode {
    TOO_MANY_FAILED_SCANS("Too many failed scans"),
    WRONG_PARAM_DEF("Wrong parameter for default max angle count"),
    WRONG_PARAM_EXCL("Wrong parameter for first product angle count"),
    INVALID_PATH("Invalid path"),
    EMPTY_FOLDER("Folder does not exist or does not contain any files"),
    RES_FOLDER_CREATION_FAILED("Could not create folder for results"),
    INVALID_SCAN_RESULT("Invalid scan result"),
    SUCCESS("Scanning finished successfully"),
    INVALID_FIRST_FILE("The first image is not barcode image"),
    MORE_ANGLES_REQUIRED("First product has more angles than specified"),
    INCORRECT_BARCODE_VALUE("Barcode value was not specified");

    private static Label errorLabel;
    private final String message;

    ExitCode(String message) {
        this.message = message;
    }

    public void showMessage() {
        errorLabel.setText(this.message);
    }

    public boolean isAbortive() {
        return !this.equals(ExitCode.SUCCESS);
    }

    public static void setErrorLabel(Label errorLabel) {
        ExitCode.errorLabel = errorLabel;
    }
}
