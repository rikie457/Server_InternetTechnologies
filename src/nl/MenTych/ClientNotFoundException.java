package nl.MenTych;

import java.util.ArrayList;

public class ClientNotFoundException extends Exception {
    ClientNotFoundException(String clientname, ArrayList<ClientThread> users) {
        super("Client '" + clientname + "' Not found. Userlist exists of following: " + users.toString());
    }
}
