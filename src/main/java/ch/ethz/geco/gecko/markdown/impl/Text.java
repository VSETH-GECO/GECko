package ch.ethz.geco.gecko.markdown.impl;

import ch.ethz.geco.gecko.markdown.AtomType;

import java.util.regex.Pattern;

public class Text implements AtomType {
    @Override
    public Pattern getPattern() {
        return null;
    }
}
