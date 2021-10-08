package uk.gemwire.waitress.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.config.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // The regex that filters "traditional" valid Maven Coordinates.
    // Courtesy of AterAnimAvis.
    private static final Pattern MAVEN_PATTERN = Pattern.compile("^/(?<group>(\\w+\\/)+)(?<artifact>[\\w-]+)\\/(?<version>[\\d.\\-\\w]+)\\/\\3-\\4(?<classifier>-\\w+)?\\.(?<extension>\\w+)$");

    /**
     * Handle a GET request to an endpoint.
     * Searches for the endpoint in question.
     * If none matches, a 404 will be returned.
     * @param request The metadata of the request.
     */
    private static void getEndpoint(Context request) {
        Waitress.LOGGER.warn("Invalid request " + request.path() + " cannot be handled.");
        request.status(400);


    }

    /**
     * Handle a GET request to a maven coordinate.
     * Searches for the coordinate in question.
     * If none matches, it will search for an endpoint. See {@link #getEndpoint(Context)}
     * @param request The metadata of the request.
     */
    private static void getMaven(Context request) {
        // We need to determine what's in the url.
        // We can assume that we won't hit this for the endpoints.
        // so, it's a matter of looking for /group/file/version/file-version.ext

        // Note that this is more complicated because of the fact that repo proxies exist.
        // We need to be able to reorganize any request into something appropriate for another repository.

        final Matcher matcher = MAVEN_PATTERN.matcher(request.path());

        // Early exit if this isn't a valid coordinate.
        if(!matcher.matches()) {
            getEndpoint(request);
            return;
        }

        // Set up components of the request.
        String groupID = matcher.group("group");
        final String artifactID = matcher.group("artifact");
        final String version = matcher.group("version");
        String classifier = matcher.group("classifier");
        final String extension = matcher.group("extension");

        // Cleanup the group. Sometimes it ends with a "/".
        if (groupID.charAt(groupID.length() - 1) == '/')
            groupID = groupID.substring(0, groupID.length() - 1);

        // Sometimes the classifier is null. We need it to be "".
        if (classifier == null)
            classifier = "";

        Waitress.LOGGER.info("Request for " + groupID + "/" + artifactID +  "/" + version + "/" + artifactID +  "-" + version + classifier + "." + extension + " located. Checking whether we can handle it..");

        if (RepoCache.contains(groupID, artifactID, version, classifier, extension)) {
            Waitress.LOGGER.info("Requested file is in the cache. Serving..");
            try {
                // TODO: binary stream non-text files.
                request.result(new FileInputStream(Config.DATA_DIR + groupID + "/" + artifactID + "/" + version + "/" + artifactID + "-" + version + classifier + "." + extension));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Waitress.LOGGER.warn("File not handled by this repository. Mirroring not implemented.");
            request.status(404);
            request.result("Not Found");
        }

    }

    /**
     * Sets up all the routing.
     *
     * By default, all maven coordinates are handled by a wrapper on "/".
     * TODO: All endpoints are handled by their own wrapper.
     */
    public static void start() throws InterruptedException {
        Javalin server = Javalin.create().start(Config.LISTEN_PORT);

        server.get("/*", Server::getMaven);

        Waitress.LOGGER.info("Server started. Waiting for requests.");

        // Deadlock the current thread to keep it active.
        Thread.currentThread().join();
    }

}
