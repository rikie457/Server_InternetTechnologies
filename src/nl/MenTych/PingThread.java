package nl.MenTych;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class PingThread implements Runnable {

    private ClientThread ct;
    private DataOutputStream out;

    public PingThread(ClientThread ct) {
        this.ct = ct;
        out = ct.out;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!ct.sendingFile) {
                    Thread.sleep(2000);
                    send("PING");
                    System.out.println("SENDING PING");
                    Thread.sleep(3000);
                    if (!ct.pongRecieved ) {
                       send("DSCN Pong timeout");
                        //Maybe remove thread?
                        this.ct.socket.close();
                    }
                }

                ct.pongRecieved = false;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void send(String message) {
        try {
            System.out.println(message);
            out.writeUTF(message);
            out.flush();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }
}