package nl.MenTych;

import java.io.DataOutputStream;

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
                System.out.println(" SENDING " + message + " TO " + this.client.username);
            }
            out.writeUTF(message);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
