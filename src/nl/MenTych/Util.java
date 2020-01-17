package nl.MenTych;

import java.io.DataOutputStream;
import java.util.Arrays;

public class Util {
    private DataOutputStream out;
    private ClientThread client;

    public Util(DataOutputStream out, ClientThread client) {
        this.out = out;
        this.client = client;
    }

    public Util(DataOutputStream out) {
        this.out = out;
    }

    public void send(String message) {

        try {
            if (this.client != null) {
                if (!message.contains("PING") && !message.contains("VERSION")) {
                    System.out.println(" SENDING " + message + " TO " + this.client.getUsername());
                }
            }
            out.writeUTF(message);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendBytes(byte[] message) {
        try {
            if (this.client != null) {
                System.out.println(" SENDING bytes" + Arrays.toString(message) + " TO " + this.client.getUsername());
            }
            out.write(message);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
