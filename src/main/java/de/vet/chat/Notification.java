package de.vet.chat;

import java.io.Serializable;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;
    private final NotificationType type;

    public Notification(final NotificationType type) {
        this.type = type;
    }

    public NotificationType getType() {
        return type;
    }

    public enum NotificationType {
        NEW_CHAT,
        NEW_USER,
        USER_LEFT,
        MESSAGE;
    }
}
