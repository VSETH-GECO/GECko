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

package ch.ethz.geco.gecko.command.audio;

import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.List;

public class Join extends Command {
    public Join() {
        this.setName("join");
        this.setParams("[channel]");
        this.setDescription("Joins a given voice channel.");
        this.setRemoveAfterCall(true);
    }

    @Override
    public void execute(IMessage msg, List<String> args) {
        List<IVoiceChannel> voiceChannels = msg.getGuild().getVoiceChannels();

        try {
            if (voiceChannels.size() == 1) {    // If there is only one voice channel, join this one
                voiceChannels.get(0).join();
            } else {
                if (args.size() >= 1) {
                    List<IVoiceChannel> voiceChannelsByName = msg.getGuild().getVoiceChannelsByName(args.get(0));
                    if (voiceChannelsByName.size() == 1) {
                        voiceChannelsByName.get(0).join();
                    } else if (voiceChannelsByName.size() == 0) {
                        CommandUtils.respond(msg, "There is no voice channel: **" + args.get(0) + "**");
                    } else {
                        CommandUtils.respond(msg, "There are multiple voice channels with the same name.\n Please rename one of them.");
                    }
                } else {
                    IVoiceChannel connectedVoiceChannel = null;
                    for (IVoiceChannel voiceChannel : voiceChannels) {
                        if (voiceChannel.getConnectedUsers().contains(msg.getAuthor())) {
                            connectedVoiceChannel = voiceChannel;
                            break;
                        }
                    }

                    if (connectedVoiceChannel != null) {
                        connectedVoiceChannel.join();
                    } else {
                        CommandUtils.respond(msg, "You are not in a voice channel.");
                        this.printUsage(msg);
                    }
                }
            }
        } catch (MissingPermissionsException e) {
            CommandUtils.respond(msg, "I'm not allowed to join this channel");
        }
    }
}