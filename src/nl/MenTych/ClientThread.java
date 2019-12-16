package nl.MenTych;

import java.io.*;
import java.net.Socket;

public class ClientThread implements Runnable {

    Socket socket;
    PrintWriter out;
    private BufferedReader reader;
    String username;
    private Server server;
    private States state;
    boolean pongRecieved = true;
    private Group group;

    public ClientThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        this.state = States.CONNECTING;

        try {
            InputStream input = socket.getInputStream();
            out = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(input));
        } catch (IOException e) {
            e.printStackTrace();
        }

        send("HELO Welkom to WhatsUpp!");
        Thread pingThread = new Thread(new PingThread(this));
        pingThread.start();

        while (!this.state.equals(States.FINISHED)) {
            try {
                String line = this.reader.readLine();

                if (line != null) {
                    String[] splits = line.split("\\s+");

                    switch (splits[0]) {

                        // New user connects
                        case "HELO":
                            username = splits[1];
                            boolean validname = username.matches("[a-zA-Z0-9_]{3,14}");

                            if (!validname) {
                                this.state = States.FINISHED;
                                send("-ERR username has an invalid format (only characters, numbers and underscores are allowed");
                                continue;
                            }

                            boolean userexists = false;

                            for (ClientThread ct : server.threads) {
                                if (ct != this && username.equals(ct.username)) {
                                    userexists = true;
                                    break;
                                }
                            }

                            if (userexists) {
                                this.state = States.FINISHED;
                                send("-ERR user already logged in");
                                continue;
                            }

                            this.state = States.CONNECTED;
                            send("+OK HELO " + username);
                            break;

                        case "VERSION":
                            System.out.println("VERSION");
                            send("+VERSION 2");
                            break;

                        // User responded with pong.
                        case "PONG":
                            this.pongRecieved = true;
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
                            send("+OK Goodbye");
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
              break;
            }
        }
        kill(pingThread);
    }

    public void send(String message) {
        out.println(message);
        out.flush();
    }

        public void setGroup(Group group) {
            this.group = group;
        }

    public void kill(Thread pt) {
        try {
            pt.stop();
            System.out.println("[DROP CONNECTION] " + this.username);
            server.threads.remove(this);
            this.group.removeMember(this);
            this.socket.close();
        } catch (Exception ex) {
            System.out.println("Exception when closing outputstream: " + ex.getMessage());
        }
        this.state = States.FINISHED;
    }

    public String getUsername() {
        return username;
    }
}
