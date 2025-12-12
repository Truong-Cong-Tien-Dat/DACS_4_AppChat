package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DatabaseService {
    private static final String DB_URL = "jdbc:mysql://localhost:3308/tinder_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "050655";
    private static Connection conn;

    public static boolean connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Mở kết nối
            System.out.println("Đang kết nối đến CSDL MySQL...");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            if (conn != null) {
                System.out.println("Kết nối CSDL MySQL thành công!");
            } else {
                System.out.println("Kết nối CSDL thất bại!");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Lỗi: Không tìm thấy MySQL JDBC Driver.");
            e.printStackTrace();
        }
        return false;
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean registerUser(JSONObject profile) {
        String sql = "INSERT INTO users (username, password, full_name, age, gender, interests, habits, relationship_status, bio, seeking, photo1, photo2, photo3, photo4) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profile.getString("username"));
            pstmt.setString(2, profile.getString("password")); // Nên hash mật khẩu này
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
            if (e.getErrorCode() == 1062) {
                System.err.println("Lỗi: Tên đăng nhập đã tồn tại.");
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }
    public static JSONArray getPotentialCandidates(String currentUsername, String myGender, String mySeeking) {
        JSONArray candidates = new JSONArray();
        String sql;

        sql = "SELECT * FROM users WHERE " +
                "username != ? AND ( " +
                "   ? = 'Khác' OR ? = 'Khác' OR " +
                "   gender = 'Khác' OR seeking = 'Khác' OR " +
                "   (gender = ? AND seeking = ?) " +
                ")";

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

    public static void recordSwipe(String swiper, String swiped, boolean liked) {
        String sql = "REPLACE INTO swipes (swiper_username, swiped_username, liked) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, swiper);
            pstmt.setString(2, swiped);
            pstmt.setBoolean(3, liked);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkForMatch(String userA, String userB) {
        String sql = "SELECT liked FROM swipes WHERE swiper_username = ? AND swiped_username = ? AND liked = 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userB); // Người quẹt là B
            pstmt.setString(2, userA); // Người bị quẹt là A

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    createMatchRecord(userA, userB);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void createMatchRecord(String userA, String userB) {
        String user1 = userA.compareTo(userB) < 0 ? userA : userB;
        String user2 = userA.compareTo(userB) < 0 ? userB : userA;

        String sql = "INSERT IGNORE INTO matches (user1_username, user2_username) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user1);
            pstmt.setString(2, user2);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject getUserProfile(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToProfile(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean saveMessage(String from, String to, String content, String type) {
        String sql = "INSERT INTO messages (from_username, to_username, content, msg_type) VALUES (?, ?, ?, ?)";
        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, from);
            pstmt.setString(2, to);
            pstmt.setString(3, content);
            pstmt.setString(4, type);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static JSONArray getMatchesForUser(String username) {
        JSONArray matches = new JSONArray();
        String sql = "SELECT user1_username, user2_username FROM matches WHERE user1_username = ? OR user2_username = ?";

        System.out.println("DB DEBUG: Đang tìm match cho " + username); // <--- IN

        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String u1 = rs.getString("user1_username");
                    String u2 = rs.getString("user2_username");

                    // Xác định ai là "người kia"
                    String partnerUsername = u1.equals(username) ? u2 : u1;
                    System.out.println("DB DEBUG: Tìm thấy cặp với " + partnerUsername); // <--- IN

                    // Lấy thông tin chi tiết
                    JSONObject partnerProfile = getUserProfile(partnerUsername);

                    if (partnerProfile != null) {
                        matches.put(partnerProfile);
                        System.out.println("DB DEBUG: -> Đã thêm profile của " + partnerUsername); // <--- IN
                    } else {
                        System.err.println("DB LỖI: Không tìm thấy hồ sơ (Profile) của " + partnerUsername + " trong bảng users!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matches;
    }

    public static boolean updateUserProfile(String username, JSONObject profileData) {
        String sql = "UPDATE users SET full_name = ?, age = ?, gender = ?, seeking = ?, interests = ?, habits = ?, relationship_status = ?, bio = ?, photo1 = ?, photo2 = ?, photo3 = ?, photo4 = ? WHERE username = ?";

        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profileData.getString("full_name"));
            pstmt.setInt(2, profileData.getInt("age"));
            pstmt.setString(3, profileData.getString("gender"));
            pstmt.setString(4, profileData.getString("seeking"));
            pstmt.setString(5, profileData.getString("interests"));
            pstmt.setString(6, profileData.getString("habits"));
            pstmt.setString(7, profileData.getString("relationship_status"));
            pstmt.setString(8, profileData.getString("bio"));
            pstmt.setString(9, profileData.optString("photo1", ""));
            pstmt.setString(10, profileData.optString("photo2", ""));
            pstmt.setString(11, profileData.optString("photo3", ""));
            pstmt.setString(12, profileData.optString("photo4", ""));

            pstmt.setString(13, username);

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Đã cập nhật hồ sơ cho: " + username + ". Seeking mới: " + profileData.getString("seeking"));
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
        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, swiperUsername);
            pstmt.executeUpdate();
            System.out.println("Đã reset lượt bỏ qua cho: " + swiperUsername);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static org.json.JSONArray getChatHistory(String userA, String userB) {
        org.json.JSONArray history = new org.json.JSONArray();
        String sql = "SELECT from_username, content, msg_type, sent_time FROM messages " +
                "WHERE (from_username = ? AND to_username = ?) OR (from_username = ? AND to_username = ?) " +
                "ORDER BY sent_time ASC";

        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userA);
            pstmt.setString(2, userB);
            pstmt.setString(3, userB);
            pstmt.setString(4, userA);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    org.json.JSONObject msg = new org.json.JSONObject();
                    msg.put("from_username", rs.getString("from_username"));
                    msg.put("content", rs.getString("content"));
                    msg.put("type", rs.getString("msg_type")); // Đọc loại tin nhắn
                    history.put(msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }
    public static JSONArray getProfilesByUsernames(List<String> usernames, String currentUsername, boolean filterSwiped) {
        JSONArray profiles = new JSONArray();
        if (usernames.isEmpty()) {
            return profiles;
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM users WHERE username IN (");
        for (int i = 0; i < usernames.size(); i++) {
            sqlBuilder.append("?");
            if (i < usernames.size() - 1) sqlBuilder.append(",");
        }
        sqlBuilder.append(")");

        if (filterSwiped) {
            sqlBuilder.append(" AND username NOT IN (SELECT swiped_username FROM swipes WHERE swiper_username = ?)");
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            for (String username : usernames) {
                pstmt.setString(paramIndex++, username);
            }

            if (filterSwiped) {
                pstmt.setString(paramIndex, currentUsername);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    profiles.put(resultSetToProfile(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return profiles;
    }
    public static JSONArray getPotentialCandidates(String currentUsername, String seekingGender) {
        JSONArray candidates = new JSONArray();
        String sql;

        // Xây dựng câu lệnh SQL dựa trên 'seekingGender'
        if (seekingGender.equals("Khác")) {
            // Lấy tất cả, không phân biệt giới tính
            sql = "SELECT * FROM users WHERE username != ?";
        } else {
            // Chỉ lấy người có giới tính mà user đang tìm
            sql = "SELECT * FROM users WHERE username != ? AND gender = ?";
        }

        // TODO: Tối ưu hơn bằng cách lọc cả những người MÀ 'seeking' CỦA HỌ
        // cũng phù hợp với 'currentUsername' (ví dụ: họ cũng tìm 'Nam')

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUsername);
            if (!seekingGender.equals("Khác")) {
                pstmt.setString(2, seekingGender);
            }

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



    public static String generateRecoveryCode(String username) {
        try (java.sql.PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            checkStmt.setString(1, username);
            if (!checkStmt.executeQuery().next()) return null; // User không tồn tại
        } catch (Exception e) { return null; }

        String code = String.valueOf((int) (Math.random() * 900000) + 100000);

        String sql = "UPDATE users SET recovery_code = ? WHERE username = ?";
        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            return code;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean resetPassword(String username, String code, String newPassword) {

        String sqlCheck = "SELECT id FROM users WHERE username = ? AND recovery_code = ?";
        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {
            pstmt.setString(1, username);
            pstmt.setString(2, code);
            if (!pstmt.executeQuery().next()) return false;
        } catch (Exception e) { return false; }

        String sqlUpdate = "UPDATE users SET password = ?, recovery_code = NULL WHERE username = ?";
        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}