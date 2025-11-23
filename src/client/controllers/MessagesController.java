package client.controllers;

import client.ClientApp;
import client.gui.ChatCellRenderer;
import common.models.ChatMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class MessagesController {

    @FXML private TextField searchField;
    @FXML private ListView<String> matchesListView;
    @FXML private Label currentPartnerLabel;
    @FXML private ListView<ChatMessage> chatListView;
    @FXML private TextField messageInputField;
    @FXML private VBox chatArea;
    @FXML private VBox imageOverlay;
    @FXML private ImageView enlargedImageView;
    @FXML private ImageView partnerAvatarView;

    private Map<String, String> userMap = new HashMap<>();

    private ObservableList<String> matchesList = FXCollections.observableArrayList();
    private Map<String, ObservableList<ChatMessage>> chatHistories = new HashMap<>();
    private String currentPartner;

    public void initialize() {
        // 1. Setup danh sách Match
        FilteredList<String> filteredMatches = new FilteredList<>(matchesList, p -> true);
        matchesListView.setItems(filteredMatches);
        setupMatchesListView();

        // 2. Setup ô tìm kiếm
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredMatches.setPredicate(username -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String displayName = userMap.getOrDefault(username, username).toLowerCase();
                return displayName.contains(newValue.toLowerCase());
            });
        });

        // 3. Khi chọn người chat
        matchesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) openChat(newVal);
        });

        // 4. Setup danh sách Chat (Renderer bong bóng)
        chatListView.setCellFactory(param -> new ChatCellRenderer(this));

        // 5. Tải danh sách ngay khi mở
        loadMatches();
    }

    // --- 1. CÀI ĐẶT GIAO DIỆN DANH SÁCH (CÓ AVATAR) ---
    private void setupMatchesListView() {
        matchesListView.setCellFactory(param -> new ListCell<String>() {
            private final ImageView imageView = new ImageView();
            private final Label nameLabel = new Label();
            private final HBox hBox = new HBox(10);

            {
                // Cài đặt style cho HBox
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.setPadding(new Insets(5, 0, 5, 5));

                // Cài đặt Avatar
                imageView.setFitWidth(45);
                imageView.setFitHeight(45);
                imageView.setPreserveRatio(true);
                // Bo tròn Avatar
                Circle clip = new Circle(22.5, 22.5, 22.5);
                imageView.setClip(clip);

                // Cài đặt Tên
                nameLabel.setStyle("-fx-text-fill: #262626; -fx-font-size: 15px; -fx-font-weight: bold;");

                hBox.getChildren().addAll(imageView, nameLabel);
            }

            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);

                if (empty || username == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String displayName = userMap.getOrDefault(username, username);
                    nameLabel.setText(displayName);
                    String photoName = "default_avatar.png";
                    // TODO: String photoName = avatarMap.getOrDefault(username, "default_avatar.png");

                    try {
                        File file = new File("images/" + photoName);
                        String imagePath = file.exists() ? file.toURI().toString() : "file:images/default_avatar.png";
                        imageView.setImage(new Image(imagePath));
                    } catch (Exception e) {
                        imageView.setImage(null);
                    }

                    setGraphic(hBox);
                    setText(null);
                }
            }
        });
    }

    private void loadMatches() {
        System.out.println("MessagesController: Gửi lệnh lấy danh sách Match...");
        JSONObject req = new JSONObject().put("action", "GET_MATCHES");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    // --- XỬ LÝ PHẢN HỒI TỪ SERVER ---
    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");

        Platform.runLater(() -> {
            if ("MATCH_LIST".equals(status)) {
                JSONArray arr = response.getJSONArray("matches");
                matchesList.clear();
                userMap.clear();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String username = obj.getString("username");
                    String fullName = obj.optString("full_name", username);

                    userMap.put(username, fullName);
                    matchesList.add(username);
                }
            }
            else if ("NEW_MESSAGE".equals(status)) {
                String from = response.getString("from_username");
                String content = response.getString("content");
                String typeStr = response.optString("type", "TEXT");

                addMessageToHistory(from, new ChatMessage(from, content, false,
                        "IMAGE".equals(typeStr) ? ChatMessage.Type.IMAGE : ChatMessage.Type.TEXT));
            }
            else if ("CHAT_HISTORY".equals(status)) {
                String withUser = response.getString("with_username");
                JSONArray history = response.getJSONArray("history");

                ObservableList<ChatMessage> items = getChatListForUser(withUser);
                items.clear();

                String myName = ClientApp.getInstance().getMyUsername();

                for (int i = 0; i < history.length(); i++) {
                    JSONObject msg = history.getJSONObject(i);
                    String from = msg.getString("from_username");
                    String content = msg.getString("content");
                    String typeStr = msg.optString("type", "TEXT");

                    boolean isMe = from.equals(myName);
                    ChatMessage.Type type = "IMAGE".equals(typeStr) ? ChatMessage.Type.IMAGE : ChatMessage.Type.TEXT;

                    items.add(new ChatMessage(from, content, isMe, type));
                }

                if (withUser.equals(currentPartner)) {
                    chatListView.scrollTo(items.size() - 1);
                }
            }
            else if ("PARTNER_PROFILE".equals(status)) {
                JSONObject profile = response.getJSONObject("profile");

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/views/ProfileView.fxml"));
                        Parent root = loader.load();

                        ProfileController ctrl = loader.getController();
                        ctrl.setupProfile(profile, false); // false = Khách xem

                        // Mở cửa sổ mới hoặc thay thế Scene hiện tại
                        ClientApp.getInstance().getPrimaryStage().getScene().setRoot(root);
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }

        });
    }

    private void openChat(String username) {
        this.currentPartner = username;

        // Hiển thị tên thật trên tiêu đề (Chữ Đen cho nền Trắng)
        String displayName = userMap.getOrDefault(username, username);
        currentPartnerLabel.setText(displayName);
        currentPartnerLabel.setStyle("-fx-text-fill: #050505;"); // Đảm bảo tiêu đề màu đen
        currentPartnerLabel.setStyle("-fx-text-fill: #262626; -fx-font-size: 18px; -fx-font-weight: bold;");
        chatArea.setDisable(false);
        chatListView.setItems(getChatListForUser(username));

        String photoName = "default_avatar.png";
        JSONObject req = new JSONObject();
        req.put("action", "GET_CHAT_HISTORY");
        req.put("with_username", username);
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }


    @FXML
    private void handleSendMessage() {
        String text = messageInputField.getText().trim(); // Lấy từ messageInputField
        if (text.isEmpty() || currentPartner == null) return;

        JSONObject req = new JSONObject()
                .put("action", "SEND_MESSAGE")
                .put("to_username", currentPartner)
                .put("content", text)
                .put("type", "TEXT");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);

        addMessageToHistory(currentPartner, new ChatMessage("Bạn", text, true, ChatMessage.Type.TEXT));
        messageInputField.clear();
    }

    @FXML
    private void handleSendFile() {
        if (currentPartner == null) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            try {
                String newName = "chat_" + System.currentTimeMillis() + "_" + file.getName();
                File dest = new File("images/" + newName);
                if (!dest.getParentFile().exists()) dest.getParentFile().mkdir();
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                JSONObject req = new JSONObject()
                        .put("action", "SEND_MESSAGE")
                        .put("to_username", currentPartner)
                        .put("content", newName)
                        .put("type", "IMAGE");
                ClientApp.getInstance().getNetworkClient().sendRequest(req);

                addMessageToHistory(currentPartner, new ChatMessage("Bạn", newName, true, ChatMessage.Type.IMAGE));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private ObservableList<ChatMessage> getChatListForUser(String username) {
        if (!chatHistories.containsKey(username)) {
            chatHistories.put(username, FXCollections.observableArrayList());
        }
        return chatHistories.get(username);
    }
    @FXML
    private void viewPartnerProfile() {
        if (currentPartner == null) return;

        JSONObject req = new JSONObject();
        req.put("action", "GET_PROFILE_BY_USERNAME"); // Cần thêm case này ở Server
        req.put("target_username", currentPartner);
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    private void addMessageToHistory(String username, ChatMessage msg) {
        ObservableList<ChatMessage> items = getChatListForUser(username);
        items.add(msg);
        if (username.equals(currentPartner)) {
            chatListView.scrollTo(items.size() - 1);
        }
    }

    @FXML private void backToHome() { ClientApp.getInstance().switchScene("HomeView.fxml"); }
    public void showImageOverlay(Image image) { enlargedImageView.setImage(image); imageOverlay.setVisible(true); imageOverlay.toFront(); }
    @FXML private void closeImageOverlay() { imageOverlay.setVisible(false); }
}