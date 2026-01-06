package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class AIService {

    // Trọng số cho các yếu tố (Có thể điều chỉnh để demo kết quả khác nhau)
    private static final double WEIGHT_INTERESTS = 0.7; // Sở thích chiếm 70% quyết định
    private static final double WEIGHT_AGE = 0.3;       // Tuổi tác chiếm 30%

    public static List<String> getRecommendations(String username) {
        System.out.println("AI [Content-Based]: Bắt đầu tính toán Vector tương đồng cho: " + username);

        // 1. Lấy hồ sơ (Feature Vector) của user hiện tại
        JSONObject myProfile = DatabaseService.getUserProfile(username);
        if (myProfile == null) return Collections.emptyList();

        // 2. Lấy danh sách ứng viên thô (Candidate Generation)
        // Bước này vẫn dùng SQL để lọc cứng (Hard Filter) các tiêu chí bắt buộc như Giới tính/Seeking
        String myGender = myProfile.optString("gender", "Khác");
        String mySeeking = myProfile.optString("seeking", "Khác");
        JSONArray candidates = DatabaseService.getPotentialCandidates(username, myGender, mySeeking);

        // 3. Tính điểm Similarity (Ranking)
        Map<String, Double> scores = new HashMap<>();

        // Chuẩn bị Vector đặc trưng cho User hiện tại
        Set<String> myFeatures = extractFeatures(myProfile);
        int myAge = myProfile.optInt("age", 25);

        for (int i = 0; i < candidates.length(); i++) {
            JSONObject candidate = candidates.getJSONObject(i);
            String candidateUsername = candidate.getString("username");

            // A. Tính điểm tương đồng về nội dung (Cosine Similarity)
            Set<String> candidateFeatures = extractFeatures(candidate);
            double contentScore = calculateCosineSimilarity(myFeatures, candidateFeatures);

            // B. Tính điểm phù hợp về tuổi (Age Proximity Score)
            // Công thức: 1.0 - (Khoảng cách tuổi / 100). Càng gần tuổi càng cao.
            int candidateAge = candidate.optInt("age", 25);
            double ageScore = 1.0 - (Math.abs(myAge - candidateAge) / 100.0);

            // C. Tổng hợp điểm (Weighted Sum)
            double finalScore = (contentScore * WEIGHT_INTERESTS) + (ageScore * WEIGHT_AGE);

            scores.put(candidateUsername, finalScore);

            // Log để debug/show cho thầy xem quá trình tính toán
            System.out.printf("User: %-10s | Cosine: %.3f | AgeScore: %.3f | Final: %.3f%n",
                    candidateUsername, contentScore, ageScore, finalScore);
        }

        // 4. Sắp xếp danh sách theo điểm số từ cao xuống thấp
        List<String> sortedUsernames = new ArrayList<>(scores.keySet());
        sortedUsernames.sort((a, b) -> scores.get(b).compareTo(scores.get(a)));

        return sortedUsernames;
    }

    // --- CÁC HÀM HỖ TRỢ THUẬT TOÁN ---

    /**
     * Trích xuất đặc trưng (Feature Extraction)
     * Biến đổi dữ liệu text (sở thích, thói quen) thành tập hợp từ khóa
     */
    private static Set<String> extractFeatures(JSONObject profile) {
        Set<String> features = new HashSet<>();
        // Gộp cả sở thích và thói quen vào chung một "túi từ" (Bag of Words)
        addKeywords(features, profile.optString("interests", ""));
        addKeywords(features, profile.optString("habits", ""));
        // Có thể thêm Bio vào đây nếu muốn AI đọc cả Bio
        return features;
    }

    private static void addKeywords(Set<String> features, String text) {
        if (text == null || text.trim().isEmpty()) return;
        // Tách chuỗi, bỏ dấu câu, chuyển về chữ thường
        String[] words = text.toLowerCase().split("[,\\.\\s]+");
        for (String w : words) {
            if (w.length() > 2) { // Chỉ lấy từ có nghĩa (độ dài > 2)
                features.add(w.trim());
            }
        }
    }

    /**
     * THUẬT TOÁN COSINE SIMILARITY
     * Tính góc giữa 2 vector sở thích.
     * Kết quả từ 0.0 (không giống) đến 1.0 (giống hệt)
     */
    private static double calculateCosineSimilarity(Set<String> vectorA, Set<String> vectorB) {
        if (vectorA.isEmpty() || vectorB.isEmpty()) return 0.0;

        // 1. Tạo không gian vector chung (Union)
        Set<String> allFeatures = new HashSet<>(vectorA);
        allFeatures.addAll(vectorB);

        // 2. Tính Dot Product (Tích vô hướng)
        double dotProduct = 0.0;

        // 3. Tính Magnitude (Độ dài vector)
        double normA = 0.0;
        double normB = 0.0;

        // Vì đây là vector nhị phân (có/không), ta có thể tính nhanh:
        // Dot Product = Số lượng phần tử trùng nhau (Intersection)
        Set<String> intersection = new HashSet<>(vectorA);
        intersection.retainAll(vectorB);
        dotProduct = intersection.size();

        // Độ dài vector = Căn bậc 2 của số lượng phần tử
        normA = Math.sqrt(vectorA.size());
        normB = Math.sqrt(vectorB.size());

        if (normA == 0 || normB == 0) return 0.0;

        // Công thức Cosine = (A . B) / (||A|| * ||B||)
        return dotProduct / (normA * normB);
    }
}