import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public class Action implements Serializable {
    private static final long serialVersionUID = -254277887890921414L;
    public enum ActionType {Connect, Disconnect, Edit};
    private ActionType actionType;
    private String editText = null;
    private BigInteger hash = null;

    public Action(ActionType type) {
        setActionType(type);
    }

    public String getEditText() throws RuntimeException{
        if(!this.actionType.equals(ActionType.Edit))
            throw new RuntimeException("only when the type is Edit you can call getEditText");
        return editText;
    }

    public void setEditText(String text) throws RuntimeException{
        if(!this.actionType.equals(ActionType.Edit))
            throw new RuntimeException("only when the type is Edit you can call setEditText");
        editText = text;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType type) {
        actionType = type;
    }

    public BigInteger getHash() throws RuntimeException{
        if(!this.actionType.equals(ActionType.Connect))
            throw new RuntimeException("only when the type is Connect you can call getHash");
        return hash;
    }

    public void setHash(BigInteger hash) throws RuntimeException{
        if(!this.actionType.equals(ActionType.Connect))
            throw new RuntimeException("only when the type is Connect you can call setHash");
        this.hash = hash;
    }
}
