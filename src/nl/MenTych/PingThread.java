package nl.MenTych;

import java.io.IOException;
import java.io.PrintWriter;

public class PingThread implements Runnable {

    private ClientThread ct;
    private PrintWriter out;

    public PingThread(ClientThread ct) {
        this.ct = ct;
        out = ct.out;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);
                out.println("PING");
                out.flush();

                Thread.sleep(3000);
                if (!ct.pongRecieved) {
                    out.println("DSCN Pong timeout");
                    out.flush();
                    //Maybe remove thread?
                    this.ct.socket.close();
                }

                ct.pongRecieved = false;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}