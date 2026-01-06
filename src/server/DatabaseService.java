package server;

import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.DriverManager;
import java.sql.*;
import java.util.List;

public class DatabaseService {
    // Đảm bảo thông tin DB này khớp với máy của bạn
    private static final String DB_URL = "jdbc:mysql://localhost:3308/tinder_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "050655";
    private static Connection conn;

    public static boolean connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Đang kết nối đến CSDL MySQL...");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            if (conn != null) {
                System.out.println("Kết nối CSDL MySQL thành công!");
                return true;
            } else {
                System.out.println("Kết nối CSDL thất bại!");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("Lỗi: Không tìm thấy MySQL JDBC Driver.");
            e.printStackTrace();
            return false;
        }
    }

    public static Connection getConnection() {
        return conn;
    }

    public static JSONObject loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToProfile(rs);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean registerUser(JSONObject profile) {
        String sql = "INSERT INTO users (username, password, full_name, age, gender, interests, habits, relationship_status, bio, seeking, photo1, photo2, photo3, photo4) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profile.getString("username"));
            pstmt.setString(2, profile.getString("password"));
            pstmt.setString(3, profile.getString("full_name"));
            pstmt.setInt(4, profile.getInt("age"));
            pstmt.setString(5, profile.getString("gender"));
            pstmt.setString(6, profile.optString("interests", null));
            pstmt.setString(7, profile.optString("habits", null));
            pstmt.setString(8, profile.optString("relationship_status", null));
            pstmt.setString(9, profile.optString("bio", null));
            pstmt.setString(10, profile.getString("seeking"));
            pstmt.setString(11, profile.optString("photo1", "default_avatar.png"));
            pstmt.setString(12, profile.optString("photo2", null));
            pstmt.setString(13, profile.optString("photo3", null));
            pstmt.setString(14, profile.optString("photo4", null));
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) System.err.println("Lỗi: Tên đăng nhập đã tồn tại.");
            else e.printStackTrace();
            return false;
        }
    }

    // --- (QUAN TRỌNG) ĐÃ BỔ SUNG LẠI HÀM NÀY ĐỂ SỬA LỖI ---
    public static JSONArray getPotentialCandidates(String currentUsername, String myGender, String mySeeking) {
        JSONArray candidates = new JSONArray();
        String sql = "SELECT * FROM users WHERE username != ? AND ( ? = 'Khác' OR ? = 'Khác' OR gender = 'Khác' OR seeking = 'Khác' OR (gender = ? AND seeking = ?) )";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUsername);
            pstmt.setString(2, myGender);
            pstmt.setString(3, mySeeking);
            pstmt.setString(4, mySeeking);
            pstmt.setString(5, myGender);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    candidates.put(resultSetToProfile(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return candidates;
    }
    // -----------------------------------------------------

    public static boolean deleteMessage(int msgId) {
        String sql = "UPDATE messages SET status = 'DELETED' WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, msgId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int saveMessage(String from, String to, String content, String type, String fileName) {
        String sql = "INSERT INTO messages (from_username, to_username, content, msg_type, file_name, sent_time, status) VALUES (?, ?, ?, ?, ?, NOW(), 'ACTIVE')";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, from);
            stmt.setString(2, to);
            stmt.setString(3, content);
            stmt.setString(4, type);
            stmt.setString(5, fileName);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean saveMessage(String from, String to, String content, String type) {
        return saveMessage(from, to, content, type, null) != -1;
    }

    public static JSONArray getChatHistory(String userA, String userB) {
        JSONArray history = new JSONArray();
        String sql = "SELECT id, from_username, content, msg_type, file_name, sent_time FROM messages " +
                "WHERE ((from_username = ? AND to_username = ?) OR (from_username = ? AND to_username = ?)) " +
                "AND status = 'ACTIVE' " +
                "ORDER BY sent_time ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userA);
            pstmt.setString(2, userB);
            pstmt.setString(3, userB);
            pstmt.setString(4, userA);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject msg = new JSONObject();
                    msg.put("id", rs.getInt("id"));
                    msg.put("msg_id", rs.getInt("id"));
                    msg.put("from_username", rs.getString("from_username"));
                    msg.put("content", rs.getString("content"));
                    msg.put("type", rs.getString("msg_type"));
                    if(rs.getString("file_name") != null)
                        msg.put("file_name", rs.getString("file_name"));

                    history.put(msg);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return history;
    }

    public static void recordSwipe(String swiper, String swiped, boolean liked) {
        String sql = "REPLACE INTO swipes (swiper_username, swiped_username, liked) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, swiper); pstmt.setString(2, swiped); pstmt.setBoolean(3, liked);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static boolean checkForMatch(String userA, String userB) {
        String sql = "SELECT liked FROM swipes WHERE swiper_username = ? AND swiped_username = ? AND liked = 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userB); pstmt.setString(2, userA);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    createMatchRecord(userA, userB);
                    return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private static void createMatchRecord(String userA, String userB) {
        String user1 = userA.compareTo(userB) < 0 ? userA : userB;
        String user2 = userA.compareTo(userB) < 0 ? userB : userA;
        String sql = "INSERT IGNORE INTO matches (user1_username, user2_username) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user1); pstmt.setString(2, user2);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static JSONObject getUserProfile(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return resultSetToProfile(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static JSONArray getMatchesForUser(String username) {
        JSONArray matches = new JSONArray();
        String sql = "SELECT user1_username, user2_username FROM matches WHERE user1_username = ? OR user2_username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username); pstmt.setString(2, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String u1 = rs.getString("user1_username");
                    String u2 = rs.getString("user2_username");
                    String partnerUsername = u1.equals(username) ? u2 : u1;
                    JSONObject partnerProfile = getUserProfile(partnerUsername);
                    if (partnerProfile != null) matches.put(partnerProfile);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return matches;
    }

    public static boolean updateUserProfile(String username, JSONObject newProfileData) {
        // Danh sách các cột cho phép update
        String[] allowedColumns = {
                "full_name", "gender", "dob", "bio", "address",
                "interests", "habits", "seeking", "relationship_status",
                "photo1", "photo2", "photo3", "photo4"
        };

        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new java.util.ArrayList<>();
        boolean first = true;

        for (String col : allowedColumns) {
            if (newProfileData.has(col)) {
                if (!first) sql.append(", ");
                sql.append(col).append(" = ?");
                params.add(newProfileData.get(col));
                first = false;
            }
        }

        if (params.isEmpty()) {
            System.out.println(">>> KHÔNG CÓ DỮ LIỆU HỢP LỆ ĐỂ UPDATE CHO: " + username);
            return false;
        }

        sql.append(" WHERE username = ?");
        params.add(username);

        // --- SỬA ĐOẠN KẾT NỐI TẠI ĐÂY ---
        try (java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object val = params.get(i);
                if (val instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) val);
                } else {
                    stmt.setString(i + 1, String.valueOf(val));
                }
            }

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static JSONObject resultSetToProfile(ResultSet rs) throws SQLException {
        JSONObject profile = new JSONObject();
        profile.put("username", rs.getString("username"));
        profile.put("full_name", rs.getString("full_name"));
        profile.put("age", rs.getInt("age"));
        profile.put("gender", rs.getString("gender"));
        profile.put("interests", rs.getString("interests"));
        profile.put("habits", rs.getString("habits"));
        profile.put("relationship_status", rs.getString("relationship_status"));
        profile.put("bio", rs.getString("bio"));
        profile.put("seeking", rs.getString("seeking"));
        profile.put("photo1", rs.getString("photo1"));
        profile.put("photo2", rs.getString("photo2"));
        profile.put("photo3", rs.getString("photo3"));
        profile.put("photo4", rs.getString("photo4"));
        return profile;
    }

    public static void clearNopedSwipes(String swiperUsername) {
        String sql = "DELETE FROM swipes WHERE swiper_username = ? AND liked = 0";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, swiperUsername);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static JSONArray getProfilesByUsernames(List<String> usernames, String currentUsername, boolean filterSwiped) {
        JSONArray profiles = new JSONArray();
        if (usernames.isEmpty()) return profiles;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM users WHERE username IN (");
        for (int i = 0; i < usernames.size(); i++) {
            sqlBuilder.append("?");
            if (i < usernames.size() - 1) sqlBuilder.append(",");
        }
        sqlBuilder.append(")");
        if (filterSwiped) sqlBuilder.append(" AND username NOT IN (SELECT swiped_username FROM swipes WHERE swiper_username = ?)");
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            for (String username : usernames) pstmt.setString(paramIndex++, username);
            if (filterSwiped) pstmt.setString(paramIndex, currentUsername);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) profiles.put(resultSetToProfile(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return profiles;
    }

    public static String generateRecoveryCode(String username) {
        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            checkStmt.setString(1, username);
            if (!checkStmt.executeQuery().next()) return null;
        } catch (Exception e) { return null; }
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        String sql = "UPDATE users SET recovery_code = ? WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code); pstmt.setString(2, username);
            pstmt.executeUpdate();
            return code;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public static boolean resetPassword(String username, String code, String newPassword) {
        String sqlCheck = "SELECT id FROM users WHERE username = ? AND recovery_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {
            pstmt.setString(1, username); pstmt.setString(2, code);
            if (!pstmt.executeQuery().next()) return false;
        } catch (Exception e) { return false; }
        String sqlUpdate = "UPDATE users SET password = ?, recovery_code = NULL WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
            pstmt.setString(1, newPassword); pstmt.setString(2, username);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}