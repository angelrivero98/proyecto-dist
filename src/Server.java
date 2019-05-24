import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private HashMap<String, String> tiendas = new HashMap<String, String>();
    private String name = "";

    public void start(int port, String name) throws IOException{
        System.out.println(name+" escuchando en el puerto "+port);
        this.name = name;
        this.tiendas.put(name,port+"#"+"127.0.0.1");
        serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();
        out = new PrintWriter(clientSocket.getOutputStream(),true );
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String greeting = in.readLine();
        if(greeting.startsWith("RegistrarTienda")){
            String[] msg = greeting.split("#");
            String tienda = msg[1];
            String puerto = msg[2];
            String ip = msg[3];

            this.tiendas.put(tienda, puerto+"#"+ip);

            for (Map.Entry<String, String> entry : tiendas.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (!key.equals(this.name)) {

                    ClientMessage sendmessage = new ClientMessage();
                    sendmessage.startConnection(value.substring(5), new Integer(value.substring(0, 4)));
                    sendmessage.sendMessage("ActualizarLista" + this.serializarLista());

                }

            }
            out.println("agregadatienda");

        } else if (greeting.startsWith("ActualizarLista")) {

            String lista = greeting.substring("ActualizarLista,".length());

            String[] listatmp = lista.split(",");
            this.tiendas = new HashMap<String, String>();
            for (String tmp : listatmp) {
                String[] finaltmp = tmp.split("#");
                this.tiendas.put(finaltmp[1], finaltmp[2]+"#"+finaltmp[3]);
            }
            System.out.println("se agrego la tienda ");
        }

        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
        for(Map.Entry<String, String> entry : tiendas.entrySet()){
            System.out.println(entry.getKey()+ " "+entry.getValue());
        }
        this.start(port, name);
    }

    private String serializarLista() {

        String finallista = "";

        for (Map.Entry<String, String> entry : tiendas.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            finallista += ",#"+key + "#" + value;

        }

        return finallista;
    }


}
