package uk.gemwire.waitress.authentication.entity;

import uk.gemwire.waitress.authentication.PermissionLevel;

public interface Entity {
    /**
     * Add a permission override for a specific artifact.
     * @param groupID The group of the artifact to override the permission on.
     * @param artifactID The ID of the artifact to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this Organization.
     */
    Entity addArtifactOverride(String groupID, String artifactID, PermissionLevel perm);

    /**
     * Add a permission override for every artifact in a given group.
     * @param groupID The group to override the permission on.
     * @param perm The permission to override with.
     * @return The instance of this Organization.
     */
    Entity addGroupOverride(String groupID, PermissionLevel perm);
}
