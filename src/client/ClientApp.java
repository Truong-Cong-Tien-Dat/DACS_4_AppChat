package client;

import client.controllers.LoginController;
import client.controllers.RegisterController;
import client.controllers.HomeController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;

public class ClientApp extends Application {

    private static ClientApp instance;
    private Stage primaryStage;
    private NetworkClient networkClient;

    private JSONObject userProfile;
    private String myUsername;

    private Object currentController;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        this.primaryStage = stage;
        networkClient = new NetworkClient("localhost", 12345);
        new Thread(networkClient).start();
        switchScene("LoginView.fxml");

        primaryStage.setTitle("D&D App chat");
        primaryStage.show();
    }

    public static ClientApp getInstance() { return instance; }
    public NetworkClient getNetworkClient() { return networkClient; }

    public JSONObject getUserProfile() { return userProfile; }
    public String getMyUsername() { return myUsername; }


    public void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/views/" + fxmlFile));
            Parent root = loader.load();

            currentController = loader.getController();
            System.out.println("Đã chuyển sang màn hình: " + fxmlFile);
            System.out.println("Controller hiện tại là: " + currentController.getClass().getName()); // <--- THÊM LOG NÀY
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            if (currentController instanceof HomeController) {
                ((HomeController) currentController).initData();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void logout() {
        this.userProfile = null;
        this.myUsername = null;

        System.out.println("Đăng xuất thành công.");

        Platform.runLater(() -> {
            switchScene("LoginView.fxml");
        });
    }

    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");
        System.out.println("Client xử lý response: " + status);

        Platform.runLater(() -> {
            try {
                if (currentController instanceof LoginController) {
                    LoginController loginCtrl = (LoginController) currentController;
                    if ("LOGIN_SUCCESS".equals(status)) {
                        JSONObject profile = response.getJSONObject("profile");
                        this.userProfile = profile;
                        this.myUsername = profile.getString("username");

                        System.out.println("Đang chuyển sang Home...");
                        switchScene("HomeView.fxml");

                    } else if ("LOGIN_FAIL".equals(status)) {
                        loginCtrl.showError(response.optString("message", "Lỗi đăng nhập"));
                    }
                }

                else if (currentController instanceof RegisterController) {
                    RegisterController regCtrl = (RegisterController) currentController;
                    if ("REGISTER_SUCCESS".equals(status)) {
                        switchScene("LoginView.fxml");
                    } else if ("REGISTER_FAIL".equals(status)) {
                        regCtrl.showError(response.optString("message", "Đăng ký thất bại"));
                    }
                }

                else if (currentController instanceof HomeController) {
                    HomeController homeCtrl = (HomeController) currentController;
                    homeCtrl.handleServerResponse(response);
                }
                else if (currentController instanceof client.controllers.MessagesController) {
                    client.controllers.MessagesController msgCtrl = (client.controllers.MessagesController) currentController;
                    msgCtrl.handleServerResponse(response);
                }

                else if (currentController instanceof client.controllers.ProfileController) {

                }
            } catch (Exception e) {
                System.err.println("LỖI GIAO DIỆN (Exception): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}