import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws ArgumentParserException {
        ArgumentParser parser = ArgumentParsers.newFor("Proyecto 1 - Sist. Distribuidos")
                .fromFilePrefix("@").build();

        MutuallyExclusiveGroup group = parser.addMutuallyExclusiveGroup().required(true);
        group.addArgument("-s", "--server").action(Arguments.storeTrue());
        group.addArgument("-m", "--message").action(Arguments.storeTrue());

        parser.addArgument("-n", "--name").dest("storeName").type(String.class);
        parser.addArgument("-i", "--ip").dest("ip").required(true).type(String.class);
        parser.addArgument("-p", "--port").dest("port").required(true).type(Integer.class);
        parser.addArgument("-c", "--content").dest("content").type(String.class);
        try {
            Namespace ns = parser.parseArgs(args);
            System.out.println(ns);
            if (ns.getBoolean("server")) {
                try {
                    Node node = new Node(
                            ns.getString("storeName"),
                            ns.getString("ip"),
                            ns.getInt("port")
                    );
                    node.listen();
                } catch (IOException e) {
                    System.out.println("Error initializing node.");
                }
            } else if (ns.getBoolean("message")) {
                try {
                    NetworkMessage message = new NetworkMessage(
                            new NetworkIdentifier(ns.getString("ip"), ns.getInt("port")),
                            ns.getString("content")
                    );
                    String response = message.send();
                    message.dispose();
                    System.out.println("Respuesta server: " + response);
                    processServerMessage(response);
                } catch (IOException e) {
                    System.out.println("Error enviando mensaje");
                }
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
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No hay instruccion del servidor, terminamos.");
        }
    }
}
