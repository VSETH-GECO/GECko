package ch.ethz.geco.gecko.ticket.impl;

import discord4j.common.util.Snowflake;

public class ProtectionTicket extends BaseTicket {
    public ProtectionTicket(Snowflake issuer) {
        super(issuer);

        this.questions.add("Please provide the rough X/Z coordinates of your building / property:");
        this.questions.add("Please mark the corners of the area with some blocks and state the block type here:");
        this.questions.add("List all Minecraft usernames of players, which should be allowed to build (separated by commas or spaces):");
        this.questions.add("Should PvP be allowed?");
        this.questions.add("Any comments?");

        this.name = "Protect a building / property";
        this.description = "Prevent other players from griefing your buildings or stealing your stuff.";
        this.emoji = "\uD83C\uDFD7";
    }
}
