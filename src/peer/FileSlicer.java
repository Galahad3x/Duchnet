package peer;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Used to slice large files in 25 MB slices
 */
public class FileSlicer {

    /**
     * Run a program used to split a file into chunks or join chunks into a file manually
     *
     * @param args System arguments. not used
     * @throws IOException If IO fails
     */
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("COMMAND: ");
        File f;
        if ("split".equals(scanner.nextLine())) {
            Scanner scanner2 = new Scanner(System.in);
            System.out.println("Select the route of the folder: ");
            f = new File(scanner2.nextLine());
            System.out.println("Select the name of the file: ");
            String search = scanner2.nextLine();
            File file = new File(f.getCanonicalPath() + "/" + search);
            splitFile(file);
        } else {
            Scanner scanner2 = new Scanner(System.in);
            System.out.println("Select the route of the folder: ");
            f = new File(scanner2.nextLine());
            System.out.println("Type original filename: ");
            String search = scanner2.nextLine();
            List<File> files = new ArrayList<>(List.of(Objects.requireNonNull(f.listFiles(file -> file.getName().endsWith(search) && !file.getName().equals(search)))));
            files.sort(Comparator.comparing(File::getName));
            System.out.println(files);
            mergeFilesOriginal(files, new File(search));
        }
    }

    /**
     * Split the given files into chunks
     *
     * @param f original file
     * @throws IOException If writing or reading fails
     */
    public static void splitFile(File f) throws IOException {
        int partCounter = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        int sizeOfFiles = 1024 * 1024 * 25;// 25 MB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = f.getName();
        int number_of_digits = Integer.toString((int) Math.ceil(f.length() / (float) sizeOfFiles)).length();
        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                //write each chunk of data into separate file with different number in name
                String filePartName = String.format(".%0" + number_of_digits + "d%s", partCounter++, fileName);
                File newFile = new File(f.getParent(), filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
    }

    /**
     * Merge some chunks into the original file
     *
     * @param files All the chunks
     * @param into  The resulting whole file
     * @throws IOException If writing or reading fails
     */
    public static void mergeFilesOriginal(List<File> files, File into)
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(into);
             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
        }
    }
}