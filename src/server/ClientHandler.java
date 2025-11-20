package server;

// Đảm bảo bạn đã thêm thư viện JSON vào dự án
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username; // Tên user sau khi đăng nhập

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String jsonRequest;
            // Vòng lặp vô tận để lắng nghe tin nhắn từ client này
            while ((jsonRequest = in.readLine()) != null) {
                System.out.println("Nhận từ Client (" + (username != null ? username : "chưa login") + "): " + jsonRequest);
                // Xử lý request nhận được
                processRequest(jsonRequest);
            }
        } catch (Exception e) {
            System.out.println("Client " + (username != null ? username : "") + " đã ngắt kết nối.");
        } finally {
            // Dọn dẹp
            ServerApp.removeClient(this.username);
            try {
                socket.close();
            } catch (Exception e) {}
        }
    }

    // Phân tích JSON và xử lý yêu cầu
    private void processRequest(String jsonRequest) {
        try {
            JSONObject request = new JSONObject(jsonRequest);
            String action = request.getString("action");

            switch (action) {
                case "LOGIN":
                    handleLogin(request);
                    break;
                case "REGISTER":
                    handleRegister(request);
                    break;
                case "GET_PROFILES":
                    handleGetProfiles(true);
                    break;
                case "REFRESH_PROFILES_CLEAR_NOPED": // (MỚI)
                    DatabaseService.clearNopedSwipes(this.username); // Xóa các lượt "Bỏ qua"
                    handleGetProfiles(true); // Tải lại danh sách hồ sơ sau khi xóa xong
                    break;
                case "SWIPE":
                    handleSwipe(request);
                    break;
                case "GET_MATCHES":
                    handleGetMatches();
                    break;
                case "SEND_MESSAGE":
                    handleSendMessage(request);
                    break;
                case "GET_CHAT_HISTORY":
                    handleGetChatHistory(request); // <--- Dòng này sẽ gọi hàm bên dưới
                    break;
                case "UPDATE_PROFILE":
                    handleUpdateProfile(request);
                    break;
                case "LOGOUT":
                    ServerApp.removeClient(this.username);
                    this.username = null;
                    break;
                // Thêm các action khác ở đây
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(new JSONObject().put("status", "ERROR").put("message", "Request không hợp lệ").toString());
        }
    }

    // --- CÁC HÀM XỬ LÝ LOGIC ---

    private void handleLogin(JSONObject request) {
        String user = request.getString("username");
        String pass = request.getString("password");

        JSONObject profile = DatabaseService.loginUser(user, pass);
        JSONObject response = new JSONObject();

        if (profile != null) {
            this.username = user;
            ServerApp.addClient(user, this); // Thêm user vào danh sách online
            response.put("status", "LOGIN_SUCCESS");
            response.put("profile", profile);
        } else {
            response.put("status", "LOGIN_FAIL");
            response.put("message", "Sai tên đăng nhập hoặc mật khẩu");
        }
        sendMessage(response.toString());
    }

    private void handleRegister(JSONObject request) {
        // Lấy toàn bộ đối tượng profile từ request
        JSONObject profileData = request.getJSONObject("profile");
        boolean success = DatabaseService.registerUser(profileData);

        JSONObject response = new JSONObject();
        if (success) {
            response.put("status", "REGISTER_SUCCESS");
        } else {
            response.put("status", "REGISTER_FAIL");
            response.put("message", "Tên đăng nhập đã tồn tại");
        }
        sendMessage(response.toString());
    }

    // Thay đổi dòng này:
// private void handleGetProfiles() {
// Thành dòng này:
    private void handleGetProfiles(boolean filterSwiped) {
        if (this.username == null) return;

        List<String> suggestedUsernames = AIService.getRecommendations(this.username);

        // (MỚI) Truyền biến filterSwiped xuống DatabaseService
        JSONArray profiles = DatabaseService.getProfilesByUsernames(suggestedUsernames, this.username, filterSwiped);

        JSONObject response = new JSONObject();
        response.put("status", "PROFILE_LIST");
        response.put("profiles", profiles);
        sendMessage(response.toString());
    }

    private void handleSwipe(JSONObject request) {
        if (this.username == null) return;

        String swipedUsername = request.getString("swiped_username");
        boolean liked = request.getBoolean("liked"); // "Tim" hay "X"

        // 1. Lưu hành động quẹt vào CSDL
        DatabaseService.recordSwipe(this.username, swipedUsername, liked);

        if (liked) {
            // 2. Nếu là "Tim", kiểm tra xem có match không
            boolean isMatch = DatabaseService.checkForMatch(this.username, swipedUsername);

            if (isMatch) {
                // 3. Có match! Thông báo cho cả 2 user
                System.out.println("MATCH FOUND: " + this.username + " and " + swipedUsername);

                // Tạo thông báo cho MÌNH (user A)
                JSONObject matchNotification = new JSONObject();
                matchNotification.put("status", "NEW_MATCH");
                matchNotification.put("profile", DatabaseService.getUserProfile(swipedUsername)); // Gửi hồ sơ B
                sendMessage(matchNotification.toString()); // Gửi cho A

                // Tạo thông báo cho NGƯỜI KIA (user B)
                JSONObject otherMatchNotification = new JSONObject();
                otherMatchNotification.put("status", "NEW_MATCH");
                // (SỬA LỖI Ở ĐÂY)
                otherMatchNotification.put("profile", DatabaseService.getUserProfile(this.username)); // Gửi hồ sơ A
                ServerApp.sendMessageToUser(swipedUsername, otherMatchNotification.toString()); // Gửi cho B
            }
        }
    }

//    private void handleGetMatches() {
//        if (this.username == null) return;
//        JSONArray matches = DatabaseService.getMatchesForUser(this.username);
//        JSONObject response = new JSONObject();
//        response.put("status", "MATCH_LIST");
//        response.put("matches", matches);
//        sendMessage(response.toString());
//    }
// Trong server/ClientHandler.java

    private void handleGetMatches() {
        System.out.println("DEBUG: Đang xử lý GET_MATCHES cho user: " + this.username); // <--- IN RA

        if (this.username == null) {
            System.out.println("LỖI: Username bị null, không thể lấy matches!");
            return;
        }

        JSONArray matches = DatabaseService.getMatchesForUser(this.username);

        System.out.println("DEBUG: Tìm thấy " + matches.length() + " matches."); // <--- IN RA SỐ LƯỢNG

        JSONObject response = new JSONObject();
        response.put("status", "MATCH_LIST");
        response.put("matches", matches);
        sendMessage(response.toString());
    }
    private void handleSendMessage(JSONObject request) {
        if (this.username == null) return;

        String toUsername = request.getString("to_username");
        String messageContent = request.getString("content");
        String type = request.optString("type", "TEXT");
        // 1. Lưu tin nhắn vào CSDL
        DatabaseService.saveMessage(this.username, toUsername, messageContent, type);

        JSONObject message = new JSONObject();
        message.put("status", "NEW_MESSAGE");
        message.put("from_username", this.username);
        message.put("content", messageContent);
        message.put("type", type); // <--- QUAN TRỌNG: Gửi kèm type cho bên kia biết

        ServerApp.sendMessageToUser(toUsername, message.toString());
    }

    private void handleUpdateProfile(JSONObject request) {
        if (this.username == null) return;

        JSONObject profileData = request.getJSONObject("profile");
        boolean success = DatabaseService.updateUserProfile(this.username, profileData);

        JSONObject response = new JSONObject();
        response.put("status", success ? "UPDATE_SUCCESS" : "UPDATE_FAIL");
        sendMessage(response.toString());
    }

    // Gửi tin nhắn (JSON) về cho client này
    public void sendMessage(String jsonMessage) {
        out.println(jsonMessage);
    }
    /**
     * Xử lý yêu cầu lấy lịch sử tin nhắn
     */
    private void handleGetChatHistory(JSONObject request) {
        if (this.username == null) return;

        // 1. Lấy tên người muốn xem tin nhắn cùng
        String withUsername = request.getString("with_username");

        // 2. GỌI HÀM TRONG DATABASE (Hàm này đang bị xám, gọi xong nó sẽ sáng lên)
        JSONArray history = DatabaseService.getChatHistory(this.username, withUsername);

        // 3. Đóng gói và gửi về Client
        JSONObject response = new JSONObject();
        response.put("status", "CHAT_HISTORY");
        response.put("with_username", withUsername);
        response.put("history", history);

        sendMessage(response.toString());
    }
}
