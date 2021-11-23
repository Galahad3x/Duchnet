import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FileSlicer {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("COMMAND: ");
        if ("split".equals(scanner.nextLine())) {
            Scanner scanner2 = new Scanner(System.in);
            System.out.println("Select the route of the folder: ");
            File f = new File(scanner2.nextLine());
            System.out.println("Select the name to search for: ");
            String search = scanner2.nextLine();
            File file = new File(f.getCanonicalPath() + "/" + search);
            split_file(file);
        } else {
            Scanner scanner2 = new Scanner(System.in);
            System.out.println("Select the route of the folder: ");
            File f = new File(scanner2.nextLine());
            System.out.println("Select the name to search for: ");
            String search = scanner2.nextLine();
            List<File> files = new ArrayList<>(List.of(Objects.requireNonNull(f.listFiles(file -> file.getName().startsWith(search)))));
            files.sort(Comparator.comparing(File::getName));
            mergeFiles(files, new File("Prova" + search + ".mp4"));
        }
    }

    public static void split_file(File the_file) throws IOException {
        // 75 MB chunks
        // the_file.length() es bytes
        int slices = (int) Math.ceil((the_file.length() / (1024.0 * 1024.0)) / 50);
        int number_of_digits = Integer.toString(slices).length();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(the_file.getCanonicalPath()));
        for (int i = 0; i < slices; i++) {
            String filename = the_file.getName().split("\\.")[0] + " - " + String.format("%0" + number_of_digits + "d", i);
            System.out.println("STARTING " + filename);
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(the_file.getPath() + filename))) {
                int bytes_read = 0;
                byte[] bytes = new byte[1024 * 1024];
                for (int j = 0; j < 50 || (bytes_read = in.read(bytes, 0, 1024 * 1024)) != -1; j++) {
                    out.write(bytes);
                }
            }
            System.out.println("FINISHED " + filename);
        }
    }

    private static void join_file() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Select the route of the folder: ");
        File f = new File(scanner.nextLine());
        System.out.println("Select the common start to search for: ");
        String search = scanner.nextLine();
        System.out.println("Type the extension of the file: ");
        String extension = scanner.nextLine();
        String filename = search.replace(" - .", ".");
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f.getCanonicalPath() + "/rb" + search + "." + extension));
        File[] files = Objects.requireNonNull(f.listFiles(file -> file.getName().startsWith(search)));
        Arrays.sort(files);
        for (File file : files) {
            System.out.println(file.getName());
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f.getCanonicalPath() + "/" + file.getName()));
            byte[] bytes = new byte[1000 * 1000];
            int bytes_read;
            while ((bytes_read = in.read(bytes, 0, 1000 * 1000)) != -1) {
                out.write(bytes);
            }
        }
    }

    public static void splitFile(File f) throws IOException {
        int partCounter = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        int sizeOfFiles = 1024 * 1024 * 50;// 50 MB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = f.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                //write each chunk of data into separate file with different number in name
                String filePartName = String.format("%s.%03d", fileName, partCounter++);
                System.out.println(filePartName);
                File newFile = new File(f.getParent(), filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
    }

    public static void mergeFiles(List<File> files, File into)
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(into);
             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
        }
    }
}