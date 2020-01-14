package nl.MenTych;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientFileServerThread implements Runnable {

    ClientSentFileThread clientSentFileThread;
    ClientRecieveFileThread clientRecieveThread;
    ClientThread reciever;
    int connected = 0;

    public ClientFileServerThread(ClientThread reciever) {
        this.reciever = reciever;
    }

    @Override
    public void run() {
        int PORT = Server.PORT + 1;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Started filetransfer on port: " + PORT);
            File file = null;
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("CONNECTED WITH NEW CLIENT");
                System.out.println("CONNECTED:" + connected);
                if (connected == 0) {
                    connected++;
                    clientRecieveThread = new ClientRecieveFileThread(socket, reciever);
                    file = clientRecieveThread.getFile();
                    System.out.println("CREATING RECIEVER");
                    Thread t2 = new Thread(clientRecieveThread);
                    t2.start();
                } else {
                    clientSentFileThread = new ClientSentFileThread(socket);
                    clientSentFileThread.setFile(file);
                    System.out.println("CREATING SENTFILE");
                    Thread t1 = new Thread(clientSentFileThread);
                    t1.start();
                }
            }
        } catch (IOException e) {
            e.getStackTrace();
//            kill();
        }
    }

    void kill() {
        System.out.println("KILLING CLIENTFILESERVERTHREAD");
        Thread.currentThread().stop();
    }
}