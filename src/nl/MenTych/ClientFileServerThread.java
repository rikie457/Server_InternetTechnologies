package nl.MenTych;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientFileServerThread implements Runnable {
    private ClientThread reciever;
    private String filename;
    private ClientThread sender;
    private boolean stop;
    private ServerSocket serverSocket;
    private boolean sent = false;

    public ClientFileServerThread(ClientThread reciever, String filename, ClientThread sender) {
        this.reciever = reciever;
        this.filename = filename;
        this.sender = sender;
    }

    @Override
    public void run() {
        int PORT = Server.PORT + 1;
        try {
            serverSocket = new ServerSocket(PORT);
            while (!this.stop) {
                try {
                    Socket socket = serverSocket.accept();
                    if (!sent) {
                        sent = true;
                        ClientRecieveFileThread clientRecieveThread = new ClientRecieveFileThread(socket, reciever);
                        System.out.println("CREATING RECIEVER FROM CLIENT");
                        Thread t2 = new Thread(clientRecieveThread);
                        t2.start();
                    } else {
                        ClientSentFileThread clientSentFileThread = new ClientSentFileThread(socket, "files/" + filename, this);
                        System.out.println("CREATING SENDER TO CLIENT");
                        Thread t2 = new Thread(clientSentFileThread);
                        t2.start();
                    }
                } catch (Exception e) {
                   if(e.getMessage().equals("Interrupted function call: accept failed")){
                       break;
                   }else {
                       e.printStackTrace();
                   }
                }

            }
            //Just to make sure it stops
            kill();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void kill() {
        this.stop = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("KILLING CLIENTFILESERVERTHREAD");
        Thread.currentThread().stop();
    }
}