package common.models;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    // Các loại tin nhắn hỗ trợ
    public enum Type { TEXT, IMAGE, FILE, VOICE }

    private int id; // <--- (MỚI) ID dùng để xóa tin nhắn
    private String sender;
    private String content;
    private boolean isMe;
    private Type type;
    private String fileName;

    // Constructor cơ bản
    public ChatMessage(String sender, String content, boolean isMe, Type type) {
        this.sender = sender;
        this.content = content;
        this.isMe = isMe;
        this.type = type;
        this.id = -1; // Mặc định chưa có ID
    }

    // --- GETTER & SETTER CHO ID (MỚI) ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    // ------------------------------------

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isMe() { return isMe; }
    public void setMe(boolean me) { isMe = me; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}