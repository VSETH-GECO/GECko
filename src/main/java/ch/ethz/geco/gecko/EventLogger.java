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
import discord4j.core.object.entity.*;
import org.jetbrains.annotations.Nullable;
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
        rollingPolicy.setFileNamePattern("data/log/events_%d{yyyy-ww}.%i.log");
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

    @Nullable
    private static String getMessageTrace(Message message) {
        if (message.getAuthor().isEmpty() || message.getAuthor().get().isBot())
            return null;

        GuildMessageChannel channel = (GuildMessageChannel) message.getChannel().block();

        if (channel == null)
            return null;

        String msg = "";

        if (channel.getCategoryId().isPresent()) {
            Category category = channel.getCategory().block();

            if (category == null)
                return null;

            msg += category.getName() + " > ";
        }

        msg += "#" + channel.getName() + " > " +
                message.getAuthor().get().getUsername() + ": " +
                message.getContent().orElse("");

        return msg;
    }

    private static void handleMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();

        if (message.getAuthor().isEmpty() || message.getAuthor().get().isBot())
            return;

        String messageTrace = getMessageTrace(message);

        if (messageTrace == null)
            return;

        String msg = "MSG_CREATE | MSG_ID: " +
                message.getId().asString() +
                " | " +
                "CHAN_ID: " +
                message.getChannelId().asString() +
                " | " +
                "USER_ID: " +
                message.getAuthor().get().getId().asString() +
                "\n    " + messageTrace;

        log(msg);
    }

    private static void handleMessageDelete(MessageDeleteEvent event) {
        String msg = "";

        if (event.getMessage().isEmpty()) {
            msg += "MSG_DELETE | MSG_ID: " +
                    event.getMessageId().asString() +
                    " | " +
                    "CHAN_ID: " +
                    event.getChannelId().asString();
        } else {
            Message message = event.getMessage().get();

            if (message.getAuthor().isEmpty() || message.getAuthor().get().isBot())
                return;

            String messageTrace = getMessageTrace(message);

            if (messageTrace == null)
                return;

            msg += "MSG_DELETE | MSG_ID: " +
                    message.getId().asString() +
                    " | " +
                    "CHAN_ID: " +
                    message.getChannelId().asString() +
                    " | " +
                    "USER_ID: " +
                    message.getAuthor().get().getId().asString() +
                    "\n    " + messageTrace;
        }

        log(msg);
    }

    private static void handleMessageUpdate(MessageUpdateEvent event) {
        if (!event.isContentChanged()) // Do not track updates without changes
            return;

        Message message = event.getMessage().block();

        if (message == null)
            return;

        if (message.getAuthor().isEmpty() || message.getAuthor().get().isBot())
            return;

        String messageTrace = getMessageTrace(message);

        if (messageTrace == null)
            return;

        String msg = "MSG_UPDATE | MSG_ID: " +
                message.getId().asString() +
                " | " +
                "CHAN_ID: " +
                message.getChannelId().asString() +
                " | " +
                "USER_ID: " +
                message.getAuthor().get().getId().asString() +
                "\n    ";

        if (event.getOld().isPresent()) {
            String oldTrace = getMessageTrace(event.getOld().get());

            if (oldTrace == null)
                return;

            msg += oldTrace +
                    "\n  → " +
                    messageTrace;
        } else {
            msg += messageTrace;
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

        Member member = event.getMember().block();
        String oldNick = event.getOld().get().getNickname().orElse(member != null ? member.getDisplayName() : "NULL");
        String newNick = event.getCurrentNickname().orElse(member != null ? member.getDisplayName() : "NULL");

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
                member.getId().asString() +
                " | " +
                member.getDisplayName() + "#" + member.getDiscriminator() +
                " joined the server!";

        log(msg);
    }

    private static void handleMemberLeave(MemberLeaveEvent event) {
        User member = event.getUser();

        String msg = "MEMBER_LEFT | USER_ID: " +
                member.getId().asString() +
                " | " +
                member.getUsername() + "#" + member.getDiscriminator() +
                " left the server!";

        log(msg);
    }

    private static void log(String msg) {
        appender.doAppend(new LoggingEvent("", logger, Level.INFO, msg, null, null));
    }
}
