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

    // Biến lưu thông tin người dùng (Global State)
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
        // Lưu ý: Đổi localhost thành IP máy chủ nếu chạy khác máy (VD: 192.168.1.x)
        networkClient = new NetworkClient("localhost", 12345);
        new Thread(networkClient).start();

        switchScene("LoginView.fxml");

        primaryStage.setTitle("D&D App chat");
        primaryStage.show();
    }

    public static ClientApp getInstance() { return instance; }
    public NetworkClient getNetworkClient() { return networkClient; }

    // --- CÁC HÀM GETTER/SETTER CHO USER (Đã chuẩn hóa) ---

    // Hàm này ProfileController đang gọi
    public JSONObject getCurrentUser() {
        return userProfile;
    }

    // Hàm cập nhật user từ bên ngoài (ProfileController gọi sau khi update)
    public void setCurrentUser(JSONObject user) {
        this.userProfile = user;
        if (user != null) {
            this.myUsername = user.optString("username");
        }
    }

    // Giữ lại hàm cũ để tránh lỗi code cũ của bạn
    public JSONObject getUserProfile() { return userProfile; }

    public String getMyUsername() { return myUsername; }


    public void switchScene(String fxmlFile) {
        try {
            // Lưu ý: Đường dẫn này phải đúng với cấu trúc thư mục của bạn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/views/" + fxmlFile));
            Parent root = loader.load();

            currentController = loader.getController();
            System.out.println("Đã chuyển sang màn hình: " + fxmlFile);
            System.out.println("Controller hiện tại là: " + currentController.getClass().getName());

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            // Logic khởi tạo riêng cho từng màn hình
            if (currentController instanceof HomeController) {
                ((HomeController) currentController).initData();
            }
            // Nếu chuyển sang ProfileView thì không cần init ở đây
            // vì ProfileController thường được setupProfile() thủ công từ controller trước đó.

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file FXML: " + fxmlFile);
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

    // Hàm update user cục bộ (Deprecated - nên dùng setCurrentUser)
    public void updateUserProfileLocal(JSONObject newProfile) {
        this.userProfile = newProfile;
        System.out.println("ClientApp: Đã cập nhật profile cục bộ.");
    }

    // --- XỬ LÝ PHẢN HỒI TỪ SERVER ---
    public void handleServerResponse(JSONObject response) {
        String status = response.optString("status");
        System.out.println("Client xử lý response: " + status);

        Platform.runLater(() -> {
            try {
                // 1. Màn hình Login
                if (currentController instanceof LoginController) {
                    LoginController loginCtrl = (LoginController) currentController;
                    if ("LOGIN_SUCCESS".equals(status)) {
                        JSONObject profile = response.getJSONObject("profile");

                        // Lưu user vào biến toàn cục
                        setCurrentUser(profile);

                        System.out.println("Đang chuyển sang Home...");
                        switchScene("HomeView.fxml");

                    } else if ("LOGIN_FAIL".equals(status)) {
                        loginCtrl.showError(response.optString("message", "Lỗi đăng nhập"));
                    }
                }

                // 2. Màn hình Register
                else if (currentController instanceof RegisterController) {
                    RegisterController regCtrl = (RegisterController) currentController;
                    if ("REGISTER_SUCCESS".equals(status)) {
                        switchScene("LoginView.fxml");
                    } else if ("REGISTER_FAIL".equals(status)) {
                        regCtrl.showError(response.optString("message", "Đăng ký thất bại"));
                    }
                }

                // 3. Màn hình Home
                else if (currentController instanceof HomeController) {
                    HomeController homeCtrl = (HomeController) currentController;
                    homeCtrl.handleServerResponse(response);
                }

                // 4. Màn hình Messages
                else if (currentController instanceof client.controllers.MessagesController) {
                    client.controllers.MessagesController msgCtrl = (client.controllers.MessagesController) currentController;
                    msgCtrl.handleServerResponse(response);
                }

                // 5. Màn hình Profile
                else if (currentController instanceof client.controllers.ProfileController) {
                    // ProfileController tự xử lý update optimistic rồi,
                    // nhưng nếu cần hiển thị thông báo từ server thì thêm vào đây.
                    System.out.println("Profile Controller nhận tín hiệu: " + status);
                    if ("UPDATE_SUCCESS".equals(status)) {
                        // Có thể hiển thị Alert "Lưu thành công" ở đây nếu muốn
                    }
                }

                // 6. Màn hình Quên mật khẩu
                else if (currentController instanceof client.controllers.ForgotPassController) {
                    client.controllers.ForgotPassController forgotCtrl = (client.controllers.ForgotPassController) currentController;
                    forgotCtrl.handleServerResponse(response);
                }
            } catch (Exception e) {
                System.err.println("LỖI GIAO DIỆN (Exception): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void restart() {
        System.out.println("Đăng xuất khỏi hệ thống...");
        this.myUsername = null;
        this.userProfile = null;
        switchScene("LoginView.fxml");
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}