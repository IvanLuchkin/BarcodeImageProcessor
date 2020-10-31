import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    static Path RESFOLDER;
    static Path INITFOLDER;
    static File[] FILES;
    static int MAX_CHAIN_SIZE_DEFAULT;
    static int MAX_CHAIN_SIZE_EXCLUSIVE;
    static final Map<File, String> toBeRenamed = new HashMap<>();

    public static void main(String...args) {
        Scanner input = new Scanner(System.in);
        String[] params = input.nextLine().split(" +");
        INITFOLDER = Paths.get(params[0]);
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
                System.out.println("Incorrect argument(s) for maximum angle count. Please, restart the program with integer argument.");
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
                System.out.println("Incorrect argument for maximum angle count. Please, restart the program with integer argument.");
                System.exit(1);
            }
        } else {
            MAX_CHAIN_SIZE_DEFAULT = 2;
            MAX_CHAIN_SIZE_EXCLUSIVE = -1;
            System.out.println("No arguments for maximum angle count provided. Running with default value: 2.");
        }

        File[] files = new File(INITFOLDER.toString()).listFiles();
        if (files == null) {
            System.out.println("Folder does not exist or does not contain any files.");
            System.exit(1);
        }
        FILES = Arrays.stream(files).filter(V -> (V.getName().contains(".jpg") ||
                                                             V.getName().contains(".JPG") ||
                                                             V.getName().contains(".jpeg") ||
                                                             V.getName().contains(".JPEG") ||
                                                             V.getName().contains(".png") ||
                                                             V.getName().contains(".tif"))).toArray(File[]::new);
        checkForIncorrectlyRenamed();
        Arrays.sort(FILES, (prev, next) -> {
            String prevName = prev.getName();
            String nextName = next.getName();
            String prevID = prevName.substring(prevName.length() - 8, prevName.length() - 4);
            String nextID = nextName.substring(nextName.length() - 8, nextName.length() - 4);
            if (prevID.equals("") || nextID.equals("")) return 0;
            return Integer.parseInt(prevID) - Integer.parseInt(nextID);
        });
        try {
            RESFOLDER = Paths.get(INITFOLDER + "/res");
            Files.createDirectory(RESFOLDER);
        } catch (FileAlreadyExistsException faee) {
            System.out.println("Result folder already exists. File processing continues.");

        } catch (IOException ioe) {
            System.out.println("Could not create result folder.");
        }
        processFiles(0, 0,"NO_RES_YET", "");
    }

    public static void processFiles(int fileIndex, int counter, String lastResult, String prevFileType) {
        String lastBarcodeResult = lastResult;
        if (MAX_CHAIN_SIZE_EXCLUSIVE == -1) {
            if (counter > MAX_CHAIN_SIZE_DEFAULT) {
                System.out.println("Too many failed scans starting from " + lastBarcodeResult + ".");
                return;
            }
        } else {
            if (counter > MAX_CHAIN_SIZE_EXCLUSIVE) {
                System.out.println(MAX_CHAIN_SIZE_EXCLUSIVE);
                System.out.println("!Too many failed scans starting from " + lastBarcodeResult + ".");
                return;
            }
        }
        if (fileIndex >= FILES.length) {
            executeTransaction();
            return;
        }
        File file = FILES[fileIndex];
        String result = getResult(file);

        switch(result.length()) {
            case(38) :
                switch (prevFileType) {
                    case "ANGLE" :
                        toBeRenamed.put(file, RESFOLDER.toString() + "/" + lastBarcodeResult + "_" + counter + ".jpg");
                        break;
                    case "BARCODE" :
                        toBeRenamed.put(file, RESFOLDER.toString() + "/" + lastBarcodeResult  + ".jpg");
                        break;
                }
                processFiles(++fileIndex, ++counter, lastBarcodeResult, "ANGLE");
                break;
            case(5) :
                if (lastBarcodeResult.equals(result)) {
                    switch (prevFileType) {
                        case "ANGLE" :
                            toBeRenamed.put(file, RESFOLDER.toString() + "/" + lastBarcodeResult + "_" + counter + ".jpg");
                            break;
                        case "BARCODE" :
                            toBeRenamed.put(file, RESFOLDER.toString() + "/" + lastBarcodeResult  + ".jpg");
                            break;
                    }
                    processFiles(++fileIndex, ++counter, lastBarcodeResult, "ANGLE");
                } else {
                    if (!lastBarcodeResult.equals("NO_RES_YET")) MAX_CHAIN_SIZE_EXCLUSIVE = -1;
                    lastBarcodeResult = result;
                    executeTransaction();
                    toBeRenamed.clear();
                    toBeRenamed.put(file, RESFOLDER.toString() + "/_" + lastBarcodeResult + "_" + file.getName());
                    processFiles(++fileIndex, 0, lastBarcodeResult, "BARCODE");
                }
                break;
            default :
                System.out.println("Invalid scan result on " + file.getName() + ".");
                break;
        }

         /*
        if (result.equals("Could not find a barcode on the image.")) {
            switch (prevFileType) {
                case "CHILD" :
                    toBeRenamed.put(file, RESFOLDER.toString() + "/" + lastBarcodeResult + "_" + counter + ".jpg");
                    break;
                case "PARENT" :
                    toBeRenamed.put(file, RESFOLDER.toString() + "/" + lastBarcodeResult  + ".jpg");
                    break;
            }
            processFiles(++fileIndex, ++counter, lastBarcodeResult, "CHILD");
        } else if(result.length() == 5) {
            if (lastBarcodeResult.equals(result)) {
                toBeRenamed.put(file, RESFOLDER.toString() + "/" + lastBarcodeResult  + ".jpg");
                processFiles(++fileIndex, ++counter, lastBarcodeResult, "CHILD");
            } else {
                lastBarcodeResult = result;
                executeTransaction();
                toBeRenamed.clear();
                toBeRenamed.put(file, RESFOLDER.toString() + "/_" + lastBarcodeResult + "_" + file.getName());
                processFiles(++fileIndex, 0, lastBarcodeResult, "PARENT");
            }
        } else {
            System.out.println("Invalid scan result. Please, review the source folder.");
        }

          */

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
            if (name.contains("!")) return name.substring(1, 6);
            LuminanceSource src = new BufferedImageLuminanceSource(ImageIO.read(file.toURI().toURL()));
            BinaryBitmap imageBitmap = new BinaryBitmap(new HybridBinarizer(src));
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
        } catch (IOException ioe) {
            System.out.println("File reading issue.");
            return "File reading issue.";
        }
        return "NOTRACE";
    }
}