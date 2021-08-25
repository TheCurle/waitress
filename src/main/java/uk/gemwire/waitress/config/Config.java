package uk.gemwire.waitress.config;

import java.util.Map;

/**
 * After the TOML configuration is read via the {@link TOMLReader}, the fields here are populated.
 * These options can change during the runtime of the program, via the web interface.
 */
public final class Config {

    public static int LISTEN_PORT = 0;

    public static void set(Map<String,String> args) {
        LISTEN_PORT = Integer.parseInt(args.get("listen_port"));
    }
}
