package filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import tasks.ScanFilesTask;

public class FolderCreator {
    public static boolean createResFolder() {
        try {
            ScanFilesTask.setResFolder(Paths.get(
                    ScanFilesTask.getInitFolder() + File.separator + "res"));
            Files.createDirectory(ScanFilesTask.getResFolder());
            return true;
        } catch (FileAlreadyExistsException e) {
            return true;
        } catch (IOException e) {
            //errorLabel.setText("Could not create result folder.");
            return false;
        }
    }
}
