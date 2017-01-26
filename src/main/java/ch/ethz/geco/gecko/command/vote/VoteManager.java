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
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Manages and stores the votes of all channels
 */
public class VoteManager {
    /**
     * A map of all channel ids and their votes
     */
    private static List<Vote> currentVotes = new ArrayList<>();

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
     * Adds a vote to the vote manager. As soon as a vote was added, it will automatically end after expiring.
     *
     * @param vote the vote to add
     */
    public static void addVote(Vote vote) {
        currentVotes.add(vote);
    }

    /**
     * Checks if one of the current vote has finished
     */
    private static void checkVotes() {
        for (Vote vote : currentVotes) {
            // If we exceeded the time limit
            if (ZonedDateTime.now(Clock.systemUTC()).isAfter(vote.getTimelimit())) {
                // End vote
                vote.endVote();
                currentVotes.remove(vote);
            }
        }
    }
}
