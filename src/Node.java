import constants.Alerts;
import constants.Instructions;
import data.*;
import exceptions.NodeShutDownException;
import exceptions.ProductNotFoundException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Node {
    private NetworkIdentifier networkIdentifier;
    private ServerSocket listeningSocket;
    private Store store;
    private HashMap<String, NetworkIdentifier> knownStores = new HashMap<>();
    private List<Product> products;
    private List<Transaction> transactions;

    // Used to only print once that the server is listening on a ip:port
    private boolean initialListening = true;

    public Node(String storeName, String ip, int port) throws IOException {
        store = new Store(storeName);
        readTransactionsFile();
        readProductsFile();
        listeningSocket = new ServerSocket(port);
        networkIdentifier = new NetworkIdentifier(ip, port);
        readKnownStoresFile();
    }

    private static void clear() {
        for (int i = 0; i < 50; ++i) System.out.println();
    }

    public void listen() {
        clear();
        printStatus();
        try {
            Socket clientSocket = listeningSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String receivedMessage = in.readLine();
            processReceivedMessage(clientSocket, receivedMessage);
            in.close();
            clientSocket.close();
            this.listen();
        } catch (IOException e) {
            System.out.println("Error while establishing connection with client");
            this.listen();
        }
    }

    private void processReceivedMessage(Socket sender, String receivedMessage) throws IOException {
        if (receivedMessage == null)
            return;

        PrintWriter senderOutput = new PrintWriter(sender.getOutputStream(), true);
        String[] split = receivedMessage.split("\\$");
        String instruction = split[0];
        if (instruction.equals(Instructions.REGISTER_NODE)) {
            // Message format: register_node${store_name}${ip}${port}
            String storeBeingRegistered = split[1];
            String ipBeingRegistered = split[2];
            int portBeingRegistered = Integer.parseInt(split[3]);
            NetworkIdentifier storeNetworkId = new NetworkIdentifier(ipBeingRegistered, portBeingRegistered);
            if (knownStores.containsKey(storeBeingRegistered)) {
                // No podemos permitir que se registren tiendas con nombres repetidos
                senderOutput.println(Alerts.REPEATED_NAME_ERROR);
                sendMessage(storeNetworkId, Instructions.SHUTDOWN + "$" + Alerts.REPEATED_NAME_ERROR);
            } else {
                System.out.println(String.format("Registrando %s (%s)", storeBeingRegistered, storeNetworkId));
                knownStores.put(storeBeingRegistered, storeNetworkId);
                writeKnownStoresFile();
                broadcast(Instructions.UPDATE_NODE_TABLE + "$" + serializeKnownNodes());
                if (this.products.size() > 0)
                    sendMessage(storeNetworkId, Instructions.UPDATE_PRODUCTS + "$" + serializeProducts());
                // Let the process that sent the message know that it was successfully processed
                senderOutput.println(Alerts.NODE_REGISTERED);
            }
        } else if (instruction.equals(Instructions.UPDATE_NODE_TABLE)) {
            // Message format: update_nodes${serialized_list_of_nodes}
            deserializeNodeListAndUpdate(split[1]);
            writeKnownStoresFile();
        } else if (instruction.equals(Instructions.REGISTER_PRODUCT)) {
            // Message format: register_product${[product_name]#{amount}}
            String productBeingRegistered = split[1];
            String[] splitProduct = productBeingRegistered.split("#");
            Product product = new Product(this.store.getName(), splitProduct[0], Integer.valueOf(splitProduct[1]));
            addProduct(product);
            writeProductsFile();
            broadcast(Instructions.UPDATE_PRODUCTS+"$"+serializeProducts());
            // Let the process that sent the message know that it was successfully processed
            senderOutput.println(Alerts.PRODUCT_REGISTERED);
        } else if (instruction.equals(Instructions.UPDATE_PRODUCTS)) {
            deserializeProductsList(split[1]);
            writeProductsFile();
        } else if (instruction.equals(Instructions.LIST_PRODUCTS_BY_COMPANY)) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Integer> entry : getAccumulatedCompanyProducts().entrySet()) {
                String productCode = entry.getKey();
                Integer productAmountInCompany = entry.getValue();
                builder.append(String.format("%s#%d,", productCode, productAmountInCompany));
            }
            String result = builder.toString();
            // Send an instruction to the sender to let him know that he needs to format the list of products
            senderOutput.println(Instructions.LIST_PRODUCTS_BY_COMPANY + "$" + result);
        } else if (instruction.equals(Instructions.LIST_PRODUCTS_BY_STORE)) {
            String result = serializeProducts();
            senderOutput.println(Instructions.LIST_PRODUCTS_BY_STORE + "$" + result);
        } else if (instruction.equals(Instructions.BUY_PRODUCT)) {
            // Message format: buy_product${product_code}${amount}${client_name}${client_code}
            String productCode = split[1];
            int amountBought = Integer.valueOf(split[2]);
            Client client = new Client(split[4], split[3]);
            try {
                Product product = searchProductByCode(productCode);
                Transaction tx = product.buy(client, amountBought);
                transactions.add(tx);
                writeTransactionsFile();
                writeProductsFile();
                senderOutput.println(client.getName() + " compro exitosamente " + amountBought + " " + productCode);
                // Let the other stores know about your new amount of products
                broadcast(Instructions.UPDATE_PRODUCTS + "$" + serializeProducts());
            } catch (ProductNotFoundException e) {
                senderOutput.println(Alerts.INVALID_PRODUCT_CODE);
            } catch (IllegalArgumentException e) {
                senderOutput.println(Alerts.INVALID_BUY_AMOUNT);
            }
        } else if (instruction.equals(Instructions.UPDATE_PRODUCT)){
            // Message format: update_product${{product_code}#{amount}}
            String productBeingUpdated = split[1];
            String[] splitProduct = productBeingUpdated.split("#");
            try {
                Product product = searchProductByCode(splitProduct[0]);
                product.setAmount(Integer.valueOf(splitProduct[1]));
                senderOutput.println("Se actualizo el producto: " + product.getCode() + " con " + product.getAmount());
                writeProductsFile();
                broadcast(Instructions.UPDATE_PRODUCTS + "$" + serializeProducts());
            } catch (ProductNotFoundException e) {
                senderOutput.println(Alerts.INVALID_PRODUCT_CODE);
            }

        }
        if (instruction.equals(Instructions.LIST_STORE_TRANSACTIONS)) {
            String result = serializeTransactions();
            senderOutput.println(Instructions.LIST_STORE_TRANSACTIONS + "$" + result);
        } else if (instruction.equals(Instructions.SHUTDOWN)) {
            clear();
            throw new NodeShutDownException(split[1]);
        } else {
            System.out.println("Mensaje no reconocido: " + receivedMessage);
        }
        senderOutput.close();
    }

    /**
     * Return a mapping of product codes to amounts, which corresponds to the amount of products
     * in the company (sum of every amount per store).
     */
    private Map<String, Integer> getAccumulatedCompanyProducts() {
        HashMap<String, Integer> productToQuantity = new HashMap<>();
        for (Product p : products) {
            if (productToQuantity.containsKey(p.getCode())) {
                int newAmount = productToQuantity.get(p.getCode()) + p.getAmount();
                productToQuantity.put(p.getCode(), newAmount);
            } else {
                productToQuantity.put(p.getCode(), p.getAmount());
            }
        }
        return productToQuantity;
    }

    /**
     * Receives a serialized list of nodes and deserializes it while updating
     * the table of known nodes.
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
     * Receives a serialized list of products and deserializes it while updating
     * the list of known products.
     */

    private void deserializeProductsList(String serializedList) {
        String[] splitProductsList = serializedList.split(",");
        this.products = new ArrayList<>();
        for(String serializedProduct: splitProductsList){
            String[] productComponents = serializedProduct.split("#");
            Product product = new Product(productComponents[0],productComponents[1],new Integer(productComponents[2]));
            System.out.println(String.format("Actualizado lista de productos | %s %s %s",productComponents[0],productComponents[1],productComponents[2]));
            this.products.add(product);
        }
        System.out.println("------------------------");
    }

    private void deserializeTransactionList(String serializedList) {
        String[] splitTransactionList = serializedList.split(",");
        this.transactions = new ArrayList<>();
        for (String serializedTransaction : splitTransactionList) {
            String[] transactionComponents = serializedTransaction.split("#");
            Transaction transaction = new Transaction(
                    transactionComponents[0],
                    transactionComponents[1],
                    Integer.valueOf(transactionComponents[2]),
                    new Client(transactionComponents[4],
                            transactionComponents[3])
            );
            this.transactions.add(transaction);
        }
        System.out.println("------------------------");
    }

    private void addProduct(Product product) {
        boolean added = false;
        for(Product p: this.products){
            if(p.getCode().equals(product.getCode()) && p.getStore().equals(product.getStore())){
                p.sumAmount(product.getAmount());
                System.out.println(String.format("Producto actualizado | cod : %s cantidad : %s",p.getCode(),p.getAmount()));
                added = true;
            }
        }
        if (!added) {
            System.out.println(String.format("Agregado producto | cod : %s cantidad : %s",product.getCode(),product.getAmount()));
            this.products.add(product);
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

    /**
     * Generates a String containing the data of all the known products in the following format:
     * {store1_name}#{product_code}#{product_amount}, ... ,{storeN_name}#{storeN_network_id}
     * <p>
     * For example: Store1#5#2000,Store2#1#5000
     */
    private String serializeProducts() {
        this.products.sort(Comparator.comparing(Product::getCode));
        StringBuilder resultBuilder = new StringBuilder();
        int i = 0, size = this.products.size();
        for (Product p : this.products) {
            resultBuilder.append(String.format("%s#%s#%s",p.getStore(),p.getCode(),p.getAmount()));
            if(++i != size){
                resultBuilder.append(",");
            }
        }
        return resultBuilder.toString();
    }

    private void writeProductsFile() throws IOException {
        FileWriter write = new FileWriter(this.store.getName() + "-products.txt", false);
        PrintWriter print = new PrintWriter(write);
        print.println(serializeProducts());
        print.close();
    }

    private void writeTransactionsFile() throws IOException {
        FileWriter write = new FileWriter(this.store.getName() + "-transactions.txt", false);
        PrintWriter print = new PrintWriter(write);
        print.println(serializeTransactions());
        print.close();
    }

    private void writeKnownStoresFile() throws IOException {
        FileWriter write = new FileWriter(this.store.getName() + "-knowStores.txt", false);
        PrintWriter print = new PrintWriter(write);
        print.println(serializeKnownNodes());
        print.close();
    }

    private void readKnownStoresFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.store.getName() + "-knowStores.txt"));
            String st = br.readLine();
            deserializeNodeListAndUpdate(st);
            br.close();
        } catch (IOException e) {
            knownStores.put(store.getName(), networkIdentifier);
        }
    }

    private void readProductsFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.store.getName() + "-products.txt"));
            String st = br.readLine();
            System.out.println(st);
            deserializeProductsList(st);
            br.close();
        } catch (IOException e) {
            this.products = new ArrayList<>();
        }
    }

    private void readTransactionsFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(store.getName() + "-transactions.txt"));
            String transactions = br.readLine();
            deserializeTransactionList(transactions);
            br.close();
        } catch (IOException e) {
            this.transactions = new ArrayList<>();
        }
    }

    /**
     * Generates a String containing the data of all the known transactions in the following format:
     * {store_name}#{product_code}#{product_amount}#{client_name}#{client_code}, ...
     * <p>
     * For example: Store1#WiPod#50#Juan C#001, Store1#WiPod#20#Someone#002
     */
    private String serializeTransactions() {
        this.transactions.sort(Comparator.comparing(transaction -> transaction.getClient().getCode()));
        StringBuilder resultBuilder = new StringBuilder();
        int i = 0, size = this.transactions.size();
        for (Transaction t : this.transactions) {
            resultBuilder.append(
                    String.format("%s#%s#%d#%s#%s",
                            this.store.getName(),
                            t.getProductCode(),
                            t.getAmountBought(),
                            t.getClient().getName(),
                            t.getClient().getCode()
                    )
            );
            if (++i != size) {
                resultBuilder.append(",");
            }
        }
        return resultBuilder.toString();
    }

    private Product searchProductByCode(String code) {
        for (Product product : products) {
            if (product.getCode().equals(code))
                return product;
        }

        throw new ProductNotFoundException();
    }

    private void printStatus() {
        System.out.println(store.getName() + " - Known nodes");
        System.out.println("---------------------------------------------");
        System.out.println(String.format("| %15s | %15s | %5s |", "Name", "IP Address", "Port"));
        for (Map.Entry<String, NetworkIdentifier> node : knownStores.entrySet()) {
            System.out.println(String.format("| %15s | %15s | %5d |",
                    node.getKey(),
                    node.getValue().ipAddress,
                    node.getValue().port));
        }
        System.out.println("---------------------------------------------");
        System.out.println();
        System.out.println("Inventario");
        System.out.println("-------------------------------------------------------");
        System.out.println(String.format("| %15s | %15s | %15s |", "Codigo", "Tienda", "Quantity"));
        for (Product p : products) {
            System.out.println(String.format("| %15s | %15s | %15d |",
                    p.getCode(),
                    p.getStore(),
                    p.getAmount())
            );
        }
        System.out.println("-------------------------------------------------------");

        System.out.println();
        System.out.println("Transacciones");
        System.out.println("-------------------------------------------------------------------------------------------");
        System.out.println(String.format("| %15s | %15s | %15s | %15s | %15s |", "Tienda", "Codigo usuario", "Nombre usuario", "Codigo prod", "Cantidad"));
        for (Transaction tx : transactions) {
            System.out.println(String.format("| %15s | %15s | %15s | %15s | %15s |",
                    tx.getStoreName(),
                    tx.getClient().getCode(),
                    tx.getClient().getName(),
                    tx.getProductCode(),
                    tx.getAmountBought())
            );
        }
        System.out.println("-------------------------------------------------------------------------------------------");
    }
}
