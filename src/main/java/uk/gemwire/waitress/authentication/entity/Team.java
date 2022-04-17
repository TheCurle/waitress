package uk.gemwire.waitress.authentication.entity;

import uk.gemwire.waitress.authentication.PermissionLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A group of {@link User}s that provides permission overrides for a set of Groups and Artifacts.
 *
 * A Team must belong to exactly one Organization.
 * A Team may have any number of member Users.
 *
 * A User in this Team will inherit all of the permission overrides.
 *
 * @author Curle
 */
public class Team implements Entity{

    // The name of this Team.
    private final String name;

    // The organization this team belongs to.
    // A Team must have a parent Organization.
    private final Organization org;

    // Every Organization that this user is a member of.
    // The Organization also contains a list of member users, so this is a bidirectional link.
    private final List<User> users;

    // A map of Group ID/Artifact ID -> Permissions.
    private final HashMap<String, PermissionLevel> artifactPermissions;

    // A map of Group ID -> Permissions.
    private final HashMap<String, PermissionLevel> groupPermissions;

    /**
     * Set the name and parent Organization of this Team.
     * All other fields are initialized to empty.
     * @param name The name of this Team as a String.
     * @param org The Organization that this team belongs to.
     */
    public Team(String name, Organization org) {
        this.name = name;
        this.org = org;
        this.users = new ArrayList<>();
        this.artifactPermissions = new HashMap<>();
        this.groupPermissions = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    /**
     * Returns whether this Team is a member of the specified Organization.
     */
    public boolean isIn(Organization org) {
        return this.org == org;
    }

    /**
     * Add a specified User to this Team.
     * It is expected that {@code user.memberOf(this)} is true at some point in the future.
     * The user will inherit all permissions granted to this team.
     * @param newUser The user to add to the team.
     * @return The instance of this Team.
     */
    public Team addUser(User newUser) {
        this.users.add(newUser);
        return this;
    }

    /**
     * Returns whether this Team tracks a specific User.
     * @param toCheck The User to check membership of.
     */
    public boolean contains(User toCheck) {
        return this.users.contains(toCheck);
    }

    /**
     * Add a permission override for a specific artifact.
     * This override will take precedence over any granted by organization.
     * @param groupID The group of the artifact to override the permission on.
     * @param artifactID The ID of the artifact to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this Team.
     */
    public Team addArtifactOverride(String groupID, String artifactID, PermissionLevel perm) {
        this.artifactPermissions.put(groupID + "/" + artifactID, perm);
        return this;
    }

    /**
     * Add a permission override for every artifact in a given group.
     * This override will take precedence over any granted by organization.
     * @param groupID The group to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this Team.
     */
    public Team addGroupOverride(String groupID, PermissionLevel perm) {
        this.groupPermissions.put(groupID, perm);
        return this;
    }

    /**
     * Checking all overrides and organizations, get the most relevant permission for this artifact granted to this Team.
     *
     * The order of priority is as such:
     *
     *   - If the Team has an Override for a specific Artifact, that is stored.
     *   - If the Team has an Override for a Group that contains the Artifact, that is stored.
     *   - For the Organization the Team belongs to:
     *     - If the Organization has an Override for a specific Artifact, that is stored.
     *     - If the Organization has an Override for a Group that contains the Artifact, that is stored
     *
     * This layout allows for nontrivial setups, as described in the javadoc for {@link User}.
     *
     * For more information, see the Waitress documentation.
     *
     * @param group    The Group ID to check.
     * @param artifact Optionally, a specific Artifact ID to check.
     * @return The most relevant Permission Level of the Team. This is the Permission that should be obeyed.
     */
    public PermissionLevel getPermissionFor(String group, String artifact) {
        boolean hasGroupOverride = groupPermissions.containsKey(group);
        boolean hasArtifactOverride = artifactPermissions.containsKey(artifact);

        // Team specific permissions always take priority.
        if (hasArtifactOverride) return artifactPermissions.get(group + "/" + artifact);
        if (hasGroupOverride) return groupPermissions.get(group);

        // The only thing left to fall back on is the organization we belong to.
        // The org will handle falling back to NONE for us.
        return org.getPermissionFor(group, artifact);
    }
}
