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
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IReaction;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Just a simple vote data structure
 */
public class Vote {
    private IMessage message;
    private String question;
    private ArrayList<String> answers;
    private ArrayList<IReaction> validReactions;
    private LocalDateTime timelimit;

    public Vote(IChannel channel, String question, ArrayList<String> answers, LocalDateTime timelimit) {
        this.question = question;
        this.answers = answers;
        this.timelimit = timelimit;

        message = CommandUtils.respond(channel, "Test");
    }

    public String getQuestion() {
        return question;
    }

    public ArrayList<String> getAnswers() {
        return answers;
    }

    public ArrayList<IReaction> getValidReactions() {
        return validReactions;
    }

    public LocalDateTime getTimelimit() {
        return timelimit;
    }

    public void endVote() {
        CommandUtils.editMessage(message, "It's over man.");
    }
}
