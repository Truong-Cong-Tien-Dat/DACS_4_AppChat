package client.controllers;

import client.ClientApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.JSONObject;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Gửi request LOGIN
        JSONObject req = new JSONObject();
        req.put("action", "LOGIN");
        req.put("username", username);
        req.put("password", password);

        ClientApp.getInstance().getNetworkClient().sendRequest(req);
    }
    @FXML void goToForgot() {
        ClientApp.getInstance().switchScene("ForgotPassView.fxml");
    }
    @FXML
    private void goToRegister() {
        ClientApp.getInstance().switchScene("RegisterView.fxml");
    }

    public void showError(String msg) {
        errorLabel.setText(msg);
    }
}