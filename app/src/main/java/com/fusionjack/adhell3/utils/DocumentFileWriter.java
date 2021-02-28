package com.fusionjack.adhell3.utils;

import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

// Create the file if it doesn't exist and either append or overwrite the existing file
public final class DocumentFileWriter implements AutoCloseable {

    private static final byte[] LINE_SEPARATOR = System.lineSeparator().getBytes();

    private Optional<OutputStream> out;
    private boolean appendMode;

    private DocumentFileWriter() {
        this.out = Optional.empty();
    }

    public static DocumentFileWriter overrideMode(String fileName) throws IOException {
        DocumentFileWriter writer = new DocumentFileWriter();
        writer.openOutputStream(fileName, false);
        return writer;
    }

    public static DocumentFileWriter appendMode(String fileName) throws IOException {
        DocumentFileWriter writer = new DocumentFileWriter();
        writer.openOutputStream(fileName, true);
        return writer;
    }

    private void openOutputStream(String fileName, boolean append) throws IOException {
        this.appendMode = append;
        DocumentFile file = DocumentFileUtils.findFile(fileName);
        if (file == null) {
            file = DocumentFileUtils.createFile(fileName);
        }
        this.out = DocumentFileUtils.getOutputStreamFrom(file, append);
    }

    public Optional<OutputStream> getOutputStream() {
        return out;
    }

    public void write(String content) throws IOException {
        if (out.isPresent()) {
            if (appendMode) out.get().write(LINE_SEPARATOR);
            out.get().write(content.getBytes());
            if (!appendMode) out.get().write(LINE_SEPARATOR);
        }
    }

    @Override
    public void close() throws Exception {
        if (out.isPresent()) {
            out.get().flush();
            out.get().close();
        }
    }
}
