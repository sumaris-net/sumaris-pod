package net.sumaris.core.util.file;

import java.io.Closeable;
import java.util.concurrent.Callable;

public interface FileOperation extends Callable<FileOperation>, Runnable, Closeable {
    void cancel();
}
