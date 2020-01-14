package nl.MenTych;

import java.io.DataOutputStream;

public class Util {
    private DataOutputStream out;

    public Util(DataOutputStream out) {
        this.out = out;
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
