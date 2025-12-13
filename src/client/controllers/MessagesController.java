package client.controllers;

import client.ClientApp;
import client.gui.ChatCellRenderer;
import client.models.ConversationItem;
import common.models.ChatMessage;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class MessagesController {

    @FXML private TextField searchField;
    @FXML private ListView<ConversationItem> userListView;
    @FXML private Label currentPartnerLabel;

    // Đã xóa messageScrollPane vì không cần dùng nữa
    @FXML private ListView<ChatMessage> chatListView;

    @FXML private TextField messageInputField;
    @FXML private VBox chatArea;
    @FXML private VBox placeholderArea;
    @FXML private VBox imageOverlay;
    @FXML private ImageView enlargedImageView;
    @FXML private ImageView partnerAvatarView;

    private ObservableList<ConversationItem> conversationList = FXCollections.observableArrayList();
    private FilteredList<ConversationItem> filteredList;

    private Map<String, ObservableList<ChatMessage>> chatHistories = new HashMap<>();
    private String currentPartner;

    private static final PseudoClass UNREAD_PSEUDO_CLASS = PseudoClass.getPseudoClass("unread");

    public void initialize() {
        filteredList = new FilteredList<>(conversationList, p -> true);
        userListView.setItems(filteredList);

        setupListViewCellFactory();

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredList.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return item.getUsername().toLowerCase().contains(newValue.toLowerCase());
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
    }

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
                    nameLabel.setText(item.getUsername());

                    String photoName = (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty())
                            ? item.getAvatarUrl() : "default_avatar.png";
                    try {
                        File file = new File("images/" + photoName);
                        if (file.exists()) {
                            avatarImageView.setImage(new Image(file.toURI().toString(), 100, 100, false, true));
                        } else {
                            avatarImageView.setImage(null);
                        }
                    } catch (Exception e) {
                        avatarImageView.setImage(null);
                    }

                    item.hasUnreadMessagesProperty().addListener((obs, wasUnread, isUnread) -> {
                        avatarContainer.pseudoClassStateChanged(UNREAD_PSEUDO_CLASS, isUnread);
                    });
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

    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");

        Platform.runLater(() -> {
            if ("MATCH_LIST".equals(status)) {
                JSONArray arr = response.getJSONArray("matches");
                conversationList.clear();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String username = obj.getString("username");
                    String photo1 = obj.optString("photo1", "");
                    conversationList.add(new ConversationItem(username, photo1));
                }
            }
            else if ("NEW_MESSAGE".equals(status)) {
                String from = response.getString("from_username");
                String content = response.getString("content");
                String typeStr = response.optString("type", "TEXT");

                addMessageToHistory(from, new ChatMessage(from, content, false,
                        "IMAGE".equals(typeStr) ? ChatMessage.Type.IMAGE : ChatMessage.Type.TEXT));

                conversationList.stream()
                        .filter(item -> item.getUsername().equals(from))
                        .findFirst()
                        .ifPresent(item -> {
                            if (!from.equals(currentPartner)) {
                                item.setHasUnreadMessages(true);
                            }
                        });
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

    private void openChat(String username) {
        this.currentPartner = username;
        currentPartnerLabel.setText(username);

        if (placeholderArea != null) placeholderArea.setVisible(false);
        if (chatArea != null) chatArea.setVisible(true);

        if (partnerAvatarView != null) {
            conversationList.stream()
                    .filter(i -> i.getUsername().equals(username))
                    .findFirst()
                    .ifPresent(item -> {
                        String p = (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) ? item.getAvatarUrl() : "default_avatar.png";
                        try {
                            File f = new File("images/" + p);
                            if (f.exists()) {
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

    @FXML
    private void handleSendMessage() {
        String text = messageInputField.getText().trim();
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
        req.put("action", "GET_PROFILE_BY_USERNAME");
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