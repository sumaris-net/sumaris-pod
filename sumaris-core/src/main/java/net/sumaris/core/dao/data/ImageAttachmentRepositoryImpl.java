package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.Images;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.file.FileOperation;
import net.sumaris.core.util.file.FileOperationBuilder;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/**
 * @author peck7 on 31/08/2020.
 */
@Slf4j
public class ImageAttachmentRepositoryImpl
    extends DataRepositoryImpl<ImageAttachment, ImageAttachmentVO, ImageAttachmentFilterVO, ImageAttachmentFetchOptions>
    implements ImageAttachmentSpecifications {

    @Autowired
    protected ReferentialDao referentialDao;

    protected boolean enableImagesDirectory;
    protected File imagesDirectory;
    protected File tempDirectory;

    protected ImageAttachmentRepositoryImpl(EntityManager entityManager) {
        super(ImageAttachment.class, ImageAttachmentVO.class, entityManager);
        setLockForUpdate(false);
        setCheckUpdateDate(false);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        boolean enableImagesDirectory = configuration.enableDataImagesDirectory();
        this.imagesDirectory = configuration.getImagesDirectory();
        this.tempDirectory = configuration.getTempDirectory();

        // Create images dir, if need
        if (enableImagesDirectory && !Files.exists(this.imagesDirectory.toPath())) {
            log.info("Creating images directory {}", this.imagesDirectory);
            try {
                Files.forceMkdir(imagesDirectory);
            } catch (IOException e) {
                log.error("Failed to create images directory {}", this.imagesDirectory, e);
                enableImagesDirectory = false; // Continue
            }
        }

        this.enableImagesDirectory = enableImagesDirectory;
    }

    @Override
    public ImageAttachmentVO save(ImageAttachmentVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getContentType());
        Preconditions.checkNotNull(source.getDateTime());
        Preconditions.checkNotNull(source.getRecorderDepartment());
        Preconditions.checkNotNull(source.getRecorderDepartment().getId());
        Preconditions.checkArgument(StringUtils.isNotBlank(source.getPath()) || source.getContent() != null);
        Preconditions.checkArgument(
            (!enableImagesDirectory && source.getObjectId() == null && source.getObjectTypeId() == null)
                || (source.getObjectId() != null && source.getObjectTypeId() != null));

        // Save content as file
        if (enableImagesDirectory && source.getContent() != null) {
            String objectTypeLabel = ObjectTypeEnum.valueOf(source.getObjectTypeId()).getLabel();

            // Read content
            byte[] bytesContent = Base64.getDecoder().decode(source.getContent());
            source.setContent(null);

            // If new, save without content and using a fake path
            // We use a fake path, because can path is required in Adagio DB (will be override later)
            if (source.getId() == null) {
                // TODO remove previous path file - should never happen
                // => load previous entity, then delete file

                source.setPath("-");
                source = super.save(source);
            }

            // Create the file (into the temporary dir)
            String relativePath = Images.saveImage(
                bytesContent,
                source.getContentType(),
                objectTypeLabel,
                source.getObjectId(),
                source.getId(),
                tempDirectory
            );
            source.setPath(relativePath);

            try {
                LinkedList<File> tempFiles = new LinkedList<>(Images.getAllImageFiles(new File(tempDirectory, relativePath)));
                LinkedList<File> destFiles = new LinkedList<>(Images.getAllImageFiles(new File(imagesDirectory, relativePath)));

                FileOperationBuilder copyOperationBuilder = FileOperationBuilder.prepareComposite();
                FileOperationBuilder deleteOperationBuilder = FileOperationBuilder.prepareComposite();
                while (!tempFiles.isEmpty()) {
                    File src = tempFiles.pop();
                    File dest = destFiles.pop();
                    copyOperationBuilder.add(
                        FileOperationBuilder.prepareCopy(src, dest).withLock().withUndo()
                    );
                    deleteOperationBuilder.add(FileOperationBuilder.prepareDelete(src).silentIfError());
                }

                // Execute copy, and deletion
                executeFileOperation(FileOperationBuilder.prepareComposite(copyOperationBuilder, deleteOperationBuilder));

            } catch (Exception e) {
                throw new SumarisTechnicalException("Failed to copy image files", e);
            }
        }

        // Save
        ImageAttachmentVO target = super.save(source);
        
        return target;
    }

    @Override
    public void deleteAllByObjectId(int objectId, int objectTypeId) {
        List<ImageAttachment> entities = findAll(hasObjectId(objectId)
            .and(hasObjectTypeId(objectTypeId)));
        deleteAll(entities);
    }

    public void deleteAllByObjectIds(Iterable<Integer> objectIds, int objectTypeId) {
        List<ImageAttachment> entities = findAll(hasObjectIds(Beans.asIntegerArray(objectIds))
            .and(hasObjectTypeId(objectTypeId)));
        deleteAll(entities);
    }

    @Override
    public void deleteAllById(Iterable<? extends Integer> ids) {
        List<ImageAttachment> entities = findAll(includedIds(Beans.asIntegerArray(ids)));
        deleteAll(entities);
    }

    @Override
    public void deleteAllInBatch(@NonNull Iterable<ImageAttachment> entities) {
        super.deleteAllInBatch(entities);
        deleteFiles(entities);
    }

    @Override
    public void delete(@NonNull ImageAttachment entity) {
        super.delete(entity);
        deleteImageFiles(entity);
    }

    @Override
    public void toEntity(ImageAttachmentVO source, ImageAttachment target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

        // Object type
        if (source.getObjectTypeId() != null || copyIfNull) {
            if (source.getObjectTypeId() == null) {
                target.setObjectType(null);
            } else {
                target.setObjectType(getReference(ObjectType.class, source.getObjectTypeId()));
            }
        }
    }

    @Override
    public void toVO(ImageAttachment source, ImageAttachmentVO target, ImageAttachmentFetchOptions fetchOptions, boolean copyIfNull) {

        Beans.copyProperties(source, target, ImageAttachment.Fields.CONTENT /*Avoid to fetch Lob properties*/);

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Recorder department
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = departmentRepository.toVO(source.getRecorderDepartment());
            target.setRecorderDepartment(recorderDepartment);
        }

        // Object type
        if (source.getObjectType() != null) {
            target.setObjectTypeId(source.getObjectType().getId());
        } else {
            target.setObjectTypeId(null);
        }

        // Fetch content only not a file image (no path) and when need to be fetched (e.g. from ImageRestController)
        if (target.getPath() == null && fetchOptions != null && fetchOptions.isWithContent()) {
            target.setContent(source.getContent());
        }
    }

    @Override
    protected void onBeforeSaveEntity(ImageAttachmentVO source, ImageAttachment target, boolean isNew) {
        super.onBeforeSaveEntity(source, target, isNew);

        // When new entity: set the creation date
        if (isNew || target.getCreationDate() == null) {
            target.setCreationDate(target.getUpdateDate());
        }
    }

    @Override
    protected void onAfterSaveEntity(ImageAttachmentVO vo, ImageAttachment savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }
    }

    @Override
    protected Specification<ImageAttachment> toSpecification(ImageAttachmentFilterVO filter, ImageAttachmentFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(includedIds(filter.getIncludeIds()))
            .and(includedIds(filter.getExcludeIds()))
            .and(hasRecorderPersonId(filter.getRecorderPersonId()))
            .and(hasObjectIds(concat(filter.getObjectId(), filter.getObjectIds())))
            .and(hasObjectTypeId(filter.getObjectTypeId()));
    }

    protected void deleteFiles(Iterable<ImageAttachment> entities) {
        if (entities == null) return; // Skip if empty
        entities.forEach(this::deleteImageFiles);
    }

    protected void deleteImageFiles(ImageAttachment entity) {
        if (!enableImagesDirectory || this.imagesDirectory == null)
            return; // Skip if images are not stored into a directory

        if (StringUtils.isBlank(entity.getPath())) return; // Skip if no path

        // Prepare a deletion operation, of images files
        List<File> imageFiles = Images.getAllImageFiles(new File(imagesDirectory, entity.getPath()), Files::exists);
        if (CollectionUtils.isEmpty(imageFiles)) return; // No file to delete

        // Execute deletions (with undo + lock)
        executeFileOperation(FileOperationBuilder.prepareDeletes(imageFiles).withUndo().withLock());

    }

    /**
     * Executes a file operation using the provided {@code FileOperationBuilder}.
     * The method ensures transactional safety by registering synchronization
     * callbacks to commit or rollback the operation and its matching undo operation.
     *
     * @param fileOperationBuilder the builder for creating the file operation object
     *                             with necessary configurations
     */
    protected void executeFileOperation(FileOperationBuilder fileOperationBuilder) {
        try (FileOperation fileOperation = fileOperationBuilder.build()) {
            executeFileOperation(fileOperation);
        } catch (Exception e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /**
     * Executes a given file operation and manages its transactional behavior, ensuring
     * proper cleanup and rollback in case of transaction failure.
     *
     * @param fileOperation The file operation to execute. This must implement the {@link FileOperation}
     *                      interface and provide logic for file manipulation and undo operation.
     * @throws Exception If an error occurs during the execution of the file operation or its undo logic.
     */
    protected void executeFileOperation(FileOperation fileOperation) throws Exception {
        // Execute the operation
        FileOperation undoOperation = fileOperation.call();

        if (undoOperation == null) return; // Exit if no undo operation

        // Manage transaction commit/rollback
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                // Execute the rollback operation
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        undoOperation.call();
                    } catch (Exception e) {
                        // Continue (close)
                    }
                }

                // Close the undo operation (e.g. release file locks)
                IOUtils.closeQuietly(undoOperation);
            }
        });
    }
}
