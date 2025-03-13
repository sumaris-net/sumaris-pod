package net.sumaris.core.util.file.impl;

import com.google.common.base.Preconditions;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.file.FileOperation;

import java.io.IOException;

@Slf4j
public abstract class AbstractFileOperation implements FileOperation {

    @Setter
    private Runnable onCancel;
    protected boolean isExecuted = false;
    protected boolean isCancelled = false;
    @Setter
    protected boolean enableUndo = false;
    @Setter
    protected boolean silentIfError = false;

    public AbstractFileOperation() {
    }

    public boolean isUndoEnable() {
        return this.enableUndo;
    }

    public boolean isSilentIfError() {
        return this.silentIfError;
    }

    public void run() {
        try {
            this.call();
        } catch (Exception e) {
            throw new SumarisTechnicalException(e);
        }
    }

    public FileOperation call() throws Exception {
        Preconditions.checkArgument(!this.isExecuted, "File operation already executed");
        Preconditions.checkArgument(!this.isCancelled, "File operation already cancelled.");

        FileOperation undoOperation;
        try {
            undoOperation = this.enableUndo ? this.createUndoOperation() : null;
        } catch (IOException e) {
            throw new SumarisTechnicalException(e);
        }

        this.doRun();
        this.isExecuted = true;
        return undoOperation;
    }

    protected void doRun() {
    }

    public void close() {
        if (!this.isExecuted && !this.isCancelled) {
            log.debug("Cancelling file operation: " + this);
            this.cancel();
        }
    }

    public void cancel() {
        Preconditions.checkArgument(!this.isExecuted, "Unable to cancel a executed file operation.");
        Preconditions.checkArgument(!this.isCancelled, "File operation already cancelled.");
        this.doCancel();
        this.isCancelled = true;
        if (this.onCancel != null) {
            try {
                this.onCancel.run();
            } catch (Throwable e) {
                this.manageException(e);
            }
        }

    }

    protected abstract void doCancel();

    protected FileOperation createUndoOperation() throws IOException {
        return null;
    }

    protected void manageException(Throwable e) {
        if (this.silentIfError) {
            log.warn(e.getMessage());
        } else {
            throw new SumarisTechnicalException(e);
        }
    }
}
