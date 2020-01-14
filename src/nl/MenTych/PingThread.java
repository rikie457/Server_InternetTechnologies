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
        out = ct.out;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);
                util.send("PING");
                Thread.sleep(3000);
                if (!ct.pongRecieved) {
                    util.send("DSCN Pong timeout");
                    this.ct.socket.close();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

            ct.pongRecieved = false;
        }
    }
}
