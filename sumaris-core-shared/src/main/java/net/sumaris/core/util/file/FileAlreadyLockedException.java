package net.sumaris.core.util.file;

import java.io.IOException;

public class FileAlreadyLockedException extends IOException {
    public FileAlreadyLockedException() {
    }

    public FileAlreadyLockedException(String message) {
        super(message);
    }

    public FileAlreadyLockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileAlreadyLockedException(Throwable cause) {
        super(cause);
    }
}
