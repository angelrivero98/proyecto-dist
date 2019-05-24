import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Node {
    private NetworkIdentifier networkIdentifier;
    private ServerSocket listeningSocket;
    private Store store;
    private HashMap<String, NetworkIdentifier> knownStores = new HashMap<>();
    // Used to only print once that the server is listening on a ip:port
    private boolean initialListening = true;

    public Node(String storeName, String ip, int port) throws IOException {
        store = new Store(storeName);
        listeningSocket = new ServerSocket(port);
        networkIdentifier = new NetworkIdentifier(ip, port);
        knownStores.put(storeName, networkIdentifier);
    }

    public void listen() {
        if (initialListening) {
            System.out.println(
                    String.format("Tienda %s escuchando en %s:%d",
                            store.getName(),
                            networkIdentifier.ipAddress,
                            networkIdentifier.port)
            );
            initialListening = false;
        }
        try {
            Socket clientSocket = listeningSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String receivedMessage = in.readLine();
            processReceivedMessage(clientSocket, receivedMessage);
            in.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error while establishing connection with client");
        } finally {
            this.listen();
        }
    }

    private void processReceivedMessage(Socket sender, String receivedMessage) throws IOException {
        if (receivedMessage == null)
            return;

        PrintWriter senderOuput = new PrintWriter(sender.getOutputStream(), true);
        String[] split = receivedMessage.split("\\$");
        String instruction = split[0];
        if (instruction.equals(Instructions.REGISTER_NODE)) {
            // Message format: register_node${store_name}${ip}${port}
            String storeBeingRegistered = split[1];
            String ipBeingRegistered = split[2];
            Integer portBeingRegistered = new Integer(split[3]);
            NetworkIdentifier storeNetworkId = new NetworkIdentifier(ipBeingRegistered, portBeingRegistered);
            System.out.println(String.format("Registrando %s (%s)", storeBeingRegistered, storeNetworkId));
            knownStores.put(storeBeingRegistered, storeNetworkId);
            broadcast(Instructions.UPDATE_NODE_TABLE + "$" + serializeKnownNodes());
            // Let the process that sent the message know that it was successfully processed
            senderOuput.println(Instructions.NODE_REGISTERED);
        } else if (instruction.equals(Instructions.UPDATE_NODE_TABLE)) {
            // Message format: update_nodes${serialized_list_of_nodes}
            deserializeNodeListAndUpdate(split[1]);
        }
        senderOuput.close();
    }

    /**
     * Receives a serialized list of nodes and deserializes it while updating
     * the table of know nodes.
     */
    private void deserializeNodeListAndUpdate(String serializedList) {
        String[] splitStoreList = serializedList.split(",");
        for (String serializedStore : splitStoreList) {
            String[] storeComponents = serializedStore.split("#");
            String storeName = storeComponents[0];
            NetworkIdentifier storeNetworkId = new NetworkIdentifier(storeComponents[1]);
            System.out.println(String.format("Actualizando lista de nodos | %s (%s)", storeName, storeNetworkId));
            knownStores.put(storeName, storeNetworkId);
        }
    }

    /**
     * Wrapper to send a message when you dont care about the response.
     */
    private void sendMessage(NetworkIdentifier storeNetworkId, String content) throws IOException {
        NetworkMessage networkMessage = new NetworkMessage(storeNetworkId, content);
        networkMessage.send();
        networkMessage.dispose();
    }

    /**
     * Sends a message to every known node except itself.
     */
    private void broadcast(String message) {
        for (Map.Entry<String, NetworkIdentifier> entry : knownStores.entrySet()) {
            String storeName = entry.getKey();
            NetworkIdentifier storeNetworkId = entry.getValue();
            if (!storeName.equals(this.store.getName())) {
                try {
                    sendMessage(storeNetworkId, message);
                } catch (IOException e) {
                    System.out.println("Error enviando mensaje a " + storeNetworkId);
                    // TODO: Aqui podria ir algo relacionado a tolerancia a fallos
                }
            }
        }
    }

    /**
     * Generates a String containing the data of all the known nodes in the following format:
     * {store1_name}#{store1_network_id}, ... ,{storeN_name}#{storeN_network_id}
     * <p>
     * For example: Store1#127.0.0.1:9000,Store2#192.168.1.2:8732
     */
    private String serializeKnownNodes() {
        StringBuilder resultBuilder = new StringBuilder();
        int i = 0, size = knownStores.size();
        for (Map.Entry<String, NetworkIdentifier> entry : knownStores.entrySet()) {
            String storeName = entry.getKey();
            NetworkIdentifier storeNetworkId = entry.getValue();
            resultBuilder.append(String.format("%s#%s", storeName, storeNetworkId));
            if (++i != size)
                resultBuilder.append(",");
        }
        return resultBuilder.toString();
    }
}