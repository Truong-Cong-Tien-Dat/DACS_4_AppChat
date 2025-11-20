package common.models;

public class ChatMessage {
    public enum Type { TEXT, IMAGE, FILE }

    private String sender;
    private String content; // Nội dung text hoặc tên file ảnh
    private boolean isFromMe;
    private Type type; // (MỚI) Loại tin nhắn

    public ChatMessage(String sender, String content, boolean isFromMe, Type type) {
        this.sender = sender;
        this.content = content;
        this.isFromMe = isFromMe;
        this.type = type;
    }

    // Constructor cũ (mặc định là TEXT)
    public ChatMessage(String sender, String content, boolean isFromMe) {
        this(sender, content, isFromMe, Type.TEXT);
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public boolean isFromMe() { return isFromMe; }
    public Type getType() { return type; }
}