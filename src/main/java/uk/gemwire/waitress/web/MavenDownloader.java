package uk.gemwire.waitress.web;

import io.javalin.core.util.FileUtil;
import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.config.Config;

import java.io.File;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.URL;

/**
 * Simple Maven artifact downloader
 * Simply just constructs URL from coordinates and configured proxy, and downloads from it
 * @author tmvkrpxl0 
 */
public class MavenDownloader {
    /**
     * Downloads artifact at coordinate from proxy into directory
     * @return Downloaded file
     */
    public static File downloadArtifact(String groupID, String artifactID, String version, String classifier, String extension) throws IOException {
        if (!Config.SHOULD_PROXY) throw new IllegalStateException("Unable to download artifact, proxy is disabled");
        final String path = groupID + "/" + artifactID + "/" + version + "/" + artifactID + "-" + version + classifier + "." + extension;
        if (!Waitress.checker.isProxyAlive) throw new NoRouteToHostException("Proxy is down!");
        final URL url = new URL(Config.PROXY_REPO + path);
        FileUtil.streamToFile(url.openStream(), Config.DATA_DIR + path);
        return new File(Config.DATA_DIR + path);
    }
}
