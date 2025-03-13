package net.sumaris.core.util.file;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StreamUtils;
import net.sumaris.core.util.file.impl.BasicFileOperation;
import net.sumaris.core.util.file.impl.CompositeFileOperation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@Slf4j
public class FileOperationBuilder {
    private File src;
    private File dest;
    private BasicFileOperation.Type basicType;
    private boolean enableLock = false;
    private boolean enableUndo = false;
    private Runnable onCancelHandler;
    private boolean silentIfError = false;
    private List<FileOperation> subOperations = null;
    private List<FileOperationBuilder> subBuilders = null;

    public static FileOperationBuilder prepareCopy(File src, File dest) throws IOException {
        Preconditions.checkNotNull(src);
        Preconditions.checkNotNull(dest);
        FileOperationBuilder result = new FileOperationBuilder();
        if (!src.exists() && !src.isFile()) {
            throw new FileNotFoundException("File not found, or invalid path: " + src.getPath());
        } else {
            result.basicType = BasicFileOperation.Type.COPY;
            result.src = src;
            result.dest = dest;
            return result;
        }
    }

    public static FileOperationBuilder prepareDelete(File file) {
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(file.exists() && file.isFile(), "Invalid file");
        FileOperationBuilder result = new FileOperationBuilder();
        result.basicType = BasicFileOperation.Type.DELETE_FILE;
        result.dest = file;
        return result;
    }


    public static FileOperationBuilder prepareDeleteDir(File directory) {
        Preconditions.checkNotNull(directory);
        Preconditions.checkArgument(directory.exists() && directory.isDirectory(), "Invalid directory");
        FileOperationBuilder result = new FileOperationBuilder();
        result.basicType = BasicFileOperation.Type.DELETE_DIRECTORY;
        result.dest = directory;
        return result;
    }

    public static FileOperationBuilder prepareComposite(FileOperation... operations) {
        return prepareComposite(ImmutableList.copyOf(operations));
    }

    public static FileOperationBuilder prepareComposite(FileOperationBuilder... operations) {
        return prepareComposite(ImmutableList.copyOf(operations));
    }

    public static <T> FileOperationBuilder prepareComposite(Iterable<T> operations) {
        FileOperationBuilder result = prepareComposite();
        Beans.getList(operations).forEach(operation -> {
            if (operation instanceof FileOperationBuilder subBuilder) {
                result.subBuilders.add(subBuilder);
            } else if (operation instanceof FileOperation subOperation) {
                result.subOperations.add(subOperation);
            } else {
                throw new SumarisTechnicalException(String.format("Unknown operation class '%s'. Can only add %s or %s",
                    operation.getClass().getName(),
                    FileOperation.class.getName(),
                    FileOperationBuilder.class.getName()
                ));
            }
        });
        return result;
    }

    public static FileOperationBuilder prepareComposite() {
        FileOperationBuilder result = new FileOperationBuilder();
        result.subOperations = Lists.newArrayList();
        result.subBuilders = Lists.newArrayList();
        return result;
    }

    public static FileOperationBuilder prepareDeletes(File... files) {
        return prepareComposite(
            StreamUtils.getStream(files).map(FileOperationBuilder::prepareDelete).toList()
        );
    }

    public static FileOperationBuilder prepareDeletes(Iterable<File> files) {
        return prepareComposite(
            StreamUtils.getStream(files).map(FileOperationBuilder::prepareDelete).toList()
        );
    }

    public static FileOperationBuilder prepareMove(File src, File dest) throws IOException {
        return prepareComposite(
            prepareCopy(src, dest),
            prepareDelete(src)
        );
    }

    private FileOperationBuilder() {
    }

    public FileOperationBuilder onlyIfEmpty() {
        Preconditions.checkArgument(this.basicType == BasicFileOperation.Type.DELETE_DIRECTORY || this.basicType == BasicFileOperation.Type.DELETE_DIRECTORY_IF_EMPTY);
        this.basicType = BasicFileOperation.Type.DELETE_DIRECTORY_IF_EMPTY;
        return this;
    }

    public FileOperationBuilder withLock() {
        this.enableLock = true;
        return this;
    }

    public FileOperationBuilder withUndo() {
        this.enableUndo = true;
        return this;
    }

    public FileOperationBuilder silentIfError() {
        this.silentIfError = true;
        return this;
    }

    public FileOperationBuilder onCancel(Runnable fileOperation) {
        this.onCancelHandler = fileOperation;
        return this;
    }

    public FileOperationBuilder add(FileOperation fileOperation) {
        Preconditions.checkArgument(this.subOperations != null || this.subBuilders != null, "Should be a composite builder. Please use prepareComposite() or prepareCompositeBuilder()");
        if (this.subOperations == null) this.subOperations = Lists.newArrayList();
        this.subOperations.add(fileOperation);
        return this;
    }

    public FileOperationBuilder add(FileOperationBuilder fileOperationBuilder) {
        Preconditions.checkArgument(this.subOperations != null || this.subBuilders != null, "Should be a composite builder. Please use prepareComposite() or prepareCompositeBuilder()");
        if (this.subBuilders == null) this.subBuilders = Lists.newArrayList();
        this.subBuilders.add(fileOperationBuilder);
        return this;
    }

    public FileOperation build() throws IOException {

        // Build and add sub builders
        if (this.subBuilders != null) {
            if (this.subOperations == null) this.subOperations = Lists.newArrayList();
            for (FileOperationBuilder builder : this.subBuilders) {
                // Configure the child builder, using the root builder
                if (this.enableUndo) builder.withUndo();
                if (this.enableLock) builder.withLock();
                if (this.silentIfError) builder.silentIfError();
                if (this.onCancelHandler != null && builder.onCancelHandler == null)
                    builder.onCancel(this.onCancelHandler);

                // Add into composite operations
                this.subOperations.add(builder.build());
            }

            // Reset builders
            this.subBuilders = null;
        }

        // Create composite operation
        if (this.subOperations != null) {
            CompositeFileOperation result = new CompositeFileOperation();
            result.setOperations(this.subOperations);
            return result;
        }

        // Create basic operation
        else {
            BasicFileOperation result = new BasicFileOperation();
            result.setSrc(this.src);
            result.setDest(this.dest);
            result.setType(this.basicType);
            result.setEnableUndo(this.enableUndo);
            result.setSilentIfError(this.silentIfError);
            result.setOnCancel(this.onCancelHandler);
            if (this.enableLock) {
                result.lock();
            }

            return result;
        }
    }
}
