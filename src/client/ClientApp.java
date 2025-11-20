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

    // (MỚI) KHAI BÁO BIẾN LƯU TRỮ HỒ SƠ
    private JSONObject userProfile;
    private String myUsername;

    // Controller hiện tại đang hoạt động (để nhận phản hồi từ server)
    private Object currentController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        this.primaryStage = stage;

        // 1. Kết nối Server
        networkClient = new NetworkClient("localhost", 12345);
        new Thread(networkClient).start(); // Chạy luồng nền

        // 2. Mở màn hình Login
        switchScene("LoginView.fxml");

        primaryStage.setTitle("Tinder Clone App");
        primaryStage.show();
    }

    public static ClientApp getInstance() { return instance; }
    public NetworkClient getNetworkClient() { return networkClient; }

    // (MỚI) GETTER để các Controller khác lấy hồ sơ
    public JSONObject getUserProfile() { return userProfile; }
    public String getMyUsername() { return myUsername; }
    // public String getMyUsername() { return myUsername; } // Dùng cho logic Chat

    /**
     * Hàm chuyển cảnh (Scene)
     */
    public void switchScene(String fxmlFile) {
        try {
            // Sửa lỗi: Thêm "/resources/views/"
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/views/" + fxmlFile));
            Parent root = loader.load();

            currentController = loader.getController();
            System.out.println("Đã chuyển sang màn hình: " + fxmlFile); // <--- THÊM LOG NÀY
            System.out.println("Controller hiện tại là: " + currentController.getClass().getName()); // <--- THÊM LOG NÀY
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            // Nếu chuyển sang Home, cần tải dữ liệu ban đầu
            if (currentController instanceof HomeController) {
                ((HomeController) currentController).initData();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void logout() {
        // 1. Xóa dữ liệu người dùng
        this.userProfile = null;
        this.myUsername = null;

        // 2. Chuyển về màn hình đăng nhập
        System.out.println("Đăng xuất thành công.");

        // Chạy trên luồng JavaFX để đảm bảo an toàn
        Platform.runLater(() -> {
            switchScene("LoginView.fxml");
        });
    }

    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");
        System.out.println("Client xử lý response: " + status);

        Platform.runLater(() -> {
            try {
                // 1. Xử lý cho màn hình LOGIN
                if (currentController instanceof LoginController) {
                    LoginController loginCtrl = (LoginController) currentController;
                    if ("LOGIN_SUCCESS".equals(status)) {
                        // (MỚI) LƯU HỒ SƠ VÀ USERNAME
                        JSONObject profile = response.getJSONObject("profile");
                        this.userProfile = profile;
                        this.myUsername = profile.getString("username");

                        System.out.println("Đang chuyển sang Home...");
                        switchScene("HomeView.fxml");

                    } else if ("LOGIN_FAIL".equals(status)) {
                        loginCtrl.showError(response.optString("message", "Lỗi đăng nhập"));
                    }
                }

                // 2. Xử lý cho màn hình REGISTER
                else if (currentController instanceof RegisterController) {
                    RegisterController regCtrl = (RegisterController) currentController;
                    if ("REGISTER_SUCCESS".equals(status)) {
                        switchScene("LoginView.fxml");
                    } else if ("REGISTER_FAIL".equals(status)) {
                        regCtrl.showError(response.optString("message", "Đăng ký thất bại"));
                    }
                }

                // 3. Xử lý cho màn hình HOME (QUAN TRỌNG: Nhận list profile)
                else if (currentController instanceof HomeController) {
                    HomeController homeCtrl = (HomeController) currentController;
                    homeCtrl.handleServerResponse(response);
                }
                else if (currentController instanceof client.controllers.MessagesController) {
                    client.controllers.MessagesController msgCtrl = (client.controllers.MessagesController) currentController;
                    msgCtrl.handleServerResponse(response);
                }

                // --- (MỚI) 5. THÊM PHẦN NÀY CHO PROFILE ---
                else if (currentController instanceof client.controllers.ProfileController) {
                    // ProfileController có thể cần nhận thông báo cập nhật thành công
                    // Ví dụ: hiển thị Alert thành công
                }
            } catch (Exception e) {
                System.err.println("LỖI GIAO DIỆN (Exception): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}