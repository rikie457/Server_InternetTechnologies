package nl.MenTych;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientFileServerThread implements Runnable {
    //TODO: ALLOW DIFFERENT TYPE OF RECIEVING OR SENDING
    @Override
    public void run() {
        int PORT = Server.PORT + 1;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Started filetransfer on port: " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("CONNECTED WITH CLIENT");
                Thread fileThread = new Thread(new ClientFileThread(socket));
                fileThread.start();
            }
        } catch (IOException e) {
            kill();
        }
    }

    void kill() {
        System.out.println("KILLING CLIENTFILESERVERTHREAD");
        Thread.currentThread().stop();
    }
}