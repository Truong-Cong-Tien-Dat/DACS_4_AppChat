package client.controllers;

import client.ClientApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class RegisterController {

    @FXML private TextField nameField, usernameField, passwordField, confirmPasswordField, ageField;
    @FXML private TextField interestsField, habitsField, relationshipField, bioField;
    @FXML private RadioButton genderMale, genderFemale, seekingMale, seekingFemale;
    @FXML private ToggleGroup genderGroup, seekingGroup;

    // (MỚI) Mảng các nút ảnh (lên đến 6 nút)
    @FXML private Button photoBtn1, photoBtn2, photoBtn3, photoBtn4, photoBtn5, photoBtn6;
    private Button[] photoButtons; // Mảng này sẽ chứa các nút trên

    private String[] photoFiles = new String[6]; // Lưu tên file ảnh đã chọn (lên đến 6 ảnh)

    public void initialize() {
        // (MỚI) Khởi tạo mảng photoButtons
        photoButtons = new Button[]{photoBtn1, photoBtn2, photoBtn3, photoBtn4, photoBtn5, photoBtn6};
    }

    @FXML
    private void handleRegister() {
        try {
            // 1. Lấy dữ liệu từ form
            String fullName = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            int age = Integer.parseInt(ageField.getText().trim());
            String gender = genderMale.isSelected() ? "Nam" : "Nữ";
            String seeking = seekingMale.isSelected() ? "Nam" : "Nữ";
            String interests = interestsField.getText().trim();
            String habits = habitsField.getText().trim();
            String relationshipStatus = relationshipField.getText().trim();
            String bio = bioField.getText().trim();

            // 2. Validate dữ liệu
            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || ageField.getText().isEmpty()) {
                showAlert("Lỗi", "Vui lòng điền đầy đủ thông tin bắt buộc!");
                return;
            }
            if (!password.equals(confirmPassword)) {
                showAlert("Lỗi", "Mật khẩu xác nhận không khớp!");
                return;
            }
            if (age < 18 || age > 99) {
                showAlert("Lỗi", "Tuổi phải từ 18 đến 99!");
                return;
            }

            int photoCount = 0;
            for(String photo : photoFiles) {
                if(photo != null && !photo.isEmpty()) photoCount++;
            }
            if (photoCount < 2) {
                showAlert("Lỗi", "Vui lòng tải lên ít nhất 2 ảnh hồ sơ!");
                return;
            }


            JSONObject profile = new JSONObject();
            profile.put("username", username);
            profile.put("password", password);
            profile.put("full_name", fullName);
            profile.put("age", age);
            profile.put("gender", gender);
            profile.put("seeking", seeking);
            profile.put("interests", interests);
            profile.put("habits", habits);
            profile.put("relationship_status", relationshipStatus);
            profile.put("bio", bio);

            for(int i=0; i < photoFiles.length; i++) {
                profile.put("photo" + (i + 1), photoFiles[i] != null ? photoFiles[i] : "");
            }

            ClientApp.getInstance().getNetworkClient().sendRequest(profile);

            // Xóa form sau khi đăng ký thành công (hoặc chuyển cảnh)
            showAlert("Thành công", "Đăng ký tài khoản " + username + " thành công!");
            clearForm(); // (MỚI) Thêm hàm này
            ClientApp.getInstance().switchScene("LoginView.fxml");

            JSONObject req = new JSONObject();
            req.put("action", "REGISTER");
            req.put("profile", profile);

            ClientApp.getInstance().getNetworkClient().sendRequest(req);
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Tuổi phải là số!");
        } catch (Exception e) {
            showAlert("Lỗi", "Có lỗi xảy ra: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void choosePhoto(javafx.event.ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        // (MỚI) Tìm index của nút trong mảng
        int slot = Arrays.asList(photoButtons).indexOf(clickedBtn);

        if (slot == -1) return; // Bảo vệ: Nếu không tìm thấy nút

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);

        if (file != null) {
            try {
                String ext = file.getName().substring(file.getName().lastIndexOf('.'));
                // Đổi tên file: "temp_randomstring_photo1.jpg"
                String newName = "temp_" + System.currentTimeMillis() + "_photo" + (slot + 1) + ext;

                File dest = new File("images/" + newName);
                if (!dest.getParentFile().exists()) dest.getParentFile().mkdir();
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                photoFiles[slot] = newName; // Lưu tên file vào mảng
                updatePhotoButtonUI(clickedBtn, newName); // (MỚI) Cập nhật UI nút

            } catch (Exception e) { e.printStackTrace(); }
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
                        "-fx-background-repeat: no-repeat; -fx-border-color: #21d07c; " +
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

    // (MỚI) Thêm hàm xóa form
    private void clearForm() {
        nameField.clear(); usernameField.clear(); passwordField.clear(); confirmPasswordField.clear();
        ageField.clear(); interestsField.clear(); habitsField.clear(); relationshipField.clear(); bioField.clear();
        genderMale.setSelected(true); seekingFemale.setSelected(true); // Reset radio buttons

        // Reset ảnh
        for(int i=0; i<photoFiles.length; i++) {
            photoFiles[i] = null;
            updatePhotoButtonUI(photoButtons[i], null);
        }
    }

    @FXML
    private void backToLogin() {
        ClientApp.getInstance().switchScene("LoginView.fxml");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }

    public void showError(String s) {
    }
}