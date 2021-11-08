package ml.comet.examples;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * Provides common utilitites used bby examples.
 */
public class Utils {

    /**
     * Waits for user input.
     *
     * @param message the message for the user
     * @return the user input.
     */
    public static String askUserForInputOn(String message) {
        System.out.println(message);
        Scanner scan = new Scanner(System.in);
        String s = scan.next();
        scan.close();
        return s;
    }

    /**
     * Allows getting access to the file from the bundled resources (main/resources).
     *
     * @param name the name of the resource file.
     * @return the resource file or null if not found.
     */
    public static File getResourceFile(String name) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
        if (resource == null) {
            return null;
        }
        return new File(resource.getFile());
    }

    /**
     * Reads resource file to string.
     *
     * @param fileName the name of resource file.
     * @return the string read from resource file.
     * @throws IOException if any I/O exception occurs.
     */
    public static String readResourceToString(String fileName) throws IOException {
        File file = getResourceFile(fileName);
        if (file == null) {
            throw new FileNotFoundException(fileName);
        }
        return FileUtils.readFileToString(file, "UTF-8");
    }
}
