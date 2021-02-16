package filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import scanner.ExitCode;
import tasks.ScanFilesTask;

public class FolderCreator {
    public static ExitCode createResFolder() {
        try {
            Path resultFolderPath =
                    Paths.get(ScanFilesTask.getInitFolder() + File.separator + "res");
            ScanFilesTask.setResFolder(resultFolderPath);
            Files.createDirectory(resultFolderPath);
            return ExitCode.SUCCESS;
        } catch (FileAlreadyExistsException e) {
            return ExitCode.SUCCESS;
        } catch (IOException e) {
            return ExitCode.RES_FOLDER_CREATION_FAILED;
        }
    }
}
