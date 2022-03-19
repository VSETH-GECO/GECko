package ch.ethz.geco.gecko.ticket.impl;

import discord4j.common.util.Snowflake;

public class ReportTicket extends BaseTicket {
    public ReportTicket(Snowflake issuer) {
        super(issuer);

        this.questions.add("Please provide the Minecraft username of the player, you want to report:");
        this.questions.add("Why do you want to report the player? (Cheating, offensive behaviour, griefing etc.)");

        this.name = "Report a player";
        this.description = "Report a player for offensive behaviour, cheating etc.";
        this.emoji = "\uD83D\uDC80";
    }
}
