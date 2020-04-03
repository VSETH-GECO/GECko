package ch.ethz.geco.gecko.ticket.impl;

import discord4j.core.object.util.Snowflake;

public class TestTicket extends BaseTicket {
    public TestTicket(Snowflake issuer) {
        super(issuer);

        this.questions.add("What is your name?");
        this.questions.add("Where did it happen?");
        this.questions.add("Soos?");

        this.name = "A test ticket";
        this.description = "This is the description of the test ticket.";
        this.emoji = "\uD83C\uDFD7";
    }
}
