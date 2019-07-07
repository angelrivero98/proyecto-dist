import constants.Instructions;
import data.NetworkIdentifier;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws ArgumentParserException {
        ArgumentParser parser = ArgumentParsers.newFor("Proyecto 1 - Sist. Distribuidos")
                .fromFilePrefix("@").build();

        MutuallyExclusiveGroup group = parser.addMutuallyExclusiveGroup().required(true);
        group.addArgument("-s", "--server").action(Arguments.storeTrue());
        group.addArgument("-m", "--message").action(Arguments.storeTrue());
        group.addArgument("-mm", "--menu").action(Arguments.storeTrue());

        parser.addArgument("-n", "--name").dest("storeName").type(String.class);
        parser.addArgument("-i", "--ip").dest("ip").type(String.class);
        parser.addArgument("-p", "--port").dest("port").type(Integer.class);
        parser.addArgument("-c", "--content").dest("content").type(String.class);
        try {
            Namespace ns = parser.parseArgs(args);
            System.out.println(ns);
            if (ns.getBoolean("server")) {
                nodeSetup(ns.getString("storeName"),ns.getString("ip"),ns.getInt("port"));
            } else if (ns.getBoolean("message")) {
                networkMessageSetup(ns.getString("ip"),ns.getInt("port"),ns.getString("content"));
                System.out.println(ns.getString("content"));
            } else if (ns.getBoolean("menu")) {
                menu();
            }

        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }
    }

    private static void processServerMessage(String message) {
        try {
            String[] split = message.split("\\$");
            String serverInstruction = split[0];
            if (serverInstruction.equals(Instructions.LIST_PRODUCTS_BY_COMPANY)) {
                System.out.println("Productos de compañia");
                System.out.println(String.format("| %10s | %10s |", "Codigo", "Cantidad"));
                String[] serializedProducts = split[1].split(",");
                for (String serializedProduct : serializedProducts) {
                    String[] components = serializedProduct.split("#");
                    System.out.println(String.format("| %10s | %10s |", components[0], components[1]));
                }
            } else if (serverInstruction.equals(Instructions.LIST_PRODUCTS_BY_STORE)) {
                System.out.println("Productos de compañia por tienda");
                System.out.println(String.format("| %15s | %10s | %10s |", "Tienda", "Codigo", "Cantidad"));
                String[] serializedProducts = split[1].split(",");
                for (String serializedProduct : serializedProducts) {
                    String[] components = serializedProduct.split("#");
                    System.out.println(String.format("| %15s | %10s | %10s |", components[0], components[1], components[2]));
                }
            } else if (serverInstruction.equals(Instructions.LIST_STORE_TRANSACTIONS)){
                System.out.println("Transacciones de clientes");
                System.out.println(String.format("| %15s | %10s | %10s | %10s | %10s |", "Tienda", "Producto", "Cantidad","Cliente","Codigo Cliente"));
                String[] serializedProducts = split[1].split(",");
                for (String serializedProduct : serializedProducts) {
                    String[] components = serializedProduct.split("#");
                    System.out.println(String.format("| %15s | %10s | %10s | %10s | %10s |", components[0], components[1], components[2],components[3],components[4]));
                }
            }
        } catch (Exception e) {
            System.out.println(message);

        }
    }

    private static void nodeSetup(String store, String ip, int port){
        try {
            Node node = new Node(
                    store,
                    ip,
                    port
            );
            node.listen();
        } catch (IOException e) {
            System.out.println("Error initializing node.");
        }
    }

    private static void networkMessageSetup(String ip, int port,String content){
        try {
            NetworkMessage message = new NetworkMessage(
                    new NetworkIdentifier(ip, port),
                    content
            );
            String response = message.send();
            message.dispose();
            System.out.println("Respuesta server: " + response);
            processServerMessage(response);
        } catch (IOException e) {
            System.out.println("Error enviando mensaje");
        }
    }


    private static void menu(){
        Scanner sn = new Scanner(System.in);
        boolean exit = false;
        int option;
        String messageContent;
        HashMap<String, String> parameters = new HashMap<String, String>();
//        parameters.put("ipConnection", "");
//        parameters.put("portConnection", "");
//        parameters.put("ipDestination", "");
//        parameters.put("portDestination", "");
//        parameters.put("store", "");
//        parameters.put("productName", "");
        System.out.println("IP para conectarse");
        parameters.put("ipConnection",sn.next());
        System.out.println("Puerto para conectarse");
        parameters.put("portConnection",sn.next());
        while (!exit) {
            System.out.println("1. Levantar servidor");
            System.out.println("2. Agregar tienda al sistema");
            System.out.println("3. Cargar producto");
            System.out.println("4. Actualizar cantidad disponible de inventario"); //Falta hacer
            System.out.println("5. Registrar compra");
            System.out.println("6. Listar productos de la empresa");
            System.out.println("7. Listar productos de la empresa por tienda");
            System.out.println("8. Listar compras de clientes");
            System.out.println("9. Conectarse a otro servidor");
            System.out.println("10. Salir");

            try {
                System.out.println("Escribe una de las opciones");
                option = sn.nextInt();

                switch (option) {
                    case 1:
                        System.out.println("Nombre de tienda");
                        parameters.put("store",sn.next());
                        nodeSetup(parameters.get("store"),parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")));
                        break;
                    case 2:
                        //"register_node$Store2$127.0.0.1$7002$"
                        System.out.println("Nombre de tienda");
                        parameters.put("store",sn.next());
                        System.out.println("IP de la tienda a registrar");
                        parameters.put("ipDestination",sn.next());
                        System.out.println("Puerto de la tienda a registrar");
                        parameters.put("portDestination",sn.next());
                        messageContent = String.format("register_node$%s$%s$%d$",parameters.get("store"),parameters.get("ipDestination"),
                                        Integer.parseInt(parameters.get("portDestination")));
                        System.out.println(messageContent);
                        networkMessageSetup(parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")),messageContent);
                        break;
                    case 3:
                        //"register_product$WiPod#50"
                        System.out.println("Nombre del producto");
                        parameters.put("productName", sn.next());
                        System.out.println("Cantidad");
                        parameters.put("quantity", sn.next());
                        messageContent = String.format("register_product$%s#%d",parameters.get("productName"),Integer.parseInt(parameters.get("quantity")));
                        networkMessageSetup(parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")),messageContent);
                        break;
                    case 4:
                        //"update_product$001#5"
                        System.out.println("Nombre del producto");
                        parameters.put("productName",sn.next());
                        System.out.println("Cantidad para actualizar");
                        parameters.put("quantity",sn.next());
                        messageContent = String.format("update_product$%s#%d",parameters.get("productName"),Integer.parseInt(parameters.get("quantity")));
                        networkMessageSetup(parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")),messageContent);
                        break;
                    case 5:
                        //"buy_product$WiPod$5$Juan C.$0001"
                        System.out.println("Codigo del producto");
                        parameters.put("productCode",sn.next());
                        System.out.println("Cantidad a comprar");
                        parameters.put("quantity",sn.next());
                        System.out.println("Nombre del cliente");
                        parameters.put("clientName",sn.next());
                        System.out.println("Codigo del cliente");
                        parameters.put("clientCode",sn.next());
                        messageContent = String.format("buy_product$%s$%d$%s$%s",parameters.get("productCode"),Integer.parseInt(parameters.get("quantity")),
                                            parameters.get("clientName"),parameters.get("clientCode"));
                        networkMessageSetup(parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")),messageContent);
                        break;
                    case 6:
                        messageContent = "list_company_products$";
                        networkMessageSetup(parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")),messageContent);
                        break;
                    case 7:
                        messageContent = "list_store_products$";
                        networkMessageSetup(parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")),messageContent);
                        break;
                    case 8:
                        messageContent = "list_your_transactions$";
                        networkMessageSetup(parameters.get("ipConnection"),Integer.parseInt(parameters.get("portConnection")),messageContent);
                        break;
                    case 9:
                        System.out.println("IP para conectarse");
                        parameters.put("ipConnection",sn.next());
                        System.out.println("Puerto para conectarse");
                        parameters.put("portConnection",sn.next());
                        break;
                    case 10:
                        exit = true;
                        break;
                    default:
                        System.out.println("Solo números entre 1 y 10");
                }
            } catch (InputMismatchException e) {
                System.out.println("Debes insertar un número");
                sn.next();
            }
        }
    }

}
