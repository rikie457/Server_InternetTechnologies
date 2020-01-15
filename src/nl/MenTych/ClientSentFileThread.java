package nl.MenTych;

import java.io.*;
import java.net.Socket;

public class ClientSentFileThread implements Runnable {

    private Socket socket;
    private String filePath;
    private Util util;
    private ClientFileServerThread server;


    public ClientSentFileThread(Socket socket, String filePath, ClientFileServerThread server) {
        this.socket = socket;
        this.filePath = filePath;
        this.server = server;

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
            os.close();
            System.out.println("FILE SEND DONE!");
            if(file.delete())
            {
                System.out.println("Sent File deleted successfully");
            }
            else
            {
                System.out.println("Failed to delete the  sent file");
            }
            kill();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void kill() throws IOException {
        socket.close();
        this.server.kill();
        System.out.println("KILLING CLIENTFILESENDTHREAD");
        Thread.currentThread().stop();
    }

    @Override
    public void run() {
        sendFile();
    }
}
