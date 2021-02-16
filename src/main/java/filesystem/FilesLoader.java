package filesystem;

import java.io.File;
import java.util.Arrays;
import scanner.ExitCode;
import tasks.ScanFilesTask;

public class FilesLoader {
    public static void filterFiles() {
        ScanFilesTask.setFiles(Arrays.stream(ScanFilesTask.getFiles())
                .filter(file -> (file.getName().contains(".jpg")
                || file.getName().contains(".JPG")
                || file.getName().contains(".jpeg")
                || file.getName().contains(".JPEG")
                || file.getName().contains(".png")
                || file.getName().contains(".tif"))).toArray(File[]::new));
    }

    public static int loadFiles() {
        File[] files = new File(ScanFilesTask.getInitFolder().toString()).listFiles();
        if (files == null) {
            ExitCode.EMPTY_FOLDER.showMessage();
        }
        ScanFilesTask.setFiles(files);
        filterFiles();
        sortFiles();
        return ScanFilesTask.getFiles().length;
    }

    public static void sortFiles() {
        Arrays.sort(ScanFilesTask.getFiles(), FilesLoader::compareFileNames);
    }

    private static int compareFileNames(File prev, File next) {
        String prevName = prev.getName();
        String nextName = next.getName();
        String prevID = prevName.substring(prevName.length() - 8, prevName.length() - 4);
        String nextID = nextName.substring(nextName.length() - 8, nextName.length() - 4);
        if (prevID.equals("") || nextID.equals("")) {
            return 0;
        }
        return Integer.parseInt(prevID) - Integer.parseInt(nextID);
    }
}
