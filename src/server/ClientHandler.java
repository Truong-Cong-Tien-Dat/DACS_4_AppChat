package server;

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
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String jsonRequest;
            while ((jsonRequest = in.readLine()) != null) {
                System.out.println("Nhận từ Client (" + (username != null ? username : "chưa login") + "): " + jsonRequest);
                processRequest(jsonRequest);
            }
        } catch (Exception e) {
            System.out.println("Client " + (username != null ? username : "") + " đã ngắt kết nối.");
        } finally {
            ServerApp.removeClient(this.username);
            try {
                socket.close();
            } catch (Exception e) {}
        }
    }

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
                case "REFRESH_PROFILES_CLEAR_NOPED":
                    DatabaseService.clearNopedSwipes(this.username);
                    handleGetProfiles(true);
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
                    handleGetChatHistory(request);
                    break;
                case "GET_PROFILE_BY_USERNAME":
                    handleGetProfileByUsername(request);
                    break;
                case "UPDATE_PROFILE":
                    handleUpdateProfile(request);
                    break;
                case "DELETE_MESSAGE":
                    handleDeleteMessage(request);
                    break;
                case "LOGOUT":
                    ServerApp.removeClient(this.username);
                    this.username = null;
                    break;
                case "REQ_FORGOT_PASS":
                    handleForgotPass(request);
                    break;
                case "REQ_RESET_PASS":
                    handleResetPass(request);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(new JSONObject().put("status", "ERROR").put("message", "Request không hợp lệ").toString());
        }
    }

    // --- XỬ LÝ XÓA TIN NHẮN ---
    private void handleDeleteMessage(JSONObject request) {
        int msgId = request.getInt("message_id");
        String partnerUsername = request.optString("partner_username");

        boolean success = DatabaseService.deleteMessage(msgId);

        if (success) {
            System.out.println(">>> Đã xóa tin nhắn ID: " + msgId);

            JSONObject response = new JSONObject();
            response.put("status", "MESSAGE_DELETED");
            response.put("deleted_id", msgId);

            sendMessage(response.toString());

            if (partnerUsername != null && !partnerUsername.isEmpty()) {
                ServerApp.sendMessageToUser(partnerUsername, response.toString());
            }
        } else {
            sendMessage(new JSONObject().put("status", "ERROR").put("message", "Lỗi khi xóa tin nhắn").toString());
        }
    }

    private void handleSendMessage(JSONObject request) {
        if (this.username == null) return;
        String toUsername = request.getString("to_username");
        String messageContent = request.getString("content");
        String type = request.optString("type", "TEXT");
        String fileName = request.optString("file_name", null);

        int newMsgId = DatabaseService.saveMessage(this.username, toUsername, messageContent, type, fileName);

        JSONObject message = new JSONObject();
        message.put("status", "NEW_MESSAGE");
        message.put("from_username", this.username);
        message.put("content", messageContent);
        message.put("type", type);
        message.put("id", newMsgId);
        message.put("msg_id", newMsgId);
        if (fileName != null) message.put("file_name", fileName);

        ServerApp.sendMessageToUser(toUsername, message.toString());
        sendMessage(message.toString());
    }

    private void handleGetProfileByUsername(JSONObject request) {
        String targetUser = request.getString("target_username");
        JSONObject profile = DatabaseService.getUserProfile(targetUser);
        JSONObject response = new JSONObject();
        if (profile != null) {
            response.put("status", "PARTNER_PROFILE");
            response.put("profile", profile);
        } else {
            response.put("status", "ERROR");
            response.put("message", "Không tìm thấy người dùng.");
        }
        sendMessage(response.toString());
    }

    private void handleForgotPass(JSONObject request) {
        String uName = request.getString("username");
        String code = DatabaseService.generateRecoveryCode(uName);
        JSONObject res1 = new JSONObject();
        if (code != null) {
            System.out.println(">>> MÃ KHÔI PHỤC CHO " + uName + ": " + code + " <<<");
            res1.put("status", "FORGOT_PASS_SENT");
        } else {
            res1.put("status", "FORGOT_PASS_FAIL");
            res1.put("message", "Tên đăng nhập không tồn tại.");
        }
        sendMessage(res1.toString());
    }

    private void handleResetPass(JSONObject request) {
        boolean ok = DatabaseService.resetPassword(request.getString("username"), request.getString("code"), request.getString("new_password"));
        JSONObject res2 = new JSONObject();
        res2.put("status", ok ? "RESET_PASS_SUCCESS" : "RESET_PASS_FAIL");
        if (!ok) res2.put("message", "Mã xác nhận không đúng.");
        sendMessage(res2.toString());
    }

    private void handleLogin(JSONObject request) {
        String user = request.getString("username");
        String pass = request.getString("password");
        JSONObject profile = DatabaseService.loginUser(user, pass);
        JSONObject response = new JSONObject();
        if (profile != null) {
            this.username = user;
            ServerApp.addClient(user, this);
            response.put("status", "LOGIN_SUCCESS");
            response.put("profile", profile);
        } else {
            response.put("status", "LOGIN_FAIL");
            response.put("message", "Sai tên đăng nhập hoặc mật khẩu");
        }
        sendMessage(response.toString());
    }

    private void handleRegister(JSONObject request) {
        JSONObject profileData = request.getJSONObject("profile");
        boolean success = DatabaseService.registerUser(profileData);
        JSONObject response = new JSONObject();
        response.put("status", success ? "REGISTER_SUCCESS" : "REGISTER_FAIL");
        if (!success) response.put("message", "Tên đăng nhập đã tồn tại");
        sendMessage(response.toString());
    }

    private void handleGetProfiles(boolean filterSwiped) {
        if (this.username == null) return;
        List<String> suggestedUsernames = AIService.getRecommendations(this.username);
        JSONArray profiles = DatabaseService.getProfilesByUsernames(suggestedUsernames, this.username, filterSwiped);
        JSONObject response = new JSONObject();
        response.put("status", "PROFILE_LIST");
        response.put("profiles", profiles);
        sendMessage(response.toString());
    }

    private void handleSwipe(JSONObject request) {
        if (this.username == null) return;
        String swipedUsername = request.getString("swiped_username");
        boolean liked = request.getBoolean("liked");
        DatabaseService.recordSwipe(this.username, swipedUsername, liked);
        if (liked) {
            boolean isMatch = DatabaseService.checkForMatch(this.username, swipedUsername);
            if (isMatch) {
                System.out.println("MATCH FOUND: " + this.username + " and " + swipedUsername);
                JSONObject matchNotification = new JSONObject();
                matchNotification.put("status", "NEW_MATCH");
                matchNotification.put("profile", DatabaseService.getUserProfile(swipedUsername));
                sendMessage(matchNotification.toString());
                JSONObject otherMatchNotification = new JSONObject();
                otherMatchNotification.put("status", "NEW_MATCH");
                otherMatchNotification.put("profile", DatabaseService.getUserProfile(this.username));
                ServerApp.sendMessageToUser(swipedUsername, otherMatchNotification.toString());
            }
        }
    }

    private void handleGetMatches() {
        if (this.username == null) return;
        JSONArray matches = DatabaseService.getMatchesForUser(this.username);
        JSONObject response = new JSONObject();
        response.put("status", "MATCH_LIST");
        response.put("matches", matches);
        sendMessage(response.toString());
    }

    // --- HÀM UPDATE PROFILE ĐÃ CHUẨN HÓA ---
    private void handleUpdateProfile(JSONObject request) {
        if (this.username == null) return;

        System.out.println(">>> Đang xử lý update cho: " + this.username);

        // 1. Xác định cục dữ liệu chứa thông tin
        JSONObject rawData;
        if (request.has("profile")) {
            rawData = request.getJSONObject("profile");
        } else {
            rawData = request;
        }

        // 2. TẠO RA MỘT OBJECT SẠCH (Chỉ chứa thông tin profile thực sự)
        JSONObject cleanProfileData = new JSONObject();

        // Danh sách các trường được phép update (Phải khớp với Client gửi lên và cột trong Database)
        String[] allowedKeys = {
                "full_name", "age", "gender", "seeking", "bio",
                "interests", "habits", "relationship_status",
                "photo1", "photo2", "photo3", "photo4",
                "address", "dob" // Thêm các trường khác nếu DB có
        };

        for (String key : allowedKeys) {
            if (rawData.has(key)) {
                cleanProfileData.put(key, rawData.get(key));
            }
        }

        System.out.println(">>> Dữ liệu sạch chuẩn bị lưu DB: " + cleanProfileData.toString());

        // 3. Gọi Database update
        boolean success = false;
        if (cleanProfileData.length() > 0) {
            success = DatabaseService.updateUserProfile(this.username, cleanProfileData);
        } else {
            System.out.println(">>> Không có dữ liệu hợp lệ (trùng khớp key) để update!");
        }

        // 4. Phản hồi client
        JSONObject response = new JSONObject();
        response.put("status", success ? "UPDATE_SUCCESS" : "UPDATE_FAIL");
        if (!success) response.put("message", "Lỗi SQL hoặc dữ liệu rỗng");
        sendMessage(response.toString());
    }
    // ----------------------------------------

    public void sendMessage(String jsonMessage) {
        out.println(jsonMessage);
    }

    private void handleGetChatHistory(JSONObject request) {
        if (this.username == null) return;
        String withUsername = request.getString("with_username");
        JSONArray history = DatabaseService.getChatHistory(this.username, withUsername);
        JSONObject response = new JSONObject();
        response.put("status", "CHAT_HISTORY");
        response.put("with_username", withUsername);
        response.put("history", history);
        sendMessage(response.toString());
    }
}