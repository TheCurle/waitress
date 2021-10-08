package uk.gemwire.waitress.web;

import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.config.Config;
import uk.gemwire.waitress.web.repository.Artifact;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Stores a list of artifacts that are currently handled by *this* repository.
 * If an artifact does not exist in this cache, it should be queried from mirrored repos.
 *
 * @author Curle
 */
public class RepoCache {

    private static final List<Artifact> artifacts = new ArrayList<>();

    /**
     * Read the data directory and enumerate every artifact into the cache.
     * This should only be called once, when the server is starting.
     *
     * This operation is rather costly, due to the disk thrashing.
     */
    public static void enumerate() {
        try {
            var dirs = recurseDirs(new File(Config.DATA_DIR), new ArrayList<>());

            dirs.forEach(d ->  {
                // Remove the data/ part of the name.
                Path dpath = d.toPath();
                dpath = dpath.subpath(1, dpath.getNameCount());

                // If we don't have at least group/artifact/version, skip.
                if(dpath.getNameCount() < 3)
                    return;
                // If there's a higher level, skip. We want only the version folders.
                File[] dirList = d.listFiles(File::isDirectory);
                if(dirList != null && dirList.length > 0)
                    return;

                // If there aren't any files in the folder, skip.
                // TODO: delete.
                File[] flist = d.listFiles(File::isFile);
                if(flist == null)
                    return;

                // Version is last.
                String version = dpath.getName(dpath.getNameCount() - 1).toString();
                // Artifact name is immediately preceding it.
                String artifact = dpath.getName(dpath.getNameCount() - 2).toString();
                // Group is the rest. URLs only come with forward slashes, so cope with Windows weirdness.
                String group = dpath.subpath(0, dpath.getNameCount() - 2).toString().replaceAll("\\\\", "/");

                if(!contains(group, artifact))
                    artifacts.add(new Artifact(group, artifact));

                List<File> contained = Arrays.asList(flist);
                contained.forEach(c -> {
                    String classifier = c.getName().substring(artifact.length() + version.length() + 1, c.getName().lastIndexOf('.'));
                    if(classifier.length() > 0)
                        get(group, artifact).addVersion(version, classifier);
                    else
                        get(group, artifact).addVersion(version);
                    Waitress.LOGGER.warn("Artifact " + artifact + " version " + version + (classifier.length() != 0 ? "-" + classifier : "") + " is now tracked.");
                });

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get a full list of all subdirectories we can see.
     * @param root The root folder of the search.
     * @return A List<File> containing every directory contained within the root.
     */
    private static List<File> recurseDirs(File root, List<File> result) {
        List<File> currentSubDirs = Arrays.asList(Objects.requireNonNull(root.listFiles(File::isDirectory), "Root file has to be directory"));
        result.addAll(currentSubDirs);
        currentSubDirs.forEach(file -> recurseDirs(file, result));
        return result;
    }

    /**
     * Returns whether an artifact with the given Group and ID exists in the cache.
     * @see #get(String, String) 
     */
    public static boolean contains(String groupID, String artifactID) {
        for (Artifact a : artifacts)
            if (a.getGroupID().equals(groupID) && a.getArtifactID().equals(artifactID)) {
                return true;
            }
        
        return false;
    }

    /**
     * Returns whether an artifact with the given Group, ID and Version exist in the cache.
     * @see #contains(String, String)
     */
    public static boolean contains(String groupID, String artifactID, String version) {
        if (!contains(groupID, artifactID))
            return false;

        Artifact a = get(groupID, artifactID);
        return a.tracksVersion(version);
    }

    /**
     * Returns whether an artifact with the given Group, ID, Version and Classifier exist in the cache.
     * @see #contains(String, String, String)
     * @see #contains(String, String)
     */
    public static boolean contains(String groupID, String artifactID, String version, String classifier) {
        if (!contains(groupID, artifactID))
            return false;

        Artifact a = get(groupID, artifactID);
        return a.tracksVersion(version, classifier);
    }

    /**
     * Get the Artifact instance for a given Group and ID.
     * If it does not exist, null is returned.
     * @see #contains(String, String) 
     */
    public static Artifact get(String groupID, String artifactID) {
        for (Artifact a : artifacts) 
            if (a.getGroupID().equals(groupID) && a.getArtifactID().equals(artifactID))
                return a;
            
        return null;
    }
}
