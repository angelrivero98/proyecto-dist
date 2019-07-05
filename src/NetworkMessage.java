import data.NetworkIdentifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkMessage {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String message;

    public NetworkMessage(NetworkIdentifier target, String message) throws IOException {
        this.clientSocket = new Socket(target.ipAddress, target.port);
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.message = message;
    }

    public String send() throws IOException {
        out.println(this.message);
        return in.readLine();
    }

    public void dispose() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

}
