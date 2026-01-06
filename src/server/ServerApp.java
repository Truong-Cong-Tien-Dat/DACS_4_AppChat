package server;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    public static final int PORT = 12345;
    private static ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    public static void main(String[] args) {

        new ServerDiscoveryThread().start();

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

    static class ServerDiscoveryThread extends Thread {
        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"))) {
                socket.setBroadcast(true);
                System.out.println("Server đang lắng nghe tín hiệu tìm kiếm trên cổng 8888...");

                byte[] recvBuf = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength()).trim();

                    if (message.equals("DISCOVER_DND_CHAT_SERVER")) {

                        String response = "DND_CHAT_SERVER_HERE";
                        byte[] sendData = response.getBytes();

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

}