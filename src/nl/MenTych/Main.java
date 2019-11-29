package nl.MenTych;

public class Main {

    public static void main(String[] args) {
	// write your code here
        Thread server = new Thread(new Server());
        server.start();
    }
}
