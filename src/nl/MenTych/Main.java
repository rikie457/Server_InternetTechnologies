package nl.MenTych;

public class Main {

    public static void main(String[] args) {
        Thread server = new Thread(new Server());
        server.start();
    }
}
