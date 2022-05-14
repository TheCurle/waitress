package uk.gemwire.waitress.authentication.entity;

import org.junit.jupiter.api.Test;
import uk.gemwire.waitress.authentication.PermissionLevel;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void permissionOrderTest() {
        Organization orgLow = new Organization("low");
        Organization orgMiddle = new Organization("middle");
        Organization orgHigh = new Organization("high");

        Team teamLow = new Team("low", orgLow);
        Team teamMiddle = new Team("middle", orgMiddle);
        Team teamHigh = new Team("high", orgHigh);
        orgLow.addTeam(teamLow);
        orgMiddle.addTeam(teamMiddle);
        orgHigh.addTeam(teamHigh);

        orgHigh.addArtifactOverride("g", "a", PermissionLevel.MANAGE);
        orgMiddle.addGroupOverride("g", PermissionLevel.READ);
        orgLow.addGroupOverride("g", PermissionLevel.BLOCKED);

        User tester = new User("tester");
        tester.addTeam(teamLow);
        tester.addTeam(teamMiddle);
        tester.addTeam(teamHigh);
        teamLow.addUser(tester);
        teamMiddle.addUser(tester);
        teamHigh.addUser(tester);

        assertEquals(PermissionLevel.MANAGE, tester.getPermissionFor("g", "a"));
    }

    @Test
    void inheritanceTest() {
        Organization org = new Organization("org");
        Team team = new Team("team", org);
        org.addTeam(team);

        org.addArtifactOverride("g1", "a1", PermissionLevel.READ);
        org.addGroupOverride("g2", PermissionLevel.WRITE);

        User user = new User("user");
        team.addUser(user);
        user.addTeam(team);

        assertEquals(PermissionLevel.READ, user.getPermissionFor("g1", "a1"));
        assertEquals(PermissionLevel.WRITE, user.getPermissionFor("g2", "aiosfhdodggvvinheritanceTestvb"));
    }

    @Test
    void priorityTest() {
        Organization org = new Organization("org");
        Team team = new Team("team", org);
        org.addTeam(team);

        User user = new User("user");
        team.addUser(user);
        user.addTeam(team);

        org.addArtifactOverride("g1", "a1", PermissionLevel.ADMINISTRATE);
        org.addGroupOverride("g2", PermissionLevel.WRITE);

        team.addGroupOverride("g1", PermissionLevel.READ);
        team.addArtifactOverride("g2", "a2", PermissionLevel.BROWSE);
        assertEquals(user.getPermissionFor("g1", "a1"), PermissionLevel.READ);
        assertEquals(user.getPermissionFor("g2", "a2"), PermissionLevel.BROWSE);

    }

}