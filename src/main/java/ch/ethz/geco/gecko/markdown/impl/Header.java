package ch.ethz.geco.gecko.markdown.impl;

import ch.ethz.geco.gecko.markdown.AtomType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Header implements AtomType {
    private static final Pattern pattern = Pattern.compile("(?>[\\r\\n]|^)((#+)\\s+([^\\r\\n]+))(?>\\r\\n|\\r|\\n|$)");

    @Override
    public String parse(String raw) {
        Matcher matcher = pattern.matcher(raw);

        if (matcher.matches()) {
            int heading = matcher.group(1).length();

            if (heading == 1) {
                return matcher.replaceFirst("**__$2__**\n");
            } else {
                return matcher.replaceFirst("__$2__\n");
            }
        }

        return null;
    }

    @Override
    public List<Result> findAll(String raw) {
        Matcher matcher = pattern.matcher(raw);
        List<Result> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(new Result(matcher.group(1), matcher.start(1), matcher.end(1)));
        }

        return results;
    }
}
