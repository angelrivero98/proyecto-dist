import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class SendMessageThread implements Runnable {

    private String value = null;
    private String message = null;
    private Integer seconds = 10;

    public void setValue(String value) {
        this.value = value;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }


    @Override
    public void run() {

        try {
            if (this.seconds > 0) {
                TimeUnit.SECONDS.sleep(10);
            }
            ClientMessage sendmessage = new ClientMessage();
            sendmessage.startConnection(value.substring(5), new Integer(value.substring(0, 4)));
            sendmessage.sendMessage(this.message);


        } catch (InterruptedException ex) {
            System.out.println("Error (1001): sending -- " + value.substring(5) + " , " + value.substring(0, 4) + " , " + this.message);
            //Logger.getLogger(SendMessageThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            System.out.println("Error (1002): sending -- " + value.substring(5) + " , " + value.substring(0, 4) + " , " + this.message);
            //Logger.getLogger(SendMessageThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
