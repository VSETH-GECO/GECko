package ch.ethz.geco.gecko.voice.command;

import ch.ethz.geco.gecko.GECko;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.voice.VoiceChannelSpawner;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VCSpawner extends Command {
    public static Pattern channelIDPattern = Pattern.compile("^(?><#(\\d+)>|(\\d+))$");

    public VCSpawner() {
        this.setName("vcspawner");
        this.setParams("<add | remove | clear> [channelID | #channel]");
        this.setDescription("Manages the voice channel spawners.");
        this.getPermissions().addPermittedRoleID(Snowflake.of(248454555438678017L));
        this.getPermissions().addPermittedRoleID(Snowflake.of(687777083044134919L));
    }

    @Override
    public void execute(Message msg, List<String> args) {
        if (args.isEmpty()) {
            printUsage(msg).subscribe();
            return;
        }

        Snowflake channelID;
        Matcher matcher;
        switch (args.get(0)) {
            case "add":
                if (args.size() <= 1) {
                    printUsage(msg).subscribe();
                    return;
                }

                matcher = channelIDPattern.matcher(args.get(1));
                if (matcher.find()) {
                    if (matcher.group(1) == null) {
                        channelID = Snowflake.of(Long.parseLong(matcher.group(2)));
                    } else {
                        channelID = Snowflake.of(Long.parseLong(matcher.group(1)));
                    }
                } else {
                    printUsage(msg).subscribe();
                    return;
                }

                GECko.discordClient.getChannelById(channelID).subscribe(channel -> {
                    if (channel.getType().equals(Channel.Type.GUILD_TEXT)) {
                        VoiceChannelSpawner.createSpawner((TextChannel) channel);
                    } else {
                        CommandUtils.respond(msg, "The given channel is not a text channel.");
                    }
                });
                break;
            case "remove":
                if (args.size() <= 1) {
                    printUsage(msg).subscribe();
                    return;
                }

                matcher = channelIDPattern.matcher(args.get(1));
                if (matcher.find()) {
                    if (matcher.group(1) == null) {
                        channelID = Snowflake.of(Long.parseLong(matcher.group(2)));
                    } else {
                        channelID = Snowflake.of(Long.parseLong(matcher.group(1)));
                    }
                } else {
                    printUsage(msg).subscribe();
                    return;
                }

                VoiceChannelSpawner.removeSpawner(channelID);
                break;
            case "clear":
                VoiceChannelSpawner.clearSpawners();
                break;
            default:
                printUsage(msg).subscribe();
                break;
        }
    }
}
