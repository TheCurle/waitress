package uk.gemwire.waitress.web;

import io.javalin.core.util.FileUtil;
import uk.gemwire.waitress.config.Config;

import java.io.File;
import java.io.IOException;
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
    public static File getArtifact(String groupID, String artifactID, String version, String classifier, String extension) throws IOException {
        assert Config.SHOULD_PROXY;
        final String path = groupID + "/" + artifactID + "/" + version + "/" + artifactID + "-" + version + classifier + "." + extension;
        final URL url = new URL(Config.PROXY_REPO + path);
        FileUtil.streamToFile(url.openStream(), Config.DATA_DIR + path);
        return new File(Config.DATA_DIR + path);
    }
}
