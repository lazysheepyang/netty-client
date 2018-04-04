import client.NettyClient;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.json.JSONObject;

/**
 * Created by ly on 2018/2/2.
 */
public class User extends Thread {
    private final static InternalLogger log = InternalLoggerFactory.getInstance(User.class);
    private NettyClient nettyClient;


    private void send(JSONObject head, JSONObject body) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("head", head);
        jsonObject.put("body", body);
        nettyClient.sendMessage(jsonObject.toString());
    }


    @Override
    public void run() {
        log.info("客户端启动" + Thread.currentThread().getName());

        try {
            nettyClient = new NettyClient();
            nettyClient.connect(1234, "12.345.678.9");
            JSONObject head = new JSONObject();
            head.put("hello","world");
            JSONObject body = new JSONObject();
            body.put("hello","world");
            send(head,body);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
