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
    public static boolean SHOULD_PROXY = false;
    public static String PROXY_REPO = "";
    public static String USER_DATA = "";
    public static String PERM_DATA = "";

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

        if (args.containsKey("proxy")){
            SHOULD_PROXY = Boolean.parseBoolean(args.get("proxy"));
            if (SHOULD_PROXY) {
                assert args.containsKey("proxy_repo");
                PROXY_REPO = args.get("proxy_repo");
            }
        }
        assert args.containsKey("user_data");
        USER_DATA = args.get("user_data");
        assert args.containsKey("permission_data");
        PERM_DATA = args.get("permission_data");

        System.out.println("Port: " + LISTEN_PORT);
        System.out.println("Data directory: " + DATA_DIR);
        System.out.println("Admin username: " + ADMIN_USERNAME);
        System.out.println("Admin hash: " + ADMIN_HASH);
        if (SHOULD_PROXY){
            System.out.println("Proxy enabled, url: " + PROXY_REPO);
        }
        System.out.println("User data path: " + USER_DATA);
    }
}
