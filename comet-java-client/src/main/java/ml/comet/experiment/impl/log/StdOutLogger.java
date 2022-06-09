package ml.comet.experiment.impl.log;

import lombok.NonNull;
import ml.comet.experiment.OnlineExperiment;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The logger to capture StdOut/StdErr streams and log collected text to the Comet.
 */
public class StdOutLogger implements Runnable, Closeable {
    final AtomicLong offset = new AtomicLong();

    OutputStream outputStream;
    InputStream inputStream;
    PrintStream original;
    OnlineExperiment experiment;
    boolean stdOut;

    /**
     * Creates logger instance that captures StdOut stream for a given OnlineExperiment.
     *
     * @param experiment the OnlineExperiment instance
     * @return the initialized StdOutLogger instance.
     * @throws IOException if any I/O exception occurs.
     */
    public static StdOutLogger createStdoutLogger(@NonNull OnlineExperiment experiment) throws IOException {
        return createLogger(experiment, System.out, true);
    }

    /**
     * Creates logger instance that captures StdErr stream for a given OnlineExperiment.
     *
     * @param experiment the OnlineExperiment instance
     * @return the initialized StdOutLogger instance.
     * @throws IOException if any I/O exception occurs.
     */
    public static StdOutLogger createStderrLogger(@NonNull OnlineExperiment experiment) throws IOException {
        return createLogger(experiment, System.err, false);
    }

    /**
     * Closes this logger and release any hold resources.
     *
     * @throws IOException if I/O exception occurs.
     */
    @Override
    public void close() throws IOException {
        this.restoreOriginalAndStop();

        // close output stream to release resources - this will cause logger thread to stop as well.
        this.outputStream.close();
    }

    /**
     * Flushes this logger by flushing intercepted system stream. It is recommended to call this method before closing
     * this logger to make sure that all outputs properly propagated.
     */
    public void flush() {
        if (this.stdOut) {
            System.out.flush();
        } else {
            System.err.flush();
        }
    }

    private StdOutLogger(PrintStream original, OnlineExperiment experiment,
                         InputStream in, OutputStream out, boolean stdOut) {
        this.original = original;
        this.experiment = experiment;
        this.inputStream = in;
        this.outputStream = out;
        this.stdOut = stdOut;
    }

    @SuppressWarnings("checkstyle:EmptyCatchBlock")
    private void restoreOriginalAndStop() {
        // close input stream for graceful interception stop
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException ignore) {
            }
        }

        if (this.stdOut) {
            System.setOut(original);
        } else {
            System.setErr(original);
        }
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                experiment.logLine(line.concat("\n"), offset.incrementAndGet(), !stdOut);
            }
        } catch (Throwable t) {
            // restore original
            this.restoreOriginalAndStop();
            // nothing to do except to inform
            if (this.stdOut) {
                System.out.println("\nStdOut capturing error occurred");
            } else {
                System.out.println("\nStdErr capturing error occurred");
            }
            t.printStackTrace();
        }

        if (this.stdOut) {
            System.out.println("\nStdOut interception stopped");
        } else {
            System.out.println("\nStdErr interception stopped");
        }
    }

    private static StdOutLogger createLogger(@NonNull OnlineExperiment experiment,
                                             @NonNull PrintStream original, boolean stdOut) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        OutputStream copyStream = new CopyOutputStream(original, out);
        PrintStream replacement = new PrintStream(copyStream);
        if (stdOut) {
            System.setOut(replacement);
        } else {
            System.setErr(replacement);
        }

        StdOutLogger logger = new StdOutLogger(original, experiment, in, out, stdOut);
        Thread loggerThread = new Thread(logger);
        loggerThread.setDaemon(true);
        loggerThread.start();
        return logger;
    }
}
