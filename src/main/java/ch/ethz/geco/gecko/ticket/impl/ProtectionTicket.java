package ch.ethz.geco.gecko.ticket.impl;

import discord4j.core.object.util.Snowflake;

public class ProtectionTicket extends BaseTicket {
    public ProtectionTicket(Snowflake issuer) {
        super(issuer);

        this.questions.add("Please provide the X/Z coordinates of your building / property:");
        this.questions.add("List all Minecraft usernames of players, which should be allowed to build:");
        this.questions.add("Should PvP be allowed?");
        this.questions.add("Any comments?");

        this.name = "Protect a building / property";
        this.description = "Prevent other players from griefing your buildings or stealing your stuff.";
        this.emoji = "\uD83C\uDFD7";
    }
}
