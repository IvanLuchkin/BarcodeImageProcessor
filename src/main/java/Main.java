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
    static final int MAX_CHAIN_SIZE = 7;
    static Map<File, String> toBeRenamed = new HashMap<>();

    public static void main(String...args) {
        INITFOLDER = Paths.get("/Users/ivanluchkin/Downloads/mcopy");
        File[] files = new File(INITFOLDER.toString()).listFiles();
        if (files == null) {
            System.out.println("No files found in this folder.");
            System.exit(1);
        }
        FILES = Arrays.stream(files).filter(V -> (V.getName().contains(".jpg") ||
                                                             V.getName().contains(".JPG") ||
                                                             V.getName().contains(".jpeg") ||
                                                             V.getName().contains(".JPEG") ||
                                                             V.getName().contains(".png") ||
                                                             V.getName().contains(".tif"))).toArray(File[]::new);
        System.out.println(Arrays.toString(files));
        System.out.println(Arrays.toString(FILES));
        File[] copy = Arrays.copyOf(FILES, FILES.length);
        Arrays.sort(copy, Comparator.naturalOrder());
        System.out.println(Arrays.toString(copy));
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
            System.out.println("Result folder already exists.");

        } catch (IOException ioe) {
            System.out.println("Could not create result folder.");
        }
        processFiles(0, 0,"NO_RES_YET", "");
    }

    public static void processFiles(int fileIndex, int counter, String lastResult, String prevFileType) {
        String lastBarcodeResult = lastResult;
        if (counter > MAX_CHAIN_SIZE) {
            System.out.println("Too many failed scans. Please, review the source folder.");
            return;
        }
        if (fileIndex >= FILES.length) return;
        File file = FILES[fileIndex];
        String result = getResult(file);
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
        } else {
            lastBarcodeResult = result;
            executeTransaction(toBeRenamed);
            toBeRenamed.clear();
            toBeRenamed.put(file, RESFOLDER.toString() + "/_" + lastBarcodeResult + "_" + file.getName());
            processFiles(++fileIndex, 0, lastBarcodeResult, "PARENT");
        }

    }

    private static void executeTransaction(Map<File, String> map) {
        for (Map.Entry<File, String> entry : map.entrySet()) {
            entry.getKey().renameTo(new File(entry.getValue()));
        }
    }

    private static String getResult(File file) {
        try {
            if (file.getName().contains("!")) {
                String result = file.getName().substring(1, 6);
                return result;
            }
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
                System.out.println(result.getText());
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