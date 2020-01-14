package nl.MenTych;

import java.io.*;
import java.net.Socket;

public class ClientFileThread implements Runnable {

    private Socket socket;
    private int type;
    private Util util;

    public ClientFileThread(Socket socket, int type) {
        this.socket = socket;
        this.type = type;
        try {
            this.util = new Util(new DataOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {

            if (type == 1) {
                util.send("FILERECIEVEREADY");
                recieveFile();
            } else if (type == 2) {

            }

        } catch (IOException e) {
            kill();
        }
    }

    private void sendFile(File file) {


    }

    private void recieveFile() throws IOException {
        int bytesRead;
        InputStream in = null;
        in = socket.getInputStream();

        DataInputStream clientData = new DataInputStream(in);

        String fileName = clientData.readUTF();
        System.out.println(fileName);
        FileOutputStream output = new FileOutputStream("files/" + fileName);

        long size = clientData.readLong();
        System.out.println(size);
        byte[] buffer = new byte[1024];
        while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            output.write(buffer, 0, bytesRead);
            size -= bytesRead;
        }

        output.close();
    }

    void kill() {
        System.out.println("KILLING CLIENTFILETHREAD");
        Thread.currentThread().stop();
    }
}
