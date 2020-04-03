package ch.ethz.geco.gecko.ticket.command;

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.GECko;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.ticket.TicketManager;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketChannel extends Command {
    public static Pattern channelIDPattern = Pattern.compile("^(?><#(\\d+)>|(\\d+))$");

    public TicketChannel() {
        this.setName("ticketchannel");
        this.setParams("<channelID | #channel>");
        this.setDescription("Sets the ticket channel.");
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
        Matcher matcher = channelIDPattern.matcher(args.get(0));
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
                TicketManager.setTicketChannel(channelID);
                ConfigManager.setProperty("ticket_channel", channelID.asString());
                ConfigManager.saveConfig();

                CommandUtils.respond(msg, "The ticket channel was set to <#" + channelID.asLong() + ">");
            } else {
                CommandUtils.respond(msg, "The given channel is not a text channel.");
            }
        });
    }
}
