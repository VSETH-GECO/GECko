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

import ch.ethz.geco.gecko.GECko;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * This class represents a basic command structure
 */
public abstract class Command {
    /**
     * Basic information
     */
    private String[] names;
    private String params = "";
    private String description = "";
    private String prefix;
    private final CommandPermissions permissions = new CommandPermissions();
    /**
     * Flags with default values
     */
    private boolean isMentionCommand = false;
    private boolean allowPrivateMessage = true;
    private boolean forcePrivateReply = false;
    private boolean removeAfterCall = false;

    /**
     * Returns the names of this command. Those are the names which trigger this command.
     *
     * @return the names of this command
     */
    public String[] getNames() {
        return names;
    }

    /**
     * Sets the names of this command. Those are the names which trigger this command.
     *
     * @param names the names of this command
     */
    public void setNames(String[] names) {
        this.names = names;
    }

    /**
     * Sets a single name for this command. Those are the names which trigger this command.
     *
     * @param name the name of this command
     */
    public void setName(String name) {
        this.names = new String[]{name};
    }

    /**
     * Gets the parameters of this command.
     *
     * @return the parameters of this command
     */
    public String getParams() {
        return params;
    }

    /**
     * Sets the parameters of this command. This is mainly used for looking up how to use this command.
     *
     * @param params the parameters of this command
     */
    public void setParams(String params) {
        this.params = params;
    }

    /**
     * Gets the description of this command.
     *
     * @return the description of this command
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this command.
     *
     * @param description the description of this command
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the prefix of this command.
     *
     * @return the prefix of this command or null if using default prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix of this command. NOTE: This overrides the default prefix.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the permissions of this command.
     *
     * @return the permissions of this command
     */
    public CommandPermissions getPermissions() {
        return permissions;
    }

    /**
     * Returns whether or not this is a mention command.
     *
     * @return whether or not this is a mention command
     */
    public boolean isMentionCommand() {
        return isMentionCommand;
    }

    /**
     * Sets whether or not this is a mention command. If it is, it gets triggered when the bot gets mentioned with the command name as message.
     *
     * @param mentionCommand if this is a mention command
     */
    public void setMentionCommand(boolean mentionCommand) {
        isMentionCommand = mentionCommand;
    }

    /**
     * Returns whether or not this command can be triggered via private messages.
     *
     * @return whether or not this command can be triggered via private messages
     */
    public boolean isAllowPrivateMessage() {
        return allowPrivateMessage;
    }

    /**
     * Sets whether or not this command can be triggered via private messages.
     *
     * @param allowPrivateMessage if this command can be triggered via private messages
     */
    public void setAllowPrivateMessage(boolean allowPrivateMessage) {
        this.allowPrivateMessage = allowPrivateMessage;
    }

    /**
     * Returns whether or not this command only replies privately.
     *
     * @return whether or not this command only replies privately
     */
    public boolean isForcePrivateReply() {
        return forcePrivateReply;
    }

    /**
     * Sets whether or not the bot should only reply privately when calling this command.
     *
     * @param forcePrivateReply if the bot only replies privately when calling this command
     */
    public void setForcePrivateReply(boolean forcePrivateReply) {
        this.forcePrivateReply = forcePrivateReply;
    }

    /**
     * Returns whether or not the command message gets removed after the command call.
     *
     * @return whether or not the command message gets removed after the command call
     */
    public boolean isRemoveAfterCall() {
        return removeAfterCall;
    }

    /**
     * Sets whether or not the command message should be removed after command call.
     *
     * @param removeAfterCall if the command message should be removed after command call
     */
    public void setRemoveAfterCall(boolean removeAfterCall) {
        this.removeAfterCall = removeAfterCall;
    }

    /**
     * Executes this command.
     *
     * @param args the arguments passed to this command
     */
    public void execute(Message msg, List<String> args) {
        GECko.logger.warn("[Command] Command called without overriding execute method.");
        Thread.dumpStack();
    }

    /**
     * Sends a short message on how to use this command.
     *
     * @param msg The message which triggered this command.
     * @return A Mono of the sent message.
     */
    public Mono<Message> printUsage(Message msg) {
        return CommandUtils.respond(msg, "**Usage:** `!" + this.getNames()[0] + " " + this.getParams() + "`");
    }
}
