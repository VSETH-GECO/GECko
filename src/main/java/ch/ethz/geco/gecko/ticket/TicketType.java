package ch.ethz.geco.gecko.ticket;

public class TicketType {
    private final Class<? extends Ticket> clazz;
    private final String name;
    private final String description;
    private final String emoji;

    public TicketType(Class<? extends Ticket> clazz, String name, String description, String emoji) {
        this.clazz = clazz;
        this.name = name;
        this.description = description;
        this.emoji = emoji;
    }

    public String getName() {
        return name;
    }

    public Class<? extends Ticket> getTypeClass() {
        return clazz;
    }

    public String getDescription() {
        return description;
    }

    public String getEmoji() {
        return emoji;
    }
}
