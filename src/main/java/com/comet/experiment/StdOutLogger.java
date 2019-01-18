package com.comet.experiment;

import java.io.*;

public class StdOutLogger implements Runnable {
    PrintStream originalOut;
    Experiment experiment;
    BufferedReader stdoutReader;
    int offset = 0;
    Thread loggerThread;

    private StdOutLogger(PrintStream originalOut, Experiment experiment, BufferedReader stdoutReader) {
        this.originalOut = originalOut;
        this.experiment = experiment;
        this.stdoutReader = stdoutReader;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                System.err.println("reading line");
                String line = stdoutReader.readLine();
                System.err.println("read line: " + line);
                experiment.logLine(line, offset);
                offset++;
            } catch (IOException ex) {
                break;
            }
        }
    }

    public void stop() throws IOException {
        System.setOut(originalOut);
        //stdoutReader.close();
        System.err.println("closed");
    }

    public static StdOutLogger createStdoutLogger(Experiment experiment) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        PrintStream originalOut = System.out;
        OutputStream copyStream = new CopyOutputStream(originalOut, out);
        System.setOut(new PrintStream(copyStream));
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(in)));
        StdOutLogger logger = new StdOutLogger(originalOut, experiment, stdoutReader);
        Thread loggerThread = new Thread(logger);
        loggerThread.start();
        return logger;
    }
}
