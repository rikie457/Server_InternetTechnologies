package nl.MenTych;

import java.io.*;
import java.net.Socket;

public class ClientSentFileThread implements Runnable {

    private Socket socket;
    private String filePath;
    private Util util;


    public ClientSentFileThread(Socket socket) {
        this.socket = socket;

        try {
            this.util = new Util(new DataOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void sendFile() {
        try {
            util.send("FILESENDREADY");
            boolean ready = false;
            DataInputStream in = new DataInputStream(socket.getInputStream());

            while (!ready) {
                if (in.readUTF().equals("FILESENDREADY")) {
                    ready = true;
                }
            }
            File file = new File(filePath);
            byte[] mybytearray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            OutputStream os = socket.getOutputStream();

            os.flush();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(file.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            kill();
        } catch (IOException e) {
            e.getStackTrace();
        }
    }


    void kill() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("KILLING CLIENTFILESENDTHREAD");
        Thread.currentThread().stop();
    }

    @Override
    public void run() {
        sendFile();
    }
}
