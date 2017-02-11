import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public class ClientsMap extends HashMap<BigInteger, HashSet<HandleAClient>> implements  ReceiveListener {

    public ClientsMap() {
    }

    public void add(Socket socket) {
        try {
            HandleAClient handleAClient = new HandleAClient(socket);
            handleAClient.getReceiver().addReceiveListener(this);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    @Override
    public void onReceive(ReceiveEvent event) {
        Receiver receiver = (Receiver)event.getSourceObject();
        HashSet<HandleAClient> handleAClients = this.get(receiver.getHash());
        Action action = event.getAction();
        Action.ActionType actionType = action.getActionType();

        if(Action.ActionType.Connect.equals(actionType)) {
            HandleAClient aClient = receiver.getFather();
            BigInteger hash = action.getHash();
            aClient.setHash(hash);
            if(this.containsKey(hash)) {
                this.get(hash).add(aClient);
            } else {
                this.put(hash, new HashSet<>(Arrays.asList(aClient)));
            }

            System.out.println(receiver.getSocket().getInetAddress() + " has been connected. Hash code: " + hash.toString(16));
            System.out.println("The size in " + hash.toString(16) + " is " + this.get(hash).size());
        }
        else if(Action.ActionType.Edit.equals(actionType)) {
            for (HandleAClient handleAClient : handleAClients) {
                if (!handleAClient.getReceiver().equals(event.getSourceObject())) {
                    handleAClient.send(event.getAction());
                }
            }
        }
        else if(Action.ActionType.Disconnect.equals(actionType)) {
            HandleAClient aClient = receiver.getFather();
            aClient.disconnect();
            handleAClients.remove(aClient);
            if(handleAClients.size() == 0) {
                this.remove(receiver.getHash());
            }

            System.out.println(receiver.getSocket().getInetAddress() + " has benn disconnected. The hash code is " + receiver.getHash().toString(16));
            System.out.println("The size in " + receiver.getHash().toString(16) + " is " + handleAClients.size());
        }
    }
}
