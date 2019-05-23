package com.company;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	// write your code here
        if(args[0].startsWith("server")) {
            try {

                Server serer = new Server();
                serer.start(new Integer(args[1]), args[2]);

            } catch (IOException ex) {
                System.out.println("error server");
                ex.printStackTrace();
                for (String s : args) {
                    System.out.println(s);
                }
                //Logger.getLogger(Taller1sd.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else {
            try {
                //participante

                ClientMessage message = new ClientMessage();
                message.startConnection(args[1], new Integer(args[2]));

                String response = message.sendMessage(args[3]);

                System.out.println("Respuesta server: " + response);

            } catch (IOException ex) {
                System.out.println("error client message");
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }



        }
    }
}
