import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public class Server {

    private ClientsMap map;
    private static final int PORT = 8000;

    public Server() {
        map = new ClientsMap();
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server start. Wait for clients....");
            while(true) {
                Socket socket = serverSocket.accept();
                map.add(socket);
            }
        } catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    public static void main(String[] arg) {
        new Server();
    }
}
