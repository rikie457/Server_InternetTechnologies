package nl.MenTych;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable {

    public ArrayList<ClientThread> threads;

    public Server() {
        this.threads = new ArrayList<>();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(1337)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientThread ct = new ClientThread(socket, this);
                threads.add(ct);
                (new Thread(ct)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
