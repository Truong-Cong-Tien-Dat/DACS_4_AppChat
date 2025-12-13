package client.controllers;

import client.ClientApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.ImagePattern; // IMPORT QUAN TRỌNG
import javafx.scene.shape.Circle;       // IMPORT QUAN TRỌNG
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ProfileController {

    // VIEW MODE
    @FXML private ScrollPane viewModePane;
    @FXML private Region coverRegion;   // Đổi thành Region
    @FXML private Circle avatarCircle;  // Đổi thành Circle
    @FXML private Region viewPhoto1, viewPhoto2, viewPhoto3, viewPhoto4;

    @FXML private Label viewNameLabel, viewAgeLabel, viewGenderLabel, viewInterestsLabel, viewHabitsLabel, viewRelationLabel, viewBioLabel;
    @FXML private Button btnEditProfile;
    @FXML private Button btnChangeAvatar;
    @FXML private Button btnLogout;

    // EDIT MODE
    @FXML private HBox editModePane;
    @FXML private TextField nameField, ageField, interestsField, habitsField, relationshipField, bioField;
    @FXML private RadioButton genderMale, genderFemale, seekingMale, seekingFemale;
    @FXML private Button photoBtn1, photoBtn2, photoBtn3, photoBtn4;

    private Button[] photoButtons;
    private String[] photoFiles = new String[5];
    private JSONObject profileData;
    private boolean isMe = true;

    public void initialize() {
        photoButtons = new Button[]{photoBtn1, photoBtn2, photoBtn3, photoBtn4};
        if (ClientApp.getInstance().getUserProfile() != null) {
            setupProfile(ClientApp.getInstance().getUserProfile(), true);
        }
    }

    public void setupProfile(JSONObject profile, boolean isMe) {
        this.profileData = profile;
        this.isMe = isMe;
        loadViewModeData();
        if (btnEditProfile != null) btnEditProfile.setVisible(isMe);
        if (btnChangeAvatar != null) btnChangeAvatar.setVisible(isMe);
        viewModePane.setVisible(true);
        editModePane.setVisible(false);
    }

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

        // 1. Load Avatar vào hình tròn (Dùng ImagePattern)
        loadAvatarToCircle(photo1, avatarCircle);

        // 2. Load Ảnh bìa vào Region (Dùng CSS Cover) - Mặc định lấy ảnh photo1 nếu chưa có ảnh bìa riêng
        // Nếu bạn muốn ảnh bìa riêng, thay photo1 bằng key khác
        setRegionImage(coverRegion, photo1);

        // 3. Load Album
        setRegionImage(viewPhoto1, photo1);
        setRegionImage(viewPhoto2, profileData.optString("photo2"));
        setRegionImage(viewPhoto3, profileData.optString("photo3"));
        setRegionImage(viewPhoto4, profileData.optString("photo4"));
    }

    // Hàm load Avatar chuẩn không méo
    private void loadAvatarToCircle(String photoName, Circle circle) {
        if (circle == null) return;
        try {
            Image image = null;
            if (photoName != null && !photoName.isEmpty()) {
                File file = new File("images/" + photoName);
                if (file.exists()) {
                    image = new Image(file.toURI().toString());
                }
            }
            // Fallback ảnh mặc định
            if (image == null) {
                try { image = new Image(getClass().getResource("/images/default_avatar.png").toExternalForm()); } catch (Exception e) {}
            }

            if (image != null) {
                circle.setFill(new ImagePattern(image));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Hàm load Region CSS (Bìa + Album)
    private void setRegionImage(Region region, String photoName) {
        if (region == null) return;
        region.setStyle("");
        if (photoName != null && !photoName.isEmpty()) {
            try {
                File file = new File("images/" + photoName);
                if (file.exists()) {
                    String path = file.toURI().toString().replace("\\", "/");
                    region.setStyle("-fx-background-image: url('" + path + "'); -fx-background-position: center center; -fx-background-repeat: no-repeat; -fx-background-size: cover;");
                }
            } catch (Exception e) {}
        }
    }

    @FXML private void showEditMode() { if (!isMe) return; loadEditModeData(); viewModePane.setVisible(false); editModePane.setVisible(true); }
    @FXML private void hideEditMode() { viewModePane.setVisible(true); editModePane.setVisible(false); }

    private void loadEditModeData() {
        JSONObject p = ClientApp.getInstance().getUserProfile();
        if (p == null) return;
        nameField.setText(p.optString("full_name", ""));
        ageField.setText(String.valueOf(p.optInt("age", 18)));
        interestsField.setText(p.optString("interests", ""));
        habitsField.setText(p.optString("habits", ""));
        relationshipField.setText(p.optString("relationship_status", ""));
        bioField.setText(p.optString("bio", ""));
        if ("Nam".equals(p.optString("gender"))) genderMale.setSelected(true); else genderFemale.setSelected(true);
        if ("Nam".equals(p.optString("seeking"))) seekingMale.setSelected(true); else seekingFemale.setSelected(true);
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
                btn.setStyle("-fx-background-image: url('" + file.toURI().toString() + "'); -fx-background-size: cover; -fx-background-position: center;");
                btn.setText("");
            }
        } else {
            btn.setStyle(null); btn.getStyleClass().setAll("photo-placeholder"); btn.setText("+");
        }
    }

    @FXML private void handleUpdate() {
        try {
            if (nameField.getText().isEmpty() || ageField.getText().isEmpty()) return;
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
            p.put("photo1", photoFiles[0]); p.put("photo2", photoFiles[1]); p.put("photo3", photoFiles[2]); p.put("photo4", photoFiles[3]);

            JSONObject req = new JSONObject().put("action", "UPDATE_PROFILE").put("profile", p);
            ClientApp.getInstance().getNetworkClient().sendRequest(req);
            ClientApp.getInstance().updateUserProfileLocal(p);
            this.profileData = p;
            loadViewModeData();
            hideEditMode();
        } catch (Exception e) {}
    }

    @FXML private void choosePhoto(javafx.event.ActionEvent event) {
        if (!isMe) return;
        Button clickedBtn = (Button) event.getSource();
        int slot = -1;
        for (int i = 0; i < photoButtons.length; i++) { if (clickedBtn == photoButtons[i]) { slot = i; break; } }
        if (slot == -1) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            try {
                String username = ClientApp.getInstance().getMyUsername();
                String newName = username + "_photo" + (slot + 1) + "_" + System.currentTimeMillis() + file.getName().substring(file.getName().lastIndexOf('.'));
                File dest = new File("images/" + newName);
                if(!dest.getParentFile().exists()) dest.getParentFile().mkdir();
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                photoFiles[slot] = newName;
                updatePhotoButtonUI(clickedBtn, newName);
            } catch (Exception e) {}
        }
    }

    @FXML private void quickChangeAvatar() {
        if (!isMe) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            try {
                String username = ClientApp.getInstance().getMyUsername();
                String newName = username + "_photo1_" + System.currentTimeMillis() + file.getName().substring(file.getName().lastIndexOf('.'));
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
            } catch (Exception e) {}
        }
    }

    @FXML private void handleLogout() {
        JSONObject req = new JSONObject().put("action", "LOGOUT");
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
        ClientApp.getInstance().logout();
    }

    @FXML private void backToHome() {
        if (isMe) ClientApp.getInstance().switchScene("HomeView.fxml");
        else ClientApp.getInstance().switchScene("MessagesView.fxml");
    }
}