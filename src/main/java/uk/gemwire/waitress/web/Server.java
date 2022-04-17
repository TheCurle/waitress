package uk.gemwire.waitress.web;

import io.javalin.Javalin;
import io.javalin.core.security.BasicAuthCredentials;
import io.javalin.core.util.FileUtil;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.jetbrains.annotations.NotNull;
import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.authentication.Auth;
import uk.gemwire.waitress.authentication.CalledTooEarlyException;
import uk.gemwire.waitress.authentication.PermissionLevel;
import uk.gemwire.waitress.authentication.entity.User;
import uk.gemwire.waitress.config.Config;
import uk.gemwire.waitress.web.repository.Artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        MavenCoordinate coordinate = parseCoordinate(request, true);
        if (coordinate == null) return;
        final String groupID = coordinate.groupID;
        final String artifactID = coordinate.artifactID;
        final String version = coordinate.version;
        final String classifier = coordinate.classifier;
        final String extension = coordinate.extension;

        User user = null;
        try{
            if(request.basicAuthCredentialsExist()) {
                BasicAuthCredentials credentials = request.basicAuthCredentials();
                if (!Auth.checkPassword(credentials.getUsername(), credentials.getPassword())) {
                    request.status(401);
                    return;
                }
                user = Auth.getUser(credentials.getUsername()).get();
            } else {
                user = Auth.getUser("anonymous").get();
            }
        }catch (CalledTooEarlyException ignored) {
            // ....I hope this catch case never happens
        }

        Waitress.LOGGER.info("Request for " + groupID + "/" + artifactID +  "/" + version + "/" + artifactID +  "-" + version + classifier + "." + extension + " located. Checking whether we can handle it..");

        if (RepoCache.contains(groupID, artifactID, version, classifier, extension)) {
            Waitress.LOGGER.info("Requested file is in the cache.");
            PermissionLevel permissionLevel = user.getPermissionFor(groupID, artifactID);
            if (permissionLevel.level < PermissionLevel.READ.level) {
                Waitress.LOGGER.info("User " + user.getUsername() + " does not have permission for requested file");
                request.status(403);
                return;
            }
            try {
                // TODO: binary stream non-text files.
                request.result(new FileInputStream(Config.DATA_DIR + groupID + "/" + artifactID + "/" + version + "/" + artifactID + "-" + version + classifier + "." + extension));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            // Download artifact if it does not exist
            Waitress.LOGGER.info("Requested file is not in the cache. Downloading..");
            try {
                // TODO This probably should be async, downloading takes time
                File file = MavenDownloader.getArtifact(groupID, artifactID, version, classifier, extension);
                {
                    Artifact artifact = RepoCache.get(groupID, artifactID);
                    if (artifact == null){
                        artifact = new Artifact(groupID, artifactID);
                        RepoCache.addArtifact(artifact);
                    }
                    // add artifact to cache
                    artifact.addVersion(version, classifier, extension);
                }
                request.result(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                request.status(404);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Handles a PUT request to a maven coordinate
     * Uploads file into cache at provided  maven coordinate
     * If none matches, the request will be ignored
     * @param request The metadata of the request
     */
    private static void setMaven(Context request) {
        MavenCoordinate coordinate = parseCoordinate(request, false);
        if (coordinate == null) return;
        final String groupID = coordinate.groupID;
        final String artifactID = coordinate.artifactID;
        final String version = coordinate.version;
        final String classifier = coordinate.classifier;
        final String extension = coordinate.extension;

        Waitress.LOGGER.info("Upload request for " + groupID + "/" + artifactID +  "/" + version + "/" + artifactID +  "-" + version + classifier + "." + extension + " located.");
        if (RepoCache.contains(groupID, artifactID, version, classifier, extension)) {
            // If the file already exists, return 409
            Waitress.LOGGER.warn("Artifact " + groupID + "/" + artifactID +  "/" + version + "/" + artifactID +  "-" + version + classifier + "." + extension + " already exists!");
            request.status(409);
        } else {
            // Retrieve file from request and save it into cache
            UploadedFile file = request.uploadedFile(artifactID +  "-" + version + classifier + "." + extension);
            if (file == null) {
                request.status(400);
                return;
            }
            FileUtil.streamToFile(file.getContent(), Config.DATA_DIR + groupID + "/" + artifactID +  "/" + version + "/" + artifactID +  "-" + version + classifier + "." + extension);
            request.status(201);

            Artifact artifact = RepoCache.get(groupID, artifactID);
            if (artifact == null){
                artifact = new Artifact(groupID, artifactID);
                RepoCache.addArtifact(artifact);
            }
            artifact.addVersion(version, classifier, extension);
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
        server.put("/*", Server::setMaven);

        Waitress.LOGGER.info("Server started. Waiting for requests.");

        // Deadlock the current thread to keep it active.
        Thread.currentThread().join();
    }

    private static MavenCoordinate parseCoordinate(Context request, boolean getEndpoint) {
        // We need to determine what's in the url.
        // We can assume that we won't hit this for the endpoints.
        // so, it's a matter of looking for /group/file/version/file-version.ext

        // Note that this is more complicated because of the fact that repo proxies exist.
        // We need to be able to reorganize any request into something appropriate for another repository.

        final Matcher matcher = MAVEN_PATTERN.matcher(request.path());

        // Early exit if this isn't a valid coordinate.
        if(!matcher.matches()) {
            if (getEndpoint) getEndpoint(request);
            return null;
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
        return new MavenCoordinate(groupID, artifactID, version, classifier, extension);
    }

    /**
     * Data class for describing maven coordinate
     */
    private record MavenCoordinate(
            @NotNull String groupID,
            @NotNull String artifactID,
            @NotNull String version,
            @NotNull String classifier,
            @NotNull String extension
    ) {

    }
}
