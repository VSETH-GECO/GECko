package ch.ethz.geco.gecko.markdown.impl;

import ch.ethz.geco.gecko.markdown.AtomType;

import java.util.List;
import java.util.regex.Pattern;

public class Table implements AtomType {
    private static final Pattern pattern = Pattern.compile("(?>^|\\r|\\n)\\|[^|]*\\|.*[\\r\\n]\\|\\s*-+\\s*\\|");

    @Override
    public String parse(String raw) {
        // TODO
        return raw;
    }

    @Override
    public List<Result> findAll(String raw) {
        // TODO
        return null;
    }
}
