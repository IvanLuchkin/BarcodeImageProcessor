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
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    static Path RES_FOLDER;
    static Path INIT_FOLDER;
    static File[] FILES;
    static int MAX_CHAIN_SIZE_DEFAULT;
    static int MAX_CHAIN_SIZE_EXCLUSIVE;
    static final Map<File, String> toBeRenamed = new HashMap<>();

    public static void initParams() {
        Scanner input = new Scanner(System.in);
        System.out.println("Enter parameters:\n1st -> path to folder\n2nd (optional) -> max amount of angles for this run\n3rd (optional) -> max amount of angles for the first product.");
        String[] params = input.nextLine().split(" +");
        if (params.length == 0) {
            System.out.println("No arguments provided. Please, try again.");
            initParams();
        }
        INIT_FOLDER = Paths.get(params[0]);
        if (params.length == 3) {
            try {
                int maxChainArgDef = Integer.parseInt(params[1]);
                int maxChainArgExcl = Integer.parseInt(params[2]);
                if (maxChainArgExcl < 11 && maxChainArgDef > 0 && maxChainArgDef < 11 && maxChainArgDef < maxChainArgExcl) {
                    MAX_CHAIN_SIZE_EXCLUSIVE = maxChainArgExcl;
                    MAX_CHAIN_SIZE_DEFAULT = maxChainArgDef;
                } else {
                    System.out.println("Incorrect argument(s) for maximum angle count.");
                    System.exit(1);
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Incorrect argument(s) for maximum angle count.");
                System.exit(1);
            }
        } else if (params.length == 2) {
            try {
                int maxChainArgDef = Integer.parseInt(params[1]);
                if (maxChainArgDef > 0 && maxChainArgDef < 11) {
                    MAX_CHAIN_SIZE_EXCLUSIVE = -1;
                    MAX_CHAIN_SIZE_DEFAULT = maxChainArgDef;
                } else {
                    System.out.println("Incorrect argument for maximum angle count.");
                    System.exit(1);
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Incorrect argument for maximum angle count.");
                System.exit(1);
            }
        } else {
            MAX_CHAIN_SIZE_DEFAULT = 2;
            MAX_CHAIN_SIZE_EXCLUSIVE = -1;
            System.out.println("No arguments for maximum angle count provided. Running with the default value: 2.");
        }
    }

    public static void sortFiles() {
        Arrays.sort(FILES, (prev, next) -> {
            String prevName = prev.getName();
            String nextName = next.getName();
            String prevID = prevName.substring(prevName.length() - 8, prevName.length() - 4);
            String nextID = nextName.substring(nextName.length() - 8, nextName.length() - 4);
            if (prevID.equals("") || nextID.equals("")) return 0;
            return Integer.parseInt(prevID) - Integer.parseInt(nextID);
        });
    }

    public static File[] getFiles() {
        File[] files = new File(INIT_FOLDER.toString()).listFiles();
        if (files == null) {
            System.out.println("Folder does not exist or does not contain any files.");
            System.exit(1);
        }
        return files;
    }

    public static void filterFiles() {
        FILES = Arrays.stream(getFiles()).filter(V -> (V.getName().contains(".jpg") ||
                V.getName().contains(".JPG") ||
                V.getName().contains(".jpeg") ||
                V.getName().contains(".JPEG") ||
                V.getName().contains(".png") ||
                V.getName().contains(".tif"))).toArray(File[]::new);
    }

    public static void createResFolder() {
        try {
            RES_FOLDER = Paths.get(INIT_FOLDER + "/res");
            Files.createDirectory(RES_FOLDER);
        } catch (FileAlreadyExistsException fae) {
            System.out.println("Result folder already exists. File processing continues.");

        } catch (IOException ioe) {
            System.out.println("Could not create result folder.");
        }
    }

    public static void main(String...args) {
        initParams();
        filterFiles();
        checkForIncorrectlyRenamed();
        sortFiles();
        createResFolder();
        System.out.println(processFiles(0, 0,"NO_RES_YET", "NONE"));
    }

    public static void analyzeAngleCounter(int counter, String lastBarcodeResult) {
        if (MAX_CHAIN_SIZE_EXCLUSIVE == -1) {
            if (counter > MAX_CHAIN_SIZE_DEFAULT) {
                System.out.println("Too many failed scans starting from " + lastBarcodeResult + ".");
                System.exit(1);
            }
        } else {
            if (counter > MAX_CHAIN_SIZE_EXCLUSIVE) {
                System.out.println(MAX_CHAIN_SIZE_EXCLUSIVE);
                System.out.println("This product's photos need more angles: " + lastBarcodeResult + ".");
                System.exit(1);
            }
        }
    }

    public static String analyzeScanResult(int fileIndex, int counter, String lastBarcodeResult, String newResult, String prevFileType, File file) {
        switch(newResult.length()) {
            case(38) :
                analyzePreviousFile(prevFileType, lastBarcodeResult, file, counter);
                processFiles(++fileIndex, ++counter, lastBarcodeResult, "ANGLE");
                break;
            case(5) :
                if (lastBarcodeResult.equals(newResult)) {
                    analyzePreviousFile(prevFileType, lastBarcodeResult, file, counter);
                    processFiles(++fileIndex, ++counter, lastBarcodeResult, "ANGLE");
                } else {
                    if (!lastBarcodeResult.equals("NO_RES_YET")) MAX_CHAIN_SIZE_EXCLUSIVE = -1;
                    lastBarcodeResult = newResult;
                    executeTransaction();
                    toBeRenamed.clear();
                    toBeRenamed.put(file, RES_FOLDER.toString() + "/_" + lastBarcodeResult + "_" + file.getName());
                    processFiles(++fileIndex, 0, lastBarcodeResult, "BARCODE");
                }
                break;
            default :
                System.out.println("Invalid scan result on " + file.getName() + ".");
                System.exit(1);
        }
        return "";
    }

    public static void analyzePreviousFile(String prevFileType, String lastBarcodeResult, File file, int counter) {
        switch (prevFileType) {
            case "ANGLE" :
                toBeRenamed.put(file, RES_FOLDER.toString() + "/" + lastBarcodeResult + "_" + counter + ".jpg");
                break;
            case "BARCODE" :
                toBeRenamed.put(file, RES_FOLDER.toString() + "/" + lastBarcodeResult  + ".jpg");
                break;
            case "NONE" :
                System.out.println("The first photo in the folder is not a barcode photo.");
                break;
        }
    }

    public static String processFiles(int fileIndex, int counter, String lastResult, String prevFileType) {
        analyzeAngleCounter(counter, lastResult);
        if (fileIndex >= FILES.length) {
            executeTransaction();
            return ("File processing has been finished successfully.");
        }
        File file = FILES[fileIndex];
        return analyzeScanResult(fileIndex, counter, lastResult, getResult(file), prevFileType, file);
    }

    private static void checkForIncorrectlyRenamed() {
        for (File file : FILES) {
            String name = file.getName();
            if (name.contains("!") && name.length() < 11) {
                System.out.println("File " + name + " was renamed incorrectly.");
                System.exit(1);
            }
        }
    }

    private static void executeTransaction() {
        for (Map.Entry<File, String> entry : Main.toBeRenamed.entrySet()) {
            entry.getKey().renameTo(new File(entry.getValue()));
        }
    }

    private static String getResult(File file) {
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

        } catch (NotFoundException nfe) {
            return "Could not find a barcode on the image.";
        } catch (IOException | ImageReadException ioe) {
            System.out.println("File reading issue.");
            return "File reading issue.";
        }
        return "NO_TRACE";
    }
}