package uk.gemwire.waitress;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gemwire.waitress.authentication.Auth;
import uk.gemwire.waitress.config.Config;
import uk.gemwire.waitress.config.TOMLReader;
import uk.gemwire.waitress.permissions.PermissionStore;
import uk.gemwire.waitress.permissions.entity.User;
import uk.gemwire.waitress.web.RepoCache;
import uk.gemwire.waitress.web.Server;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

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
 * - ~~Password management~~
 * - ~~Repository reading~~
 * - Permission management
 * - Upload
 * - Download
 * - Management Pages
 *
 * @author Curle
 */
public class Waitress {

    // Anonymous logger instance for the tool.
    public static final Logger LOGGER = LoggerFactory.getLogger("Waitress");

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
            if(arg.startsWith("-hash-password"))
                hashPassword(arg);
            if(arg.startsWith("-cfg"))
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
            // Read config into the {@link Config} fields
            Config.set(map);
            // Prepare password authentication maps.
            Auth.setupAuth();
            // Read permissions from disk.
            PermissionStore.setupPermissions();
            // Cache all known repositories.
            RepoCache.enumerate();

            LOGGER.info("Set up. Starting route management.");
            // Start the server with the loaded config.
            Server.start();
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
        }

        LOGGER.info("Shutting down. Saving passwords and permission caches.");

        try {
            Auth.writeMap();
        } catch (IOException e) {
            LOGGER.error("Unable to write passwords to disk. Error: " + e.getMessage());
        }

        try {
            // Deserialize all users from disk, should they be there.
            Gson gson = new Gson();
            Writer permsWriter = new FileWriter(Config.DATA_DIR + "waitress/permissions.json");

            gson.toJson(PermissionStore.getInstance(), permsWriter);
        } catch (IOException e) {
            LOGGER.error("Unable to write permissions to disk. Error: " + e.getMessage());
        }

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
            System.out.println("The hash-password argument requires a =PATH_TO_FILE segment.");
            System.exit(0);
        }

        byte[] hash;

        // This block means the password is purged from memory as soon as the hash is created.
        {
            byte[] password;

            try {
                password = Files.readAllBytes(Paths.get(parts[1]));
            } catch (IOException e) {
                System.out.println("File " + parts[1] + " can't be read: " + e.getMessage());
                System.exit(-1);
                return;
            }

            // Password is read. Hash it and purge the plaintext password from memory.
            hash = Auth.createPasswordHash(password);
        }

        try {
            // Write the hash into the file.
            Files.write(Paths.get(parts[1]), hash, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("File " + parts[1] + " can't be written to: " + e.getMessage());
        }

        LOGGER.info("Password hashed and saved into the same file.");

        // Done.
        System.exit(0);
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
