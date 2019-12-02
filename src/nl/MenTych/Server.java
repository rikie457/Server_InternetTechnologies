package nl.MenTych;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(1337)) {
            while (true) {
                Socket socket = serverSocket.accept();
                Thread ct = new Thread(new ClientThread(socket));
                ct.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientThread implements Runnable {

        private Socket socket;
        private InputStream input;
        private PrintWriter out;
        private BufferedReader reader;
        private boolean pongRecieved = true;

        private ClientThread(Socket socket) {
            this.socket = socket;
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
                            case "HELO":
                                out.println("+OK HELO");
                                out.flush();
                                break;
                            case "PONG":
                                this.pongRecieved = true;
                                break;
                            default:
                                System.out.println("UNKOWN!");
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private class PingThread implements Runnable {

        private Server.ClientThread ct;
        private PrintWriter out;

        private PingThread(ClientThread ct) {
            this.ct = ct;
            out = ct.out;
        }

        @Override
        public void run() {
            System.out.println("STARTING PINGTHREAD");
            while (true) {
                try {
                    Thread.sleep(2000);
                    out.println("PING");
                    out.flush();

                    Thread.sleep(3000);
                    if (!ct.pongRecieved) {
                        out.println("DSCN Pong timeout");
                        out.flush();
                        //Maybe remove thread?
                     this.ct.socket.close();
                    }

                    ct.pongRecieved = false;
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
