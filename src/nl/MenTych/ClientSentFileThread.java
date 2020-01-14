package nl.MenTych;

import java.io.*;
import java.net.Socket;

public class ClientSentFileThread {

    private Socket socket;
    private File file;
    private Util util;


    public ClientSentFileThread(Socket socket) {
        this.socket = socket;

        try {
            this.util = new Util(new DataOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void sendFile(File file) {
        try {
            util.send("FILESENDREADY");
            boolean ready = false;
            DataInputStream in = new DataInputStream(socket.getInputStream());

            while (!ready) {
                if (in.readUTF().equals("FILESENDREADY")) {
                    ready = true;
                }
            }

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
        } catch (IOException e) {
            e.getStackTrace();
        }
    }

    public void setFile(File file) {
        this.file = file;
    }

    void kill() {
        System.out.println("KILLING CLIENTFILETHREAD");
        Thread.currentThread().stop();
    }

//    @Override
//    public void run() {
//            sendFile(this.file);
//    }
}
