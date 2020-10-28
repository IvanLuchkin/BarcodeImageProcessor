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
    /**
     * upper limit of photos
     * export valid groups of images in transactions to another folder
     */

    static Path RESFOLDER;
    static Path INITFOLDER;
    static File[] FILES;

    public static void main(String...args) {
        INITFOLDER = Paths.get("/Users/ivanluchkin/Downloads/barcodes");
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
        Arrays.sort(FILES, new Comparator<File>() {
            @Override
            public int compare(File prev, File next) {
                String prevName = prev.getName();
                String nextName = next.getName();
                String prevID = prevName.substring(prevName.length() - 8, prevName.length() - 4);
                String nextID = nextName.substring(nextName.length() - 8, nextName.length() - 4);
                if (prevID.equals("") || nextID.equals("")) return 0;
                return Integer.parseInt(prevID) - Integer.parseInt(nextID);
            }
        });
        try {
            RESFOLDER = Paths.get("/Users/ivanluchkin/Downloads/barcodes" + "/res");
            Files.createDirectory(RESFOLDER);
        } catch (FileAlreadyExistsException faee) {
            System.out.println("Result folder already exists.");

        } catch (IOException ioe) {
            System.out.println("Could not create result folder.");
        }
        showFiles(0, 0,"NO_RES_YET", "");
    }

    public static void showFiles(int fileIndex, int counter, String lastResult, String prevFileType) {
        String lastBarcodeResult = lastResult;
        if (fileIndex >= FILES.length) return;
        File file = FILES[fileIndex];
        String result = getResult(file);
        if (result.equals("Could not find a barcode on the image.")) {
            switch (prevFileType) {
                case "CHILD" : file.renameTo(new File(INITFOLDER.toString() + "/" + lastBarcodeResult + "_" + counter + ".jpg")); break;
                case "PARENT" : file.renameTo(new File(INITFOLDER.toString() + "/" + lastBarcodeResult  + ".jpg")); break;
            }
            showFiles(++fileIndex, ++counter, lastBarcodeResult, "CHILD");
        } else {
            lastBarcodeResult = result;
            file.renameTo(new File(INITFOLDER.toString() + "/_" + file.getName()));
            showFiles(++fileIndex, 0, lastBarcodeResult, "PARENT");
        }

    }

    private static String getResult(File file) {
        try {
            System.out.println(file.getName());
            if (file.getName().contains("!")) {
                String result = file.getName().substring(1, 6);
                System.out.println(result);
                return result;
            }
            LuminanceSource src = new BufferedImageLuminanceSource(ImageIO.read(file.toURI().toURL()));
            BinaryBitmap imageBitmap = new BinaryBitmap(new HybridBinarizer(src));
            Collection<Result> results = new ArrayList<>(1);
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
            file.renameTo(new File(INITFOLDER.toString() + "/_" + file.getName()));

            for (Result result : results) {
                System.out.println(result.getText());
                return result.getText();
            }

        } catch (NotFoundException nfe) {
            System.out.println("Could not find a barcode on the image.");
            return "Could not find a barcode on the image.";
        } catch (IOException ioe) {
            System.out.println("File reading issue.");
            return "File reading issue.";
        }
        return "NOTRACE";
    }
}