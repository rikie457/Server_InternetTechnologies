package nl.MenTych;

import java.util.ArrayList;

public class Group {
    public String name;

    private ArrayList<ClientThread> members = new ArrayList<>();

    public Group(String name) {
        this.name = name;
    }

    public ArrayList<ClientThread> getMembers() {
        return members;
    }

    public String getConnectedUsernames() {
        StringBuilder usernames = new StringBuilder();
        for (ClientThread member : members) {
            usernames.append(member.getUsername()).append(", ");
        }
        return usernames.toString();
    }

    public void addMember(ClientThread member) {
        this.members.add(member);
        member.setGroup(this);
    }

    public void removeMember(ClientThread clientThread) {
        this.members.remove(clientThread);
    }
}
