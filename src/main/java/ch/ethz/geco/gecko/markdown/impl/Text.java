package ch.ethz.geco.gecko.markdown.impl;

import ch.ethz.geco.gecko.markdown.AtomType;

import java.util.ArrayList;
import java.util.List;

public class Text implements AtomType {
    @Override
    public boolean isDividable() {
        return true;
    }

    @Override
    public String parse(String raw) {
        return raw;
    }

    @Override
    public List<Result> findAll(String raw) {
        return new ArrayList<>();
    }
}
