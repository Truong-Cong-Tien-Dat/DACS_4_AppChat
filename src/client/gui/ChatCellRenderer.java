package client.gui;

import client.ClientApp;
import client.controllers.MessagesController;
import common.models.ChatMessage;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ChatCellRenderer extends ListCell<ChatMessage> {

    private MessagesController controller;

    public ChatCellRenderer(MessagesController controller) {
        this.controller = controller;
    }

    @Override
    protected void updateItem(ChatMessage msg, boolean empty) {
        super.updateItem(msg, empty);

        if (empty || msg == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color: transparent;"); // Fix lỗi vệt màu
        } else {
            HBox container = new HBox();
            VBox bubble = new VBox(5);

            // --- XỬ LÝ ẢNH ---
            if (msg.getType() == ChatMessage.Type.IMAGE) {
                try {
                    File file = new File("images/" + msg.getContent());
                    String imagePath = file.exists() ? file.toURI().toString() : "file:images/default_avatar.png";
                    Image imgObj = new Image(imagePath);

                    ImageView imageView = new ImageView(imgObj);
                    imageView.setFitWidth(220); // Ảnh nhỏ trong khung chat
                    imageView.setPreserveRatio(true);
                    imageView.getStyleClass().add("chat-image-view");

                    // 1. Sự kiện CLICK CHUỘT (Zoom & Menu)
                    imageView.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {

                            controller.showImageOverlay(imgObj);
                        }
                    });

                    // 2. Context Menu (Chuột phải)
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem saveItem = new MenuItem("Lưu về máy");
                    saveItem.setOnAction(e -> saveImageToDisk(file));

                    MenuItem copyItem = new MenuItem("Copy hình ảnh (Demo)"); // JavaFX copy clipboard phức tạp hơn chút
                    MenuItem deleteItem = new MenuItem("Xóa (Chỉ phía tôi)");
                    deleteItem.setOnAction(e -> getListView().getItems().remove(msg)); // Xóa tạm khỏi list

                    contextMenu.getItems().addAll(saveItem, copyItem, new SeparatorMenuItem(), deleteItem);

                    imageView.setOnContextMenuRequested(e ->
                            contextMenu.show(imageView, e.getScreenX(), e.getScreenY())
                    );

                    bubble.getChildren().add(imageView);

                } catch (Exception e) {
                    bubble.getChildren().add(new Label("❌ Lỗi ảnh"));
                }
            }
            else {
                Label contentLabel = new Label(msg.getContent());
                contentLabel.setWrapText(true);
                contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
                bubble.getChildren().add(contentLabel);
            }
            bubble.getStyleClass().clear();
            if (msg.getType() == ChatMessage.Type.IMAGE) {
                bubble.getStyleClass().add("image-bubble-container");
            } else {
                bubble.getStyleClass().add("chat-bubble");
                bubble.getStyleClass().add(msg.isFromMe() ? "bubble-me" : "bubble-other");
            }
            if (msg.isFromMe()) {
                container.setAlignment(Pos.CENTER_RIGHT);
            } else {
                container.setAlignment(Pos.CENTER_LEFT);
            }

            container.getChildren().add(bubble);
            setGraphic(container);
            setStyle("-fx-background-color: transparent;");
        }
    }

    private void saveImageToDisk(File sourceFile) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu hình ảnh");
        fileChooser.setInitialFileName(sourceFile.getName());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png"));
        File destFile = fileChooser.showSaveDialog(null);

        if (destFile != null) {
            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}