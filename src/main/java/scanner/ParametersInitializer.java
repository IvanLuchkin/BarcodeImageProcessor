package scanner;

import java.nio.file.Paths;
import java.util.List;
import javafx.scene.control.TextField;
import tasks.ScanFilesTask;

public class ParametersInitializer {
    private static TextField initFolderInput;
    private static TextField maxChainSizeDefaultInput;
    private static TextField maxChainSizeExclusiveInput;

    public static ExitCode initParams() {
        List<ExitCode> initializationResults = List.of(initInitialFolder(),
                initDefaultMaxChainSize(),
                initExclusiveMaxChainSize());
        return initializationResults.stream()
                .filter(ExitCode::isAbortive)
                .findFirst().orElse(ExitCode.SUCCESS);
    }

    private static ExitCode initInitialFolder() {
        String initFolderParam = initFolderInput.getText();
        if (!initFolderParam.trim().equals("")) {
            ScanFilesTask.setInitFolder(Paths.get(initFolderParam));
            return ExitCode.SUCCESS;
        } else {
            return ExitCode.INVALID_PATH;
        }
    }

    private static ExitCode initExclusiveMaxChainSize() {
        String exclusiveMaxAngleCountParam = maxChainSizeExclusiveInput.getText();
        if (!exclusiveMaxAngleCountParam.isEmpty()) {
            try {
                int exclMaxChainSize = Integer.parseInt(exclusiveMaxAngleCountParam);
                if (exclMaxChainSize < 20 && exclMaxChainSize > 0) {
                    ScanFilesTask.setMaxChainSizeExclusive(exclMaxChainSize);
                    return ExitCode.SUCCESS;
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                return ExitCode.WRONG_PARAM_EXCL;
            }
        } else {
            ScanFilesTask.setMaxChainSizeExclusive(-1);
            return ExitCode.SUCCESS;
        }
    }

    private static ExitCode initDefaultMaxChainSize() {
        String defaultMaxAngleSizeParam = maxChainSizeDefaultInput.getText();
        if (!defaultMaxAngleSizeParam.isEmpty()) {
            try {
                int defMaxChainSize = Integer.parseInt(defaultMaxAngleSizeParam);
                if (defMaxChainSize < 20 && defMaxChainSize > 0) {
                    ScanFilesTask.setMaxChainSizeDefault(defMaxChainSize);
                    return ExitCode.SUCCESS;
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return ExitCode.WRONG_PARAM_DEF;
            }
        } else {
            ScanFilesTask.setMaxChainSizeDefault(2);
            return ExitCode.SUCCESS;
        }
    }

    public static void setInitFolderInput(TextField initFolderInput) {
        ParametersInitializer.initFolderInput = initFolderInput;
    }

    public static void setMaxChainSizeDefaultInput(TextField maxChainSizeDefaultInput) {
        ParametersInitializer.maxChainSizeDefaultInput = maxChainSizeDefaultInput;
    }

    public static void setMaxChainSizeExclusiveInput(TextField maxChainSizeExclusiveInput) {
        ParametersInitializer.maxChainSizeExclusiveInput = maxChainSizeExclusiveInput;
    }
}
