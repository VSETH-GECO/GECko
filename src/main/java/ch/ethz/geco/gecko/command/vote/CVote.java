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
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import sx.blah.discord.handle.obj.IMessage;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to create a new vote
 */
public class CVote extends Command {
    public CVote() {
        this.setNames(new String[]{"cvote", "createvote"});
        this.setParams("<question> <answer1> <answer2> [answer3] ... <timelimit>");
        this.setDescription("Creates a vote where users can respond to using reactions. You need to specify a question followed by at least two answers. The last argument specifies how long a vote should be active. You can specify an offset in HMS format like:\n- 5m (5 minutes)\n- 4h3m2s (4 hours 3 minutes and 2 seconds)\n\nBut you can also set a date as time limit in DD.MM.YY HH:MM:SS format:\n- 24.12.2042 14:53:12\nAny missing parts get zeroed if applicable, for example:\n- 24.12.2042 14:53 (note that the seconds are missing)\n- 24.12.2042 (will end when the 24th begins)");
        this.setAllowPrivateMessage(false);
        //this.setRemoveAfterCall(true);

        // Initialize VoteManager on command load
        VoteManager.init();
    }

    private static DateTimeFormatter dateTimeFormat = new DateTimeFormatterBuilder().appendPattern("[[dd.]MM.]yyyy[[ HH][:mm][:ss]] O ")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .toFormatter();

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

            // Get current time
            ZonedDateTime dateNow = ZonedDateTime.now(Clock.systemUTC());
            try {
                // Parse time limit from given string
                ZonedDateTime dateLimit = ZonedDateTime.parse(args.get(args.size()-1) + " " + ConfigManager.getProperties().getProperty("timezone") + " ", dateTimeFormat);

                if (dateLimit.isBefore(dateNow)) {
                    CommandUtils.respond(msg, "I can't create a vote that ends before now, duh.");
                } else {
                    Vote newVote = new Vote(msg.getChannel(), args.get(0), answers, dateLimit);
                    VoteManager.addVote(newVote);
                }
            } catch (DateTimeParseException e) {
                CommandUtils.respond(msg, "Please give me the date/time format I requested. I'm not Wolfram Alpha.");
            }
        } else {
            printUsage(msg);
        }
    }
}
