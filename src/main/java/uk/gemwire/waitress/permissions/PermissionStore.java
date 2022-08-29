package uk.gemwire.waitress.permissions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.authentication.PermissionLevel;
import uk.gemwire.waitress.config.Config;
import uk.gemwire.waitress.permissions.entity.Organization;
import uk.gemwire.waitress.permissions.entity.Team;
import uk.gemwire.waitress.permissions.entity.User;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * The cache that manages all active Users, Teams and Organizations.
 * This centralized class can be serialized to keep a snapshot of all links and permissions.
 *
 * @author Curle
 */
public class PermissionStore {

    private final List<User> users = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();
    private final List<Organization> orgs = new ArrayList<>();

    // TODO: come up with a better solution than this!

    // Allows this class to be called via static context, but remain serializable.
    private static PermissionStore instance;

    // Get the static instance of this class. For serialization only.
    public static PermissionStore getInstance() { return instance; }
    // Set the static instance of this class. For deserialization only.
    private static void setInstance(PermissionStore store) { instance = store; }

    /**
     * Read the permissions cache from DATA_DIR/waitress/permissions.json.
     * Set the static instance field to the new instance that contains all relevant data.
     */
    public static void setupPermissions() {
        try {
            Gson gson = new Gson();
            Reader authReader = new FileReader(Config.DATA_DIR + "waitress/permissions.json");

            instance = gson.fromJson(authReader, PermissionStore.class);
        } catch (IOException e) {
            Waitress.LOGGER.error("Unable to read permissions: " + e.getMessage());
        }
    }

    /**
     * Add a User to the tracking list.
     */
    public static void trackUser(User newUser) {
        instance.users.add(newUser);
    }

    /**
     * Add a Team to the tracking list.
     */
    public static void trackTeam(Team newTeam) {
        instance.teams.add(newTeam);
    }

    /**
     * Add an Organization to the tracking list.
     */
    public static void trackOrg(Organization newOrg) {
        instance.orgs.add(newOrg);
    }

    /**
     * Query the tracking list for a User with a specific username.
     * @throws NoSuchElementException if no user with that name is tracked.
     */
    public static User userByName(String username) throws NoSuchElementException {
        for (User user : instance.users) {
            if(user.getUsername().equals(username))
                return user;
        }

        throw new NoSuchElementException();
    }

    /**
     * Query the tracking list for a Team with a specific name.
     * @throws NoSuchElementException if no team with that name is tracked.
     */
    public static Team teamByName(String teamName) throws NoSuchElementException {
        for (Team team : instance.teams) {
            if(team.getName().equals(teamName))
                return team;
        }

        throw new NoSuchElementException();
    }

    /**
     * Query the tracking list for an Organization with a specific name.
     * @throws NoSuchElementException if no Organization with that name is tracked.
     */
    public static Organization orgByName(String orgName) throws NoSuchElementException {
        for (Organization org : instance.orgs) {
            if(org.getName().equals(orgName))
                return org;
        }

        throw new NoSuchElementException();
    }
}
