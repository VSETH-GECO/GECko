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

import ch.ethz.geco.gecko.command.CommandUtils;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.ReactionRemoveEvent;
import sx.blah.discord.handle.impl.obj.Reaction;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IReaction;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Just a simple vote data structure
 */
public class Vote {
    private IMessage message;
    private String question;
    private List<String> answers;
    private Map<String, IReaction> reactions;
    private ZonedDateTime timelimit;

    public Vote(IChannel channel, String question, List<String> answers, ZonedDateTime timelimit) {
        this.question = question;
        this.answers = answers;
        this.timelimit = timelimit;

        message = CommandUtils.respond(channel, "--- " + StringUtils.capitalize(question) + " ---\n" + getAnswerString() + "\n\n" + "Ends at: " + timelimit.format(DateTimeFormatter.RFC_1123_DATE_TIME));
        /**
         * --- Bla? ---
         * 1) Ja:   React to this message to set a reaction for this answer.
         * 2) Nein: ...
         * 3) Vllt: ...
         *
         * Ends at: Tue, 3 Jun 2008 11:05:30 GMT
         */

    }

    private String getAnswerString() {
        String answerString = "";
        int i = 1;
        for (String answer : answers) {
            answerString += i + ")" + StringUtils.capitalize(answer) + ": React to set a reaction for this answer.\n";
            i++;
        }

        return answerString;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public Map<String, IReaction> getReactions() {
        return reactions;
    }

    public ZonedDateTime getTimelimit() {
        return timelimit;
    }

    public void endVote() {
        CommandUtils.editMessage(message, "It's over man.");
    }

    @EventSubscriber
    public void onReactionAddEvent(ReactionAddEvent e) {

    }

    @EventSubscriber
    public void onReactionRemoveEvent(ReactionRemoveEvent e) {

    }
}
