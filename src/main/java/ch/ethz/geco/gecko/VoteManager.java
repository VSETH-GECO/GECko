package ch.ethz.geco.gecko;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

public class VoteManager {
    class Games {
        Map<String, List<String>> maps;
    }

    class Vote {
        class Map {
            String name;
            byte vote;
        }

        String game;
        java.util.Map<String, Map> maps;
        int status;
    }

    Map<Integer, Vote> votes;
    Map<Integer, Integer> votesP2_P1;
    Map<Integer, Integer> votesP1_P2;

    /**
     * Inserts a vote and associates it with both given player ids
     *
     * @param player1ID
     * @param player2ID
     * @param vote
     */
    private void insertNewVote(int player1ID, int player2ID, Vote vote) {
        if (getVoteByPlayerID(player1ID) != null || getVoteByPlayerID(player2ID) != null)
            return;

        votes.put(player1ID, vote);
        votesP1_P2.put(player1ID, player2ID);
        votesP2_P1.put(player2ID, player1ID);
    }

    /**
     * Removes a active vote from the system, does nothing if there
     * is no active vote associated with the given player id
     *
     * @param playerID associated with the vote to be removed
     */
    private void removeVote(int playerID) {
        int player2ID;

        if (votesP1_P2.containsKey(playerID)) {
            player2ID = votesP1_P2.get(playerID);
            votes.remove(playerID);
            votesP1_P2.remove(playerID);
            votesP2_P1.remove(player2ID);

        } else if (votesP2_P1.containsKey(playerID)) {
            player2ID = votesP2_P1.get(playerID);
            votes.remove(player2ID);
            votesP1_P2.remove(player2ID);
            votesP2_P1.remove(playerID);
        }
    }

    /**
     * Returns the vote associated with a given players id.
     * If no active votes match the id null is returned.
     *
     * @param playerID associated with the desired vote
     * @return the vote for the given player id or null
     */
    private Vote getVoteByPlayerID(int playerID) {
        if (votes.containsKey(playerID)) {
            return votes.get(playerID);
        } else if (votesP2_P1.containsKey(playerID)) {
            return votes.get(votesP2_P1.get(playerID));
        } else {
            return null;
        }
    }

    /**
     * (Re-)reads the json file to update game and map list
     * @throws FileNotFoundException
     */
    private void readMapJson() throws FileNotFoundException {
        Gson gson = new GsonBuilder().create();
        JsonReader jr = new JsonReader(new FileReader(System.getProperty("user.dir") + "/data/map_list.json"));

        gson.fromJson(jr, Games.class);
    }


    public void newVote(int player1ID, int player2ID) {
        Vote newVote = new Vote();
        newVote.status = 0;

        insertNewVote(player1ID, player2ID, newVote);
    }

    public String castVote(int playerID, byte playerVote, Vote.Map map) {
        Vote vote = getVoteByPlayerID(playerID);

        if (vote != null) {
            vote.maps.get(map.name);
            return "Vote casted";
        } else {
            return "Error: no active vote for this player found. Start one with '/mapvote @other_player'";
        }
    }

    /**
     * Returns a string of the current vote state
     *
     * @param playerID of one of the players in the vote
     * @return string representing the vote state
     */
    public String getVotes(int playerID) {
        return "Not implemented";
    }

}