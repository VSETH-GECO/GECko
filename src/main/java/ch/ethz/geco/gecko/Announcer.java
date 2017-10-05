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
 * For more information, please refer to <http://unlicense.org/>
 */

package ch.ethz.geco.gecko;

import ch.ethz.geco.gecko.command.CommandHandler;
import ch.ethz.geco.gecko.rest.api.AcapelaAPI;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class Announcer implements IListener<MessageReceivedEvent> {
    private static final IChannel listeningChannel = GECkO.mainChannel;

    @Override
    public void handle(MessageReceivedEvent messageReceivedEvent) {
        IMessage message = messageReceivedEvent.getMessage();
        IUser author = message.getAuthor();

        // If there is a new message in the listening channel
        String content = message.getContent();
        if (GECkO.discordClient.getConnectedVoiceChannels().size() > 0 && message.getChannel().equals(listeningChannel) && !author.isBot() && !content.startsWith(CommandHandler.getDefaultPrefix())) {
            if (content.length() <= AcapelaAPI.MAX_CHARS) {
                AudioInputStream inputStream = null;
                try {
                    inputStream = AcapelaAPI.getSoundSamples(new URL(AcapelaAPI.getSoundURL(content)));
                    AudioFormat format = inputStream.getFormat();
                    byte[] cleanSamples = AcapelaAPI.removeNoise(AcapelaAPI.subtractBackground(AcapelaAPI.audioStreamToByteArray(inputStream), format), format);
                    long sampleCount = cleanSamples.length / format.getFrameSize();
                    AudioPlayer.getAudioPlayerForGuild(GECkO.mainChannel.getGuild()).queue(new AudioInputStream(new ByteArrayInputStream(cleanSamples), format, sampleCount));
                } catch (MalformedURLException e) {
                    ErrorHandler.handleError(e);
                }
            } else {
                message.delete();
                author.getOrCreatePMChannel().sendMessage("Your announcement was removed because it exceeded the character limit of announcements. Acapela only supports up to 300 characters.");
            }
        }
    }
}
