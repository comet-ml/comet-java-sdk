package ml.comet.experiment.log;

import ml.comet.experiment.OnlineExperiment;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

public class StdOutLogger implements Runnable {
    static AtomicLong offset = new AtomicLong();

    PrintStream original;
    OnlineExperiment experiment;
    BufferedReader reader;
    boolean stdOut;

    private StdOutLogger(PrintStream original, OnlineExperiment experiment, BufferedReader reader, boolean stdOut) {
        this.original = original;
        this.experiment = experiment;
        this.reader = reader;
        this.stdOut = stdOut;
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                String line = reader.readLine();
                experiment.logLine(line, offset.incrementAndGet(), !stdOut);
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

    public static StdOutLogger createStdoutLogger(OnlineExperiment experiment) throws IOException {
        return createLogger(experiment, System.out, true);
    }

    public static StdOutLogger createStderrLogger(OnlineExperiment experiment) throws IOException {
        return createLogger(experiment, System.err, false);
    }

    private static StdOutLogger createLogger(OnlineExperiment experiment, PrintStream original, boolean stdOut) throws IOException {
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
        StdOutLogger logger = new StdOutLogger(original, experiment, stdoutReader, stdOut);
        Thread loggerThread = new Thread(logger);
        loggerThread.start();
        return logger;
    }
}
