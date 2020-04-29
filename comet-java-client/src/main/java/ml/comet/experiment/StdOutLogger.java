package ml.comet.experiment;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StdOutLogger implements Runnable {
    static AtomicInteger offset = new AtomicInteger();

    PrintStream original;
    OnlineExperiment onlineExperiment;
    BufferedReader reader;
    boolean stdOut;

    private StdOutLogger(PrintStream original, OnlineExperiment onlineExperiment, BufferedReader reader, boolean stdOut) {
        this.original = original;
        this.onlineExperiment = onlineExperiment;
        this.reader = reader;
        this.stdOut = stdOut;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                String line = reader.readLine();
                onlineExperiment.logLine(line, offset.incrementAndGet(), !stdOut);
            } catch (IOException ex) {
                break;
            }
        }
    }

    public void stop() {
        if (stdOut) {
            System.setOut(original);
        } else {
            System.setErr(original);
        }
    }

    public static StdOutLogger createStdoutLogger(OnlineExperiment onlineExperiment) throws IOException {
        return createLogger(onlineExperiment, System.out,true);
    }

    public static StdOutLogger createStderrLogger(OnlineExperiment onlineExperiment) throws IOException {
        return createLogger(onlineExperiment, System.err, false);
    }

    private static StdOutLogger createLogger(OnlineExperiment onlineExperiment, PrintStream original, boolean stdOut) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        OutputStream copyStream = new CopyOutputStream(original, out);
        PrintStream replacement = new PrintStream(copyStream);
        if (stdOut) {
            System.setOut(replacement);
        } else {
            System.setErr(replacement);
        }

        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(in)));
        StdOutLogger logger = new StdOutLogger(original, onlineExperiment, stdoutReader, stdOut);
        Thread loggerThread = new Thread(logger);
        loggerThread.start();
        return logger;
    }
}
