package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    public static final int PORT = 12345;
    private static ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chạy tại cổng " + PORT + "...");
            DatabaseService.connect();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới kết nối: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        } catch (Exception e) {
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
}