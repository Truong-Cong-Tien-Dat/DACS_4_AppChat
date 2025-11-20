package client;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String jsonResponse;
            while ((jsonResponse = in.readLine()) != null) {
                System.out.println("Server: " + jsonResponse);
                JSONObject response = new JSONObject(jsonResponse);

                // Gọi về ClientApp để cập nhật giao diện
                if (ClientApp.getInstance() != null) {
                    ClientApp.getInstance().handleServerResponse(response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendRequest(JSONObject request) {
        if (out != null) {
            out.println(request.toString());
            System.out.println("Client gửi: " + request.toString());
        }
    }
}