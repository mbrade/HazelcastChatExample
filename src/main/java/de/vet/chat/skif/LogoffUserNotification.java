package de.vet.chat.skif;

public class LogoffUserNotification extends Notification {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String userName;

    public LogoffUserNotification(final String userName) {
        super(NotificationType.USER_LEFT);
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

}
