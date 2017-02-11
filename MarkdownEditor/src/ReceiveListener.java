import java.util.EventListener;

/**
 * Created by TCX4C70 on 16/12/27 027.
 */
public interface ReceiveListener extends EventListener {
    public void onReceive(ReceiveEvent event);
}
