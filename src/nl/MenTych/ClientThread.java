package nl.MenTych;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Arrays;

public class ClientThread implements Runnable {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream reader;
    private String username;
    private Server server;
    private States state;
    private boolean pongRecieved = true;
    private Group group;
    private Util util;
    private int activegroup;

    public ClientThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }

    private void sendDM(String sender, String message) {
        util.send("+DM" + ' ' + sender + ' ' + message);
    }

    private void sendKEY(String sender, byte[] keyBytes) {
        util.send("+KEY PUBLIC" + ' ' + sender);
        util.sendBytes(keyBytes);
    }


    private void kill(Thread pt) {
        try {
            if (pt != null) {
                pt.stop();
            }
            System.out.println("DROPPED CONNECTION " + this.username);
            server.getThreads().remove(this);
            this.group.removeMember(this);
            this.socket.close();
        } catch (Exception ex) {
            System.out.println("Exception when closing outputstream: " + ex.getMessage());
        }
        this.state = States.FINISHED;
    }

    String getUsername() {
        return username;
    }

    @Override
    public void run() {
        this.state = States.CONNECTING;

        try {
            InputStream input = socket.getInputStream();
            out = new DataOutputStream(socket.getOutputStream());
            reader = new DataInputStream(input);
        } catch (IOException e) {
            kill(null);
        }
        this.util = new Util(out, this);
        util.send("HELO Welkom to WhatsUpp!");
        Thread pingThread = new Thread(new PingThread(this, util));
        pingThread.start();

        while (!this.state.equals(States.FINISHED)) {
            try {
                String line = this.reader.readUTF();

                String[] splits = line.split("\\s+");
                System.out.println(splits[0]);

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

                        for (ClientThread ct : server.getThreads()) {
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
                        this.group = server.getGroups().get(0);
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
                            for (ClientThread ct : server.getThreads()) {
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

                        for (int i = 0; i < server.getGroups().size(); i++) {
                            Group g = server.getGroups().get(i);
                            if (g.name.equals(name)) {
                                exists = true;
                            }
                        }
                        if (!exists) {
                            Group group = new Group(name, owner);
                            server.getGroups().add(group);
                            for (int i = 0; i < server.getGroups().size(); i++) {
                                Group g = server.getGroups().get(i);
                                if (g.name.equals(name)) {
                                    this.activegroup = i;
                                }
                            }
                            server.getGroups().get(0).removeMember(this);
                            group.addMember(this);
                            System.out.println(server.getGroups().size());
                            util.send("+OK GROUPCREATE " + group.name);
                        } else {
                            util.send("-ERR NOSUCHGROUP");
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

                        for (int i = 0; i < server.getGroups().size(); i++) {
                            Group g = server.getGroups().get(i);
                            if (g.name.equals(remove)) {
                                removeexists = true;
                                grouptoremove = g;
                                indextoremove = i;
                            }
                        }
                        if (removeexists) {
                            if (grouptoremove.owner.equals(askinguser)) {
                                server.sendMessageButNotToSender(this, indextoremove, "+OK GROUPREMOVED");
                                server.getGroups().remove(grouptoremove);
                                util.send("+OK GROUPLEAVE");
                                this.activegroup = 0;
                            } else {
                                util.send("-ERR NOTOWNER");
                            }
                        } else {
                            util.send("-ERR NOSUCHGROUP");
                        }
                        break;

                    case "GROUPJOIN":
                        String groupname = splits[1];
                        int newindex = 0;
                        boolean isgroup = false;
                        for (int i = 0; i < server.getGroups().size(); i++) {
                            Group g = server.getGroups().get(i);
                            if (g.name.equals(groupname)) {
                                isgroup = true;
                                newindex = i;
                            }
                        }

                        if (isgroup) {
                            System.out.println(server.getGroups().size());
                            if (server.getGroups().size() > this.activegroup) {
                                server.getGroups().get(this.activegroup).removeMember(this);
                            }
                            this.group = server.getGroups().get(newindex);
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
                        this.server.getThreads().remove(this);
                        this.group.removeMember(this);
                        break;

                    case "UPLOADFILE":
                        String filename = splits[1];
                        String username = splits[2];
                        for (ClientThread ct : server.getThreads()) {
                            if (ct != this && username.equals(ct.username)) {
                                ClientFileServerThread fileServer = new ClientFileServerThread(ct, filename, this);
                                Thread fileserverThread = new Thread(fileServer);
                                fileserverThread.start();
                                break;
                            }
                        }
                        break;

                    case "RECIEVEDFILE":
                        File file = new File("files/" + splits[1]);
                        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
                        String checksum = getFileChecksum(md5Digest, file);
                        util.send("+OK CHECKSUM " + splits[1] + " " + checksum);
                        break;

                    case "+KEY":
                        try {
                            int bytesRead;
                            byte[] buffer = new byte[1024];
                            System.out.println("Reading bytes: ");
                            while ((bytesRead = this.reader.read(buffer)) == -1) {
                                System.out.print(bytesRead + " ");
                            }

                            ClientThread reciever = this.server.getClientThreadByName(splits[2]);
                            String sender = splits[3];
                            reciever.sendKEY(sender, buffer);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    default:
                        System.out.println();
                        System.out.println("UNKNOWN: " + line);
                        break;
                }
            } catch (Exception e) {
                break;
            }
        }
        kill(pingThread);
    }


    void setGroup(Group group) {
        this.group = group;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setPongRecieved(boolean pongRecieved) {
        this.pongRecieved = pongRecieved;
    }

    public boolean hasPongRecieved() {
        return pongRecieved;
    }

    public int getActivegroup() {
        return activegroup;
    }

    public Util getUtil() {
        return util;
    }
}
