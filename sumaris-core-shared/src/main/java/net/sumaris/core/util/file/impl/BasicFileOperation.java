package net.sumaris.core.util.file.impl;

import lombok.Getter;
import lombok.Setter;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.util.file.FileLock;
import net.sumaris.core.util.file.FileOperation;
import net.sumaris.core.util.file.FileOperationBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class BasicFileOperation extends AbstractFileOperation {
    private static final Log log = LogFactory.getLog(BasicFileOperation.class);
    private Closeable srcLock;
    private Closeable destLock;
    private boolean destIsTouch = false;
    private File tempDir;

    @Getter
    @Setter
    private File src;

    @Getter
    @Setter
    private File dest;

    @Getter
    @Setter
    private Type type;

    public BasicFileOperation() {
    }

    public void doRun() {
        switch (this.type) {
            case COPY:
                try {
                    try {
                        IOUtils.closeQuietly(this.destLock);
                        this.copyFile(this.src.toPath(), this.dest.toPath());
                    } catch (IOException e) {
                        this.manageException(e);
                    }
                    break;
                } finally {
                    IOUtils.closeQuietly(this.srcLock);
                }
            case DELETE_DIRECTORY_IF_EMPTY:
                String[] files = this.dest.list();
                if (ArrayUtils.isNotEmpty(files)) {
                    break;
                }
            case DELETE_DIRECTORY:
            case DELETE_FILE:
                try {
                    IOUtils.closeQuietly(this.destLock);
                    FileUtils.forceDelete(this.dest);
                } catch (IOException e) {
                    this.manageException(e);
                }
        }

    }

    public void doCancel() {
        switch (this.type) {
            case COPY:
                IOUtils.closeQuietly(this.srcLock);
                IOUtils.closeQuietly(this.destLock);
                if (this.destIsTouch) {
                    try {
                        this.createDeleteTouchedFileOperation().run();
                    } catch (Exception ignored) {
                    }
                }
                break;
            case DELETE_DIRECTORY_IF_EMPTY:
            case DELETE_DIRECTORY:
            case DELETE_FILE:
                if (this.destLock != null) {
                    IOUtils.closeQuietly(this.destLock);
                }
        }

    }

    public String toString() {
        switch (this.type) {
            case COPY:
                return String.format("Copy [%s] -> [%s]", this.src.getPath(), this.dest.getPath());
            case DELETE_DIRECTORY_IF_EMPTY:
                return String.format("Delete dir (if empty) [%s]", this.dest.getPath());
            case DELETE_DIRECTORY:
                return String.format("Delete dir [%s]", this.dest.getPath());
            case DELETE_FILE:
                return String.format("Delete [%s]", this.dest.getPath());
            default:
                return this.type.name();
        }
    }

    public void lock() throws IOException {
        switch (this.type) {
            case COPY:
                this.srcLock = FileLock.lock(this.src);
                if (!this.dest.exists()) {
                    FileUtils.touch(this.dest);
                    this.destIsTouch = true;
                }

                this.destLock = FileLock.lock(this.dest);
            case DELETE_DIRECTORY_IF_EMPTY:
            default:
                break;
            case DELETE_DIRECTORY:
            case DELETE_FILE:
                this.destLock = FileLock.lock(this.dest);
        }

    }

    protected FileOperation createUndoOperation() throws IOException {
        switch (this.type) {
            case COPY:
            case DELETE_FILE:
                if (!this.destIsTouch && this.dest.exists()) {
                    File tempFile = this.createTempFile(this.dest);
                    FileUtils.copyFile(this.dest, tempFile);
                    return FileOperationBuilder.prepareCopy(tempFile, this.dest).onCancel(FileOperationBuilder.prepareDelete(tempFile).silentIfError().build()).build();
                } else if (this.destIsTouch) {
                    return this.createDeleteTouchedFileOperation();
                }
            case DELETE_DIRECTORY_IF_EMPTY:
            case DELETE_DIRECTORY:
            default:
                return null;
        }
    }

    private void copyFile(Path src, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());

        try (SeekableByteChannel inputChannel = Files.newByteChannel(src, StandardOpenOption.READ)) {
            SeekableByteChannel outputChannel = Files.newByteChannel(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            Throwable var6 = null;

            try {
                ByteBuffer buffer = ByteBuffer.allocateDirect(131072);

                while (inputChannel.read(buffer) != -1) {
                    buffer.flip();

                    while (buffer.hasRemaining()) {
                        outputChannel.write(buffer);
                    }

                    buffer.clear();
                }
            } catch (Throwable var29) {
                var6 = var29;
                throw var29;
            } finally {
                if (outputChannel != null) {
                    if (var6 != null) {
                        try {
                            outputChannel.close();
                        } catch (Throwable var28) {
                            var6.addSuppressed(var28);
                        }
                    } else {
                        outputChannel.close();
                    }
                }

            }
        }

    }

    private File createTempFile(File file) throws IOException {
        if (this.tempDir == null) {
            this.tempDir = SumarisConfiguration.getInstance().getTempDirectory();
        }

        if (this.tempDir != null && !this.tempDir.exists()) {
            FileUtils.forceMkdir(this.tempDir);
        }

        return File.createTempFile(file.getName(), ".undo", this.tempDir);
    }

    private FileOperation createDeleteTouchedFileOperation() throws IOException {
        return this.dest.getParentFile() != null
            ? FileOperationBuilder.prepareComposite(FileOperationBuilder.prepareDelete(this.dest).silentIfError().build(), FileOperationBuilder.prepareDeleteDir(this.dest.getParentFile()).silentIfError().onlyIfEmpty().build()).build()
            : FileOperationBuilder.prepareDelete(this.dest).silentIfError().build();
    }

    public enum Type {
        COPY,
        MOVE,
        DELETE_FILE,
        DELETE_DIRECTORY,
        DELETE_DIRECTORY_IF_EMPTY
    }
}
