package client;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class NetworkClient implements Runnable {
    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        String discoveredIP = findServerIP();

        if (discoveredIP != null) {
            this.host = discoveredIP;
            System.out.println("Client: Đang kết nối tới Server tìm thấy tại " + this.host);
        } else {
            System.out.println("Client: Không tìm thấy Server qua LAN. Fallback về " + this.host);
        }
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String jsonResponse;
            while ((jsonResponse = in.readLine()) != null) {
                System.out.println("Server: " + jsonResponse);
                try {
                    JSONObject response = new JSONObject(jsonResponse);
                    if (ClientApp.getInstance() != null) {
                        ClientApp.getInstance().handleServerResponse(response);
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi xử lý JSON: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Client: Không thể kết nối tới Server TCP.");
        }
    }

    public void sendRequest(JSONObject request) {
        if (out != null) {
            out.println(request.toString());
            System.out.println("Client gửi: " + request.toString());
        }
    }

    private String findServerIP() {
        System.out.println("Client: Đang quét mạng LAN tìm Server...");
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(3000);

            byte[] sendData = "DISCOVER_DND_CHAT_SERVER".getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                    InetAddress.getByName("255.255.255.255"), 8888);
            socket.send(sendPacket);

            byte[] recvBuf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(receivePacket);


            String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            if (message.contains("SERVER_IS_HERE") || message.contains("DND_CHAT_SERVER_HERE")) {
                String ip = receivePacket.getAddress().getHostAddress();
                System.out.println("Client: ==> TÌM THẤY SERVER TẠI: " + ip);
                return ip;
            }

        } catch (SocketTimeoutException e) {
            System.out.println("Client: Không tìm thấy Server (Timeout).");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) socket.close();
        }
        return null;
    }
}