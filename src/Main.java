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
                // Examples:
                // Register node: send_message 192.168.1.57 9000 register_node$Tienda2$192.168.1.57$9500
                // List products: send_message 192.168.1.57 9000 list_company_products$
                // List products by store: send_message 192.168.1.57 9000 list_store_products$
                String serverIp = args[1];
                Integer serverPort = new Integer(args[2]);
                NetworkIdentifier target = new NetworkIdentifier(serverIp, serverPort);
                NetworkMessage message = new NetworkMessage(target, args[3]);
                String response = message.send();
                System.out.println("Respuesta server: " + response);
                try {
                    String[] split = response.split("\\$");
                    String serverInstruction = split[0];
                    if (serverInstruction.equals(Instructions.LIST_PRODUCTS_BY_COMPANY)) {
                        System.out.println("Productos de compañia");
                        System.out.println(String.format("| %10s | %10s |", "Codigo", "Cantidad"));
                        String[] serializedProducts = split[1].split(",");
                        for (String serializedProduct : serializedProducts) {
                            String[] components = serializedProduct.split("#");
                            System.out.println(String.format("| %10s | %10s |", components[0], components[1]));
                        }
                    } else if (serverInstruction.equals(Instructions.LIST_PRODUCTS_BY_STORE)){
                        System.out.println("Productos de compañia por tienda");
                        String[] serializedProducts = split[1].split(",");
                        for (String serializedProduct : serializedProducts) {
                            System.out.println(serializedProduct);
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("No hay instruccion del servidor, terminamos.");
                }

                message.dispose();
            } catch (IOException ex) {
                // TODO: Mensaje de error mas descriptivo
                System.out.println("error client message");
            }
        }
    }
}
