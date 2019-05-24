import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // Argument order: start_node {ip} {port} {store_name}
        if (args[0].equals("start_node")) {
            try {
                String ipAddress = args[1];
                Integer port = new Integer(args[2]);
                String storeName = args[3];
                Node node = new Node(storeName, ipAddress, port);
                node.listen();
            } catch (IOException ex) {
                System.out.println("Error initializing node.");
                System.out.println("Received args: ");
                for (String arg : args) {
                    System.out.println(arg);
                }
                ex.printStackTrace();
            }
        } else {
            try {
                // Argument order: send_message {ip} {port} {message}
                // For example: send_message 192.168.1.57 9000 register_node$Tienda2$192.168.1.57$9500
                String serverIp = args[1];
                Integer serverPort = new Integer(args[2]);
                NetworkIdentifier target = new NetworkIdentifier(serverIp, serverPort);
                NetworkMessage message = new NetworkMessage(target, args[3]);
                String response = message.send();
                System.out.println("Respuesta server: " + response);
                message.dispose();
            } catch (IOException ex) {
                // TODO: Mensaje de error mas descriptivo
                System.out.println("error client message");
            }
        }
    }
}
