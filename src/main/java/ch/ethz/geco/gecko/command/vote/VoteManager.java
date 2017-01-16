/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org>
 */

package ch.ethz.geco.gecko.command.vote;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages and stores the votes of all channels
 */
public class VoteManager {
    /**
     * A map of all channel ids and their votes
     */
    private static Map<String, Vote> currentVotes = new HashMap<>();

    /**
     * Starts the periodic vote check
     */
    public static void init() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                checkVotes();
            }
        }, 5000, 5000);
    }

    /**
     * Returns whether or not there is already a vote in the given channel.
     *
     * @param channelID the channel to check passed as channel ID
     * @return whether or not there is a vote in that channel
     */
    public static boolean hasVote(String channelID) {
        return currentVotes.containsKey(channelID);
    }

    /**
     * Returns the vote of the specified channel ID
     *
     * @param channelID the channel to check passed as channel ID
     * @return the vote mapping the specified channel ID or null if there is no vote
     */
    public static Vote getVote(String channelID) {
        return currentVotes.get(channelID);
    }

    /**
     * Sets the vote for the specified channel ID. This will replace old votes if there are any.
     *
     * @param channelID the channel to modify passed as channel ID
     * @param vote      the vote to set
     */
    public static void setVote(String channelID, Vote vote) {
        currentVotes.put(channelID, vote);
    }

    /**
     * Checks if one of the current vote has finished
     */
    private static void checkVotes() {
        for (Map.Entry<String, Vote> vote : currentVotes.entrySet()) {
            // If we exceeded the time limit
            if (LocalDateTime.now(Clock.systemUTC()).isAfter(vote.getValue().getTimelimit())) {
                // End vote
                vote.getValue().endVote();
            }
        }
    }
}
