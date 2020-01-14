package nl.MenTych;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientFileServerThread implements Runnable {
    ClientSentFileThread clientSentFileThread;
    ClientRecieveFileThread clientRecieveThread;
    ClientThread reciever;
    String filename;
    ServerSocket serverSocket;
    int connected = 0;

    public ClientFileServerThread(ClientThread reciever) {
        this.reciever = reciever;
    }

    public ClientFileServerThread(String filename) {
        this.filename = filename;
    }

    @Override
    public void run() {
        int PORT = Server.PORT + 1;
        System.out.println(PORT);
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Started filetransfer on port: " + PORT);
            System.out.println(reciever);
            System.out.println(filename);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("CONNECTED WITH NEW CLIENT");
                try {

                    if (reciever != null) {
                        clientRecieveThread = new ClientRecieveFileThread(socket, reciever);
                        System.out.println("CREATING RECIEVER FROM CLIENT");
                        Thread t2 = new Thread(clientRecieveThread);
                        t2.start();
                    } else if (filename != null) {

                        clientSentFileThread = new ClientSentFileThread(socket);
                        System.out.println("CREATING SENDER TO CLIENT");
                        Thread t2 = new Thread(clientRecieveThread);
                        t2.start();
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            kill();
        }
    }

    void kill() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("KILLING CLIENTFILESERVERTHREAD");
        Thread.currentThread().stop();
    }
}