package uk.gemwire.waitress.web;


import io.javalin.Javalin;
import io.javalin.http.Context;
import uk.gemwire.waitress.config.Config;

/**
 * Web server.
 * Hosts the files. Manages endpoints.
 *
 * The logic for Maven coordinates, and where they are, is handled in the Maven subclass.
 * More information on that is on that class.
 *
 * Most of the magic is in the serve method.
 *
 * @author Curle
 */
public class Server {

    /**
     * Sets up all the routing.
     *
     * By default, all maven coordinates are handled by a wrapper on "/".
     * All endpoints are handled by their own wrapper.
     */
    public static void start() {
        Javalin server = Javalin.create().start(Config.LISTEN_PORT);
        server.get("/", Server::serve);
    }

    public static void serve(Context request) {

    }
}
