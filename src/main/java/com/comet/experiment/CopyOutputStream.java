package com.comet.experiment;


import java.io.IOException;
import java.io.OutputStream;

// https://stackoverflow.com/questions/6480542/java-read-what-another-thread-is-writing-to-stdout-stderr-how-to
class CopyOutputStream extends OutputStream {
    private final OutputStream str1;
    private final OutputStream str2;

    public CopyOutputStream(OutputStream str1, OutputStream str2) {
        this.str1 = str1;
        this.str2 = str2;
    }

    @Override
    public void write(int b) throws IOException {
        str1.write(b);
        str2.write(b);
    }

    @Override
    public void close() throws IOException {
        try {
            str1.close();
        } finally {
            str2.close();
        }
    }
}