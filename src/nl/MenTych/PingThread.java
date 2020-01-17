package nl.MenTych;

import java.io.DataOutputStream;
import java.io.IOException;

public class PingThread implements Runnable {

    private ClientThread ct;
    private DataOutputStream out;
    private Util util;

    public PingThread(ClientThread ct, Util util) {
        this.ct = ct;
        this.util = util;
        out = ct.getOut();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);
                util.send("PING");
                Thread.sleep(3000);
                if (!ct.hasPongRecieved()) {
                    util.send("DSCN Pong timeout");
                    this.ct.getSocket().close();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

            ct.setPongRecieved(false);
        }
    }
}
