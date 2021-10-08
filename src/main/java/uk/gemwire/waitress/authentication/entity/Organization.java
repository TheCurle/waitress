package uk.gemwire.waitress.authentication.entity;

import uk.gemwire.waitress.authentication.PermissionLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * A group of {@link Team}s that provides permission overrides for a set of Groups and Artifacts.
 *
 * An Organization may have any number of member Teams.
 * Teams in this Organization will inherit all permission overrides.
 *
 * @author Curle
 */
public class Organization {

    // The name of this Organization.
    private final String name;

    // Every Team that this Organization controls.
    // A user must be in at least one Team to be a member of this Org.
    private final List<Team> teams;

    // A map of Group ID/Artifact ID -> Permissions.
    private final HashMap<String, PermissionLevel> artifactPermissions;

    // A map of Group ID -> Permissions.
    private final HashMap<String, PermissionLevel> groupPermissions;

    /**
     * Set the name of this Organization.
     * All other fields are initialized to empty.
     * @param name The name of this Organization as a String.
     */
    public Organization(String name) {
        this.name = name;
        this.teams = new ArrayList<>();
        this.artifactPermissions = new HashMap<>();
        this.groupPermissions = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    /**
     * Add a specified Team to this Organization.
     * The Team will inherit all permissions granted to this Organization.
     * @param newTeam The team to add to the Organization.
     * @return The instance of this Organization.
     */
    public Organization addTeam(Team newTeam) {
        this.teams.add(newTeam);
        return this;
    }

    /**
     * Returns whether this Organization tracks a specific Team.
     * @param toCheck The Team to check membership of.
     */
    public boolean contains(Team toCheck) {
        return this.teams.contains(toCheck);
    }

    /**
     * Add a permission override for a specific artifact.
     * @param groupID The group of the artifact to override the permission on.
     * @param artifactID The ID of the artifact to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this Organization.
     */
    public Organization addArtifactOverride(String groupID, String artifactID, PermissionLevel perm) {
        this.artifactPermissions.put(groupID + "/" + artifactID, perm);
        return this;
    }

    /**
     * Add a permission override for every artifact in a given group.
     * @param groupID The group to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this Organization.
     */
    public Organization addGroupOverride(String groupID, PermissionLevel perm) {
        this.groupPermissions.put(groupID, perm);
        return this;
    }

    /**
     * Checking all overrides, get the most relevant permission for this artifact granted to this Organization.
     *
     * The order of priority is as such:
     *
     * - If the Organization has an Override for a specific Artifact, that is stored.
     * - If the Organization has an Override for a Group that contains the Artifact, that is stored.
     *
     * This layout allows for nontrivial setups, as described in the javadoc for {@link User}.
     *
     * For more information, see the Waitress documentation.
     *
     * @param group    The Group ID to check.
     * @param artifact Optionally, a specific Artifact ID to check.
     * @return The most relevant Permission Level of the Organization. This is the Permission that should be obeyed.
     */
    public PermissionLevel getPermissionFor(String group, String artifact) {
        boolean hasGroupOverride = groupPermissions.containsKey(group);
        boolean hasArtifactOverride = artifactPermissions.containsKey(artifact);

        // Team specific permissions always take priority.
        if (hasArtifactOverride) return artifactPermissions.get(group + "/" + artifact);
        if (hasGroupOverride) return groupPermissions.get(group);

        // Fall back on NONE, as we don't have anything to do with this group.
        return PermissionLevel.NONE;
    }
}
