package de.vet.chat;

public class NewChatNotification extends Notification {

    private static final long serialVersionUID = 1L;
    private final String from;
    private final String to;
    private final String channelName;

    public NewChatNotification(final String from, final String to, final String channelName) {
        super(NotificationType.NEW_CHAT);
        this.from = from;
        this.to = to;
        this.channelName = channelName;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getChannelName() {
        return channelName;
    }

}
