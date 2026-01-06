package client.gui;

import client.controllers.MessagesController;
import common.models.ChatMessage;
import common.utils.FileUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

import javax.sound.sampled.*; // Import thư viện âm thanh
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

public class ChatCellRenderer extends ListCell<ChatMessage> {
    private final MessagesController controller;

    public ChatCellRenderer(MessagesController controller) {
        this.controller = controller;
    }

    @Override
    protected void updateItem(ChatMessage msg, boolean empty) {
        super.updateItem(msg, empty);

        if (empty || msg == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color: transparent;");
        } else {
            HBox layout = new HBox();
            layout.setAlignment(msg.isMe() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            // Padding chuẩn để không dính lề màn hình
            layout.setPadding(new Insets(5, 15, 5, 15));
            layout.setSpacing(10);

            javafx.scene.Node contentNode = createMessageContent(msg);

            // --- MENU CHUỘT PHẢI ---
            ContextMenu contextMenu = new ContextMenu();

            MenuItem deleteItem = new MenuItem("Xóa tin nhắn");
            deleteItem.setOnAction(e -> controller.deleteMessageLocal(msg));
            contextMenu.getItems().add(deleteItem);

            // Chỉ cho phép lưu Ảnh, File hoặc Voice
            if (msg.getType() == ChatMessage.Type.IMAGE ||
                    msg.getType() == ChatMessage.Type.FILE ||
                    msg.getType() == ChatMessage.Type.VOICE) {

                MenuItem saveItem = new MenuItem("Lưu về máy");
                saveItem.setOnAction(e -> handleSaveFile(msg));
                contextMenu.getItems().add(saveItem);
            }

            contentNode.setOnContextMenuRequested(e -> contextMenu.show(contentNode, e.getScreenX(), e.getScreenY()));

            layout.getChildren().add(contentNode);
            setGraphic(layout);
            setStyle("-fx-background-color: transparent;");
        }
    }

    private javafx.scene.Node createMessageContent(ChatMessage msg) {
        if (msg.getType() == ChatMessage.Type.TEXT) {
            // --- XỬ LÝ TEXT ---
            Text text = new Text(msg.getContent());
            text.setFill(msg.isMe() ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
            text.setStyle("-fx-font-size: 16px;");

            text.setBoundsType(TextBoundsType.VISUAL);

            TextFlow flow = new TextFlow(text);
            flow.setStyle(msg.isMe() ?
                    "-fx-background-color: linear-gradient(to right, #fd267a, #ff6036); -fx-background-radius: 18; -fx-padding: 10px 14px;" :
                    "-fx-background-color: #e4e6eb; -fx-background-radius: 18; -fx-padding: 10px 14px;");

            flow.setMaxWidth(350);
            return flow;

        } else if (msg.getType() == ChatMessage.Type.IMAGE) {
            // --- XỬ LÝ ẢNH ---
            try {
                String base64Clean = msg.getContent().replaceAll("\\s", "");
                byte[] imgBytes = Base64.getDecoder().decode(base64Clean);

                Image img = new Image(new ByteArrayInputStream(imgBytes));
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(220);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);

                VBox imageBox = new VBox(imageView);
                imageBox.setStyle("-fx-padding: 5; -fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");

                imageView.setOnMouseClicked(e -> controller.showImageOverlay(img));
                return imageBox;
            } catch (Exception e) {
                return new Label("[Lỗi hiển thị ảnh]");
            }

        } else if (msg.getType() == ChatMessage.Type.VOICE) {
            // --- XỬ LÝ VOICE (GHI ÂM) ---
            HBox voiceBox = new HBox(10);
            voiceBox.setAlignment(Pos.CENTER_LEFT);
            // Style bong bóng chat cho Voice
            voiceBox.setStyle(msg.isMe() ?
                    "-fx-background-color: #0084ff; -fx-background-radius: 20; -fx-padding: 8px 15px;" :
                    "-fx-background-color: #f0f2f5; -fx-background-radius: 20; -fx-padding: 8px 15px;");

            // Nút Play hình tam giác (▶)
            Button btnPlay = new Button("▶");
            btnPlay.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-text-fill: black; " +
                            "-fx-background-radius: 50%; " +
                            "-fx-min-width: 35px; -fx-min-height: 35px; " +
                            "-fx-font-size: 14px; " +
                            "-fx-cursor: hand;"
            );

            VBox infoBox = new VBox(2);
            infoBox.setAlignment(Pos.CENTER_LEFT);
            Label lblTitle = new Label("Tin nhắn thoại");
            lblTitle.setStyle(msg.isMe() ? "-fx-text-fill: white; -fx-font-weight: bold;" : "-fx-text-fill: black; -fx-font-weight: bold;");

            Label lblHint = new Label("Nhấn để nghe");
            lblHint.setStyle(msg.isMe() ? "-fx-text-fill: #ddd; -fx-font-size: 10px;" : "-fx-text-fill: #777; -fx-font-size: 10px;");

            infoBox.getChildren().addAll(lblTitle, lblHint);

            // --- SỰ KIỆN: BẤM NÚT PLAY ---
            btnPlay.setOnAction(e -> {
                btnPlay.setText("..."); // Đổi text báo đang tải
                new Thread(() -> playVoice(msg.getContent(), btnPlay)).start();
            });

            voiceBox.getChildren().addAll(btnPlay, infoBox);
            return voiceBox;

        } else { // ChatMessage.Type.FILE
            // --- XỬ LÝ FILE ---
            HBox fileBox = new HBox(10);
            fileBox.setAlignment(Pos.CENTER_LEFT);
            fileBox.setStyle(msg.isMe() ?
                    "-fx-background-color: #0084ff; -fx-background-radius: 15; -fx-padding: 12px;" :
                    "-fx-background-color: #f0f2f5; -fx-background-radius: 15; -fx-padding: 12px;");

            Label iconLabel = new Label("📄");
            iconLabel.setStyle("-fx-font-size: 24px;");

            String rawName = msg.getFileName() != null ? msg.getFileName() : "Tài liệu";
            String displayName = rawName.length() > 25 ? rawName.substring(0, 22) + "..." : rawName;

            VBox infoBox = new VBox(2);
            Label nameLabel = new Label(displayName);
            nameLabel.setStyle(msg.isMe() ? "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" : "-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label hintLabel = new Label("Chuột phải để lưu");
            hintLabel.setStyle(msg.isMe() ? "-fx-text-fill: #e0e0e0; -fx-font-size: 11px;" : "-fx-text-fill: #666; -fx-font-size: 11px;");

            infoBox.getChildren().addAll(nameLabel, hintLabel);
            fileBox.getChildren().addAll(iconLabel, infoBox);
            return fileBox;
        }
    }

    // --- HÀM PHÁT VOICE TỪ BASE64 ---
    private void playVoice(String base64Content, Button btnPlay) {
        try {
            // 1. Giải mã Base64 -> Byte[]
            String cleanBase64 = base64Content.replaceAll("\\s", "");
            byte[] audioBytes = Base64.getDecoder().decode(cleanBase64);

            // 2. Tạo file tạm để phát
            File tempPlayFile = File.createTempFile("voice_play_" + System.currentTimeMillis(), ".wav");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempPlayFile)) {
                fos.write(audioBytes);
            }

            // 3. Phát âm thanh
            Clip clip = AudioSystem.getClip();
            AudioInputStream ais = AudioSystem.getAudioInputStream(tempPlayFile);
            clip.open(ais);
            clip.start();

            // Cập nhật icon đang phát
            javafx.application.Platform.runLater(() -> btnPlay.setText("🔊"));

            // Lắng nghe khi nào phát xong thì đổi lại icon Play
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    javafx.application.Platform.runLater(() -> btnPlay.setText("▶"));
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                btnPlay.setText("❌"); // Báo lỗi
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi phát âm thanh!");
                alert.show();
            });
        }
    }

    private void handleSaveFile(ChatMessage msg) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu tệp tin");

        String initialName = msg.getFileName();
        if (initialName == null || initialName.isEmpty()) {
            if (msg.getType() == ChatMessage.Type.IMAGE) initialName = "image_downloaded.png";
            else if (msg.getType() == ChatMessage.Type.VOICE) initialName = "voice_message.wav";
            else initialName = "document_downloaded";
        }
        fileChooser.setInitialFileName(initialName);

        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            boolean success = FileUtil.decodeBase64ToFile(msg.getContent(), file.getAbsolutePath());
            if (success) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã lưu thành công!");
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi khi lưu file! File có thể bị hỏng.");
                alert.show();
            }
        }
    }
}