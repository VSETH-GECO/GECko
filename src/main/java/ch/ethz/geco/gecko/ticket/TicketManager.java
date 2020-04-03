package ch.ethz.geco.gecko.ticket;

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.command.CommandHandler;
import ch.ethz.geco.gecko.command.CommandRegistry;
import ch.ethz.geco.gecko.ticket.command.TicketChannel;
import ch.ethz.geco.gecko.ticket.command.TicketSpawner;
import ch.ethz.geco.gecko.ticket.impl.GeneralTicket;
import ch.ethz.geco.gecko.ticket.impl.ProtectionTicket;
import ch.ethz.geco.gecko.ticket.impl.ReportTicket;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ch.ethz.geco.gecko.GECko.discordClient;

public class TicketManager {
    /**
     * A map of all currently open tickets and the private channel they are in.
     */
    private static final Map<Snowflake, Ticket> tickets = new HashMap<>();
    /**
     * A list of all available ticket types.
     */
    private static final List<TicketType> ticketTypes = new ArrayList<>();
    /**
     * A list of all message IDs containing a ticket spawner.
     */
    private static Snowflake ticketSpawner;
    /**
     * The ticket channel where all opened tickets are posted.
     */
    private static Snowflake ticketChannel;

    /**
     * After how many seconds uncreated tickets should expire.
     */
    private static final int TICKET_EXPIRE_SECONDS = 300;

    /**
     * The thread pool handling ticket expirations.
     */
    private static final ScheduledExecutorService executer = Executors.newScheduledThreadPool(2);

    static {
        List<Class<? extends Ticket>> ticketClasses = new ArrayList<>();

        // Add new ticket types here
        ticketClasses.add(ProtectionTicket.class);
        ticketClasses.add(ReportTicket.class);
        ticketClasses.add(GeneralTicket.class);

        for (Class<? extends Ticket> clazz : ticketClasses) {
            try {
                Ticket tmp = clazz.getConstructor(Snowflake.class).newInstance(Snowflake.of(0));
                ticketTypes.add(new TicketType(clazz, tmp.getName(), tmp.getDescription(), tmp.getEmoji()));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initializes the ticket manager, loading configurations and cleaning up.
     * This should be called once on startup.
     */
    public static void init() {
        // Add ticket commands
        CommandRegistry.registerCommand(new TicketSpawner());
        CommandRegistry.registerCommand(new TicketChannel());

        if (ConfigManager.getProperty("ticket_channel") != null) {
            ticketChannel = Snowflake.of(ConfigManager.getProperty("ticket_channel"));
        }

        if (ConfigManager.getProperty("ticket_spawnerMessage") != null && ConfigManager.getProperty("ticket_spawnerChannel") != null) {
            ticketSpawner = Snowflake.of(ConfigManager.getProperty("ticket_spawnerMessage"));
            discordClient.getMessageById(Snowflake.of(ConfigManager.getProperty("ticket_spawnerChannel")), ticketSpawner).subscribe(message -> {
                message.removeAllReactions().block();

                for (TicketType type : ticketTypes) {
                    message.addReaction(ReactionEmoji.unicode(type.getEmoji())).block();
                }
            });
        }

        // Listen to ticket related messages
        discordClient.getEventDispatcher().on(MessageCreateEvent.class).subscribe(TicketManager::handleMessage);
        discordClient.getEventDispatcher().on(ReactionAddEvent.class).subscribe(TicketManager::handleReact);
    }

    public static void setTicketChannel(Snowflake ticketChannel) {
        TicketManager.ticketChannel = ticketChannel;
    }

    public static void createSpawner(TextChannel channel) {
        channel.createEmbed(spec -> {
            spec.setTitle("**How can I help you?**");

            StringBuilder description = new StringBuilder();
            for (TicketType type : ticketTypes) {
                description.append(type.getEmoji());
                description.append(" : ");
                description.append(type.getName());
                description.append("\n");
                description.append(type.getDescription());
                description.append("\n\n");
            }

            spec.setDescription(description.toString());

            spec.setFooter("React to this message to choose an option", null);
        }).subscribe(message -> {
            for (TicketType type : ticketTypes) {
                message.addReaction(ReactionEmoji.unicode(type.getEmoji())).block();
            }

            ticketSpawner = message.getId();
            ConfigManager.setProperty("ticket_spawnerMessage", message.getId().asString());
            ConfigManager.setProperty("ticket_spawnerChannel", message.getChannelId().asString());
            ConfigManager.saveConfig();
        });
    }

    public static void handleReact(ReactionAddEvent reactEvent) {
        if (discordClient.getSelfId().isPresent() && !reactEvent.getUserId().equals(discordClient.getSelfId().get()) && ticketSpawner.equals(reactEvent.getMessageId())) {
            if (reactEvent.getEmoji().asUnicodeEmoji().isPresent()) {
                String reactionEmoji = reactEvent.getEmoji().asUnicodeEmoji().get().getRaw();

                ticketTypes.stream().filter(ticketType -> ticketType.getEmoji().equals(reactionEmoji)).forEach(ticketType -> {
                    reactEvent.getUser().flatMap(User::getPrivateChannel).subscribe(channel -> {
                        if (!tickets.containsKey(channel.getId())) {
                            channel.createEmbed(spec -> {
                                try {
                                    Ticket ticket = ticketType.getTypeClass().getConstructor(Snowflake.class).newInstance(reactEvent.getUserId());

                                    spec.setTitle("**" + ticket.getName() + "**");
                                    spec.setDescription("Before I can create a ticket for you, I need some additional information.\n\n" +
                                            ticket.nextQuestion());
                                    spec.setFooter("Write: " + CommandHandler.getDefaultPrefix() + "cancel to cancel the ticket creation.", null);

                                    tickets.put(channel.getId(), ticket);

                                    // Expire uncreated tickets after some timeout
                                    executer.schedule(() -> {
                                        if (tickets.containsKey(channel.getId())) {
                                            tickets.remove(channel.getId());

                                            channel.createEmbed(spec2 -> {
                                                spec2.setTitle("**" + ticket.getName() + "**");
                                                spec2.setDescription("❎ Your ticket creation has expired.");
                                                spec2.setFooter("~ Have Fun!", null);
                                            }).subscribe();
                                        }
                                    }, TICKET_EXPIRE_SECONDS, TimeUnit.SECONDS);
                                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    e.printStackTrace();
                                }
                            }).subscribe();
                        }
                    });
                });
            }

            reactEvent.getMessage().flatMap(message -> message.removeReaction(reactEvent.getEmoji(), reactEvent.getUserId())).subscribe();
        }
    }

    public static void handleMessage(MessageCreateEvent messageEvent) {
        if (discordClient.getSelfId().isPresent() &&
                messageEvent.getMessage().getAuthor().isPresent() &&
                !messageEvent.getMessage().getAuthor().get().isBot() &&
                tickets.containsKey(messageEvent.getMessage().getChannelId())) {
            Ticket ticket = tickets.get(messageEvent.getMessage().getChannelId());
            if (messageEvent.getMessage().getContent().isPresent()) {
                if (messageEvent.getMessage().getContent().get().strip().equals(CommandHandler.getDefaultPrefix() + "cancel")) {
                    tickets.remove(messageEvent.getMessage().getChannelId());

                    messageEvent.getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
                        spec.setTitle("**" + ticket.getName() + "**");
                        spec.setDescription("❎ Your ticket was canceled.");
                        spec.setFooter("~ Have Fun!", null);
                    })).subscribe();

                    return;
                }

                ticket.getAnswers().add(messageEvent.getMessage().getContent().get());

                if (ticket.nextQuestion() != null) {
                    messageEvent.getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
                        spec.setTitle("**" + ticket.getName() + "**");
                        spec.setDescription(ticket.nextQuestion());
                        spec.setFooter("Write: " + CommandHandler.getDefaultPrefix() + "cancel to cancel the ticket creation.", null);
                    })).subscribe();
                } else {
                    tickets.remove(messageEvent.getMessage().getChannelId());

                    messageEvent.getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
                        spec.setTitle("**" + ticket.getName() + "**");
                        spec.setDescription("✅ Your ticket was successfully created and the next available admin will process it soon.");
                        spec.setFooter("~ Have Fun!", null);
                    })).subscribe();

                    discordClient.getChannelById(ticketChannel).cast(MessageChannel.class).flatMap(channel -> channel.createMessage(spec -> {
                        spec.setContent("Issuer: <@" + ticket.getIssuer().asLong() + ">");
                        spec.setEmbed(embedSpec -> {
                            embedSpec.setTitle("**" + ticket.getName() + "**");

                            StringBuilder content = new StringBuilder();
                            for (int i = 0; i < ticket.getQuestions().size(); i++) {
                                content.append("\n\n__");
                                content.append(ticket.getQuestions().get(i));
                                content.append("__\n");
                                content.append(ticket.getAnswers().get(i));
                            }
                            embedSpec.setDescription(content.toString());
                        });
                    })).subscribe();
                }
            } else {
                messageEvent.getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
                    spec.setTitle("**" + ticket.getName() + "**");
                    spec.setDescription("Please answer this question:\n\n" + ticket.nextQuestion());
                })).subscribe();
            }
        }
    }
}
