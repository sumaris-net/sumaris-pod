package net.sumaris.core.util.file.impl;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.file.FileOperation;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.LinkedList;

@Getter
@Setter
@Slf4j
public class CompositeFileOperation extends AbstractFileOperation {

    private Collection<FileOperation> operations;

    public CompositeFileOperation() {
    }

    public FileOperation call() throws Exception {
        Preconditions.checkArgument(!this.isExecuted, "File operation already executed");
        Preconditions.checkArgument(!this.isCancelled, "File operation already cancelled.");
        LinkedList<FileOperation> undos = new LinkedList<>();

        for (FileOperation operation : this.operations) {
            FileOperation undo = operation.call();
            if (undo != null) {
                undos.add(0, undo); // Inverse order
            }
        }

        this.isExecuted = true;
        if (CollectionUtils.isEmpty(undos)) {
            return null;
        } else {
            CompositeFileOperation undoOperation = new CompositeFileOperation();
            undoOperation.setOperations(undos);
            return undoOperation;
        }
    }

    public void doCancel() {
        for (FileOperation operation : this.operations) {
            operation.cancel();
        }

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Composite operation:");

        for (FileOperation operation : this.operations) {
            sb.append("\n - ").append(operation);
        }

        return sb.toString();
    }
}
