package ch.ethz.geco.gecko.veto;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a single map veto
 */
public class Veto {
    // Veto specifications
    private final int bestOf;
    private final int bansPerTeam;
    private final Mode vetoMode;

    // Map sets to keep track of current veto state
    private final Set<String> mapsPicked = new HashSet<>();
    private final Set<String> mapsBanned = new HashSet<>();
    private final Set<String> mapsRemaining = new HashSet<>();

    public Veto(int bestOf, int bansPerTeam, Mode vetoMode) {
        this.bestOf = bestOf;
        this.bansPerTeam = bansPerTeam;
        this.vetoMode = vetoMode;
    }

    /**
     * Picks the given map returning true if the map was picked and false if either
     * there is no such map or the map was already picked or banned.
     *
     * @param map the map to pick
     * @return true on success, false otherwise
     */
    public boolean pickMap(String map) {
        if (mapsRemaining.contains(map)) {
            mapsRemaining.remove(map);
            mapsPicked.add(map);
            return true;
        }

        return false;
    }

    public boolean banMap(String map) {
        if (mapsRemaining.contains(map)) {
            mapsRemaining.remove(map);
            mapsBanned.add(map);
            return true;
        }

        return false;
    }

    /**
     * This represents the different veto modes
     */
    public enum Mode {
        BAN, RANDOM
    }
}
