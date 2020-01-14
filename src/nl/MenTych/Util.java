package nl.MenTych;

import java.io.DataOutputStream;
import java.io.IOException;

public class Util {
    private DataOutputStream out;


    public Util(DataOutputStream out) {
        this.out = out;
    }

    public void send(String message) {


        try {
//            System.out.println(" SENDING " + message + " TO " + this.client.username);
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.getStackTrace();
        }
    }
}
