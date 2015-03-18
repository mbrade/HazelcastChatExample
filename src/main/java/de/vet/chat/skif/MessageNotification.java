package de.vet.chat.skif;

public class MessageNotification extends Notification {

    private static final long serialVersionUID = 1L;
    private final String from;
    private final String message;

    public MessageNotification(final String from, final String message) {
        super(NotificationType.MESSAGE);
        this.from = from;
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public String getMessage() {
        return message;
    }

}
