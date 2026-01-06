package client.controllers;

import client.ClientApp;
import client.gui.ChatCellRenderer;
import client.models.ConversationItem;
import common.models.ChatMessage;
import common.utils.AudioRecorder;
import common.utils.FileUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessagesController {

    // --- CÁC BIẾN FXML ---
    @FXML private TextField searchField;
    @FXML private ListView<ConversationItem> userListView;
    @FXML private Label currentPartnerLabel;
    @FXML private ListView<ChatMessage> chatListView;
    @FXML private VBox chatArea;
    @FXML private VBox placeholderArea;
    @FXML private VBox imageOverlay;
    @FXML private ImageView enlargedImageView;
    @FXML private ImageView partnerAvatarView;
    @FXML private Button btnEmoji;

    // --- UI GHI ÂM ---
    @FXML private HBox boxInputMessage;
    @FXML private HBox boxRecording;
    @FXML private TextField messageInputField;
    @FXML private Label lblRecordingTimer;
    @FXML private Button btnVoice;

    // --- BIẾN LOGIC ---
    private ObservableList<ConversationItem> conversationList = FXCollections.observableArrayList();
    private FilteredList<ConversationItem> filteredList;
    private Map<String, ObservableList<ChatMessage>> chatHistories = new HashMap<>();
    private String currentPartner;
    private static final PseudoClass UNREAD_PSEUDO_CLASS = PseudoClass.getPseudoClass("unread");

    // Xử lý ghi âm
    private AudioRecorder audioRecorder = new AudioRecorder();
    private File tempVoiceFile = new File("temp_voice_record.wav");
    private Timeline recordingTimer;
    private int secondsRecorded = 0;

    // --- HÀM KHỞI TẠO (INITIALIZE) ---
    public void initialize() {
        filteredList = new FilteredList<>(conversationList, p -> true);
        userListView.setItems(filteredList);

        setupListViewCellFactory();

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredList.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerVal = newValue.toLowerCase();
                return item.getUsername().toLowerCase().contains(lowerVal) ||
                        item.getFullName().toLowerCase().contains(lowerVal);
            });
        });

        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                openChat(newVal.getUsername());
                newVal.setHasUnreadMessages(false);
                userListView.refresh();
            }
        });

        chatListView.setCellFactory(param -> new ChatCellRenderer(this));

        loadMatches();

        // Reset UI ghi âm
        if(boxRecording != null) boxRecording.setVisible(false);
        if(boxInputMessage != null) boxInputMessage.setVisible(true);
    }

    // ==========================================================
    // KHU VỰC CÁC HÀM HỖ TRỢ UI
    // ==========================================================

    private void setupListViewCellFactory() {
        userListView.setCellFactory(lv -> new ListCell<ConversationItem>() {
            private final HBox rootBox = new HBox(10);
            private final StackPane avatarContainer = new StackPane();
            private final ImageView avatarImageView = new ImageView();
            private final Label nameLabel = new Label();

            {
                rootBox.setAlignment(Pos.CENTER_LEFT);
                rootBox.getStyleClass().add("conversation-list-cell");
                rootBox.setPadding(new Insets(8, 10, 8, 10));

                avatarImageView.setFitWidth(45);
                avatarImageView.setFitHeight(45);
                avatarImageView.setPreserveRatio(false);
                Circle clip = new Circle(22.5, 22.5, 22.5);
                avatarImageView.setClip(clip);

                avatarContainer.getChildren().add(avatarImageView);
                avatarContainer.getStyleClass().add("avatar-container");

                nameLabel.getStyleClass().add("username-label");
                nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

                rootBox.getChildren().addAll(avatarContainer, nameLabel);
            }

            @Override
            protected void updateItem(ConversationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    avatarContainer.pseudoClassStateChanged(UNREAD_PSEUDO_CLASS, false);
                } else {
                    nameLabel.setText(item.getFullName());
                    String p = (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) ? item.getAvatarUrl() : "default_avatar.png";
                    try {
                        File f = new File("images/" + p);
                        if(f.exists()) avatarImageView.setImage(new Image(f.toURI().toString()));
                        else avatarImageView.setImage(null);
                    } catch (Exception e) { avatarImageView.setImage(null); }

                    item.hasUnreadMessagesProperty().addListener((obs, wasUnread, isUnread) ->
                            avatarContainer.pseudoClassStateChanged(UNREAD_PSEUDO_CLASS, isUnread));
                    avatarContainer.pseudoClassStateChanged(UNREAD_PSEUDO_CLASS, item.isHasUnreadMessages());

                    setGraphic(rootBox);
                }
            }
        });
    }

    private void loadMatches() {
        JSONObject req = new JSONObject().put("action", "GET_MATCHES");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    private void openChat(String username) {
        this.currentPartner = username;
        String displayName = username;

        for (ConversationItem item : conversationList) {
            if (item.getUsername().equals(username)) {
                displayName = item.getFullName();
                break;
            }
        }
        currentPartnerLabel.setText(displayName);

        if (placeholderArea != null) placeholderArea.setVisible(false);
        if (chatArea != null) chatArea.setVisible(true);

        if (partnerAvatarView != null) {
            conversationList.stream().filter(i -> i.getUsername().equals(username)).findFirst().ifPresent(item -> {
                String p = (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) ? item.getAvatarUrl() : "default_avatar.png";
                try {
                    File f = new File("images/" + p);
                    if(f.exists()) {
                        partnerAvatarView.setImage(new Image(f.toURI().toString()));
                        Circle clip = new Circle(20, 20, 20);
                        partnerAvatarView.setClip(clip);
                    }
                } catch (Exception e) {}
            });
        }

        chatListView.setItems(getChatListForUser(username));

        JSONObject req = new JSONObject();
        req.put("action", "GET_CHAT_HISTORY");
        req.put("with_username", username);
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    // ==========================================================
    // KHU VỰC GHI ÂM
    // ==========================================================

    @FXML private void startRecordingUI() {
        boxInputMessage.setVisible(false);
        boxRecording.setVisible(true);
        audioRecorder.startRecording(tempVoiceFile.getAbsolutePath());

        secondsRecorded = 0;
        lblRecordingTimer.setText("0:00");
        if (recordingTimer != null) recordingTimer.stop();

        recordingTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRecorded++;
            int m = secondsRecorded / 60;
            int s = secondsRecorded % 60;
            lblRecordingTimer.setText(String.format("%d:%02d", m, s));
        }));
        recordingTimer.setCycleCount(Timeline.INDEFINITE);
        recordingTimer.play();
    }

    @FXML private void cancelRecording() {
        stopRecordingProcess();
        boxRecording.setVisible(false);
        boxInputMessage.setVisible(true);
    }

    @FXML private void finishAndSendRecording() {
        stopRecordingProcess();
        boxRecording.setVisible(false);
        boxInputMessage.setVisible(true);
        if (secondsRecorded >= 1) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(this::sendVoiceMessage);
                } catch (InterruptedException ex) { ex.printStackTrace(); }
            }).start();
        } else {
            showAlert("Quá ngắn", "Ghi âm quá ngắn, đã hủy.");
        }
    }

    private void stopRecordingProcess() {
        audioRecorder.stopRecording();
        if (recordingTimer != null) recordingTimer.stop();
    }

    private void sendVoiceMessage() {
        if (currentPartner == null || !tempVoiceFile.exists()) return;
        try {
            String base64Content = FileUtil.encodeFileToBase64(tempVoiceFile);
            String fileName = "voice_" + System.currentTimeMillis() + ".wav";

            JSONObject req = new JSONObject()
                    .put("action", "SEND_MESSAGE")
                    .put("to_username", currentPartner)
                    .put("content", base64Content)
                    .put("file_name", fileName)
                    .put("type", "VOICE");

            ClientApp.getInstance().getNetworkClient().sendRequest(req);
            // KHÔNG addMessageToHistory Ở ĐÂY NỮA
            // Đợi Server phản hồi NEW_MESSAGE có ID rồi mới hiện
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể gửi tin nhắn thoại.");
        }
    }

    // ==========================================================
    // KHU VỰC XỬ LÝ SERVER
    // ==========================================================

    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");
        Platform.runLater(() -> {
            if ("MATCH_LIST".equals(status)) {
                JSONArray arr = response.getJSONArray("matches");
                conversationList.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String u = obj.getString("username");
                    conversationList.add(new ConversationItem(u, obj.optString("full_name", u), obj.optString("photo1", "")));
                }
            }
            else if ("NEW_MESSAGE".equals(status)) {
                String from = response.getString("from_username");
                String content = response.getString("content");
                String typeStr = response.optString("type", "TEXT");
                String fileName = response.optString("file_name", null);

                // --- ĐỌC ID TỪ SERVER (QUAN TRỌNG) ---
                int msgId = response.optInt("id", -1);
                if (msgId == -1) msgId = response.optInt("msg_id", -1); // Thử cả 2 key

                // Xác định tin nhắn của ai
                String myName = ClientApp.getInstance().getMyUsername();
                boolean isMe = from.equals(myName);

                // Xác định loại tin
                ChatMessage.Type type;
                if ("IMAGE".equals(typeStr)) type = ChatMessage.Type.IMAGE;
                else if ("FILE".equals(typeStr)) type = ChatMessage.Type.FILE;
                else if ("VOICE".equals(typeStr)) type = ChatMessage.Type.VOICE;
                else type = ChatMessage.Type.TEXT;

                ChatMessage msg = new ChatMessage(from, content, isMe, type);
                if (fileName != null) msg.setFileName(fileName);
                msg.setId(msgId); // Gán ID chuẩn từ DB

                // Nếu là tin mình gửi (Echo), hiện vào khung chat hiện tại
                // Nếu là tin người khác gửi, hiện vào khung chat của họ
                String targetChat = isMe ? currentPartner : from;

                if (targetChat != null) {
                    addMessageToHistory(targetChat, msg);
                }

                // Cập nhật thông báo chưa đọc (nếu tin đến từ người khác và không phải chat hiện tại)
                if (!isMe) {
                    conversationList.stream().filter(item -> item.getUsername().equals(from))
                            .findFirst().ifPresent(item -> { if (!from.equals(currentPartner)) item.setHasUnreadMessages(true); });
                }
            }

            else if ("CHAT_HISTORY".equals(status)) {
                String withUser = response.getString("with_username");
                JSONArray history = response.getJSONArray("history");
                ObservableList<ChatMessage> items = getChatListForUser(withUser);
                items.clear();
                String myName = ClientApp.getInstance().getMyUsername();

                for (int i = 0; i < history.length(); i++) {
                    JSONObject msgObj = history.getJSONObject(i);
                    String from = msgObj.getString("from_username");
                    String typeStr = msgObj.optString("type", "TEXT");
                    boolean isMe = from.equals(myName);

                    int msgId = msgObj.optInt("id", -1);
                    if (msgId == -1) msgId = msgObj.optInt("msg_id", -1);

                    ChatMessage.Type type;
                    if ("IMAGE".equals(typeStr)) type = ChatMessage.Type.IMAGE;
                    else if ("FILE".equals(typeStr)) type = ChatMessage.Type.FILE;
                    else if ("VOICE".equals(typeStr)) type = ChatMessage.Type.VOICE;
                    else type = ChatMessage.Type.TEXT;

                    ChatMessage msg = new ChatMessage(from, msgObj.getString("content"), isMe, type);
                    if (msgObj.has("file_name")) msg.setFileName(msgObj.getString("file_name"));
                    msg.setId(msgId);

                    items.add(msg);
                }
                if (withUser.equals(currentPartner)) chatListView.scrollTo(items.size() - 1);
            }
            // Xử lý khi có tin nhắn bị xóa
            else if ("MESSAGE_DELETED".equals(status)) {
                int deletedId = response.optInt("deleted_id", -1);
                if (deletedId != -1) removeMessageById(deletedId);
            }
            else if ("PARTNER_PROFILE".equals(status)) {
                try {
                    JSONObject profile = response.getJSONObject("profile");
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/views/ProfileView.fxml"));
                    Parent root = loader.load();
                    ProfileController ctrl = loader.getController();
                    ctrl.setupProfile(profile, false);
                    ClientApp.getInstance().getPrimaryStage().getScene().setRoot(root);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    // ==========================================================
    // CÁC HÀM GỬI (ĐÃ BỎ ADD LOCAL)
    // ==========================================================

    @FXML private void handleSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty() || currentPartner == null) return;
        JSONObject req = new JSONObject().put("action", "SEND_MESSAGE").put("to_username", currentPartner).put("content", text).put("type", "TEXT");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);

        // BỎ DÒNG NÀY: addMessageToHistory(...)
        // Đợi server gửi lại NEW_MESSAGE kèm ID
        messageInputField.clear();
    }

    @FXML private void handleSendDocument() {
        if (currentPartner == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn tài liệu");
        File file = fc.showOpenDialog(null);
        if (file != null) {
            if (file.length() > 10 * 1024 * 1024) {
                showAlert("File quá lớn", "Vui lòng chọn file nhỏ hơn 10MB."); return;
            }
            try {
                String base64Content = FileUtil.encodeFileToBase64(file);
                String fileName = file.getName();
                JSONObject req = new JSONObject().put("action", "SEND_MESSAGE").put("to_username", currentPartner).put("content", base64Content).put("file_name", fileName).put("type", "FILE");
                ClientApp.getInstance().getNetworkClient().sendRequest(req);
                // BỎ DÒNG NÀY: addMessageToHistory(...)
            } catch (Exception e) { e.printStackTrace(); showAlert("Lỗi", "Không thể gửi file."); }
        }
    }

    @FXML private void handleSendFile() {
        if (currentPartner == null) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            String base64Content = FileUtil.encodeFileToBase64(file);
            JSONObject req = new JSONObject().put("action", "SEND_MESSAGE").put("to_username", currentPartner).put("content", base64Content).put("type", "IMAGE");
            ClientApp.getInstance().getNetworkClient().sendRequest(req);
            // BỎ DÒNG NÀY: addMessageToHistory(...)
        }
    }

    @FXML private void handleEmoji() {
        ContextMenu emojiMenu = new ContextMenu();
        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "😡", "👍", "❤️", "💔", "🎉", "🔥", "💩", "👻"};
        for (String icon : emojis) {
            MenuItem item = new MenuItem(icon);
            item.setStyle("-fx-font-size: 20px; -fx-padding: 5 10;");
            item.setOnAction(e -> {
                messageInputField.appendText(icon);
                messageInputField.requestFocus();
                messageInputField.positionCaret(messageInputField.getText().length());
            });
            emojiMenu.getItems().add(item);
        }
        if (btnEmoji != null) emojiMenu.show(btnEmoji, javafx.geometry.Side.TOP, 0, 0);
    }

    // --- LOGIC XÓA TIN NHẮN ---
    public void deleteMessageLocal(ChatMessage msg) {
        if (msg.getId() > 0) {
            JSONObject req = new JSONObject();
            req.put("action", "DELETE_MESSAGE");
            req.put("message_id", msg.getId());
            req.put("partner_username", currentPartner);
            ClientApp.getInstance().getNetworkClient().sendRequest(req);

            // Xóa tạm trên UI cho nhanh (Server sẽ confirm và xóa bên kia sau)
            getChatListForUser(currentPartner).remove(msg);
        } else {
            // Nếu không có ID (hiếm gặp vì ta đã đợi server), chỉ xóa local
            getChatListForUser(currentPartner).remove(msg);
        }
    }

    private void removeMessageById(int id) {
        if (currentPartner != null) {
            ObservableList<ChatMessage> items = getChatListForUser(currentPartner);
            items.removeIf(msg -> msg.getId() == id);
        }
    }

    private ObservableList<ChatMessage> getChatListForUser(String username) {
        if (!chatHistories.containsKey(username)) chatHistories.put(username, FXCollections.observableArrayList());
        return chatHistories.get(username);
    }

    private void addMessageToHistory(String username, ChatMessage msg) {
        ObservableList<ChatMessage> items = getChatListForUser(username);
        items.add(msg);
        if (username.equals(currentPartner)) chatListView.scrollTo(items.size() - 1);
    }

    @FXML private void viewPartnerProfile() {
        if (currentPartner == null) return;
        JSONObject req = new JSONObject();
        req.put("action", "GET_PROFILE_BY_USERNAME");
        req.put("target_username", currentPartner);
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
    @FXML private void backToHome() { ClientApp.getInstance().switchScene("HomeView.fxml"); }
    public void showImageOverlay(Image image) { enlargedImageView.setImage(image); imageOverlay.setVisible(true); imageOverlay.toFront(); }
    @FXML private void closeImageOverlay() { imageOverlay.setVisible(false); }
}