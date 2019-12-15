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
    private Group group;

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

                        // New user connects
                        case "HELO":
                            username = splits[1];
                            send("+OK HELO");
                            break;

                        // User responded with pong.
                        case "PONG":
                            this.pongRecieved = true;
                            //System.out.println("PONG " + username);
                            break;

                        // user sends message
                        case "BCST":
                            if (line.length() > 0) {
                                for (ClientThread ct : server.threads) {
                                    // echo if message sender is self
                                    System.out.println(ct == this);
                                    if (ct == this) {
                                        server.sendMessage(this, "BCST [" + username + "] " + line.replaceAll("[*BCST $]", ""));
                                    }
                                }
                                send("+OK " + line);
                            }
                            break;
                            
                        case "CLIENTLIST":
                            System.out.println(group.getConnectedUsernames());
                            send("+OK CLIENTLIST " + group.getConnectedUsernames());
                            break;

                        case "QUIT":
                            pingThread.stop();
                            socket.close();
                            System.out.println("User disconnected: " + this.username);
                            this.server.threads.remove(this);
                            this.group.removeMember(this);
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
                    this.group.removeMember(this);
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

    public void setGroup(Group group) {
        this.group = group;
    }
}
