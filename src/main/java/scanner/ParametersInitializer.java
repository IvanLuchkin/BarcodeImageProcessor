package scanner;

import controllers.MainWindowController;
import java.nio.file.Paths;
import tasks.ScanFilesTask;

public class ParametersInitializer {
    private static MainWindowController mainWindowController;

    public ParametersInitializer(MainWindowController mainWindowController) {
        ParametersInitializer.mainWindowController = mainWindowController;
    }

    public static boolean initParams() {
        return initInitialFolder() && initDefaultMaxChainSize() && initExclusiveMaxChainSize();
    }

    private static boolean initInitialFolder() {
        String initFolderParam = mainWindowController.getInitFolderInput().getText();
        if (!initFolderParam.trim().equals("")) {
            ScanFilesTask.setInitFolder(Paths.get(initFolderParam));
            return true;
        } else {
            mainWindowController.getErrorLabel().setText("Invalid path");
            return false;
        }
    }

    private static boolean initExclusiveMaxChainSize() {
        String exclusiveMaxAngleCountParam =
                mainWindowController.getMaxChainSizeExclusiveInput().getText();
        if (!exclusiveMaxAngleCountParam.isEmpty()) {
            try {
                int exclMaxChainSize = Integer.parseInt(exclusiveMaxAngleCountParam);
                if (exclMaxChainSize < 20 && exclMaxChainSize > 0) {
                    ScanFilesTask.setMaxChainSizeExclusive(exclMaxChainSize);
                    return true;
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                mainWindowController.getErrorLabel()
                        .setText("Wrong parameter for first product angle count");
                return false;
            }
        } else {
            ScanFilesTask.setMaxChainSizeExclusive(-1);
            return true;
        }
    }

    private static boolean initDefaultMaxChainSize() {
        String defaultMaxAngleSizeParam = 
                mainWindowController.getMaxChainSizeDefaultInput().getText();
        if (!defaultMaxAngleSizeParam.isEmpty()) {
            try {
                int defMaxChainSize = Integer.parseInt(defaultMaxAngleSizeParam);
                if (defMaxChainSize < 20 && defMaxChainSize > 0) {
                    ScanFilesTask.setMaxChainSizeDefault(defMaxChainSize);
                    return true;
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                mainWindowController.getErrorLabel()
                        .setText("Wrong parameter for default max angle count");
                return false;
            }
        } else {
            ScanFilesTask.setMaxChainSizeDefault(2);
            return true;
        }
    }
}
