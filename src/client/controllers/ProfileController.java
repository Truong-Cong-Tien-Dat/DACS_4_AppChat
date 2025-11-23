package client.controllers;

import client.ClientApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class ProfileController {

    // --- VIEW MODE (Xem) ---
    @FXML private ScrollPane viewModePane;
    // Tìm dòng này và thêm viewPhoto1 vào
    @FXML private ImageView coverImageView, avatarImageView, viewPhoto1, viewPhoto2, viewPhoto3, viewPhoto4;
    @FXML private Label viewNameLabel, viewAgeLabel, viewGenderLabel, viewInterestsLabel, viewHabitsLabel, viewRelationLabel, viewBioLabel;
    @FXML private Button btnEditProfile;
    @FXML private Button btnChangeAvatar;
    // --- EDIT MODE (Sửa) ---
    @FXML private HBox editModePane;
    @FXML private TextField nameField, ageField, interestsField, habitsField, relationshipField, bioField;
    @FXML private RadioButton genderMale, genderFemale, seekingMale, seekingFemale;
    @FXML private ToggleGroup genderGroup, seekingGroup;
    @FXML private Button photoBtn1, photoBtn2, photoBtn3, photoBtn4;

    private Button[] photoButtons;
    private String[] photoFiles = new String[5];
    private JSONObject profileData;
    private boolean isMe = true;

    public void initialize() {
        photoButtons = new Button[]{photoBtn1, photoBtn2, photoBtn3, photoBtn4};
        // Nếu mở từ Home (mặc định), thì là xem chính mình
        setupProfile(ClientApp.getInstance().getUserProfile(), true);
    }

    public void setupProfile(JSONObject profile, boolean isMe) {
        this.profileData = profile;
        this.isMe = isMe;

        loadViewModeData();

        btnEditProfile.setVisible(isMe);
        viewModePane.setVisible(true);
        editModePane.setVisible(false);
    }

    // --- 1. HIỂN THỊ CHẾ ĐỘ XEM ---
    private void loadViewModeData() {
        if (profileData == null) return;

        viewNameLabel.setText(profileData.optString("full_name", "Tên Người Dùng"));
        viewAgeLabel.setText(String.valueOf(profileData.optInt("age", 18)));
        viewGenderLabel.setText(profileData.optString("gender", "Nam"));
        viewInterestsLabel.setText(profileData.optString("interests", "---"));
        viewHabitsLabel.setText(profileData.optString("habits", "---"));
        viewRelationLabel.setText(profileData.optString("relationship_status", "---"));
        viewBioLabel.setText(profileData.optString("bio", "---"));

        String photo1 = profileData.optString("photo1");
        loadPhotoToView(photo1, avatarImageView);
        loadPhotoToView(photo1, viewPhoto1);
        loadPhotoToView(profileData.optString("photo2"), viewPhoto2);      // Ảnh 2
        loadPhotoToView(profileData.optString("photo3"), viewPhoto3);      // Ảnh 3
        loadPhotoToView(profileData.optString("photo4"), viewPhoto4);

        if (btnChangeAvatar != null) btnChangeAvatar.setVisible(isMe);
    }

    private void loadPhotoToView(String photoName, ImageView view) {
        try {
            view.setImage(null); // Reset trước
            if (photoName != null && !photoName.isEmpty()) {
                File file = new File("images/" + photoName);
                if (file.exists()) {
                    view.setImage(new Image(file.toURI().toString()));
                }
            }
        } catch (Exception e) {}
    }

    // --- 2. HIỂN THỊ CHẾ ĐỘ SỬA ---
    @FXML
    private void showEditMode() {
        loadEditModeData();
        viewModePane.setVisible(false);
        editModePane.setVisible(true);
    }

    @FXML
    private void hideEditMode() {
        viewModePane.setVisible(true);
        editModePane.setVisible(false);
    }

    private void loadEditModeData() {
        JSONObject p = ClientApp.getInstance().getUserProfile(); // Luôn lấy data mới nhất của mình
        if (p == null) return;

        nameField.setText(p.optString("full_name", ""));
        ageField.setText(String.valueOf(p.optInt("age", 18)));
        interestsField.setText(p.optString("interests", ""));
        habitsField.setText(p.optString("habits", ""));
        relationshipField.setText(p.optString("relationship_status", ""));
        bioField.setText(p.optString("bio", ""));

        String gender = p.optString("gender", "Nam");
        if ("Nam".equals(gender)) genderMale.setSelected(true); else genderFemale.setSelected(true);

        String seeking = p.optString("seeking", "Nữ");
        if ("Nam".equals(seeking)) seekingMale.setSelected(true); else seekingFemale.setSelected(true);

        // SỬA LỖI NULL POINTER Ở ĐÂY: Dùng biến 'p' thay vì 'profileData'
        for(int i=0; i<4; i++) {
            String name = p.optString("photo" + (1+i));
            photoFiles[i] = name;
            updatePhotoButtonUI(photoButtons[i], name);
        }
    }

    private void updatePhotoButtonUI(Button btn, String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            File file = new File("images/" + fileName);
            if (file.exists()) {
                btn.setStyle("-fx-background-image: url('" + file.toURI().toString() + "'); " +
                        "-fx-background-size: cover; -fx-background-position: center; -fx-background-repeat: no-repeat;");
                btn.setText("");
            }
        } else {
            btn.setStyle(null); btn.getStyleClass().setAll("photo-placeholder"); btn.setText("+");
        }
    }

    // --- 3. XỬ LÝ CẬP NHẬT ---
    @FXML
    private void handleUpdate() {
        try {
            if (nameField.getText().isEmpty() || ageField.getText().isEmpty()) {
                showAlert("Lỗi", "Tên và Tuổi không được để trống!");
                return;
            }
            int age = Integer.parseInt(ageField.getText().trim());

            JSONObject p = ClientApp.getInstance().getUserProfile();
            p.put("full_name", nameField.getText());
            p.put("age", age);
            p.put("gender", genderMale.isSelected() ? "Nam" : "Nữ");
            p.put("seeking", seekingMale.isSelected() ? "Nam" : "Nữ");
            p.put("interests", interestsField.getText());
            p.put("habits", habitsField.getText());
            p.put("relationship_status", relationshipField.getText());
            p.put("bio", bioField.getText());

            p.put("photo1", photoFiles[0]);
            p.put("photo2", photoFiles[1]);
            p.put("photo3", photoFiles[2]);
            p.put("photo4", photoFiles[3]);

            // Gửi Server
            JSONObject req = new JSONObject().put("action", "UPDATE_PROFILE").put("profile", p);
            ClientApp.getInstance().getNetworkClient().sendRequest(req);

            // Cập nhật Local
            ClientApp.getInstance().updateUserProfileLocal(p);

            // Cập nhật lại View Mode
            this.profileData = p;
            loadViewModeData();

            showAlert("Thành công", "Đã cập nhật hồ sơ!");
            hideEditMode(); // Quay về chế độ xem

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Tuổi phải là số nguyên!");
        } catch (Exception e) {
            showAlert("Lỗi", "Dữ liệu không hợp lệ: " + e.getMessage());
        }
    }

    @FXML
    private void choosePhoto(javafx.event.ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();

        // Tìm index của nút trong mảng photoButtons
        int slot = -1;
        for (int i = 0; i < photoButtons.length; i++) {
            if (clickedBtn == photoButtons[i]) {
                slot = i; break;
            }
        }
        if (slot == -1) return;

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);

        if (file != null) {
            try {
                String username = ClientApp.getInstance().getMyUsername();
                String ext = file.getName().substring(file.getName().lastIndexOf('.'));

                // Thêm timestamp để tránh cache
                String newName = username + "_photo" + (slot + 1) + "_" + System.currentTimeMillis() + ext;

                File dest = new File("images/" + newName);
                if(!dest.getParentFile().exists()) dest.getParentFile().mkdir();
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Lưu tên file mới
                photoFiles[slot] = newName;
                updatePhotoButtonUI(clickedBtn, newName);

            } catch (Exception e) { e.printStackTrace(); }
        }
    }
    @FXML
    private void quickChangeAvatar() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
        File file = fc.showOpenDialog(null);

        if (file != null) {
            try {
                String username = ClientApp.getInstance().getMyUsername();
                String ext = file.getName().substring(file.getName().lastIndexOf('.'));
                String newName = username + "_photo1_" + System.currentTimeMillis() + ext; // Luôn là photo1

                File dest = new File("images/" + newName);
                if(!dest.getParentFile().exists()) dest.getParentFile().mkdir();
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                JSONObject p = ClientApp.getInstance().getUserProfile();
                p.put("photo1", newName);

                JSONObject req = new JSONObject().put("action", "UPDATE_PROFILE").put("profile", p);
                ClientApp.getInstance().getNetworkClient().sendRequest(req);

                ClientApp.getInstance().updateUserProfileLocal(p);
                this.profileData = p;
                loadViewModeData();

                ClientApp.getInstance().updateUserProfileLocal(p); // Lưu vào ClientApp
                this.profileData = p;
                loadViewModeData(); // Reload giao diện Profile

                // --- (MỚI) 5. THÊM DÒNG NÀY ĐỂ CẬP NHẬT TRANG CHỦ ---
                // Vì trang chủ dùng chung dữ liệu trong ClientApp, nên ta chỉ cần ép nó reload lại
                // Tuy nhiên, vì Home đang ẩn nên ta không gọi trực tiếp được.
                // Cách đơn giản nhất: Gửi thông báo Alert thành công,
                // khi người dùng bấm "Back to Home", Home sẽ tự reload (do ta đã code initData ở switchScene).

                System.out.println("Đã đổi Avatar thành công!");

            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleLogout() {
        JSONObject req = new JSONObject().put("action", "LOGOUT");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
        ClientApp.getInstance().logout();
    }

    @FXML private void backToHome() { ClientApp.getInstance().switchScene("HomeView.fxml"); }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
}