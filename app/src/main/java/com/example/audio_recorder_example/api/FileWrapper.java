package com.example.audio_recorder_example.api;
import java.io.File;

public class FileWrapper {
    private File file;

    public FileWrapper(String filePath) {
        this.file = new File(filePath);
    }
    public File getFile() {
        return file;
    }

    public String getFileName() {
        return file.getName();
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }
}
