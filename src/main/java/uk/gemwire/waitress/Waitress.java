package uk.gemwire.waitress;

import at.favre.lib.crypto.bcrypt.BCrypt;
import uk.gemwire.waitress.config.Config;
import uk.gemwire.waitress.config.TOMLReader;
import uk.gemwire.waitress.web.Server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
 * - ~~Configuration reading~~
 * - ~~Web serving~~ note: still wip
 * - Password management
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
            System.err.println("The cfg argument requires a =PATH_TO_FILE segment.");
            return;
        }

        try {
            // Read Config
            HashMap<String, String> map = TOMLReader.read(new FileReader(parts[1]));

            // DEBUG output
            System.out.println(map);
            // Read config into the {@link Config} fields
            Config.set(map);
            // Start the server with the loaded config.
            Server.start();
        } catch (Exception ignored) {}

        System.exit(0);
    }

    /**
     * Hash the password in the given folder, and write it in-place.
     * BCrypt is used for hashing.
     *
     * The argument is expected to be in the form <code>hash-password=FILE_LOCATION</code>.
     * @param argument the command line argument to parse.
     */
    private static void hashPassword(String argument) {
        // Get the file from the argument
        String[] parts = argument.split("=");
        if(parts.length != 2) {
            System.err.println("The hash-password argument requires a =PATH_TO_FILE segment.");
            return;
        }

        byte[] password;

        try {
            password = Files.readAllBytes(Paths.get(parts[1]));
        } catch (IOException e) {
            System.err.println("File " + parts[1] + " can't be read: " + e.getMessage());
            return;
        }

        // Password is read. Hash it and purge the plaintext password from memory.
        byte[] hash = BCrypt.withDefaults().hash(25, password);
        password = new byte[] { (byte) 255};

        try {
            // Write the hash into the file.
            Files.write(Paths.get(parts[1]), hash, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("File " + parts[1] + " can't be written to: " + e.getMessage());
        }

        // Done.
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
                                    Use this hash in the configuration file to determine the administrator's password.
            """);
    }
}
