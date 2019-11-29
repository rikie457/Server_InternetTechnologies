package nl.MenTych;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(1337)) {
            while (true) {
                Socket socket = serverSocket.accept();
                Thread ct = new Thread(new ClientThread(socket));
                ct.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
