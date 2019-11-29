package nl.MenTych;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientThread implements Runnable {

    private Socket socket;

    public ClientThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("New client connected");
        InputStream input = null;
        try {
            while (true) {
                input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String line = reader.readLine();    // reads a line of text
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
