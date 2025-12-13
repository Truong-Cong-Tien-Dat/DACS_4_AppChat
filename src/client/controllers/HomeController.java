package client.controllers;

import client.ClientApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeController {

    @FXML private Circle myAvatarCircle;
    @FXML private Label myFullNameLabel;
    @FXML private ListView<String> matchesListView;
    @FXML private ImageView profileImageView;
    @FXML private Label nameAgeLabel;
    @FXML private Label bioLabel;

    @FXML private StackPane cardContainer;

    private List<JSONObject> profileQueue = new ArrayList<>();
    private int currentProfileIndex = -1;
    private String[] currentPhotos = new String[4];
    private int currentPhotoIndex = 0;

    public void initData() {
        JSONObject myProfile = ClientApp.getInstance().getUserProfile();
        if (myProfile != null) {
            myFullNameLabel.setText(myProfile.getString("full_name"));
        } else {
            myFullNameLabel.setText("Người dùng");
        }


        if (profileImageView != null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(profileImageView.fitWidthProperty());
            clip.heightProperty().bind(profileImageView.fitHeightProperty());
            clip.setArcWidth(30);
            clip.setArcHeight(30);
            profileImageView.setClip(clip);
        }
        String avatarPath = "images/" + myProfile.optString("photo1", "default_avatar.png");
        try {
            File file = new File(avatarPath);
            if (file.exists()) {
                myAvatarCircle.setFill(new ImagePattern(new Image(file.toURI().toString())));
            }
        } catch (Exception e) {}
        loadProfiles();
    }

    private void loadProfiles() {
        JSONObject req = new JSONObject().put("action", "GET_PROFILES");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    @FXML
    private void handleCardClick(MouseEvent event) {
        if (profileQueue.isEmpty() || cardContainer == null) return;

        double width = cardContainer.getWidth();
        double clickX = event.getX();

        System.out.println("Click tại: " + clickX + " / " + width);

        if (clickX < width / 2) {
            prevPhoto();
        } else {
            nextPhoto();
        }
    }

    // --- CÁC HÀM CHUYỂN ẢNH ---
    private void prevPhoto() {
        if (currentPhotos[0] == null) return;
        currentPhotoIndex--;
        if (currentPhotoIndex < 0) currentPhotoIndex = 3;

        // Bỏ qua ảnh rỗng
        int attempts = 0;
        while ((currentPhotos[currentPhotoIndex] == null || currentPhotos[currentPhotoIndex].isEmpty()) && attempts < 4) {
            currentPhotoIndex--;
            if (currentPhotoIndex < 0) currentPhotoIndex = 3;
            attempts++;
        }
        updateProfileImage();
    }

    private void nextPhoto() {
        if (currentPhotos[0] == null) return;
        currentPhotoIndex++;
        if (currentPhotoIndex > 3) currentPhotoIndex = 0;

        // Bỏ qua ảnh rỗng
        int attempts = 0;
        while ((currentPhotos[currentPhotoIndex] == null || currentPhotos[currentPhotoIndex].isEmpty()) && attempts < 4) {
            currentPhotoIndex++;
            if (currentPhotoIndex > 3) currentPhotoIndex = 0;
            attempts++;
        }
        updateProfileImage();
    }

    private void updateProfileImage() {
        String photoName = currentPhotos[currentPhotoIndex];
        if (photoName != null && !photoName.isEmpty()) {
            try {
                File file = new File("images/" + photoName);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    profileImageView.setImage(image);
                }
            } catch (Exception e) { e.printStackTrace(); }
        } else { profileImageView.setImage(null); }
    }

    // --- XỬ LÝ SERVER RESPONSE ---
    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");
        Platform.runLater(() -> {
            if ("PROFILE_LIST".equals(status)) {
                JSONArray profiles = response.getJSONArray("profiles");
                profileQueue.clear();
                for (int i = 0; i < profiles.length(); i++) {
                    profileQueue.add(profiles.getJSONObject(i));
                }
                currentProfileIndex = -1;
                showNextProfile();
            } else if ("NEW_MATCH".equals(status)) {
                JSONObject profile = response.getJSONObject("profile");
                matchesListView.getItems().add(profile.getString("full_name"));
            }
        });
    }

    private void showNextProfile() {
        currentProfileIndex++;
        if (currentProfileIndex < profileQueue.size()) {
            JSONObject profile = profileQueue.get(currentProfileIndex);
            nameAgeLabel.setText(profile.getString("full_name") + ", " + profile.getInt("age"));
            bioLabel.setText(profile.optString("bio", ""));

            currentPhotos[0] = profile.optString("photo1", null);
            currentPhotos[1] = profile.optString("photo2", null);
            currentPhotos[2] = profile.optString("photo3", null);
            currentPhotos[3] = profile.optString("photo4", null);

            currentPhotoIndex = 0;
            updateProfileImage();
        } else {
            nameAgeLabel.setText("Hết hồ sơ");
            bioLabel.setText("Nhấn nút Quay lại (↺) để làm mới.");
            profileImageView.setImage(null);
            currentPhotos = new String[4];
        }
    }

    // --- CÁC HÀM NÚT BẤM KHÁC ---
    @FXML private void handleRefresh() {
        JSONObject req = new JSONObject().put("action", "REFRESH_PROFILES_CLEAR_NOPED");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
        nameAgeLabel.setText("Đang làm mới...");
        profileImageView.setImage(null);
    }

    @FXML private void handleLike() { sendSwipe(true); }
    @FXML private void handleNope() { sendSwipe(false); }

    private void sendSwipe(boolean liked) {
        if (currentProfileIndex < profileQueue.size()) {
            String targetUser = profileQueue.get(currentProfileIndex).getString("username");
            JSONObject req = new JSONObject().put("action", "SWIPE").put("swiped_username", targetUser).put("liked", liked);
            ClientApp.getInstance().getNetworkClient().sendRequest(req);
            showNextProfile();
        }
    }

    @FXML private void goToProfile() { ClientApp.getInstance().switchScene("ProfileView.fxml"); }
    @FXML private void goToMessages() { ClientApp.getInstance().switchScene("MessagesView.fxml"); }
}