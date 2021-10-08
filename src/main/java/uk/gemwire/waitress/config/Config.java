package uk.gemwire.waitress.config;

import java.util.HashMap;

/**
 * After the TOML configuration is read via the {@link TOMLReader}, the fields here are populated.
 * These options can change during the runtime of the program, via the web interface.
 */
public final class Config {

    // The port number that the web server listens on.
    public static int LISTEN_PORT = 0;

    // The folder where repository data is stored.
    public static String DATA_DIR = "";

    public static String ADMIN_USERNAME = "";
    public static String ADMIN_HASH = "";

    /**
     * Set the config values in the above fields.
     * Allows these fields to be used to access the below configuration values from any point in the program,
     * without propagating the configuration map to every function that reads it.
     *
     * @param args The map of config name -> value to be used to set the fields.
     */
    public static void set(HashMap<String, String> args) {
        assert args.containsKey("listen_port");
        LISTEN_PORT = Integer.parseInt(args.get("listen_port"));


        assert args.containsKey("data_dir");
        DATA_DIR = args.get("data_dir");
        assert args.containsKey("username");
        ADMIN_USERNAME = args.get("username");
        assert args.containsKey("password");
        ADMIN_HASH = args.get("password");
    }
}
