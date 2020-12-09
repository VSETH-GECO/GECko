package ch.ethz.geco.gecko.markdown;

import java.util.List;

public interface AtomType {
    /**
     * Parses the given string into a Discord compatible format.
     *
     * @param raw The raw string to parse.
     * @return A Discord compatible version of the string.
     */
    String parse(String raw);

    /**
     * Finds and returns all occurrences of the atom type in the given string.
     *
     * @param raw The raw string to process.
     * @return A list of all atoms of this type.
     */
    List<Result> findAll(String raw);

    /**
     * Returns if an atom type is dividable. Usually they should not be dividable but there are exceptions such as simple text.
     *
     * @return True if the atom type is dividable, otherwise false.
     */
    default boolean isDividable() {
        return false;
    }

    class Result {
        private final String match;
        private final int startPos;
        private final int endPos;

        public Result(String match, int startPos, int endPos) {
            this.match = match;
            this.startPos = startPos;
            this.endPos = endPos;
        }

        public String getMatch() {
            return match;
        }

        public int getStartPos() {
            return startPos;
        }

        public int getEndPos() {
            return endPos;
        }
    }
}
