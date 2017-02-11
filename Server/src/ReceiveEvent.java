import java.util.EventObject;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public class ReceiveEvent extends EventObject{
    private Object sourceObject;
    private Action action;

    public ReceiveEvent(Object source, Action action) {
        super(source);
        setSourceObject(source);
        setAction(action);
    }

    public Object getSourceObject() {
        return sourceObject;
    }

    public void setSourceObject(Object source) {
        sourceObject = source;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}
