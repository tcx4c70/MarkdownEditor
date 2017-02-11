import java.math.BigInteger;
import java.net.Socket;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public class HandleAClient {
    private Socket socket;
    private BigInteger hash;
    private Receiver receiver;
    private Sender sender;

    public HandleAClient(Socket socket) {
        this.socket = socket;
        this.hash = null;
        receiver = new Receiver(socket, hash, this);
        sender = new Sender(socket);
        receiver.start();
        sender.start();
    }

    public HandleAClient(Socket socket, BigInteger hash) {
        this.socket = socket;
        this.hash = hash;
        receiver = new Receiver(socket, hash, this);
        sender = new Sender(socket);
        receiver.start();
        sender.start();
    }

    public void setHash(BigInteger hash) {
        receiver.setHash(hash);
        this.hash = hash;
    }

    public BigInteger getHash() {
        return hash;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public Sender getSender() {
        return sender;
    }

    public void send(Action action) {
        sender.send(action);
    }

    public void disconnect() {
        receiver.kill();
        sender.kill();
    }
}
