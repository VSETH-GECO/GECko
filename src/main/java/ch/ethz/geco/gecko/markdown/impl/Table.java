package ch.ethz.geco.gecko.markdown.impl;

import ch.ethz.geco.gecko.markdown.AtomType;

import java.util.regex.Pattern;

public class Table implements AtomType {
    private static final Pattern tablePattern = Pattern.compile("(?>^|\\r|\\n)\\|[^|]*\\|.*[\\r\\n]\\|\\s*-+\\s*\\|");

    @Override
    public Pattern getPattern() {
        return tablePattern;
    }
}
