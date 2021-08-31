package uk.gemwire.waitress.authentication;

import at.favre.lib.crypto.bcrypt.BCrypt;
import uk.gemwire.waitress.config.Config;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Handles the management of user login.
 *
 * Contains a list of users and the hashes for their passwords.
 * This list is read from a TOML file that is appended to whenever a new user is created, or a user changes their password.
 *
 * @author Curle
 */
public final class Auth {

    /**
     * TODO: this should not use String usernames. This should be a map of User (or user ID) to password hash.
     */
    private static final Map<String, byte[]> users = new HashMap<>();

    // True if the Config has been loaded and we're ready to rock.
    private static boolean ready = false;

    /**
     * Initialize the Auth system.
     * Adds the admin user with global permissions to the user map.
     * This user should always exist and should always be the first user.
     */
    public static void setupAuth() {
        ready = true;
        // the above means the CalledTooEarlyException can't be thrown. ignore the error
        try {
            addUser(Config.ADMIN_USERNAME, Config.ADMIN_HASH.getBytes(StandardCharsets.UTF_8));
        } catch (CalledTooEarlyException ignored) {}
    }

    /**
     * Add a user to the list.
     * Checks if the user already exists. If so, nothing is done. This condition shouldn't be possible.
     * If the user does not exist, it is added to the map and appended to the hash file.
     *
     * This function interacts with the user map, so it must be called after setupAuth.
     * @param username The new user's login name.
     * @param password The hash of the user's password.
     */
    public static void addUser(String username, byte[] password) throws CalledTooEarlyException {
        if(!ready)
            throw new CalledTooEarlyException();

        if (users.containsKey(username))
            return;

        users.put(username, password);
        //TOMLWriter.addPair(username, password);
    }

    /**
     * Given a username and a plaintext password, hash and compare against the stored password for that user.
     * If the user does not exist, this throws {@link java.util.NoSuchElementException}.
     *
     * This function interacts with the user map, so it must be called after setupAuth.
     * @param username The username of the user to check.
     * @param password The plaintext password to check.
     * @return true if the password matches, false if the password does not match.
     */
    public static boolean checkPassword(String username, String password) throws CalledTooEarlyException, NoSuchElementException {
        if(!ready)
            throw new CalledTooEarlyException();

        if (!users.containsKey(username))
            throw new NoSuchElementException("User " + username + " does not exist.");

        return BCrypt.verifyer().verify(password.getBytes(StandardCharsets.UTF_8), users.get(username)).verified;
    }

    /**
     * Given the bytes of a password, use BCrypt to generate a cryptographically secure hash.
     * This should not be used for verification, only for initial creation of a hash.
     *
     * Uses a 2^12 (4096) iteration factor.
     * @param password the bytes of a password string to hash.
     * @return the bytes of a secure hashed password.
     */
    public static byte[] createPasswordHash(byte[] password) {
        return BCrypt.withDefaults().hash(12, password);
    }

}
