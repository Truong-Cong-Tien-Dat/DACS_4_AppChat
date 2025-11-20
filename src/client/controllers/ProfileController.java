package client.controllers;

import client.ClientApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays; // Thêm import này

public class ProfileController {

    @FXML private TextField nameField, ageField, interestsField, habitsField, relationshipField, bioField;
    @FXML private ToggleGroup genderGroup, seekingGroup;
    @FXML private RadioButton genderMale, genderFemale, seekingMale, seekingFemale;

    // (MỚI) Dùng mảng Button để dễ quản lý các ô ảnh
    @FXML private Button photoBtn1, photoBtn2, photoBtn3, photoBtn4;
    private Button[] photoButtons; // Mảng này sẽ chứa các nút trên

    private String[] photoFiles = new String[4];

    public void initialize() {
        // (MỚI) Khởi tạo mảng photoButtons
        photoButtons = new Button[]{photoBtn1, photoBtn2, photoBtn3, photoBtn4};

        loadCurrentData();
    }

    private void loadCurrentData() {
        JSONObject p = ClientApp.getInstance().getUserProfile();
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

        // 3. Load Ảnh (Hiển thị ảnh lên nút)
        // (MỚI) Lặp qua mảng photoButtons
        for (int i = 0; i < photoFiles.length; i++) {
            String photoName = p.optString("photo" + (i + 1), "");
            photoFiles[i] = photoName; // Lưu tên file vào mảng
            updatePhotoButtonUI(photoButtons[i], photoName); // Cập nhật UI
        }
    }

    // (MỚI) Hàm helper để cập nhật UI của nút ảnh
    private void updatePhotoButtonUI(Button btn, String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            File file = new File("images/" + fileName);
            if (file.exists()) {
                // Set ảnh nền cho nút
                btn.setStyle("-fx-background-image: url('" + file.toURI().toString() + "'); " +
                        "-fx-background-size: cover; -fx-background-position: center; " +
                        "-fxbackground-repeat: no-repeat; -fx-border-color: #21d07c; " +
                        "-fx-background-radius: 10; -fx-border-radius: 10;");
                btn.setText(""); // Xóa dấu "+"
            } else {
                // Nếu file không tồn tại, reset về trạng thái chưa có ảnh
                btn.getStyleClass().setAll("button", "photo-placeholder");
                btn.setText("+");
            }
        } else {
            // Chưa có ảnh
            btn.getStyleClass().setAll("button", "photo-placeholder");// Reset về CSS mặc định
            btn.setText("+");
        }
    }

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

            JSONObject req = new JSONObject().put("action", "UPDATE_PROFILE").put("profile", p);
            ClientApp.getInstance().getNetworkClient().sendRequest(req);

            showAlert("Thành công", "Đã cập nhật hồ sơ!");

        } catch (NumberFormatException e) { // Thêm bắt lỗi nhập tuổi
            showAlert("Lỗi", "Tuổi phải là số nguyên!");
        } catch (Exception e) {
            showAlert("Lỗi", "Dữ liệu không hợp lệ: " + e.getMessage());
        }
    }

    @FXML
    private void choosePhoto(javafx.event.ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        // Cách tìm index thông minh hơn (nếu bạn dùng array photoButtons)
        int slot = -1;
        if (clickedBtn == photoBtn1) slot = 0;
        else if (clickedBtn == photoBtn2) slot = 1;
        else if (clickedBtn == photoBtn3) slot = 2;
        else if (clickedBtn == photoBtn4) slot = 3;

        if (slot == -1) return;

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);

        if (file != null) {
            try {
                String username = ClientApp.getInstance().getMyUsername();
                String ext = file.getName().substring(file.getName().lastIndexOf('.'));

                // --- SỬA LỖI TẠI ĐÂY: THÊM TIMESTAMP ---
                String newName = username + "_photo" + (slot + 1) + "_" + System.currentTimeMillis() + ext;

                File dest = new File("images/" + newName);
                if(!dest.getParentFile().exists()) dest.getParentFile().mkdir();
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Cập nhật UI và biến
                photoFiles[slot] = newName; // Lưu tên mới vào mảng để tí nữa bấm "Lưu thay đổi" nó gửi đi

                // Cập nhật hiển thị ngay lập tức
                updatePhotoButtonUI(clickedBtn, newName);

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