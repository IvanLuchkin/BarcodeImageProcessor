package scanner;

import static filesystem.FilesLoader.loadFiles;
import static filesystem.FolderCreator.createResFolder;
import static scanner.ParametersInitializer.initParams;

import controllers.MainWindowController;
import tasks.ScanFilesTask;

public class ImageProcessor {
    private final MainWindowController mainWindowController;

    public ImageProcessor(MainWindowController mainWindowController) {
        this.mainWindowController = mainWindowController;
    }

    public void scan() {
        mainWindowController.getImageScrollPane().setVisible(false);
        if (!initParams()) {
            return;
        }
        if (ScanFilesTask.getTotalFiles() == 0) {
            ScanFilesTask.setTotalFiles(loadFiles());
        }
        loadFiles();
        if (!createResFolder()) {
            return;
        }
        ScanFilesTask scanFilesTask = new ScanFilesTask(mainWindowController);
        mainWindowController.getProgressBar().progressProperty()
                .bind(scanFilesTask.progressProperty());
        Thread thread = new Thread(scanFilesTask);
        thread.setDaemon(true);
        thread.start();
    }
}
