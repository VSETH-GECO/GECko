package ch.ethz.geco.gecko;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.UserUpdateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * This class is used to track important user behaviour so that we have
 * a proof of what happened in case there is a conflict.
 */
class EventLogger {
    private static final Logger logger = (Logger) LoggerFactory.getLogger("EventLogger");
    private static final RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();

    /**
     * Attaches the event logger to the given {@link EventDispatcher}, such that the logger can listen to the events of interest.
     *
     * @param dispatcher The {@link EventDispatcher} to attach to.
     */
    static void attachTo(EventDispatcher dispatcher) {
        // Setup logger
        LoggerContext contextBase = new LoggerContext();
        contextBase.start();

        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        SizeAndTimeBasedFNATP<ILoggingEvent> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<>();
        sizeAndTimeBasedFNATP.setContext(contextBase);
        sizeAndTimeBasedFNATP.setTimeBasedRollingPolicy(rollingPolicy);
        sizeAndTimeBasedFNATP.setMaxFileSize(FileSize.valueOf("20MB"));

        rollingPolicy.setContext(contextBase);
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern("data/log/events_%d{yy-MM-dd}.%i.log");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(sizeAndTimeBasedFNATP);

        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setContext(contextBase);
        patternLayoutEncoder.setParent(appender);
        patternLayoutEncoder.setCharset(StandardCharsets.UTF_8);
        patternLayoutEncoder.setPattern("%date{dd/MM/yy HH:mm:ss} | %msg%n");

        appender.setFile("data/log/events.log");
        appender.setRollingPolicy(rollingPolicy);
        appender.setEncoder(patternLayoutEncoder);
        appender.setContext(contextBase);
        appender.setImmediateFlush(true);

        rollingPolicy.start();
        sizeAndTimeBasedFNATP.start();
        patternLayoutEncoder.start();
        appender.start();

        // Message events
        dispatcher.on(MessageCreateEvent.class).subscribe(EventLogger::handleMessageCreate);
        dispatcher.on(MessageDeleteEvent.class).subscribe(EventLogger::handleMessageDelete);
        dispatcher.on(MessageUpdateEvent.class).subscribe(EventLogger::handleMessageUpdate);

        // User events
        dispatcher.on(UserUpdateEvent.class).subscribe(EventLogger::handleUserUpdate);
        dispatcher.on(MemberUpdateEvent.class).subscribe(EventLogger::handleMemberUpdate);
        dispatcher.on(MemberJoinEvent.class).subscribe(EventLogger::handleMemberJoin);
        dispatcher.on(MemberLeaveEvent.class).subscribe(EventLogger::handleMemberLeave);
    }

    static void close() {
        appender.stop();
        appender.getEncoder().stop();
        ((TimeBasedRollingPolicy) appender.getRollingPolicy()).getTimeBasedFileNamingAndTriggeringPolicy().stop();
        appender.getRollingPolicy().stop();
    }

    private static void handleMessageCreate(MessageCreateEvent event) {
        if (event.getMessage().getAuthor().isEmpty())
            return;

        String msg = "MSG_CREATE | MSG_ID: " +
                event.getMessage().getId().asString() +
                " | " +
                "USER_ID: " +
                event.getMessage().getAuthor().get().getId().asString() +
                " | " +
                event.getMessage().getContent().orElse("-");

        log(msg);
    }

    private static void handleMessageDelete(MessageDeleteEvent event) {
        String msg = "MSG_DELETE | MSG_ID: " +
                event.getMessageId().asString();

        log(msg);
    }

    private static void handleMessageUpdate(MessageUpdateEvent event) {
        String msg = "MSG_UPDATE | MSG_ID: " +
                event.getMessageId().asString() +
                " | ";

        Message newMessage = event.getMessage().block();

        if (newMessage == null)
            return;

        if (event.getOld().isPresent()) {
            msg += event.getOld().get().getContent().orElse("-") +
                    "\n    -> " + newMessage.getContent().orElse("-");

        } else {
            msg += event.getCurrentContent().orElse("-");
        }

        log(msg);
    }

    private static void handleUserUpdate(UserUpdateEvent event) {
        if (event.getOld().isEmpty()) // We can only track updates if there is an old user
            return;

        User oldUser = event.getOld().get();
        User newUser = event.getCurrent();

        if (!oldUser.getUsername().equals(newUser.getUsername()) || !oldUser.getDiscriminator().equals(newUser.getDiscriminator())) {
            String msg = "USER_UPDATE | USER_ID: " +
                    newUser.getId().asString() +
                    " | " +
                    oldUser.getUsername() + "#" + oldUser.getDiscriminator() + " -> " +
                    newUser.getUsername() + "#" + newUser.getDiscriminator();

            log(msg);
        }
    }

    private static void handleMemberUpdate(MemberUpdateEvent event) {
        if (event.getOld().isEmpty())
            return;

        // FIXME: undesired behaviour if nickname is actually "N/A"
        String oldNick = event.getOld().get().getNickname().orElse("N/A");
        String newNick = event.getCurrentNickname().orElse("N/A");

        if (!oldNick.equals(newNick)) {
            String msg = "MEMBER_UPDATE | USER_ID: " +
                    event.getMemberId().asString() +
                    " | " +
                    oldNick +
                    " -> " +
                    newNick;

            log(msg);
        }
    }

    private static void handleMemberJoin(MemberJoinEvent event) {
        Member member = event.getMember();

        String msg = "MEMBER_JOIN | USER_ID: " +
                member.getId() +
                " | " +
                member.getDisplayName() + "#" + member.getDiscriminator() +
                " joined the server!";

        log(msg);
    }

    private static void handleMemberLeave(MemberLeaveEvent event) {
        User member = event.getUser();

        String msg = "MEMBER_LEAVE | USER_ID: " +
                member.getId() +
                " | " +
                member.getUsername() + "#" + member.getDiscriminator() +
                " left the server!";

        log(msg);
    }

    private static void log(String msg) {
        GECkO.logger.debug(msg);
        appender.doAppend(new LoggingEvent("", logger, Level.INFO, msg, null, null));
    }
}
