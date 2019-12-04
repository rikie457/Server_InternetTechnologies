package nl.MenTych;

import java.io.*;
import java.net.Socket;

public class ClientThread implements Runnable {

    Socket socket;
    InputStream input;
    PrintWriter out;
    BufferedReader reader;
    String username;
    Server server;
    boolean pongRecieved = true;

    public ClientThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {


        try {
            input = socket.getInputStream();
            out = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(input));
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("New client connected");
        Thread pingThread = new Thread(new PingThread(this));
        pingThread.start();

        while (true) {
            try {
                String line = this.reader.readLine();
                if (line != null) {
                    String[] splits = line.split("\\s+");
                    switch (splits[0]) {
                        case "HELO":
                            username = splits[1];
                            send("+OK HELO");
                            break;
                        case "PONG":
                            this.pongRecieved = true;
                            System.out.println("PONG " + username);
                            break;
                        case "BCST":
                            if (line.length() > 0) {
                                for (ClientThread ct : server.threads) {
                                    System.out.println(ct != this);
                                    if (ct != this) {
                                        send("BCST [" + username + "] " + line.substring(splits.length + 1));
                                    }
                                }
                                send("+OK " + line);
                            }
                            break;
                        default:
                            System.out.println(line);
                            System.out.println("UNKOWN!");
                            break;
                    }
                }
            } catch (IOException e) {
                try {
                    pingThread.stop();
                    socket.close();
                    this.server.threads.remove(this);
                    break;
                } catch (IOException e1) {

                    e1.printStackTrace();
                }
            }
        }
    }

    public void send(String message) {
        out.println(message);
        out.flush();
    }

}
