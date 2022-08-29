package uk.gemwire.waitress.permissions;

import io.javalin.http.Context;
import uk.gemwire.waitress.authentication.PermissionLevel;

/**
 * A temporary class that allows writing permissions from the web interface.
 *
 * @author Curle
 */
public class Permission {

    public static void updatePerm(Context request) {
        String data = request.path().substring(1);

        String[] parts = data.split("/");

        switch (parts[0]) {
            case "user" -> handleUserUpdate(parts, request);
            case "team" -> handleTeamUpdate(parts, request);
            case "org" -> handleOrgUpdate(parts, request);
        }
    }

    private static void handleUserUpdate(String[] parts, Context request) {
        String user = parts[1];
        String group = parts[2].replaceAll("%2F", "/");
        PermissionLevel perm = PermissionLevel.valueOf(parts[3]);

        PermissionStore.userByName(user).addGroupOverride(group, perm);

        request.result("Permission for " + user + " on " + group + " updated to " + perm);
    }

    private static void handleTeamUpdate(String[] parts, Context request) {
        String team = parts[1];
        String group = parts[2].replaceAll("%2F", "/");
        PermissionLevel perm = PermissionLevel.valueOf(parts[3]);

        PermissionStore.teamByName(team).addGroupOverride(group, perm);

        request.result("Permission for " + team + " on " + group + " updated to " + perm);
    }

    private static void handleOrgUpdate(String[] parts, Context request) {
        String org = parts[1];
        String group = parts[2].replaceAll("%2F", "/");
        PermissionLevel perm = PermissionLevel.valueOf(parts[3]);

        PermissionStore.orgByName(org).addGroupOverride(group, perm);

        request.result("Permission for " + org + " on " + group + " updated to " + perm);
    }
}
