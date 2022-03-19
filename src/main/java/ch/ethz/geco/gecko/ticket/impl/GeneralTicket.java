package ch.ethz.geco.gecko.ticket.impl;

import discord4j.common.util.Snowflake;

public class GeneralTicket extends BaseTicket {
    public GeneralTicket(Snowflake issuer) {
        super(issuer);

        this.questions.add("Please provide your Minecraft username:");
        this.questions.add("How can we help you?");

        this.name = "General request";
        this.description = "Submit a general question about the server or mechanics.";
        this.emoji = "❓";
    }
}
