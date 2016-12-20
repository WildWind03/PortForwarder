import java.io.IOException;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            PortForwarder portForwarder = new PortForwarder(12000, "217.71.131.242", 80);
            portForwarder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
