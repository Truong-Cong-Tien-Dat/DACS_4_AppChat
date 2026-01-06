package client.controllers;

import client.ClientApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

        // 1. Hiển thị thông tin của CHÍNH MÌNH ở góc trái
        if (myProfile != null) {
            myFullNameLabel.setText(myProfile.optString("full_name", "Tôi"));

            // Load Avatar nhỏ
            String avatarPath = "images/" + myProfile.optString("photo1", "default_avatar.png");
            try {
                File file = new File(avatarPath);
                Image img = file.exists() ? new Image(file.toURI().toString()) : new Image("file:images/default_avatar.png");
                myAvatarCircle.setFill(new ImagePattern(img));
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 2. Bo tròn ảnh thẻ quẹt (Card)
        if (profileImageView != null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(profileImageView.fitWidthProperty());
            clip.heightProperty().bind(profileImageView.fitHeightProperty());
            clip.setArcWidth(30);
            clip.setArcHeight(30);
            profileImageView.setClip(clip);
        }

        // 3. Tải danh sách người lạ để quẹt
        loadProfiles();
    }

    private void loadProfiles() {
        JSONObject req = new JSONObject().put("action", "GET_PROFILES");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    // --- SỰ KIỆN: Click vào ảnh để đổi ảnh (Trái/Phải) ---
    @FXML
    private void handleCardClick(MouseEvent event) {
        if (profileQueue.isEmpty() || cardContainer == null) return;

        double width = cardContainer.getWidth();
        double clickX = event.getX();

        if (clickX < width / 2) {
            prevPhoto();
        } else {
            nextPhoto();
        }
    }

    private void prevPhoto() {
        if (currentPhotos[0] == null) return;
        currentPhotoIndex--;
        if (currentPhotoIndex < 0) currentPhotoIndex = 3;

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
                    profileImageView.setImage(new Image(file.toURI().toString()));
                } else {
                    profileImageView.setImage(null);
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
            }
            else if ("NEW_MATCH".equals(status)) {
                // Khi có Match mới, hiện thông báo hoặc thêm vào list
                JSONObject profile = response.getJSONObject("profile");
                // matchesListView.getItems().add(profile.getString("full_name")); // (Optional)
            }
            else if ("REFRESH_SUCCESS".equals(status)) {
                loadProfiles(); // Tải lại danh sách sau khi reset
            }
            else if ("MY_PROFILE_UPDATED".equals(status)) {
                // Nếu vừa sửa profile xong thì update lại avatar góc trái
                ClientApp.getInstance().updateUserProfileLocal(response.getJSONObject("profile"));
                initData();
            }
        });
    }

    private void showNextProfile() {
        currentProfileIndex++;
        if (currentProfileIndex < profileQueue.size()) {
            JSONObject profile = profileQueue.get(currentProfileIndex);

            String name = profile.optString("full_name", "No Name");
            int age = profile.optInt("age", 18);
            nameAgeLabel.setText(name + ", " + age);

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

    // --- CÁC HÀM NÚT BẤM ---

    @FXML
    private void handleRefresh() {
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

    // --- SỬA LOGIC CHUYỂN TRANG PROFILE ---
    @FXML
    private void goToProfile() {
        try {
            // 1. Lấy dữ liệu của MÌNH
            JSONObject myProfile = ClientApp.getInstance().getUserProfile();
            if (myProfile == null) return;

            // 2. Load FXML thủ công
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/views/ProfileView.fxml"));
            Parent root = loader.load();

            // 3. Lấy Controller và truyền dữ liệu
            ProfileController ctrl = loader.getController();
            ctrl.setupProfile(myProfile, true); // true = Là của tôi

            // 4. Chuyển cảnh
            ClientApp.getInstance().getPrimaryStage().getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void goToMessages() { ClientApp.getInstance().switchScene("MessagesView.fxml"); }
}