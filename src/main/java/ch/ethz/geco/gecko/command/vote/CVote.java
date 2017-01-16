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

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.command.Command;
import sx.blah.discord.handle.obj.IMessage;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to create a new vote
 */
public class CVote extends Command {
    public CVote() {
        this.setNames(new String[]{"cvote", "createvote"});
        this.setParams("<question> <answer1> <answer2> [answer3] ... <timelimit>");
        this.setDescription("Creates a vote where users can respond to using reactions. You need to specify a question followed by at least two answers. The last argument specifies how long a vote should be active. You can specify an offset in HMS format like:\n- 5m (5 minutes)\n- 4h3m2s (4 hours 3 minutes and 2 seconds)\n\nBut you can also set a date as time limit in DD.MM.YY HH:MM:SS format:\n- 24.12.2042 14:53:12\nAny missing parts get zeroed if applicable, for example:\n- 24.12.2042 14:53 (note that the seconds are missing)\n- 24.12.2042 (will end at this day at 00:00:00)");
        this.setAllowPrivateMessage(false);
        this.setRemoveAfterCall(true);

        // Initialize VoteManager on command load
        VoteManager.init();
    }

    /**
     * Planning phase:
     * We need to store:
     * - Question
     * - Answers (in form of reactions)
     * - Timelimit (for time limited vote?)
     * - Majority (for majority vote?)
     * <p>
     * On every reaction event check if vote is finished or if timelimit exceeds
     * <p>
     * Creating a vote:
     * - Store question, answers and end condition
     * - Ask for reactions for every answer and store them
     * - Activate vote (check for end condition periodically)
     * - Where to place the reaction event listener?
     */

    @Override
    public void execute(IMessage msg, List<String> args) {
        if (args.size() >= 4) {
            // Get answers
            List<String> answers = new ArrayList<>();
            for (int i = 1; i < args.size() - 1; i++) {
                answers.add(args.get(i));
            }

            LocalDateTime dateNow = LocalDateTime.now(Clock.systemUTC());
            LocalDateTime dateLimit = LocalDateTime.parse(args.get(args.size()-1) + " " + ConfigManager.getProperties().getProperty("timezone"), DateTimeFormatter.ofPattern("[[dd'.]MM'.]yyyy[' HH[':mm[':ss]]]' O"));

            GECkO.logger.debug("Day: " + dateLimit.getDayOfMonth());
            GECkO.logger.debug("Month: " + dateLimit.getMonthValue());
            GECkO.logger.debug("Year: " + dateLimit.getYear());

            GECkO.logger.debug("Hour: " + dateLimit.getHour());
            GECkO.logger.debug("Min: " + dateLimit.getMinute());
            GECkO.logger.debug("Sec: " + dateLimit.getSecond());

            //Vote newVote = new Vote(args.get(0), answers, );
        } else {
            printUsage(msg);
        }
    }
}
