package uk.gemwire.waitress.authentication;

public enum PermissionLevel {
    BLOCKED,     // No interaction allowed whatsoever.
    NONE,        // Inherit from parent.
    READ,        // Allowed to read all files.
    WRITE,       // Allowed to push files.
    MANAGE,      // Allowed to delete files.
    ADMINISTRATE // Allowed to grant permissions to others.
}
