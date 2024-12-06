import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.function.Consumer;


public class Client extends Thread{

    private Socket socketClient;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Consumer<Serializable> callback;
    private String serverIp;
    private int serverPort;

    public Client(Consumer<Serializable> callback, String serverIp, int serverPort) {
        this.callback = callback;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void run() {

        try {

            socketClient = new Socket(serverIp, serverPort);
            in = new ObjectInputStream(socketClient.getInputStream());
            out = new ObjectOutputStream(socketClient.getOutputStream());
            socketClient.setTcpNoDelay(true);
        }
        catch (Exception e) {}

            while(true) {

                try {
                    PokerInfo data = (PokerInfo) in.readObject();
                    callback.accept(data);
                }
                catch(Exception e) {}
            }

    }

    public void send(PokerInfo data) {

        try {
            out.writeObject(data);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

