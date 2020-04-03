package ch.ethz.geco.gecko.ticket.impl;

import ch.ethz.geco.gecko.ticket.Ticket;
import discord4j.core.object.util.Snowflake;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTicket implements Ticket {
    private final Snowflake issuer;
    protected List<String> questions = new ArrayList<>();
    protected String name;
    protected String emoji;
    protected String description;
    private List<String> answers = new ArrayList<>();
    private State state = State.INIT;

    protected BaseTicket(Snowflake issuer) {
        this.issuer = issuer;
    }

    @Override
    public Snowflake getIssuer() {
        return issuer;
    }

    @Override
    public List<String> getAnswers() {
        return answers;
    }

    @Override
    public List<String> getQuestions() {
        return questions;
    }

    @Override
    public String nextQuestion() {
        return questions.size() > answers.size() ? questions.get(answers.size()) : null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getEmoji() {
        return emoji;
    }
}
