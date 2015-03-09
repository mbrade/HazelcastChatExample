package de.vet.chat;

public class NewUserNotification extends Notification {

    private static final long serialVersionUID = 1L;
    private final String userName;

    public NewUserNotification(final String userName) {
        super(NotificationType.NEW_USER);
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

}
