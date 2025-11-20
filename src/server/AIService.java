package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIService {

    /**
     * Lấy danh sách gợi ý các username cho một user cụ thể.
     * Logic: Content-Based Filtering (Lọc dựa trên nội dung).
     */
    // THAY THẾ HÀM CŨ BẰNG HÀM MỚI NÀY TRONG AIService.java

    public static List<String> getRecommendations(String username) {
        System.out.println("AI: Bắt đầu tính toán gợi ý (2-way) cho: " + username);

        // 1. Lấy hồ sơ của user hiện tại
        JSONObject myProfile = DatabaseService.getUserProfile(username);
        if (myProfile == null) {
            return Collections.emptyList();
        }

        // Lấy thông tin quan trọng cho matching 2 chiều
        String myGender = myProfile.optString("gender", "Khác");
        String mySeeking = myProfile.optString("seeking", "Khác");
        List<String> myInterests = parseInterests(myProfile.optString("interests", ""));
        int myAge = myProfile.optInt("age", 25);

        // 2. Lấy TẤT CẢ các ứng cử viên tiềm năng (đã lọc 2 chiều)
        JSONArray candidates = DatabaseService.getPotentialCandidates(username, myGender, mySeeking);
        System.out.println("AI: Tìm thấy " + candidates.length() + " ứng cử viên (đã lọc 2 chiều).");

        // 3. Tính điểm tương đồng (cho sở thích, thói quen - phần phụ)
        Map<String, Double> scores = new HashMap<>();
        for (int i = 0; i < candidates.length(); i++) {
            JSONObject candidateProfile = candidates.getJSONObject(i);
            String candidateUsername = candidateProfile.getString("username");

            // Bất kỳ ai qua được vòng lọc SQL đều xứng đáng được hiển thị
            double score = 1.0;

            // Tính điểm sở thích chung
            List<String> candidateInterests = parseInterests(candidateProfile.optString("interests", ""));
            long commonInterests = candidateInterests.stream()
                    .filter(interest -> !interest.isEmpty() && myInterests.contains(interest))
                    .count();
            score += commonInterests * 0.5; // Sở thích/thói quen chỉ là điểm cộng

            // (Bạn có thể thêm điểm trừ cho tuổi nếu muốn)

            scores.put(candidateUsername, score);
        }

        // 4. Sắp xếp danh sách (người có nhiều điểm chung hơn sẽ lên đầu)
        List<String> sortedUsernames = new ArrayList<>(scores.keySet());
        sortedUsernames.sort((a, b) -> scores.get(b).compareTo(scores.get(a)));

        System.out.println("AI: Đã tính toán xong. Gợi ý hàng đầu: " + (sortedUsernames.isEmpty() ? "Không có" : sortedUsernames.get(0)));

        return sortedUsernames;
    }
    /**
     * Helper: Tách chuỗi sở thích "Java,Đá bóng,Đọc sách" thành một List
     */
    private static List<String> parseInterests(String interests) {
        if (interests == null || interests.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // Tách bằng dấu phẩy và xóa khoảng trắng thừa
        return new ArrayList<>(Arrays.asList(interests.split("\\s*,\\s*")));
    }
}