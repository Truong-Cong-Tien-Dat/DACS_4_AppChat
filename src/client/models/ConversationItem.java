package client.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ConversationItem {
    private String username;
    private String fullName;
    private String avatarUrl;
    private final BooleanProperty hasUnreadMessages = new SimpleBooleanProperty(false);

    // Constructor nhận đủ 3 tham số: username, fullName, avatarUrl
    public ConversationItem(String username, String fullName, String avatarUrl) {
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() {
        return username;
    }

    // Hàm quan trọng: Nếu có fullName thì trả về fullName, nếu không thì trả về username
    public String getFullName() {
        return (fullName != null && !fullName.isEmpty()) ? fullName : username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    // --- Các hàm hỗ trợ Property cho JavaFX (để xử lý trạng thái chưa đọc) ---
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
        return getFullName();
    }
}