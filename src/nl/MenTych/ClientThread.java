package nl.MenTych;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ClientThread implements Runnable {

    Socket socket;
    DataOutputStream out;
//    private BufferedReader reader;
    DataInputStream reader;
    String username;
    private Server server;
    private States state;
    boolean pongRecieved = true, sendingFile = false;
    private Group group;
    int activegroup;
    private DataInputStream clientData;

    public ClientThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        this.state = States.CONNECTING;

        try {
            InputStream input = socket.getInputStream();
            out = new DataOutputStream(socket.getOutputStream());
            reader = new DataInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        send("HELO Welkom to WhatsUpp!");
        Thread pingThread = new Thread(new PingThread(this));
        pingThread.start();

        while (!this.state.equals(States.FINISHED)) {
            if (!sendingFile) {
                try {
                    String line = this.reader.readUTF();

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
                                this.group = server.groups.get(0);
                                this.group.addMember(this);

                                send("+OK HELO " + username);
                                send("+OK GROUPJOIN " + this.group.name);
                                this.state = States.CONNECTED;
                                break;

                            case "VERSION":
                                send("+VERSION 2");
                                break;

                            case "DM":
                                // splits 0 to 2 are used for identifying.
                                // 0 = DM identifier
                                // 1 = reciever username
                                // 2 = sender username
                                System.out.println(Arrays.toString(splits));

                                try {
                                    ClientThread reciever = this.server.getClientThreadByName(splits[1]);
                                    String sender = splits[2];

                                    StringBuilder message = new StringBuilder();
                                    for (int i = 3; i < splits.length; i++) {
                                        message.append(splits[i]);
                                        message.append(" ");
                                    }

                                    reciever.sendDM(sender, message.toString());

                                } catch (ClientNotFoundException e) {
                                    e.printStackTrace();
                                }

                                break;

                            // User responded with pong.
                            case "PONG":
                                this.pongRecieved = true;
                                break;

                            // user sends message
                            case "BCST":
                                if (line.length() > 0) {
                                    for (ClientThread ct : server.threads) {
                                        if (ct == this) {
                                            server.sendMessageButNotToSender(this, activegroup, "BCST [" + username + "] " + line.replaceAll("BCST ", ""));
                                        }

                                    }
                                    send("+OK " + line);
                                }
                                break;

                            case "CLIENTLIST":
                                System.out.println(group.getConnectedUsernames());
                                send("+OK CLIENTLIST " + group.getConnectedUsernames());
                                break;

                            case "CLIENTLIST-DM":
                                System.out.println(group.getConnectedUsernames());
                                send("+OK CLIENTLIST-DM " + group.getConnectedUsernames());
                                break;

                            case "CLIENTLIST-GROUP":
                                System.out.println(group.getConnectedUsernames());
                                send("+OK CLIENTLIST-GROUP " + group.getConnectedUsernames());
                                break;

                            case "GROUPCREATE":
                                String owner = splits[1];
                                String name = splits[2];
                                boolean exists = false;

                                for (int i = 0; i < server.groups.size(); i++) {
                                    Group g = server.groups.get(i);
                                    if (g.name.equals(name)) {
                                        exists = true;
                                    }
                                }
                                if (!exists) {
                                    Group group = new Group(name, owner);
                                    server.groups.add(group);
                                    for (int i = 0; i < server.groups.size(); i++) {
                                        Group g = server.groups.get(i);
                                        if (g.name.equals(name)) {
                                            this.activegroup = i;
                                        }
                                    }
                                    server.groups.get(0).removeMember(this);
                                    group.addMember(this);
                                    send("+OK GROUPCREATE " + group.name);
                                } else {
                                    send("-ERR GROUPEXISTS");
                                }
                                break;

                            case "KICK":
                                try {
                                    this.group.removeMember(this.server.getClientThreadByName(splits[1]));
                                    this.server.getClientThreadByName(splits[1]).send("+OK GROUPKICK");
                                } catch (ClientNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;

                            case "LEAVEGROUP":
                                this.group.removeMember(this);
                                this.send("+OK GROUPLEAVE");
                                break;

                            case "GROUPREMOVE":
                                String remove = splits[1];
                                String askinguser = splits[2];
                                boolean removeexists = false;
                                int indextoremove = 0;
                                Group grouptoremove = null;

                                for (int i = 0; i < server.groups.size(); i++) {
                                    Group g = server.groups.get(i);
                                    if (g.name.equals(remove)) {
                                        removeexists = true;
                                        grouptoremove = g;
                                        indextoremove = i;
                                    }
                                }
                                if (removeexists) {
                                    if (grouptoremove.owner.equals(askinguser)) {
                                        for (ClientThread ct : server.threads) {
                                            Group newgroup = server.groups.get(0);
                                            System.out.println(activegroup);
                                            System.out.println(indextoremove);
                                            if (ct.activegroup == indextoremove && ct != this) {
                                                server.sendMessageToAll(activegroup, "+OK GROUPREMOVED");
                                                ct.activegroup = 0;
                                                ct.group = newgroup;
                                            }
                                        }
                                        server.groups.remove(grouptoremove);
                                    } else {
                                        send("-ERR NOTOWNER");
                                    }
                                } else {
                                    send("-ERR GROUPEXISTS");
                                }
                                break;

                            case "GROUPJOIN":
                                String groupname = splits[1];
                                int newindex = 0;
                                boolean isgroup = false;
                                for (int i = 0; i < server.groups.size(); i++) {
                                    Group g = server.groups.get(i);
                                    if (g.name.equals(groupname)) {
                                        isgroup = true;
                                        newindex = i;
                                    }
                                }

                                if (isgroup) {
                                    server.groups.get(this.activegroup).removeMember(this);
                                    this.group = server.groups.get(newindex);
                                    this.activegroup = newindex;
                                    this.group.addMember(this);
                                    send("+OK GROUPJOIN " + groupname);
                                } else {
                                    send("-ERR NOSUCHGROUP");
                                }
                                break;
                            case "QUIT":
                                send("+OK Goodbye");
                                pingThread.stop();
                                socket.close();
                                System.out.println("User disconnected: " + this.username);
                                this.server.threads.remove(this);
                                this.group.removeMember(this);
                                break;

                            case "NEWFILE":
                                send("+OK NEWFILE");
                                sendingFile = true;
                                int bytesRead;
                                int current = 0;
                                InputStream in = socket.getInputStream();
                                DataInputStream clientData = new DataInputStream(in);
                                String fileName = clientData.readUTF();
                                OutputStream output = new FileOutputStream(fileName);
                                long size = clientData.readLong();
                                byte[] buffer = new byte[1024];
                                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                                    output.write(buffer, 0, bytesRead);
                                    size -= bytesRead;
                                    System.out.println(size);
                                }
                                sendingFile = false;
                                break;

                            default:
                                System.out.println(line);
                                System.out.println("UNKOWN!");
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        kill(pingThread);
    }

    private void sendDM(String sender, String message) {
        this.send("+DM" + ' ' + sender + ' ' + message);
    }

    public void send(String message) {
        try {
            System.out.println(message);
            out.writeUTF(message);
            out.flush();
        }catch (Exception e){
            e.getStackTrace();
        }
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void kill(Thread pt) {
        try {
            pt.stop();
            System.out.println("DROPPED CONNECTION " + this.username);
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
