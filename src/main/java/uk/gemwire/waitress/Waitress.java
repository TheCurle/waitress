package uk.gemwire.waitress;

import uk.gemwire.waitress.config.TOMLReader;

import java.io.FileReader;
import java.util.HashMap;

/**
 * Entry point to the Waitress repository manager.
 *
 * The normal boot process:
 *      Loads in the configured admin account with root permissions.
 *      Loads the maven data folder.
 *      Indexes the maven groups + projects that are stored locally.
 *
 *
 * Temporary to-do list for the whole project:
 * - Configuration reading
 * - Password management
 * - Web serving
 * - Repository reading
 * - Permission management
 * - Upload
 * - Download
 * - Management Pages
 *
 * @author Curle
 */
public class Waitress {

    /**
     * Requires one argument.
     * If hash-password, then jump to the {@link #hashPassword(String)} function.
     * If cfg, then jump to the {@link #setup(String)} function.
     * If none, then jump to the {@link #instructUsage} function.
     *
     * Intentionally left as basic as possible.
     *
     */
    public static void main(String[] args) {
        for(String arg : args) {
            if(arg.startsWith("hash-password"))
                hashPassword(arg);
            if(arg.startsWith("cfg"))
                setup(arg);
        }

        // If none given, print a message and exit.
        instructUsage();
    }

    /**
     * Read the config file.
     * Setup the maven manager. Start the web server.
     * Spin up a few threads to handle requests.
     *
     * The argument is expected to be in the form <code>cfg=FILE_LOCATION</code>.
     * @param argument the command line argument to parse.
     */
    private static void setup(String argument) {
        // Get the directory from the argument
        String[] parts = argument.split("=");
        if(parts.length != 2) {
            System.err.println("The cfg argument requires a =PATH_TO_FILE appended.");
            return;
        }

        try {
            HashMap<String, String> map = TOMLReader.read(new FileReader(parts[1]));
            System.out.println(map);
        } catch (Exception ignored) {}

        System.exit(0);
    }

    /**
     * Hash the password in the given folder, and write it in-place.
     *
     * The hashing algorithm used is PBKDF2WithHmacSHA1, provided by javax.crypto.
     *
     * The argument is expected to be in the form <code>hash-password=FILE_LOCATION</code>.
     * @param argument the command line argument to parse.
     */
    private static void hashPassword(String argument) {
        // TODO: hashPassword
    }


    /**
     * Print an instructional message on the screen when the program is executed in an invalid way.
     */
    private static void instructUsage() {
          System.err.println("""
            Waitress Repository Manager v1 - Gemwire Institute
            ***************************************
            Usage: java -jar waitress.jar [-cfg=FILE | -hash-password=FILE]
                   -cfg: Bootstrap Configuration File. Required for startup.
                   -hash-password: Take the text in the given file and hash it in-place.
            """);
    }
}
