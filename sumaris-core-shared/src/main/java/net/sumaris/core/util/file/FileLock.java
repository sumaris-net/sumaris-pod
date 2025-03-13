package net.sumaris.core.util.file;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import net.sumaris.core.config.SumarisConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FileLock implements Closeable {
    private static final Log log = LogFactory.getLog(FileLock.class);
    private static final boolean LOCK_ENABLED = Objects.equals("true", System.getProperty("lock.enable", "false"));
    private static final String FLOCK_CMD = "flock";
    private static final String[] FLOCK_SEARCH_PATHS = new String[]{"/usr/bin", "/usr/local/bin", "/opt/flock/bin"};
    private static final File UNIX_FLOCK_FILE;
    private static final boolean UNIX_FLOCK_ENABLED;
    private static final LoadingCache<String, List<String>> FILE_STORE_CACHE;
    private final File file;
    private final boolean nfs;
    private Closeable lock;

    public static FileLock lock(File file) throws IOException {
        FileLock fileLock = new FileLock(file);
        fileLock.lock();
        return fileLock;
    }

    private FileLock(File file) throws IOException {
        Preconditions.checkNotNull(file);
        this.file = file;
        this.nfs = checkNFS(file);
    }

    public void lock() throws IOException {
        if (this.lock != null) {
            throw new IOException("File already locked: " + this.file.getPath());
        } else {
            this.lock = this.isLockEnabled() ? (UNIX_FLOCK_ENABLED ? this.unixFlock() : this.javaLock()) : this.nullLock();
        }
    }

    private boolean isLockEnabled() {
        return LOCK_ENABLED || SumarisConfiguration.getInstance().isFileLockEnabled();
    }

    public void close() throws IOException {
        if (this.lock != null) {
            this.lock.close();
            this.lock = null;
        }

    }

    private static File searchProgram(String command, String... paths) {
        if (SystemUtils.IS_OS_UNIX) {
            for (String path : paths) {
                File file = new File(path, command);
                if (file.exists()) {
                    return file;
                }
            }
        }

        return null;
    }

    private static boolean checkNFS(File file) throws IOException {
        try {
            for (String nfsFileStore : FILE_STORE_CACHE.get("nfs")) {
                if (file.getCanonicalPath().startsWith(nfsFileStore)) {
                    return true;
                }
            }

            return false;
        } catch (ExecutionException e) {
            throw new IOException("Unable to determine file store type", e);
        }
    }

    private Closeable unixFlock() throws IOException {
        final Process p = this.execAndGetFlockProcess();
        return p::destroy;
    }

    private Process execAndGetFlockProcess() throws IOException {
        String command = String.format("%s --exclusive --nonblock %s %s cat", UNIX_FLOCK_FILE.getCanonicalPath(), log.isDebugEnabled() ? "--verbose" : "", this.file.getCanonicalPath());

        try {
            Process p = Runtime.getRuntime().exec(command);
            Thread.sleep(10L);

            int exitCode;
            try {
                exitCode = p.exitValue();
            } catch (IllegalThreadStateException var5) {
                return p;
            }

            if (exitCode == 0) {
                if (log.isDebugEnabled()) {
                    log.debug(read(p.getInputStream()));
                }

                return p;
            } else {
                String error = read(p.getErrorStream());
                if (StringUtils.isNotBlank(error)) {
                    if (log.isDebugEnabled()) {
                        log.debug(error);
                    }

                    throw new FileAlreadyLockedException(error);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("File already locked: " + this.file.getCanonicalPath());
                    }

                    throw new FileAlreadyLockedException("File is already locked: " + this.file.getCanonicalPath());
                }
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private static String read(InputStream is) throws IOException {
        StringBuilder response = new StringBuilder();
        BufferedReader b = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = b.readLine()) != null) {
            response.append("\n").append(line);
        }

        b.close();
        return !response.isEmpty() ? response.substring(1) : null;
    }

    private Closeable javaLock() throws IOException {
        return this.nfs ? this.softLock() : this.fileChannelLock();
    }

    private Closeable fileChannelLock() throws IOException {

        RandomAccessFile rwFile = new RandomAccessFile(this.file, "rw");
        try {
            FileChannel channel = rwFile.getChannel();
            final java.nio.channels.FileLock lock = channel.lock(0L, Long.MAX_VALUE, true);
            return () -> {
                try {
                    lock.close();
                } finally {
                    channel.close();
                }
            };
        } catch (OverlappingFileLockException e) {
            rwFile.close();
            throw new FileAlreadyLockedException(e.getMessage());
        }
    }

    private Closeable softLock() throws IOException {
        final File softLockFile = new File(this.file.getCanonicalPath() + ".lock");
        if (softLockFile.exists()) {
            throw new FileAlreadyLockedException("File is already locked (.lock file exists): " + this.file.getCanonicalPath());
        } else {
            FileUtils.touch(softLockFile);
            return new Closeable() {
                public void close() throws IOException {
                    FileUtils.forceDelete(softLockFile);
                }
            };
        }
    }

    private Closeable nullLock() {
        return new Closeable() {
            public void close() throws IOException {
            }
        };
    }

    static {
        UNIX_FLOCK_FILE = searchProgram(FLOCK_CMD, FLOCK_SEARCH_PATHS);
        UNIX_FLOCK_ENABLED = UNIX_FLOCK_FILE != null && Objects.equals("true", System.getProperty("flock.enable", "false"));
        FILE_STORE_CACHE = CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.MINUTES).build(new CacheLoader<>() {
            public @NonNull List<String> load(@NonNull String key) throws Exception {
                List<String> result = new ArrayList<>();
                FileSystem fs = FileSystems.getDefault();

                for (FileStore fileStore : fs.getFileStores()) {
                    if (key.equalsIgnoreCase(fileStore.type())) {
                        String fileStoreString = fileStore.toString();
                        result.add(fileStoreString.substring(0, fileStoreString.indexOf("(")).trim());
                    }
                }

                return result;
            }
        });
    }
}
