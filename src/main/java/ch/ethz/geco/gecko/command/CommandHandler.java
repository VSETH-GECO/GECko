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

package ch.ethz.geco.gecko.command;

import ch.ethz.geco.gecko.GECkO;
import org.apache.commons.lang3.text.StrTokenizer;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.obj.Embed;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements IListener<MessageReceivedEvent> {
    /**
     * The current default command prefix if no prefix was defined inside the command.
     */
    private static String defaultPrefix = "!";

    public static String getDefaultPrefix() {
        return defaultPrefix;
    }

    /**
     * Used to set the current default command prefix.
     *
     * @param prefix the new default command prefix
     */
    public static void setDefaultPrefix(String prefix) {
        CommandHandler.defaultPrefix = prefix;
    }

    /**
     * Analyses the incoming messages to trigger matching commands.
     *
     * @param messageReceivedEvent the message received event
     */
    @Override
    public void handle(MessageReceivedEvent messageReceivedEvent) {
        IMessage msg = messageReceivedEvent.getMessage();
        String text = msg.getContent();

        // Parse command
        StrTokenizer tokenizer = new StrTokenizer(text, ' ', '"');
        List<String> tokens = tokenizer.getTokenList();

        Command command = CommandRegistry.getPrefixCommand(tokens.get(0));

        if (command != null) {
            if (!command.isMentionCommand() && (!msg.getChannel().isPrivate() || command.isAllowPrivateMessage())) {
                if (command.isForcePrivateReply()) {
                    IMessage oldmsg = msg;
                    msg = injectPrivateChannel(oldmsg);

                    if (command.isRemoveAfterCall()) {
                        CommandUtils.deleteMessage(oldmsg);
                    }
                }

                if (command.getPermissions().isUserPermitted(msg.getGuild(), msg.getAuthor())) {
                    List<String> args = tokens.subList(1, tokens.size());
                    GECkO.logger.debug("Calling command <" + tokens.get(0) + "> with arguments: " + args.toString());
                    command.execute(msg, args);
                } else {
                    CommandUtils.respond(msg, "You are not permitted to use this command.");
                }

                if (command.isRemoveAfterCall()) {
                    CommandUtils.deleteMessage(msg);
                }
            } else {
                CommandUtils.respond(msg, "Unknown command: " + tokens.get(0));
                CommandUtils.deleteMessage(msg);
            }
        } else if (tokens.get(0).startsWith(defaultPrefix)) {
            CommandUtils.respond(msg, "Unknown command: " + tokens.get(0));
            CommandUtils.deleteMessage(msg);
        }
    }

    /**
     * Injects the private channel of the author into the message.
     *
     * @param msg the message where to inject the private channel
     * @return the message after injection
     */
    private static IMessage injectPrivateChannel(IMessage msg) {
        return RequestBuffer.request(() -> {
            try {
                List<Embed> implEmbedded = msg.getEmbedded().stream().map(intfEmbedded -> (Embed) intfEmbedded).collect(Collectors.toList());
                IMessage message = new Message(msg.getClient(), msg.getID(), msg.getContent(), msg.getAuthor(), msg.getAuthor().getOrCreatePMChannel(),
                        msg.getTimestamp(), msg.getEditedTimestamp().orElse(null), msg.mentionsEveryone(),
                        ((Message) msg).getRawMentions(), ((Message) msg).getRawRoleMentions(), msg.getAttachments(),
                        msg.isPinned(), implEmbedded, msg.getReactions(), msg.getWebhookID());

                return message;
            } catch (DiscordException e) {
                GECkO.logger.error("[CommandHandler] Could not inject private channel.");
                e.printStackTrace();

                return null;
            }
        }).get();
    }
}
