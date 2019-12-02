package nl.MenTych;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.TimerTask;

public class Beat implements Runnable {

    private BufferedReader reader;
    private PrintWriter out;
    private InputStream input;



    public Beat(InputStream input, PrintWriter out, BufferedReader reader) {
        this.input = input;
        this.out = out;
        this.reader = reader;
    }

    @Override
    public void run() {

    }
}


