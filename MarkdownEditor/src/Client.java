import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

/**
 * Created by TCX4C70 on 16/12/29 029.
 */
public class Client {
    private Socket socket;
    private BigInteger hash;
    private Sender sender;
    private Receiver receiver;

    public Client() {
        this.socket = null;
        this.hash = null;
        this.sender = null;
        this.receiver = null;
    }

    public Client(String address, int port, BigInteger hash) throws IOException{
        connect(address, port, hash);
    }

    public Sender getSender() {
        return sender;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public void connect(String address, int port, BigInteger hash) throws IOException {
        this.socket = new Socket(address, port);
        this.hash = hash;
        receiver = new Receiver(socket);
        sender = new Sender(socket);
        receiver.start();
        sender.start();

        Action connect = new Action(Action.ActionType.Connect);
        connect.setHash(hash);
        send(connect);
    }

    public void disconnect() throws IOException {
        receiver.kill();
        send(new Action(Action.ActionType.Disconnect));
        sender.kill();
        try {
            receiver.join();
            sender.join();
            socket.close();
        } catch (InterruptedException exp) {
            exp.printStackTrace();
        }
    }

    public void send(Action action) {
        sender.send(action);
    }

    public void send(String editText) {
        Action edit = new Action(Action.ActionType.Edit);
        edit.setEditText(editText);
        sender.send(edit);
    }
}
