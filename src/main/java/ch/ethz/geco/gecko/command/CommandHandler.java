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

import ch.ethz.geco.gecko.ErrorHandler;
import ch.ethz.geco.gecko.GECko;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ImmutableMessageData;
import discord4j.discordjson.json.MessageData;
import org.apache.commons.text.StrTokenizer;
import org.jetbrains.annotations.Contract;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class CommandHandler {
    /**
     * The current default command prefix if no prefix was defined inside the command.
     */
    private static String defaultPrefix = "!";

    @Contract(pure = true)
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
     * Injects the private channel of the author into the message.
     *
     * @param msg the message where to inject the private channel
     * @return the message after injection
     */
    private static Mono<Message> injectPrivateChannel(Message msg) {
        if (!msg.getAuthor().isPresent())
            return Mono.just(msg);

        return msg.getAuthor().get().getPrivateChannel().map(privateChannel -> {
            try {
                // Access private field of message
                Field dataField = msg.getClass().getDeclaredField("data");
                dataField.setAccessible(true);
                ImmutableMessageData data = (ImmutableMessageData) dataField.get(msg);

                Field channelIDField = data.getClass().getDeclaredField("channelId_value");
                channelIDField.setAccessible(true);
                channelIDField.setLong(data, privateChannel.getId().asLong());

                channelIDField.setAccessible(false);
                dataField.setAccessible(false);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                ErrorHandler.handleError(e);
            }

            return msg;
        });
    }

    /**
     * Analyses the incoming messages to trigger matching commands.
     *
     * @param messageCreateEvent the message received event
     */
    public static void handle(MessageCreateEvent messageCreateEvent) {
        Message message = messageCreateEvent.getMessage();

        if (message.getContent().isBlank())
            return;

        String text = message.getContent();

        Arrays.stream(text.split("\n")).forEach((line) -> {
            // Make new modifiable message object
            Message msg = message;

            // Parse command
            StrTokenizer tokenizer = new StrTokenizer(line, ' ', '"');
            List<String> tokens = tokenizer.getTokenList();

            // Determine type of command
            Command command = null;
            if (tokens.size() > 0) {
                if (tokens.get(0).equals("<@" + GECko.discordClient.getSelfId() + ">")) {
                    if (tokens.size() > 1) {
                        command = CommandRegistry.getMentionCommand(tokens.get(1));
                    } else {
                        return;
                    }
                } else {
                    command = CommandRegistry.getPrefixCommand(tokens.get(0));
                }
            }

            if (command != null) {
                if (messageCreateEvent.getGuildId().isEmpty() || command.isAllowPrivateMessage()) {
                    if (command.isForcePrivateReply()) {
                        Message newMsg = injectPrivateChannel(msg).block();
                        if (newMsg != null) {
                            msg = newMsg;
                        }
                    }

                    if ((messageCreateEvent.getMember().isEmpty() && msg.getAuthor().isPresent() && command.getPermissions().isUserPermitted(msg.getAuthor().get())) || command.getPermissions().isMemberPermitted(messageCreateEvent.getMember().get())) {
                        List<String> args;
                        if (!command.isMentionCommand()) {
                            args = tokens.subList(1, tokens.size());
                            GECko.logger.debug("Calling command <" + tokens.get(0) + "> with arguments: " + args.toString());
                        } else {
                            args = tokens.subList(2, tokens.size());
                            GECko.logger.debug("Calling mention command <" + tokens.get(1) + "> with arguments: " + args.toString());
                        }
                        command.execute(msg, args);
                    } else {
                        CommandUtils.respond(msg, "You are not permitted to use this command.").subscribe();
                    }

                    if (command.isRemoveAfterCall()) {
                        CommandUtils.deleteMessage(msg).subscribe();
                    }
                }
            }
        });
    }
}
