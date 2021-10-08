package uk.gemwire.waitress.web.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents all the relevant metadata about a hosted artifact.
 * A single artifact may have unlimited versions, and classifiers.
 *
 * In the "group/artifact/version/artifact-version-classifier.ext" schema, an Artifact instance represents
 *  everything past the artifact element.
 *
 * Thus, multiple Artifacts may share a group.
 *
 * Artifacts may also be owned, but this information is stored elsewhere.
 *
 * @author Curle
 */
public class Artifact {

    private String groupID = "";
    private String artifactID = "";

    // The two lists here should be perfectly synchronized.
    // If a version has no classifier, an empty string should be pushed to classifiers.
    // This allows for multiple files (such as -api, -common, etc) to be stored in the same version folder.
    private List<String> versions;
    private List<String> classifiers;

    /**
     * Public constructor of Artifact.
     * Initializes all lists to zero.
     * Group and ID remain empty.
     */
    public Artifact() {
        versions = new ArrayList<>();
        classifiers = new ArrayList<>();
    }

    /**
     * Public constructor of Artifact.
     * Initializes all lists to zero.
     * Group and ID are set with the given parameters.
     */
    public Artifact(String group, String artifact) {
        groupID = group;
        artifactID = artifact;
        versions = new ArrayList<>();
        classifiers = new ArrayList<>();
    }

    public String getGroupID() {
        return groupID;
    }

    public String getArtifactID() {
        return artifactID;
    }

    /**
     * Add a tracked version of this Artifact to the cache.
     * Sets the classifier to "" (empty string).
     * @param version The version to add. May contain any special character, including "." and "-".
     */
    public void addVersion(String version) {
        versions.add(version);
        classifiers.add("");
    }

    /**
     * Add a tracked version of this Artifact to the cache.
     * Sets the classifier according to the given parameter.
     * This may be called multiple times with the same version, as long as there are different classifiers.
     * @param version The version to add. May contain any special character, including "." and "-".
     * @param classifier The classifier of the version to add. May only be alphanumeric characters. "api" is the expected value.
     */
    public void addVersion(String version, String classifier) {
        versions.add(version);
        classifiers.add(classifier);
    }

    /**
     * Returns whether this Artifact contains the given version.
     * Disregards classifiers, as an "api" release counts as a tracked version.
     */
    public boolean tracksVersion(String version) {
        return versions.contains(version);
    }

    /**
     * Returns whether this Artifact contains the given version with the given classifier.
     * Both must be valid to return true.
     */
    public boolean tracksVersion(String version, String classifier) {
        // We need to index two lists, so use a for loop.
        for (int i = 0; i < versions.size(); i++)
            if (versions.get(i).equals(version) && classifiers.get(i).equals(classifier))
                return true;

        return false;
    }


}
