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

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class CommandUtils {
    /**
     * Responds to the given message.
     *
     * @param msg  the message to respond to
     * @param text the response text
     * @return the response message
     */
    public static Mono<Message> respond(Message msg, String text) {
        return respond(msg.getChannel(), text);
    }

    /**
     * Responds to the given message.
     *
     * @param msg   the message to respond to
     * @param embed the embed to send
     * @return the response message
     */
    public static Mono<Message> respond(Message msg, Consumer<EmbedCreateSpec> embed) {
        return respond(msg.getChannel(), embed);
    }

    /**
     * Responds in the given channel.
     *
     * @param channel the channel in which to respond
     * @param text    the response text
     * @return the response message
     */
    public static Mono<Message> respond(Mono<MessageChannel> channel, String text) {
        return channel.flatMap(messageChannel -> messageChannel.createMessage(text));
    }

    /**
     * Responds in the given channel with an embed.
     *
     * @param channel the channel in which to respond
     * @param embed   the embed to send
     * @return the response message
     */
    public static Mono<Message> respond(Mono<MessageChannel> channel, Consumer<EmbedCreateSpec> embed) {
        return channel.flatMap(messageChannel -> messageChannel.createMessage(messageCreateSpec -> messageCreateSpec.setEmbed(embed)));
    }

    /**
     * Edits a given message.
     *
     * @param msg  the message to edit
     * @param text the new message text
     * @return the edited message
     */
    public static Mono<Message> editMessage(Message msg, String text) {
        return msg.edit(messageEditSpec -> messageEditSpec.setContent(text));
    }

    /**
     * Edits the given message with an embed.
     *
     * @param msg   the message to edit
     * @param embed the new embed
     * @return the edited message
     */
    public static Mono<Message> editMessage(Message msg, Consumer<EmbedCreateSpec> embed) {
        return msg.edit(messageEditSpec -> messageEditSpec.setEmbed(embed));
    }

    /**
     * Deletes the message which triggered this command.
     *
     * @param msg the message which triggered this command
     */
    public static Mono<Void> deleteMessage(Message msg) {
        return msg.delete();
    }
}
