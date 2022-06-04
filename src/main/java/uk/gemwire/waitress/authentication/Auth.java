package uk.gemwire.waitress.authentication;

import at.favre.lib.crypto.bcrypt.BCrypt;
import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.authentication.entity.Entity;
import uk.gemwire.waitress.authentication.entity.Organization;
import uk.gemwire.waitress.authentication.entity.Team;
import uk.gemwire.waitress.authentication.entity.User;
import uk.gemwire.waitress.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Handles the management of user login.
 * <p>
 * Contains a list of users and the hashes for their passwords.
 * This list is read from a TOML file that is appended to whenever a new user is created, or a user changes their password.
 *
 * @author Curle
 */
public final class Auth {

    private static final Set<Organization> organizations = new HashSet<>();
    private static final Set<Team> teams = new HashSet<>();
    private static final Map<User, byte[]> users = new HashMap<>();

    // True if the Config has been loaded and we're ready to rock.
    private static boolean ready = false;

    // Default account for requests without credentials
    public static User anonymous;

    /**
     * Initialize the Auth system.
     * Adds the admin user with global permissions and anonymous user to the user map.
     * This user should always exist and should always be the first user.
     */
    public static void setupAuth() {
        ready = true;
        // the above means the CalledTooEarlyException can't be thrown. ignore the error
        try {
            anonymous = addUser("anonymous", "".getBytes(StandardCharsets.UTF_8));
            addUser(Config.ADMIN_USERNAME, Config.ADMIN_HASH.getBytes(StandardCharsets.UTF_8));
            loadUsers(new File(Config.USER_DATA));
            loadPermissions(new File(Config.PERM_DATA));
            organizations.forEach(org -> Waitress.LOGGER.info("Organization " + org.toString()));
        } catch (CalledTooEarlyException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a user to the list.
     * Checks if the user already exists. If so, nothing is done. This condition shouldn't be possible.
     * If the user does not exist, it is added to the map and appended to the hash file.
     * <p>
     * This function interacts with the user map, so it must be called after setupAuth.
     *
     * @param username The new user's login name.
     * @param password The hash of the user's password.
     */
    public static User addUser(String username, byte[] password) throws CalledTooEarlyException {
        if (!ready)
            throw new CalledTooEarlyException();

        if (getUser(username).isPresent())
            return null;

        User user = new User(username);

        users.put(user, password);
        return user;
        // TOMLWriter.addPair(username, password);
    }

    /**
     * Returns optional organization from the list.
     * <p>
     * This function interacts with the organization list, so it must be called after setupAuth.
     *
     * @param orgName The organization's name.
     * @return Optional of organization
     */
    public static Optional<Organization> getOrganization(String orgName) throws CalledTooEarlyException {
        if (!ready) throw new CalledTooEarlyException();
        return organizations.parallelStream().filter(org -> org.getName().equals(orgName)).findFirst();
    }

    /**
     * Returns optional team from the list.
     * <p>
     * This function interacts with the team list, so it must be called after setupAuth.
     *
     * @param teamName The team's name
     * @return Optional of team
     */
    public static Optional<Team> getTeam(String teamName) throws CalledTooEarlyException {
        if (!ready) throw new CalledTooEarlyException();
        return teams.parallelStream().filter(team -> team.getName().equals(teamName)).findFirst();
    }

    /**
     * Returns optional user from the list.
     * <p>
     * This function interacts with the user map, so it must be called after setupAuth.
     *
     * @param username The user's login name.
     * @return Optional of user
     */
    public static Optional<User> getUser(String username) throws CalledTooEarlyException {
        if (!ready) throw new CalledTooEarlyException();
        return users.keySet().parallelStream().filter(user -> user.getUsername().equals(username)).findFirst();
    }

    /**
     * Given a username and a plaintext password, hash and compare against the stored password for that user.
     * If the user does not exist, this throws {@link java.util.NoSuchElementException}.
     * <p>
     * This function interacts with the user map, so it must be called after setupAuth.
     *
     * @param username The username of the user to check.
     * @param password The plaintext password to check.
     * @return true if the password matches, false if the password does not match.
     */
    public static boolean checkPassword(String username, String password) throws CalledTooEarlyException, NoSuchElementException {
        if (!ready)
            throw new CalledTooEarlyException();

        Optional<User> optional = getUser(username);

        if (optional.isEmpty())
            throw new NoSuchElementException("User " + username + " does not exist.");

        return BCrypt.verifyer().verify(password.getBytes(StandardCharsets.UTF_8), users.get(optional.get())).verified;
    }

    /**
     * Given the bytes of a password, use BCrypt to generate a cryptographically secure hash.
     * This should not be used for verification, only for initial creation of a hash.
     * <p>
     * Uses a 2^12 (4096) iteration factor.
     *
     * @param password the bytes of a password string to hash.
     * @return the bytes of a secure hashed password.
     */
    public static byte[] createPasswordHash(byte[] password) {
        return BCrypt.withDefaults().hash(12, password);
    }

    /**
     * TODO Switch to TOML in the future
     * This should only be called once during setup, as it does not clear current data
     * Password for tmvkrpxl0 account is "password"
     *
     * Currently, user file format is:
     * ORGN `List of organizations`
     * TEAM `List of teams, each team entry follows this form: team_name[organization_name]. Bracket [] is necessary`
     *
     * `user's name` `password hash` `List of organizations`
     * `user's name` `password hash` `List of organizations`
     * `user's name` `password hash` `List of organizations`
     *
     * Temporary user file is `data/users`
     * @param userFile User file path
     */
    public static void loadUsers(final File userFile) throws IOException, CalledTooEarlyException {
        try(final BufferedReader reader = new BufferedReader(new FileReader(userFile, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine().substring(5); // First line is: "ORGN {List of organizations}", "ORGN " must be skipped
            List<Organization> orgToAdd = Arrays.stream(firstLine.split(" ")).map(Organization::new).toList();
            organizations.addAll(orgToAdd);

            String secondLine = reader.readLine().substring(5);// Second line is: "TEAM {List of teams}", "TEAM " must be skipped
            for (String teamEntry : secondLine.split(" ")) {
                final int leftBracket = teamEntry.lastIndexOf('[');
                final String orgName = teamEntry.substring(leftBracket + 1, teamEntry.length() - 1);

                final String teamName = teamEntry.substring(0, leftBracket);
                getOrganization(orgName).ifPresentOrElse(
                        org -> {
                            Team team = new Team(teamName, org);
                            teams.add(team);
                            org.addTeam(team);
                        },
                        () -> Waitress.LOGGER.warn("Organization " + orgName + " for team " + teamName + " does not exist! Ignoring..")
                );
            }

            for (String userEntry : reader.lines().toList()) {// Rest of the file from now on is user entry
                if (userEntry.isEmpty()) continue; // Ignore empty lines
                final String[] split = userEntry.split(" ");
                final String name = split[0];
                final byte[] hash = split[1].getBytes(StandardCharsets.UTF_8);
                final User user = addUser(name, hash); // Should it get existing user if user already exists?
                for (int i = 2; i < split.length; i++) {
                    final String teamName = split[i];
                    getTeam(teamName).ifPresentOrElse(
                            team -> {
                                user.addTeam(team);
                                team.addUser(user);
                            },
                            () -> Waitress.LOGGER.warn("User " + name + " is in non-existing team " + teamName + "! Ignoring..")
                    );
                }
            }
            // Done loading all user data, now loading permission override
        }
    }

    /**
     * This should only be called once during setup, as it does not clear current data
     *
     * Currently, permission file format is:
     * `GroupID` `ArtifactID` `Permission Level in integer` `List of entities`
     * `GroupID` `Permission Level in integer` `List of entities`
     *
     *
     * ArtifactID can be omitted when setting group permission override
     * an entity could be one of these: organization, team or user
     * Organization is specified as: [Organization's name]
     * Team is specified as: {Team's  name}
     * User is specified as: (User's name)
     * Brackets are necessary for all of those 3 cases, and type of bracket is important
     *
     * Temporary permission file is `data/permissions`
     * @param permFile Permission file path
     */
    public static void loadPermissions(final File permFile) throws IOException, CalledTooEarlyException {
        try(final BufferedReader reader = new BufferedReader(new FileReader(permFile))) {
            int lineNumber = 0; // This is debug purpose, to see which line was invalid
            for (String permEntry : reader.lines().toList()) {
                lineNumber++;
                final String[] split = permEntry.split(" ");
                final String groupID = split[0];

                // Check if this entry contains Artifact ID, below will be null when it doesn't
                String artifactId = null;
                final PermissionLevel permission;
                {
                    int intPermission;
                    try {
                        intPermission = Integer.parseInt(split[1]);
                    } catch (NumberFormatException exception) {
                        // When Index 1 is Artifact ID and not permission level
                        artifactId = split[1];
                        try {
                            intPermission = Integer.parseInt(split[2]);
                        } catch (NumberFormatException configInvalid) {
                            Waitress.LOGGER.warn("Permission level defined at line " + lineNumber + " is invalid!");
                            continue;
                        }
                    }
                    permission = PermissionLevel.fromInt(intPermission);
                }

                if (artifactId == null) {
                    for (int i = 2; i < split.length; i++) {
                        String target = split[i];
                        Optional<? extends Entity> entity = parseEntity(target);
                        if (entity.isPresent()) {
                            entity.get().addGroupOverride(groupID, permission);
                        }else {
                            Waitress.LOGGER.warn("Entity " + target + " in permission file at line " + lineNumber + " does not exist!");
                        }
                    }
                } else {
                    for (int i = 3; i < split.length; i++) {
                        String target = split[i];
                        Optional<? extends Entity> entity = parseEntity(target);
                        if (entity.isPresent()) {
                            entity.get().addArtifactOverride(groupID, artifactId, permission);
                        }else {
                            Waitress.LOGGER.warn("Entity " + target + " in permission file at line " + lineNumber + " does not exist!");
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses given string to Entity
     * When given string is wrapped in [], it returns Organization
     * When given string is wrapped in {}, it returns Team
     * When given string is wrapped in (), it returns User
     * @param entityEntry Entity entry
     * @return One of those 3 Entities
     */
    private static Optional<? extends Entity> parseEntity(String entityEntry) throws CalledTooEarlyException {
        final String noBracket = entityEntry.substring(1, entityEntry.length() - 1);
        if (entityEntry.startsWith("[")) {
            return getOrganization(noBracket);
        }else if (entityEntry.startsWith("{")){
            return getTeam(noBracket);
        }else if (entityEntry.startsWith("(")) {
            return getUser(noBracket);
        }else return Optional.empty();
    }
}
