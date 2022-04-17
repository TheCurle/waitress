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
public class User implements Entity{

    // The String by which this user can be identified.
    // Every username is globally unique.
    private final String username;

    // Every Team that this user is a member of.
    // The Team also contains a list of member users, so this is a bidirectional link.
    private final List<Team> teams;

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
        this.artifactPermissions = new HashMap<>();
        this.groupPermissions = new HashMap<>();
    }

    public String getUsername() {
        return username;
    }

    /**
     * Returns whether this User is a member of the specified Organization.
     */
    public boolean isIn(Organization org) {
        for (Team t : teams)
            if(t.isIn(org))
                return true;

        return false;
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
     * Returns whether this User is a member of the given team.
     * Will ask the given Team if it is tracking this user also.
     * Thus, a bidirectional link is mandatory to count as a member.
     * @param toCheck The Team to check membership of.
     */
    public boolean memberOf(Team toCheck) {
        return this.teams.contains(toCheck) && toCheck.contains(this);
    }

    /**
     * Add a permission override for a specific artifact.
     * This override will take precedence over any granted by organization or team.
     * @param groupID The group of the artifact to override the permission on.
     * @param artifactID The ID of the artifact to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this user.
     */
    @Override
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
    @Override
    public User addGroupOverride(String groupID, PermissionLevel perm) {
        this.groupPermissions.put(groupID, perm);
        return this;
    }

    /**
     * Checking all overrides, teams and organizations, get the most relevant permission for this artifact granted to this user.
     *
     * The order of priority is as such:
     * First, User overrides.
     *   - If the User has an Override for a specific Artifact, that is returned.
     *   - If the User has an Override for a Group that contains the Artifact, that is returned.
     * Next, for each Team:
     *   - If the Team has an Override for a specific Artifact, that is stored.
     *   - If the Team has an Override for a Group that contains the Artifact, that is stored.
     *   - For the Organization the Team belongs to:
     *     - If the Organization has an Override for a specific Artifact, that is stored.
     *     - If the Organization has an Override for a Group that contains the Artifact, that is stored
     *
     * After Teams and Organizations are iterated, if any Artifact has a lower Override than a Group, it is returned.
     * Otherwise, the highest Group Override is returned.
     *
     * This layout allows for nontrivial setups;
     *  For example, a User may be a member of a Team that controls a group of Artifacts, but the User may not
     *   be able to be trusted with a specific Artifact. The Team may have the Group Override granting Write access,
     *   and the User may have an Artifact Override granting Read access.
     *
     * For more information, see the Waitress documentation.
     *
     * @param group    The Group ID to check.
     * @param artifact Optionally, a specific Artifact ID to check.
     * @return The most relevant Permission Level of the User. This is the Permission that should be obeyed.
     */
    public PermissionLevel getPermissionFor(String group, String artifact) {
        boolean hasGroupOverride = groupPermissions.containsKey(group);
        boolean hasArtifactOverride = artifactPermissions.containsKey(artifact);

        // User specific permissions always take priority.
        if (hasArtifactOverride) return artifactPermissions.get(group + "/" + artifact);
        if (hasGroupOverride) return groupPermissions.get(group);

        // Variables to keep track outside of the loop. If the user belongs to no relevant teams, we fall back on NONE.
        PermissionLevel tPerm = PermissionLevel.NONE;

        for (Team t : teams) {
            PermissionLevel tTemp = t.getPermissionFor(group, artifact);
            // Get the highest permission granted by a Team.
            // This allows for a user in two teams - one that is blocked to an Artifact,
            //  and one that is an administrator on the same Artifact - to only receive the widest permission.

            if (tTemp.compareTo(tPerm) < 0) // compareTo returns a negative number if the object called is lower than the object passed.
                tPerm = tTemp;
        }

        return tPerm;
    }


}
