package ch.ethz.geco.gecko.veto;

import java.util.HashMap;
import java.util.Map;

public class Games {
    // TODO: Read map lists from config
    public static final Map<String, String[]> OVERWATCH = new HashMap<>();

    static {
        OVERWATCH.put("Assault", new String[]{"Hanamura", "Horizon Lunar Colony", "Temple of Anubis", "Volskaya Industries"});
        OVERWATCH.put("Escort", new String[]{"Dorado", "Junkertown", "Rialto", "Route 66", "Watchpoint: Gibraltar"});
        OVERWATCH.put("Assault Escort", new String[]{"Blizzard World", "Eichenwalde", "Hollywood", "King's Row", "Numbani"});
        OVERWATCH.put("Control", new String[]{"Ilios", "Lijiang Tower", "Nepal", "Oasis"});
        OVERWATCH.put("Arena", new String[]{"Ayutthaya", "Black Forest", "Castillo", "Château Guillard", "Ecopoint: Antarctica", "Necropolis"});
    }
}