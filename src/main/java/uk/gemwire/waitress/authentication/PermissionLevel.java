package uk.gemwire.waitress.authentication;

public enum PermissionLevel {
    NONE(-1),        // Inherit from parent.
    BLOCKED(0),     // No interaction allowed whatsoever.
    BROWSE(1),     // Allowed to browse files.
    READ(2),        // Allowed to read files.
    WRITE(3),       // Allowed to push files.
    MANAGE(4),      // Allowed to delete files.
    ADMINISTRATE(5); // Allowed to grant permissions to others.

    public final int level;

    PermissionLevel(int level) {
        this.level = level;
    }

    public static PermissionLevel fromInt(int level) {
        if (level > 5) return NONE;
        if (level < -1) return NONE;
        return values()[level+1];
    }
}
