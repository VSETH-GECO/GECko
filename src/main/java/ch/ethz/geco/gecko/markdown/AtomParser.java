package ch.ethz.geco.gecko.markdown;

import ch.ethz.geco.gecko.markdown.impl.Text;

import java.util.ArrayList;
import java.util.List;

public class AtomParser {
    private static final List<AtomType> atomTypes = new ArrayList<>();
    private final List<Atom> atoms = new ArrayList<>();
    private final String trimMessage;
    private final String rawText;

    // Populate list of atom types
    static {
        atomTypes.add(new Text());
    }

    /**
     * Constructs a new atom parser from the given raw text and trim message.
     *
     * @param raw         The raw text to parse.
     * @param trimMessage The message to display in case the text does not fit a given maximum length.
     */
    public AtomParser(String raw, String trimMessage) {
        this.trimMessage = trimMessage;
        this.rawText = raw;

        parse();
    }

    /**
     * Parses the raw text the parser was constructed with and puts the result into the atoms list.
     */
    private void parse() {
        // TODO: implement
        /* Raw steps:
         * 1. Create a list of unparsed atoms by detecting atom type patterns globally and constructing appropriate atom objects.
         * 2. Invoke the sub-parsing of all atoms.
         */
    }

    /**
     * Gets the output of the parser trying to fit as many atoms as possible without exceeding the given maximum length.
     *
     * @param maxLength The length the output should not exceed.
     * @return The output of the parse not exceeding the maximum length.
     */
    public String getParsedContent(int maxLength) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < atoms.size(); i++) {
            Atom atom = atoms.get(i);
            if (i == atoms.size() - 1) {
                if (out.length() + atom.getContent().length() <= maxLength) {
                    out.append(atom.getContent());
                } else {
                    // TODO: Add text atom handling (they are actually dividable)
                    // If the last atom does not fit we append the trim message.
                    // But we need to ensure already in the previous iteration that the trim message fits.
                    out.append(trimMessage);
                    break;
                }
            } else {
                if (out.length() + atom.getContent().length() + trimMessage.length() <= maxLength) {
                    // If the next atom plus the trim message fits, we can simply append it.
                    out.append(atom.getContent());
                } else {
                    // TODO: Add text atom handling (they are actually dividable)
                    // If it doesn't fit, at least the trim message has to fit since we checked that in the last iteration.
                    out.append(trimMessage);
                    break;
                }
            }
        }

        return out.toString().strip();
    }

    /**
     * Gets the full output of the parsed text.
     *
     * @return The full output of the parser.
     */
    public String getParsedContent() {
        StringBuilder out = new StringBuilder();
        for (Atom atom : atoms) {
            out.append(atom.getContent());
        }

        return out.toString().strip();
    }
}
