package pt.tecnico.rrrd.crypto;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DataOperations {

    public static void writeFile(String filename, String data) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filename, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert writer != null;
        writer.print(data);
        writer.close();
    }

    public static String[] readFile(String filename) {
        String[] data = null;
        try {
            data = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8).toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert data != null;
        return data;
    }

    public static void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

}
