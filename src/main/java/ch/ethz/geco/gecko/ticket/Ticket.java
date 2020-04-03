package ch.ethz.geco.gecko.ticket;

import discord4j.core.object.util.Snowflake;

import java.util.List;

public interface Ticket {
    String getName();

    String getDescription();

    Snowflake getIssuer();

    List<String> getQuestions();

    List<String> getAnswers();

    String nextQuestion();

    String getEmoji();

    enum State {INIT, OPEN, CLOSED}
}
