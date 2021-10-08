package uk.gemwire.waitress.authentication.entity;

import uk.gemwire.waitress.authentication.Auth;
import uk.gemwire.waitress.authentication.PermissionLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The most granular form of permission available.
 *
 * Contains all relevant information about a user.
 * Passwords and authentication is handled elsewhere.
 *
 * However, all permissions are enumerated here.
 *
 * @see Auth
 *
 * @author Curle
 */
public class User {

    // The String by which this user can be identified.
    // Every username is globally unique.
    private final String username;

    // Every Team that this user is a member of.
    // The Team also contains a list of member users, so this is a bidirectional link.
    private final List<Team> teams;

    // Every Organization that this user is a member of.
    // The Organization also contains a list of member users, so this is a bidirectional link.
    private final List<Organization> organizations;

    // A map of Group ID/Artifact ID -> Permissions.
    // Only for individual overrides, as permissions can be granted to multiple users via Teams or Organizations.
    private final HashMap<String, PermissionLevel> artifactPermissions;

    // A map of Group ID -> Permissions.
    // Only for individual overrides, as permissions can be granted to multiple users via Teams or Organizations.
    private final HashMap<String, PermissionLevel> groupPermissions;

    /**
     * Public constructor for a User.
     * Sets the username, initializes all fields to default.
     * Use the helper methods to set things.
     * @param username The unique ID of this user.
     */
    public User(String username) {
        this.username = username;
        this.teams = new ArrayList<>();
        this.organizations = new ArrayList<>();
        this.artifactPermissions = new HashMap<>();
        this.groupPermissions = new HashMap<>();
    }

    /**
     * Add this user to a Team.
     * It is expected that {@code newTeam.contains(this)} is true at some point in the future.
     * This user will inherit all permissions granted to this team.
     * @param newTeam The team to add the user to.
     * @return The instance of this user.
     */
    public User addTeam(Team newTeam) {
        this.teams.add(newTeam);
        return this;
    }

    /**
     * Add this user to an Organization.
     * It is expected that {@code newOrg.contains(this)} is true at some point in the future.
     * This user will inherit all permissions granted to this organization.
     * @param newOrg The organization to add the user to.
     * @return The instance of this user.
     */
    public User addOrganization(Organization newOrg) {
        this.organizations.add(newOrg);
        return this;
    }

    /**
     * Add a permission override for a specific artifact.
     * This override will take precedence over any granted by organization or team.
     * @param groupID The group of the artifact to override the permission on.
     * @param artifactID The ID of the artifact to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this user.
     */
    public User addArtifactOverride(String groupID, String artifactID, PermissionLevel perm) {
        this.artifactPermissions.put(groupID + "/" + artifactID, perm);
        return this;
    }

    /**
     * Add a permission override for every artifact in a given group.
     * This override will take precedence over any granted by organization or team.
     * @param groupID The group to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this user.
     */
    public User addGroupOverride(String groupID, PermissionLevel perm) {
        this.groupPermissions.put(groupID, perm);
        return this;
    }


}
