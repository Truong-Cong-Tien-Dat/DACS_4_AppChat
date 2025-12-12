package server;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    public static final int PORT = 12345;
    // Danh sách client online
    private static ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // 1. Bật tính năng để Client tự tìm thấy Server (UDP)
        new ServerDiscoveryThread().start();

        // 2. Chạy Server chính (TCP)
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chạy tại cổng " + PORT + "...");
            System.out.println("Đang chờ kết nối...");

            if (DatabaseService.connect()) {
                System.out.println("Kết nối CSDL thành công!");
            }

            ExecutorService pool = Executors.newFixedThreadPool(100);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- CÁC HÀM QUẢN LÝ CLIENT (Phải nằm TRONG class ServerApp) ---

    public static void addClient(String username, ClientHandler handler) {
        activeClients.put(username, handler);
        System.out.println("User '" + username + "' đã đăng nhập.");
    }

    public static void removeClient(String username) {
        if (username != null) {
            activeClients.remove(username);
            System.out.println("User '" + username + "' đã ngắt kết nối.");
        }
    }

    public static void sendMessageToUser(String username, String jsonMessage) {
        ClientHandler handler = activeClients.get(username);
        if (handler != null) {
            handler.sendMessage(jsonMessage);
        }
    }

    // --- CLASS CON XỬ LÝ UDP BROADCAST ---
    static class ServerDiscoveryThread extends Thread {
        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"))) {
                socket.setBroadcast(true);
                System.out.println("[UDP] Server đang lắng nghe tín hiệu tìm kiếm trên cổng 8888...");

                byte[] recvBuf = new byte[1024];
                while (true) {
                    // 1. Chờ tin nhắn từ Client
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(packet);

                    // 2. Kiểm tra mật khẩu ám hiệu
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();

                    // Lưu ý: Chuỗi này phải khớp với bên Client gửi lên
                    if (message.equals("DISCOVER_DND_CHAT_SERVER")) {
                        // System.out.println("[UDP] Nhận tín hiệu tìm kiếm từ: " + packet.getAddress().getHostAddress());

                        // 3. Trả lời lại
                        String response = "DND_CHAT_SERVER_HERE";
                        byte[] sendData = response.getBytes();

                        // Gửi ngược lại cho người gọi
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                packet.getAddress(), packet.getPort());
                        socket.send(sendPacket);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

} // <--- DẤU ĐÓNG CỦA CLASS ServerApp NẰM Ở CUỐI CÙNG MỚI ĐÚNG