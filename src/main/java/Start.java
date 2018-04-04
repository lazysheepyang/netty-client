
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


/**
 * Created by ly on 2017/12/21.
 */
public class Start {

    private final static InternalLogger log = InternalLoggerFactory.getInstance(Start.class);


    public static void main(String[] args) throws InterruptedException {
        User user = new User();
        user.start();
    }

}
