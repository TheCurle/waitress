package uk.gemwire.waitress.authentication;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.gemwire.waitress.permissions.PermissionStore;
import uk.gemwire.waitress.permissions.entity.User;
import uk.gemwire.waitress.config.Config;

import java.io.*;
import java.lang.reflect.Type;
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

    private static Map<User, byte[]> hashes = new HashMap<>();

    // True if the Config has been loaded and we're ready to rock.
    private static boolean ready = false;

    /**
     * Initialize the Auth system.
     * Adds the admin user with global permissions to the user map.
     * This user should always exist and should always be the first user.
     *
     * Reads the rest of users from disk.
     */
    public static void setupAuth() {
        ready = true;
        // the above means the CalledTooEarlyException can't be thrown. ignore the error
        try {
            // Deserialize all users from disk, should they be there.
            Gson gson = new Gson();
            Type passwordMap = new TypeToken<Map<User, byte[]>>(){}.getType();
            Reader authReader = new FileReader(Config.DATA_DIR + "waitress/auth.json");

            hashes = gson.fromJson(authReader, passwordMap);

            // addUser does nothing if the user already exists, so there is no disadvantage to this.
            addUser(PermissionStore.userByName(Config.ADMIN_USERNAME), Config.ADMIN_HASH.getBytes(StandardCharsets.UTF_8));

        } catch (CalledTooEarlyException | FileNotFoundException ignored) {}
    }

    /**
     * Write all saved passwords to DATA_DIR/waitress/auth.json
     * TODO: this should be rather secure due to the salted hashed nature of the passwords,
     *  but there should be an alternative solution.
     */
    public static void writeMap() throws IOException {
        // Deserialize all users from disk, should they be there.
        Gson gson = new Gson();
        Type passwordMap = new TypeToken<Map<User, byte[]>>(){}.getType();
        Writer authWriter = new FileWriter(Config.DATA_DIR + "waitress/auth.json");

        gson.toJson(hashes, passwordMap, authWriter);

    }

    /**
     * Add a user to the list.
     * Checks if the user already exists. If so, nothing is done. This condition shouldn't be possible.
     * If the user does not exist, it is added to the map and appended to the hash file.
     *
     * This function interacts with the user map, so it must be called after setupAuth.
     * @param user The new user.
     * @param password The hash of the user's password.
     */
    public static void addUser(User user, byte[] password) throws CalledTooEarlyException {
        if(!ready)
            throw new CalledTooEarlyException();

        if (hashes.containsKey(user))
            return;

        hashes.put(user, password);
        //TOMLWriter.addPair(username, password);
    }

    /**
     * Given a username and a plaintext password, hash and compare against the stored password for that user.
     * If the user does not exist, this throws {@link java.util.NoSuchElementException}.
     *
     * This function interacts with the user map, so it must be called after setupAuth.
     * @param user The user to check.
     * @param password The plaintext password to check.
     * @return true if the password matches, false if the password does not match.
     */
    public static boolean checkPassword(User user, String password) throws CalledTooEarlyException, NoSuchElementException {
        if(!ready)
            throw new CalledTooEarlyException();

        if (!hashes.containsKey(user))
            throw new NoSuchElementException("User " + user.getUsername() + " does not exist.");

        return BCrypt.verifyer().verify(password.getBytes(StandardCharsets.UTF_8), hashes.get(user)).verified;
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
