package uk.gemwire.waitress.config;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.StringTokenizer;

/**
 * Contains a tiny parser for the TOML format, using StringTokeniser as a lexer.
 * Also happens to work for ini and java properties files.
 *
 * Intended to only be able to read the file that is output by Waitress.
 * As such, it treats duplicate keys as invalid.
 *
 * All initializations of StringTokeniser in this class specify a range of delimiters.
 * The default delimiters are " \t\n\r\f", but this poses a problem when parsing comments, so newlines are preserved.
 *
 * @author Curle
 */
public final class TOMLReader {

    private static StringTokenizer tokens;

    public static HashMap<String,String> read(String text) {
        tokens = new StringTokenizer(text, " \t\r\f");
        Parser parser = new Parser();
        return parser.parse();
    }

    public static HashMap<String,String> read(Reader reader) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        reader.close();

        tokens = new StringTokenizer(buffer.toString(), " \t\r\f");
        Parser parser = new Parser();
        return parser.parse();
    }

    /**
     * Turn the given file into a {@link java.util.HashMap} of {@link String},{@link String}.
     * Categories are ignored. Duplicate keys return an error.
     */
    static class Parser {
        private HashMap<String, String> internalMap = new HashMap<>();

        /**
         * Iterate the token list, building up the internal map.
         * If the file is valid, then the map is returned.
         *
         * Parsing is performed in three steps:
         *      1) Find a first-layer statement, like a category or a definition.
         *      2) Ignore categories.
         *      3) Parse definitions into a String-String pair, and emplace into the map.
         *
         * If something in the file cannot be coerced into a String, or if there are duplicate key pairs, then the
         * {@link IllegalStateException} is thrown.
         */
        public HashMap<String,String> parse() throws IllegalStateException {
            while(tokens.hasMoreTokens()) {

                String token = tokens.nextToken();

                // Don't need to do anything if we have a condensed x=y pair
                if(token.contains("=") && (token.indexOf('=') < token.length())) {
                    pair(token.strip());
                    continue;
                }

                // # Comments should be skipped entirely.
                if(token.contains("#")) {
                    do {
                        if(tokens.hasMoreTokens())
                            token = tokens.nextToken();
                        else
                            // Gracefully handle a comment at the end of a file
                            return internalMap;
                    } while (! (token.contains("\n")));
                }

                token = token.strip();
                if(token.length() < 1) continue;

                // Categories ([[ these ]]) are skipped in parsing.

                if(token.startsWith("[[") && token.endsWith("]]"))
                    // We're dealing with a category
                    continue;

                // TOML allows for key-value pairs to be in the forms:
                //  key=value
                //  key =value
                //  key = value
                // Due to the nature of tokenisation, this means that it can take one to three tokens to parse this.

                int counter = 2;
                // The next section may be repeated up to twice, so it is wrapped in a while loop, with a limiter of two
                while(counter > 0) {
                    // Construct a full token.
                    if (tokens.hasMoreTokens()) {
                        // Get the new token and check it.
                        final String tempToken = tokens.nextToken();
                        // TODO: cleanup
                        if (tempToken.contains("="))
                            // If we just have a = on its own, then we need one more pass.
                            if(tempToken.length() == 1)
                                counter = 1;
                            // If we have more than a = then it's right aligned, so stop passing over it
                            if(tempToken.length() > 1)
                                counter = 0;


                        // Join the next token with the one we already have.
                        // Make sure there are no spaces involved. everything that goes into token must be a=b.
                        token = new StringJoiner(" ").add(token.strip()).add(tempToken.strip()).toString();
                    } else {
                        throw new IllegalStateException("Unable to fully parse a key-value pair.");
                    }
                }

                // We have the full key-value pair parsed.
                pair(token);
            }

            return internalMap;
        }

        /**
         * Parse a key-value pair out of the given string.
         */
        private void pair(String text) {
            String[] parts = text.split("=");

            internalMap.put(parts[0], parts[1].replace("\"", ""));
        }

    }

}
