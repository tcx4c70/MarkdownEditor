import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public class Receiver extends Thread {
    private Socket socket;
    private Set<ReceiveListener> listeners;
    private boolean alive;

    public Receiver(Socket socket) {
        this.socket = socket;
        listeners = new HashSet<>();
        alive = true;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(socket.getInputStream());
            while(alive) {
                Action action = (Action)input.readObject();

                System.out.println("    Receive from " + socket.getInetAddress() + ": " + action.getActionType());
//                if(Action.ActionType.Edit.equals(action.getActionType())) {
//                    System.out.println(": " + action.getEditText());
//                } else {
//                    System.out.println("");
//                }

                notifyAllListeners(new ReceiveEvent(this, action));
            }
        } catch (Exception exp) {
            notifyAllListeners(new ReceiveEvent(this, new Action(Action.ActionType.Disconnect)));
            exp.printStackTrace();
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (IOException exp) {
                exp.printStackTrace();
            }
        }
    }

    public void addReceiveListener(ReceiveListener listener) {
        listeners.add(listener);
    }

    public void removeReceiveListener(ReceiveListener listener) {
        listeners.remove(listener);
    }

    private void notifyAllListeners(ReceiveEvent event) {
        for(ReceiveListener listener: listeners) {
            listener.onReceive(event);
        }
    }

    public void kill() {
        alive = false;
    }
}
