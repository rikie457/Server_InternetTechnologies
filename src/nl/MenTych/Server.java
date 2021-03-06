package nl.MenTych;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable {

    private ArrayList<ClientThread> threads;
    private ArrayList<Group> groups = new ArrayList<>();

    static final int PORT = 1337;

    public Server() {
        this.threads = new ArrayList<>();
        this.groups.add(new Group("Main", "#NULL"));
    }

    @Override
    public void run() {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Listening for clients on port: " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientThread ct = new ClientThread(socket, this);
                threads.add(ct);
                (new Thread(ct)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageButNotToSender(ClientThread sender, int group, String message) {
        for (ClientThread thread : threads) {
            if (thread != sender && thread.getActivegroup() == group) {
                thread.getUtil().send(message);
            }
        }
    }

    public ClientThread getClientThreadByName(String name) throws ClientNotFoundException {
        for (ClientThread t : threads) {
            if (t.getUsername().equals(name)) {
                return t;
            }
        }
        throw new ClientNotFoundException(name, this.threads);
    }

    public String getGroupsAsString() {
        StringBuilder groupsstring = new StringBuilder();
        for (Group group : groups) {
            groupsstring.append(group.name).append(",");
        }
        return groupsstring.toString();
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }

    public ArrayList<ClientThread> getThreads() {
        return threads;
    }
}
