package ch.ethz.geco.gecko.markdown;

import ch.ethz.geco.gecko.GECko;

public class Atom {
    private final AtomType type;
    private final int rawStartPos;
    private final int rawEndPos;
    private String content;
    private boolean isParsed = false;

    public Atom(String content, AtomType type, int startPos, int endPos) {
        this.content = content;
        this.type = type;
        this.rawStartPos = startPos;
        this.rawEndPos = endPos;
    }

    public void parse() {
        if (isParsed)
            return;

        String parsed = type.parse(content);

        if (parsed == null) {
            GECko.logger.warn("Parsing of atom failed!");
        } else {
            content = type.parse(content);
        }

        isParsed = true;
    }

    public String getContent() {
        return content;
    }

    public int getRawStartPos() {
        return rawStartPos;
    }

    public int getRawEndPos() {
        return rawEndPos;
    }
}
