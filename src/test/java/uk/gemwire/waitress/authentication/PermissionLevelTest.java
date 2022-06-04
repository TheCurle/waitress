package uk.gemwire.waitress.authentication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionLevelTest {

    @Test
    void fromInt() {
        assertEquals(PermissionLevel.NONE, PermissionLevel.fromInt(-2));
        assertEquals(PermissionLevel.NONE, PermissionLevel.fromInt(-1));
        assertEquals(PermissionLevel.BLOCKED, PermissionLevel.fromInt(0));
        assertEquals(PermissionLevel.BROWSE, PermissionLevel.fromInt(1));
        assertEquals(PermissionLevel.READ, PermissionLevel.fromInt(2));
        assertEquals(PermissionLevel.WRITE, PermissionLevel.fromInt(3));
        assertEquals(PermissionLevel.MANAGE, PermissionLevel.fromInt(4));
        assertEquals(PermissionLevel.ADMINISTRATE, PermissionLevel.fromInt(5));
        assertEquals(PermissionLevel.NONE, PermissionLevel.fromInt(6));
    }
}