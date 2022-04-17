package uk.gemwire.waitress.authentication;

import at.favre.lib.crypto.bcrypt.BCrypt;
import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.authentication.entity.Entity;
import uk.gemwire.waitress.authentication.entity.Organization;
import uk.gemwire.waitress.authentication.entity.Team;
import uk.gemwire.waitress.authentication.entity.User;
import uk.gemwire.waitress.config.Config;

import java.io.*;
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

    /**
     * Initialize the Auth system.
     * Adds the admin user with global permissions and anonymous user to the user map.
     * This user should always exist and should always be the first user.
     */
    public static void setupAuth() {
        ready = true;
        // the above means the CalledTooEarlyException can't be thrown. ignore the error
        try {
            addUser("anonymous", "".getBytes(StandardCharsets.UTF_8));
            addUser(Config.ADMIN_USERNAME, Config.ADMIN_HASH.getBytes(StandardCharsets.UTF_8));
            loadData();
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
        return organizations.parallelStream().filter(org -> org.getName().equals(orgName)).findAny();
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
        return teams.parallelStream().filter(team -> team.getName().equals(teamName)).findAny();
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
        return users.keySet().parallelStream().filter(user -> user.getUsername().equals(username)).findAny();
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


    // TODO Switch to TOMLWriter in the future

    /**
     * Warning: This method has to be used when user data at least specifies ORGN and TEAM
     * Otherwise it will start writing before those 2
     * Leaving ORGN and TEAM empty is fine
     */
    public static void writeUser(String username, String password) throws IOException {
        File file = new File(Config.USER_DATA);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.append(username).append(" ").append(password);
        writer.newLine();
        writer.flush();
        writer.close();
    }

    // TODO Switch to TOMLWriter in the future

    /**
     * This does not clear current team list, this works under assumption of getting invoked during setup
     * Password for tmvkrpxl0 account is "password"
     */
    public static void loadData() throws IOException, CalledTooEarlyException {
        final File userFile = new File(Config.USER_DATA);
        try(final BufferedReader reader = new BufferedReader(new FileReader(userFile, StandardCharsets.UTF_8))) {
            organizations.addAll(Arrays.stream(reader.readLine().substring(5).split(" ")).map(Organization::new).toList());
            for (String teamEntry : reader.readLine().substring(5).split(" ")) {
                final int leftBracket = teamEntry.lastIndexOf('[');
                final String orgName = teamEntry.substring(leftBracket + 1, teamEntry.length() - 1);
                final String teamName = teamEntry.substring(0, leftBracket);
                getOrganization(orgName).ifPresentOrElse(
                        org -> teams.add(new Team(teamName, org)),
                        () -> Waitress.LOGGER.warn("Organization " + orgName + " for team " + teamName + " does not exist! Ignoring..")
                );
            }

            for (String userEntry : reader.lines().toList()) {
                if (userEntry.isEmpty()) continue;
                final String[] split = userEntry.split(" ");
                final String name = split[0];
                final byte[] hash = split[1].getBytes(StandardCharsets.UTF_8);
                final User user = addUser(name, hash); // Should this line get existing user if user already exists?
                for (int i = 2; i < split.length; i++) {
                    final String teamName = split[i];
                    getTeam(teamName).ifPresentOrElse(
                            org -> user.addTeam(org),
                            () -> Waitress.LOGGER.warn("User " + name + " is in non-existing team " + teamName + "! Ignoring..")
                    );
                }
            }
            // Done loading all user data, now loading permission override
        }
        final File permFile = new File(Config.PERM_DATA);
        try(final BufferedReader reader = new BufferedReader(new FileReader(permFile))) {
            int lineNumber = 0; // This is for printing which line was invalid
            for (String permEntry : reader.lines().toList()) {
                lineNumber++;
                final String[] split = permEntry.split(" ");
                final String groupID = split[0];
                String artifactId = null;
                final PermissionLevel permission;
                {
                    int intLevel;
                    try {
                        intLevel = Integer.parseInt(split[1]);
                    } catch (NumberFormatException exception) {
                        artifactId = split[1];
                        try {
                            intLevel = Integer.parseInt(split[2]);
                        } catch (NumberFormatException configInvalid) {
                            Waitress.LOGGER.warn("Permission level defined at line " + lineNumber + " is invalid!");
                            continue;
                        }
                    }
                    permission = PermissionLevel.fromInt(intLevel);
                }
                if (artifactId == null) { // Only group id is specified
                    for (int i = 2; i < split.length; i++) {
                        String target = split[i];
                        parseEntity(target).ifPresentOrElse(
                                entity -> entity.addGroupOverride(groupID, permission),
                                () -> Waitress.LOGGER.warn(getEntityType(target) + " specified in permission file does not exist!")
                        );
                    }
                } else { // artifact id is also specified
                    for (int i = 3; i < split.length; i++) {
                        String target = split[i];
                        String finalArtifactId = artifactId;
                        parseEntity(target).ifPresentOrElse(
                                entity -> entity.addArtifactOverride(groupID, finalArtifactId, permission),
                                () -> Waitress.LOGGER.warn(getEntityType(target) + " specified in permission file does not exist!")
                        );
                    }
                }
            }
        }
    }

    /**
     * This only exists for printing error
     */
    private static String getEntityType(String entityEntry) {
        final String stripped = entityEntry.substring(1, entityEntry.length() - 1);
        if (entityEntry.startsWith("[")) {
            return "Organization" + stripped;
        }else if (entityEntry.startsWith("{")){
            return "Team" + stripped;
        }else if (entityEntry.startsWith("(")) {
            return "User" + stripped;
        }else return "Unknown entity " + entityEntry;
    }

    private static Optional<? extends Entity> parseEntity(String entityEntry) throws CalledTooEarlyException {
        final String stripped = entityEntry.substring(1, entityEntry.length() - 1);
        if (entityEntry.startsWith("[")) {
            return getOrganization(stripped);
        }else if (entityEntry.startsWith("{")){
            return getTeam(stripped);
        }else if (entityEntry.startsWith("(")) {
            return getUser(stripped);
        }else return Optional.empty();
    }
}
