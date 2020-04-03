package ch.ethz.geco.gecko.ticket.impl;

import discord4j.core.object.util.Snowflake;

public class TestTicket2 extends BaseTicket {
    public TestTicket2(Snowflake issuer) {
        super(issuer);

        this.questions.add("What is your name?");
        this.questions.add("Where did it happen?");
        this.questions.add("Soos?");

        this.name = "A second test ticket";
        this.description = "This is the description of the second test ticket.";
        this.emoji = "\uD83D\uDEA7";
    }
}