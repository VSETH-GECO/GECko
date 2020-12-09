package ch.ethz.geco.gecko.markdown;

import ch.ethz.geco.gecko.markdown.impl.Header;
import ch.ethz.geco.gecko.markdown.impl.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AtomParser {
    private static final List<AtomType> atomTypes = new ArrayList<>();
    private static final AtomType textType = new Text();

    // Populate list of atom types
    static {
        atomTypes.add(new Header());
    }

    private final String trimMessage;
    private final String rawText;
    private List<Atom> atoms = new ArrayList<>();

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
        /* Raw steps:
         * 1. Create a list of unparsed atoms by detecting atom type patterns globally and constructing appropriate atom objects.
         * 2. Invoke the sub-parsing of all atoms.
         */
        for (AtomType type : atomTypes) {
            if (type.isDividable()) // Ignore text
                continue;

            type.findAll(rawText).forEach(result -> atoms.add(new Atom(result.getMatch(), type, result.getStartPos(), result.getEndPos())));
        }

        // Sort atoms by start position
        atoms.sort(Comparator.comparingInt(Atom::getRawStartPos));

        // Remove all nested atoms by making a new list of only root atoms
        List<Atom> rootAtoms = new ArrayList<>();
        rootAtoms.add(atoms.get(0));
        for (int i = 1; i < atoms.size(); i++) {
            // If the end position of the current atom is after the end position of the last root atom, the current atom cannot be nested and is thus also root.
            if (atoms.get(i).getRawEndPos() > rootAtoms.get(rootAtoms.size() - 1).getRawEndPos()) {
                rootAtoms.add(atoms.get(i));
            }
        }
        atoms = rootAtoms;

        // Create "text" atoms from all areas in between root atoms
        List<Atom> textAtoms = new ArrayList<>();
        if (atoms.get(0).getRawStartPos() > 0) {
            textAtoms.add(new Atom(rawText.substring(0, atoms.get(0).getRawStartPos()), textType, 0, atoms.get(0).getRawStartPos()));
        }
        for (int i = 0; i < atoms.size() - 1; i++) {
            int start = atoms.get(i).getRawEndPos();
            int end = atoms.get(i + 1).getRawStartPos();

            if (end - start > 0) {
                textAtoms.add(new Atom(rawText.substring(start, end), textType, start, end));
            }
        }
        if (atoms.get(atoms.size() - 1).getRawEndPos() < rawText.length()) {
            textAtoms.add(new Atom(rawText.substring(atoms.get(atoms.size() - 1).getRawEndPos()), textType, atoms.get(atoms.size() - 1).getRawEndPos(), rawText.length()));
        }

        // Add all text atoms and sort again
        rootAtoms.addAll(textAtoms);
        atoms.sort(Comparator.comparingInt(Atom::getRawStartPos));

        // And finally parse all atoms
        atoms.forEach(Atom::parse);
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
