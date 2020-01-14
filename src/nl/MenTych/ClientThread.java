package nl.MenTych;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

public class ClientThread implements Runnable {

    Socket socket;
    DataOutputStream out;
    DataInputStream reader;
    String username;
    private Server server;
    States state;
    boolean pongRecieved = true;
    private Group group;
    Util util;
    int activegroup;
    ClientFileServerThread fileServer;

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
        this.util = new Util(out, this);
        util.send("HELO Welkom to WhatsUpp!");
        Thread pingThread = new Thread(new PingThread(this, util));
        pingThread.start();

        while (!this.state.equals(States.FINISHED)) {
            try {
                String line = null;
                //ALL ALLOWED STATES WHEN NOT SENDING OR RECIEVING FILE FOR THIS CLIENT
                line = this.reader.readUTF();

                if (line != null) {
                    String[] splits = line.split("\\s+");
                    System.out.println(line);
                    switch (splits[0]) {
                        // New user connects
                        case "HELO":
                            username = splits[1];
                            boolean validname = username.matches("[a-zA-Z0-9_]{3,14}");

                            if (!validname) {
                                this.state = States.FINISHED;
                                util.send("-ERR username has an invalid format (only characters, numbers and underscores are allowed");
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
                                util.send("-ERR user already logged in");
                                continue;
                            }
                            this.group = server.groups.get(0);
                            this.group.addMember(this);

                            util.send("+OK HELO " + username);
                            util.send("+OK GROUPJOIN " + this.group.name);
                            this.state = States.CONNECTED;
                            break;

                        case "VERSION":
                            util.send("+VERSION 2");
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
                                util.send("+OK " + line);
                            }
                            break;

                        case "CLIENTLIST":
                            System.out.println(group.getConnectedUsernames());
                            util.send("+OK CLIENTLIST " + group.getConnectedUsernames());
                            break;

                        case "CLIENTLIST-DM":
                            System.out.println(group.getConnectedUsernames());
                            util.send("+OK CLIENTLIST-DM " + group.getConnectedUsernames());
                            break;

                        case "CLIENTLIST-GROUP":
                            System.out.println(group.getConnectedUsernames());
                            util.send("+OK CLIENTLIST-GROUP " + group.getConnectedUsernames());
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
                                util.send("+OK GROUPCREATE " + group.name);
                            } else {
                                util.send("-ERR GROUPEXISTS");
                            }
                            break;

                        case "KICK":
                            try {
                                this.group.removeMember(this.server.getClientThreadByName(splits[1]));
                                this.server.getClientThreadByName(splits[1]).util.send("+OK GROUPKICK");
                            } catch (ClientNotFoundException e) {
                                e.printStackTrace();
                            }
                            break;

                        case "LEAVEGROUP":
                            this.group.removeMember(this);
                            util.send("+OK GROUPLEAVE");
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
                                    util.send("-ERR NOTOWNER");
                                }
                            } else {
                                util.send("-ERR GROUPEXISTS");
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
                                util.send("+OK GROUPJOIN " + groupname);
                            } else {
                                util.send("-ERR NOSUCHGROUP");
                            }
                            break;
                        case "QUIT":
                            util.send("+OK Goodbye");
                            pingThread.stop();
                            socket.close();
                            System.out.println("User disconnected: " + this.username);
                            this.server.threads.remove(this);
                            this.group.removeMember(this);
                            break;

                        case "UPLOADFILE":
                            String username = splits[1];
                            for (ClientThread ct : server.threads) {
                                if (ct != this && username.equals(ct.username)) {
                                    fileServer = new ClientFileServerThread(ct);
                                    Thread fileserverThread = new Thread(fileServer);
                                    fileserverThread.start();
                                    break;
                                }
                            }
                            break;

                        case "DOWNLOADFILE":
                            String filename = splits[1];
                            System.out.println(filename);
                            fileServer = new ClientFileServerThread(filename);
                            Thread fileserverThread = new Thread(fileServer);
                            fileserverThread.start();
                            break;

                        case "DONEFILE":
                            fileServer.kill();
                            break;

                        default:
                            System.out.println();
                            System.out.println("UNKOWN: " + line);
                            break;
                    }
                }
            } catch (Exception e) {
                e.getStackTrace();
            }
        }
        kill(pingThread);
    }

    private void sendDM(String sender, String message) {
        util.send("+DM" + ' ' + sender + ' ' + message);
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

    public Socket getSocket() {
        return socket;
    }
}
