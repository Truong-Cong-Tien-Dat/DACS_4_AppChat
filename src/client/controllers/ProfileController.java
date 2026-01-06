package client.controllers;

import client.ClientApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ProfileController {

    // --- Biến hiển thị (View Mode) ---
    @FXML private Circle avatarCircle;
    @FXML private Label viewNameLabel, viewAgeLabel, viewGenderLabel;
    @FXML private Label viewBioLabel, viewInterestsLabel, viewHabitsLabel, viewRelationLabel;

    // Sử dụng ImageView thay vì Region
    @FXML private ImageView viewPhoto1, viewPhoto2, viewPhoto3, viewPhoto4;

    // Nút chức năng
    @FXML private Button btnEditProfile, btnLogout, btnChangeAvatar;

    // Overlay phóng to
    @FXML private VBox imageOverlay;
    @FXML private ImageView enlargedImageView;

    // --- Biến chỉnh sửa (Edit Mode) ---
    @FXML private ScrollPane viewModePane;
    @FXML private HBox editModePane;
    @FXML private TextField nameField, ageField, bioField, interestsField, habitsField, relationshipField;
    @FXML private RadioButton genderMale, genderFemale, seekingMale, seekingFemale;
    @FXML private ToggleGroup genderGroup, seekingGroup;
    @FXML private Button photoBtn1, photoBtn2, photoBtn3, photoBtn4;

    private Map<Integer, File> selectedPhotos = new HashMap<>();
    private JSONObject currentProfile;

    // Biến để xác định logic "Ngược" hay "Xuôi"
    private boolean isMyProfile = true;
    private String previousScene = "HomeView.fxml"; // Mặc định back về Home

    @FXML
    public void initialize() {
        if (imageOverlay != null) imageOverlay.setVisible(false);
    }

    // --- HÀM QUAN TRỌNG: SETUP PROFILE ---
    public void setupProfile(JSONObject profile, boolean isMine) {
        this.currentProfile = profile;
        this.isMyProfile = isMine;

        // 1. XỬ LÝ LOGIC ĐIỀU HƯỚNG VÀ ẨN HIỆN NÚT
        if (isMine) {
            // Xem của mình (Từ Home) -> Hiện nút sửa, Back về Home
            if (btnEditProfile != null) btnEditProfile.setVisible(true);
            if (btnLogout != null) btnLogout.setVisible(true);
            if (btnChangeAvatar != null) btnChangeAvatar.setVisible(true);
            this.previousScene = "HomeView.fxml";
        } else {
            // Xem của người khác (Từ Chat) -> Ẩn nút sửa, Back về Message
            if (btnEditProfile != null) btnEditProfile.setVisible(false);
            if (btnLogout != null) btnLogout.setVisible(false);
            if (btnChangeAvatar != null) btnChangeAvatar.setVisible(false);
            this.previousScene = "MessagesView.fxml";
        }

        Platform.runLater(() -> {
            // 2. ĐIỀN DỮ LIỆU VÀO UI
            viewNameLabel.setText(profile.optString("full_name", "No Name"));
            viewAgeLabel.setText(String.valueOf(profile.optInt("age", 18)));
            viewGenderLabel.setText(profile.optString("gender", "Khác"));
            viewBioLabel.setText(profile.optString("bio", ""));
            viewInterestsLabel.setText(profile.optString("interests", ""));
            viewHabitsLabel.setText(profile.optString("habits", ""));
            viewRelationLabel.setText(profile.optString("relationship_status", ""));

            // Load Avatar
            loadAvatar(profile.optString("photo1", "default_avatar.png"));

            // Load Album ảnh
            setupAlbumImage(viewPhoto1, profile.optString("photo1", null));
            setupAlbumImage(viewPhoto2, profile.optString("photo2", null));
            setupAlbumImage(viewPhoto3, profile.optString("photo3", null));
            setupAlbumImage(viewPhoto4, profile.optString("photo4", null));
        });
    }

    // --- XỬ LÝ NÚT BACK THÔNG MINH ---
    @FXML
    private void handleBack() {
        ClientApp.getInstance().switchScene(previousScene);
    }

    private void setupAlbumImage(ImageView imageView, String photoName) {
        if (photoName != null && !photoName.isEmpty()) {
            try {
                File file = new File("images/" + photoName);
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    imageView.setImage(img);

                    // Bo tròn ảnh album
                    Rectangle clip = new Rectangle(imageView.getFitWidth(), imageView.getFitHeight());
                    clip.setArcWidth(20); clip.setArcHeight(20);
                    imageView.setClip(clip);

                    // Click để phóng to
                    imageView.setOnMouseClicked(e -> showImageOverlay(img));
                } else {
                    imageView.setImage(null);
                }
            } catch (Exception e) { imageView.setImage(null); }
        } else {
            imageView.setImage(null);
        }
    }

    private void loadAvatar(String photoName) {
        try {
            File file = new File("images/" + photoName);
            Image img = file.exists() ? new Image(file.toURI().toString()) : new Image("file:images/default_avatar.png");
            avatarCircle.setFill(new ImagePattern(img));

            // Avatar click cũng phóng to
            if (file.exists()) {
                avatarCircle.setOnMouseClicked(e -> showImageOverlay(img));
            }
        } catch (Exception e) {}
    }

    private void showImageOverlay(Image img) {
        if (img != null) {
            enlargedImageView.setImage(img);
            imageOverlay.setVisible(true);
            imageOverlay.toFront();
        }
    }
    @FXML private void closeImageOverlay() { imageOverlay.setVisible(false); }

    // --- CÁC HÀM EDIT MODE (CHỈ DÙNG KHI isMyProfile = true) ---
    @FXML
    private void showEditMode() {
        if (!isMyProfile) return;
        viewModePane.setVisible(false);
        editModePane.setVisible(true);

        nameField.setText(currentProfile.optString("full_name"));
        ageField.setText(String.valueOf(currentProfile.optInt("age")));
        bioField.setText(currentProfile.optString("bio"));
        interestsField.setText(currentProfile.optString("interests"));
        habitsField.setText(currentProfile.optString("habits"));
        relationshipField.setText(currentProfile.optString("relationship_status"));

        String g = currentProfile.optString("gender", "Nam");
        if (g.equals("Nam")) genderMale.setSelected(true); else genderFemale.setSelected(true);

        String s = currentProfile.optString("seeking", "Nữ");
        if (s.equals("Nam")) seekingMale.setSelected(true); else seekingFemale.setSelected(true);

        updateEditPhotoBtn(photoBtn1, currentProfile.optString("photo1"));
        updateEditPhotoBtn(photoBtn2, currentProfile.optString("photo2"));
        updateEditPhotoBtn(photoBtn3, currentProfile.optString("photo3"));
        updateEditPhotoBtn(photoBtn4, currentProfile.optString("photo4"));
    }

    @FXML private void hideEditMode() { editModePane.setVisible(false); viewModePane.setVisible(true); }

    private void updateEditPhotoBtn(Button btn, String photoName) {
        if (photoName != null && !photoName.isEmpty()) {
            File f = new File("images/" + photoName);
            if (f.exists()) {
                btn.setStyle("-fx-background-image: url('" + f.toURI().toString() + "'); -fx-background-size: cover;");
                btn.setText("");
            }
        } else {
            btn.setStyle(null);
            btn.getStyleClass().add("photo-placeholder");
            btn.setText("+");
        }
    }

    @FXML
    private void choosePhoto(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        int index = 0;
        if (btn == photoBtn1) index = 1;
        else if (btn == photoBtn2) index = 2;
        else if (btn == photoBtn3) index = 3;
        else if (btn == photoBtn4) index = 4;

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            selectedPhotos.put(index, file);
            btn.setStyle("-fx-background-image: url('" + file.toURI().toString() + "'); -fx-background-size: cover;");
            btn.setText("");
        }
    }

    @FXML
    private void handleUpdate() {
        try {
            // 1. Tạo JSON gửi đi
            JSONObject updateData = new JSONObject();
            updateData.put("action", "UPDATE_PROFILE");

            // Lấy dữ liệu từ giao diện
            String valName = nameField.getText();
            int valAge = 18;
            try { valAge = Integer.parseInt(ageField.getText()); } catch(Exception e){}
            String valGender = genderMale.isSelected() ? "Nam" : "Nữ";
            String valSeeking = seekingMale.isSelected() ? "Nam" : "Nữ";
            String valBio = bioField.getText();
            String valInterests = interestsField.getText();
            String valHabits = habitsField.getText();
            String valRelation = relationshipField.getText();

            // Đóng gói vào JSON gửi Server
            updateData.put("full_name", valName);
            updateData.put("age", valAge);
            updateData.put("gender", valGender);
            updateData.put("seeking", valSeeking);
            updateData.put("bio", valBio);
            updateData.put("interests", valInterests);
            updateData.put("habits", valHabits);
            updateData.put("relationship_status", valRelation);

            // Xử lý copy ảnh mới
            for (Map.Entry<Integer, File> entry : selectedPhotos.entrySet()) {
                File src = entry.getValue();
                // Lưu ý: dùng System.currentTimeMillis() để tránh cache ảnh cũ
                String newName = "user_" + ClientApp.getInstance().getMyUsername() + "_p" + entry.getKey() + "_" + System.currentTimeMillis() + ".jpg";
                File dest = new File("images/" + newName);
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                updateData.put("photo" + entry.getKey(), newName);
            }

            // Giữ lại tên ảnh cũ nếu không đổi
            if (!updateData.has("photo1")) updateData.put("photo1", currentProfile.optString("photo1"));
            if (!updateData.has("photo2")) updateData.put("photo2", currentProfile.optString("photo2"));
            if (!updateData.has("photo3")) updateData.put("photo3", currentProfile.optString("photo3"));
            if (!updateData.has("photo4")) updateData.put("photo4", currentProfile.optString("photo4"));

            // 2. Gửi request lên Server
            ClientApp.getInstance().getNetworkClient().sendRequest(updateData);

            // 3. Cập nhật giao diện hiện tại (Profile View)
            setupProfile(updateData, true);

            // --- 4. QUAN TRỌNG: CẬP NHẬT GLOBAL STATE (Để Home/Menu thấy thay đổi ngay) ---
            // Lấy object User đang lưu trong ClientApp
            JSONObject globalUser = ClientApp.getInstance().getCurrentUser();

            if (globalUser != null) {
                // Ghi đè các trường thông tin mới vào biến toàn cục
                globalUser.put("full_name", valName);
                globalUser.put("age", valAge);
                globalUser.put("gender", valGender);
                globalUser.put("seeking", valSeeking);
                globalUser.put("bio", valBio);
                globalUser.put("interests", valInterests);
                globalUser.put("habits", valHabits);
                globalUser.put("relationship_status", valRelation);

                // Cập nhật ảnh vào biến toàn cục nếu có thay đổi
                if (updateData.has("photo1")) globalUser.put("photo1", updateData.getString("photo1"));
                if (updateData.has("photo2")) globalUser.put("photo2", updateData.getString("photo2"));
                if (updateData.has("photo3")) globalUser.put("photo3", updateData.getString("photo3"));
                if (updateData.has("photo4")) globalUser.put("photo4", updateData.getString("photo4"));

                System.out.println(">>> Đã đồng bộ dữ liệu vào biến toàn cục ClientApp!");
            }
            // -----------------------------------------------------------------------------

            hideEditMode();

        } catch (Exception e) {
            e.printStackTrace();
            // Có thể thêm Alert báo lỗi ở đây
        }
    }

    @FXML private void handleLogout() { ClientApp.getInstance().restart(); }
    @FXML private void quickChangeAvatar() { photoBtn1.fire(); }
}