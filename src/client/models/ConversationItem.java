package client.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ConversationItem {
    private String username;
    private String avatarUrl; // Tên file ảnh (vd: photo1.jpg)

    private final BooleanProperty hasUnreadMessages = new SimpleBooleanProperty(false);

    public ConversationItem(String username, String avatarUrl) {
        this.username = username;
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    // --- Các hàm hỗ trợ Property (JavaFX) ---
    public BooleanProperty hasUnreadMessagesProperty() {
        return hasUnreadMessages;
    }

    public boolean isHasUnreadMessages() {
        return hasUnreadMessages.get();
    }

    public void setHasUnreadMessages(boolean hasUnreadMessages) {
        this.hasUnreadMessages.set(hasUnreadMessages);
    }

    @Override
    public String toString() {
        return username;
    }
}