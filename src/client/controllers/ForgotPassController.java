package client.controllers;

import client.ClientApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

public class ForgotPassController {

    @FXML private TextField usernameField, codeField;
    @FXML private PasswordField newPassField;
    @FXML private Button sendCodeBtn;
    @FXML private VBox resetBox;
    @FXML private Label statusLabel;

    @FXML
    private void handleSendCode() {
        String user = usernameField.getText().trim();
        if (user.isEmpty()) {
            statusLabel.setText("Vui lòng nhập username.");
            return;
        }

        JSONObject req = new JSONObject();
        req.put("action", "REQ_FORGOT_PASS");
        req.put("username", user);
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
        statusLabel.setText("Đang gửi yêu cầu...");
    }

    @FXML
    private void handleResetPass() {
        String code = codeField.getText().trim();
        String newPass = newPassField.getText();

        if (code.isEmpty() || newPass.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đủ mã và mật khẩu mới.");
            return;
        }

        JSONObject req = new JSONObject();
        req.put("action", "REQ_RESET_PASS");
        req.put("username", usernameField.getText().trim());
        req.put("code", code);
        req.put("new_password", newPass);
        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }

    @FXML
    private void backToLogin() {
        ClientApp.getInstance().switchScene("LoginView.fxml");
    }

    // Hàm để ClientApp gọi khi có phản hồi
    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");

        if ("FORGOT_PASS_SENT".equals(status)) {
            // Hiện phần nhập mã
            statusLabel.setStyle("-fx-text-fill: #21d07c;");
            statusLabel.setText("Mã đã được gửi! Kiểm tra Server Console.");
            usernameField.setDisable(true);
            sendCodeBtn.setDisable(true);
            resetBox.setVisible(true);
            resetBox.setManaged(true);
        } else if ("FORGOT_PASS_FAIL".equals(status)) {
            statusLabel.setStyle("-fx-text-fill: #ff4458;");
            statusLabel.setText(response.getString("message"));
        } else if ("RESET_PASS_SUCCESS".equals(status)) {
            statusLabel.setStyle("-fx-text-fill: #21d07c;");
            statusLabel.setText("Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            // Có thể tự động chuyển về login sau vài giây nếu muốn
        } else if ("RESET_PASS_FAIL".equals(status)) {
            statusLabel.setStyle("-fx-text-fill: #ff4458;");
            statusLabel.setText(response.optString("message", "Lỗi đổi mật khẩu."));
        }
    }
}