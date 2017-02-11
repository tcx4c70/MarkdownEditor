import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public class Sender extends Thread {
    private Socket socket;
    private Action action;
    private boolean alive;

    public Sender(Socket socket) {
        this.socket = socket;
        alive = true;
    }

    @Override
    public void run() {
        synchronized (this) {
            ObjectOutputStream output = null;
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                while (alive) {
                    this.wait();
                    output.writeObject(action);
                    System.out.println("        Send to " + socket.getInetAddress() + ": " + action.getActionType());
//                    if(Action.ActionType.Edit.equals(action.getActionType())) {
//                        System.out.println(": " + action.getEditText());
//                    } else {
//                        System.out.println("");
//                    }
                    output.flush();
                }
            } catch (Exception exp) {
                exp.printStackTrace();
            } finally {
                try {
                    if (output != null)
                        output.close();
                } catch (IOException exp) {
                    exp.printStackTrace();
                }
            }
        }
    }

    public void send(Action action) {
        synchronized (this) {
            this.action = action;
            this.notify();
        }
    }

    public void kill() {
        alive = false;
    }
}
