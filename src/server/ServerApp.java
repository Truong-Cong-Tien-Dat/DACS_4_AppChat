package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    public static final int PORT = 12345;
    // Dùng ConcurrentHashMap để lưu trữ an toàn các ClientHandler
    // Key: username, Value: ClientHandler
    private static ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Sử dụng một Thread Pool để quản lý các luồng hiệu quả
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chạy tại cổng " + PORT + "...");
            DatabaseService.connect(); // Kết nối CSDL khi server khởi động

            while (true) {
                // Chấp nhận kết nối mới
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới kết nối: " + clientSocket.getInetAddress());

                // Tạo và chạy một ClientHandler mới cho client này
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Phương thức để thêm client vào danh sách (khi đăng nhập thành công)
    public static void addClient(String username, ClientHandler handler) {
        activeClients.put(username, handler);
        System.out.println("User '" + username + "' đã đăng nhập.");
    }

    // Phương thức để xóa client (khi đăng xuất hoặc mất kết nối)
    public static void removeClient(String username) {
        if (username != null) {
            activeClients.remove(username);
            System.out.println("User '" + username + "' đã ngắt kết nối.");
        }
    }

    // Phương thức gửi tin nhắn cho một user cụ thể (nếu họ đang online)
    public static void sendMessageToUser(String username, String jsonMessage) {
        ClientHandler handler = activeClients.get(username);
        if (handler != null) {
            handler.sendMessage(jsonMessage);
        }
    }
}