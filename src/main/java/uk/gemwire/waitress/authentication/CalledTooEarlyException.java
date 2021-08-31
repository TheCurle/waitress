package uk.gemwire.waitress.authentication;

/**
 * Used exclusively in {@link Auth}, where no authentication operations can occur until the user map is populated.
 *
 * @author Curle
 */
public class CalledTooEarlyException extends Exception {

    @Override
    public String getMessage() {
        return "Method called before the ready state. Investigate.";
    }
}
