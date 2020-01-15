package nl.MenTych;

import java.io.*;
import java.net.Socket;

public class ClientRecieveFileThread implements Runnable {

    private Socket socket;
    private Util util;
    private ClientThread reciever;


    public ClientRecieveFileThread(Socket socket, ClientThread reciever) {
        this.socket = socket;
        this.reciever = reciever;
        try {
            this.util = new Util(new DataOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void recieveFile(ClientThread reciever) {
        try {
            util.send("FILERECIEVEREADY");
            int bytesRead;
            InputStream in = null;
            in = socket.getInputStream();
            System.out.println("PORT: " + socket.getLocalPort());

            DataInputStream clientData = new DataInputStream(in);


            String fileName = clientData.readUTF();
            FileOutputStream output = new FileOutputStream("files/" + fileName);

            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            output.close();

            //Notify Client 2
            Util recieverUtil = new Util(reciever.out);
            recieverUtil.send("+OK RECIEVEFILE " + fileName);
            kill();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void kill() throws IOException {
        socket.close();
        System.out.println("KILLING CLIENTFILERECIEVETHREAD");
        Thread.currentThread().stop();
    }

    @Override
    public void run() {
        recieveFile(this.reciever);
    }
}
