package uk.gemwire.waitress.config;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.StringTokenizer;

/**
 * Contains a tiny parser for the TOML format, using StringTokeniser as a lexer.
 *
 * Intended to only be able to read the file that is output by Waitress.
 * As such, it treats duplicate keys as invalid.
 *
 * @author Curle
 */
public final class TOMLReader {

    private StringTokenizer tokens;

    public TOMLReader(String text) {
        tokens = new StringTokenizer(text);
    }

    public TOMLReader(Reader reader) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        reader.close();

        tokens = new StringTokenizer(buffer.toString());
    }

    public HashMap<String, String> parse() {
        Parser parser = new Parser();
        return parser.parse();
    }


    /**
     * Turn the given file into a {@link java.util.HashMap} of {@link String},{@link String}.
     * Categories are ignored. Duplicate keys return an error.
     */
    class Parser {
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

                // Store a token. TOML allows for key-value pairs to be in the forms:
                //  key=value
                //  key =value
                //  key = value
                // Due to the nature of tokenisation, this means that it can take one to three tokens to parse this.

                String token = tokens.nextToken();

                if(token.startsWith("[[") && token.endsWith("]]"))
                    // We're dealing with a category
                    continue;

                int counter = 0;
                // The next section may be repeated up to twice, so it is wrapped in a while loop, with a limiter of two.
                while(!(token.contains("=")) && counter < 2) {
                    // Construct a full token.
                    if (tokens.hasMoreTokens()) {
                        // Get the new token and check it.
                        final String tempToken = tokens.nextToken();
                        // If it is length one, then we need another token, as we only have the =
                        if(tempToken.length() == 1) counter--;
                        // Join the next token with the one we already have.
                        token = new StringJoiner(" ").add(token).add(tempToken).toString();
                        counter++;
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

            internalMap.put(parts[0], parts[1]);
        }

    }

}
