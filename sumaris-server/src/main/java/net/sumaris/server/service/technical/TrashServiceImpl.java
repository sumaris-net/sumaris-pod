/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.service.technical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.entity.EntityEventService;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.OperationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.nuiton.i18n.I18n;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("trashService")
@RequiredArgsConstructor
@ConditionalOnBean({EntityEventService.class})
@Slf4j
public class TrashServiceImpl implements TrashService, EntityEventService.Listener {

    private static final String CLASS_FILE_NAME = "class.info";
    private static final String FILE_PREFIX_PARENT = "%s#%s_";
    private static final String JSON_FILE_EXTENSION = "json";

    private boolean enable;
    private File trashDirectory;

    private final ObjectMapper objectMapper;
    private final SumarisConfiguration configuration;

    private final EntityEventService entityEventService;

    private EntityEventService.Disposable entityEventSubscription = null;


    @Override
    public <V> Page<V> findAll(@NonNull String entityName, @NonNull Pageable pageable, Class<? extends V> clazz) {

        // Make sure sort attribute is updateDate
        // This is because we don't want to deserialize all files, then sort, but we prefer sort on file date,
        // then only deserialize files from the current page
        String sortAttribute = IUpdateDateEntity.Fields.UPDATE_DATE;
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            sortAttribute = pageable.getSort().stream().map(Sort.Order::getProperty)
                .findFirst()
                .orElse(IUpdateDateEntity.Fields.UPDATE_DATE);
        }
        Preconditions.checkArgument(IUpdateDateEntity.Fields.UPDATE_DATE.equals(sortAttribute),
                String.format("Trash data can only be sorted on '%s'", IUpdateDateEntity.Fields.UPDATE_DATE));

        // Get sort direction
        boolean isDescending = SortDirection.fromSort(pageable.getSort()).orElse(SortDirection.DESC) == SortDirection.DESC;

        File directory = new File(trashDirectory, entityName);
        if (!directory.isDirectory()) return Page.empty(); // If not exists = empty
        if (!directory.canRead()) throw new SumarisTechnicalException("Cannot read the trash directory " + entityName);

        // Get all files in trash
        List<File> files = FileUtils.listFiles(directory, new String[]{JSON_FILE_EXTENSION}, false).stream()
            // Sort by date
            .sorted((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()) * (isDescending ? 1 : -1))
            .collect(Collectors.toList());

        // Slice result
        int total = files.size();
        int fromIndex = (int)pageable.getOffset();
        int endIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        Stream<File> fileStream = files.subList(fromIndex, endIndex).stream();

        // If only string is expected: return file content
        Stream<V> result;
        if (clazz != null && String.class.isAssignableFrom(clazz)) {
            result = fileStream.map(file -> {
                try {
                    return Files.readContent(file, Files.CHARSET_UTF8);
                }
                catch(IOException e) {
                    return null; // Keep null, because of page's total
                }
            })
            .map(content -> (V)content);
        }
        else {
            // Create readers for each classes
            List<ObjectReader> readers = getObjectReaders(directory, clazz);

            // Try to deserialize
            result = fileStream.map(file -> readAsObject(file, readers, clazz)
                    // Keep null values (e.g. when cannot deserialize), because of page's total
                    .orElse(null));
        }
        return new PageImpl<>(result.collect(Collectors.toList()), pageable, total);
    }

    @Override
    public <V> List<V> findAll(String entityName, net.sumaris.core.dao.technical.Page page, Class<? extends V> clazz) {
        return (List<V>)findAll(entityName, page.asPageable(), clazz).getContent();
    }

    @Override
    public <V> V getById(String entityName, Serializable id, Class<? extends V> clazz) {
        return findById(entityName, id, clazz)
                .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.error.trash.notFound")));
    }

    @Override
    public <V> Optional<V> findById(String entityName, Serializable id, Class<? extends V> clazz) {

        File directory = new File(trashDirectory, entityName);
        if (!directory.isDirectory()) return Optional.empty();
        if (!directory.canRead()) throw new SumarisTechnicalException("Cannot read the trash directory " + entityName);

        // Compute the file
        File file = new File(directory, getFileBasename(entityName, id));

        // Read as String
        if (clazz != null && String.class.isAssignableFrom(clazz)) {
            try {
                return Optional.of((V) Files.readContent(file, Files.CHARSET_UTF8));
            }
            catch(IOException e) {
                return Optional.empty();
            }
        }

        // Read as object
        else {
            return readAsObject(file, clazz);
        }
    }

    @Override
    public void delete(String entityName, Serializable id) {
        File directory = new File(trashDirectory, entityName);
        if (!directory.isDirectory()) return; // Not exists (or already deleted)
        if (!directory.canRead()) throw new SumarisTechnicalException("Cannot read the trash directory " + entityName);

        // Compute the file
        String filename = getFileBasename(entityName, id);

        if (log.isInfoEnabled()) {
            log.info("Delete {}#{} from trash {path: '{}/{}'}", entityName, id, entityName, filename);
        }

        File file = new File(directory, filename);
        Files.deleteQuietly(file);
    }

    @Override
    public long count(String entityName) {
        Preconditions.checkNotNull(entityName);
        File directory = new File(trashDirectory, entityName);
        if (!directory.isDirectory()) return 0L; // If not exists = empty
        checkCanRead(directory);

        // Get all files in trash
        return FileUtils.listFiles(directory, new String[]{JSON_FILE_EXTENSION}, false).size();
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        this.trashDirectory = configuration.getTrashDirectory();
        boolean enable = configuration.enableEntityTrash() && this.trashDirectory != null;
        boolean changed = enable != this.enable;

        if (enable) {
            try {
                FileUtils.forceMkdir(this.trashDirectory);
                checkTrashDirectory();
                if (changed) {
                    this.enable = true;
                    this.start();
                }
            } catch (Exception e) {
                log.error("Cannot enable trash service: " + e.getMessage());
                this.enable = false;
                configuration.setEnableTrash(false);
                this.stop();
            }
        }
        // Disable
        else if (changed) {
            this.enable = false;
            this.stop();
        }
    }

    @Override
    public void onDelete(EntityDeleteEvent event) {
        if (!this.enable) return;
        Object data = event.getData();
        if (data instanceof IValueObject<?>) {
            this.onEntityDeleted((IValueObject<?>)data);
        }
    }

    /* -- protected methods -- */

    protected void start() {
        this.entityEventSubscription = entityEventService.registerListener(this,
            Trip.class,
            Operation.class,
            ObservedLocation.class,
            Landing.class);
        log.info("Started trash service at {}", this.trashDirectory.getAbsolutePath());
    }

    protected void stop() {
        if (this.entityEventSubscription != null) {
            this.entityEventSubscription.dispose();
            this.entityEventSubscription = null;
            log.info("Stopped trash service");
        }
    }

    protected void onEntityDeleted(IValueObject<?> data) {
         // Skip

        Preconditions.checkNotNull(data);

        String entityName = data.getClass().getSimpleName();
        if (entityName.lastIndexOf("VO") == entityName.length() - 2) {
            entityName = entityName.substring(0, entityName.length() - 2);
        }

        try {
            File directory = new File(trashDirectory, entityName);
            FileUtils.forceMkdir(directory);

            // Read classes from class.info file
            File classFile = new File(directory, CLASS_FILE_NAME);
            String dataClassName = data.getClass().getCanonicalName();
            List<String> classNames = classFile.exists()
                    ? FileUtils.readLines(classFile, Files.CHARSET_UTF8)
                    : ImmutableList.of();
            // Append the current class (if need)
            if (!classNames.contains(dataClassName)) {
                try (FileWriter writer = new FileWriter(classFile, classFile.exists())) {
                    writer.write(dataClassName + "\n");
                    writer.flush();
                }
            }

            String filename = StringUtils.trimToEmpty(getFilePrefix(data))
                    + getFileBasename(entityName, data.getId());
            File file = new File(directory, filename);

            if (log.isInfoEnabled()) {
                log.info("Add {}#{} to trash {path: '{}/{}'}", entityName, data.getId(), entityName, filename);
            }

            try (FileWriter writer = new FileWriter(file)) {
                objectMapper.writeValue(writer, data);
            }
            catch(IOException e) {
                log.error("Cannot serialize entity to trash file: " + e.getMessage(), e);
                throw new SumarisTechnicalException("Cannot serialize entity to trash file: " + e.getMessage(), e);
            }
        }
        catch(IOException e) {
            throw new SumarisTechnicalException("Error while processing deleted entity: " + e.getMessage(), e);
        }
    }

    protected void checkTrashDirectory() throws SumarisTechnicalException {
        if (trashDirectory == null || !trashDirectory.isDirectory()) {
            throw new SumarisTechnicalException("Invalid trash directory");
        }
        checkCanRead(trashDirectory);
        checkCanWrite(trashDirectory);
    }

    protected void checkCanRead(File directory) {
        if (!directory.canRead()) throw new SumarisTechnicalException("Cannot read from directory: " + directory);
    }

    protected void checkCanWrite(File directory) {
        if (!directory.canWrite()) throw new SumarisTechnicalException("Cannot write into directory: " + directory);
    }

    protected String getFilePrefix(IValueObject data) {
        if (data instanceof OperationVO) {
            OperationVO ope = (OperationVO)data;
            Integer tripId = ope.getTripId() != null ? ope.getTripId() :
                    (ope.getTrip() != null ? ope.getTrip().getId() : null);
            return tripId != null
                    ? String.format(FILE_PREFIX_PARENT, "Trip", tripId)
                    : null;
        }
        return null;
    }

    protected Set<Class<?>> readFileContentAsClasses(File classFile) {
        try {
            return FileUtils.readLines(classFile, Files.CHARSET_UTF8)
                    .stream()
                    .filter(StringUtils::isNoneBlank)
                    .map(className -> {
                        try {
                            return Class.forName(className);
                        }
                        catch (Throwable t){
                            log.error(String.format("Invalid class '%s' found in file: ", className, classFile.getAbsolutePath()));
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new SumarisTechnicalException("Error while reading class file " + classFile.getAbsolutePath(), e);
        }
    }

    /* -- -- */

    protected String getFileBasename(String entityName, Serializable id) {
        return new StringBuilder()
                .append(entityName.toLowerCase())
                .append('#')
                .append(id)
                .append('.').append(JSON_FILE_EXTENSION)
                .toString();
    }

    protected List<ObjectReader> getObjectReaders(File directory, Class<?> clazz) {
        File classFile = new File(directory, CLASS_FILE_NAME);
        Set<Class<?>> classes = null;
        if (clazz != null) {
            classes = ImmutableSet.of(clazz);
        }
        else if (classFile.exists()) {
            classes = readFileContentAsClasses(classFile);
        }
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(classes), "Missing or invalid file " + classFile.getAbsolutePath());

        // Create readers for each classes
        return classes.stream()
                .map(c -> objectMapper.reader().forType(c))
                .collect(Collectors.toList());
    }
    /**
     * Try to deserialize using a list of readers, and return the first valid object.
     * @param file
     * @param clazz
     * @param <V>
     * @return
     */
    protected <V> Optional<V> readAsObject(File file,
                                 Class<? extends V> clazz) {
        List<ObjectReader> readers = getObjectReaders(file.getParentFile(), clazz);
        return readAsObject(file, readers, clazz);
    }

    /**
     * Try to deserialize using a list of readers, and return the first valid object.
     * @param file
     * @param readers
     * @param clazz
     * @param <V>
     * @return
     */
    protected <V> Optional<V> readAsObject(File file,
                                 List<ObjectReader> readers,
                                 Class<? extends V> clazz) {

        // Keep null values (e.g. when cannot deserialize), because of page's total
        return readers.stream().map(reader -> {
            try {
                // Deserialize file content
                Object vo = reader.readValue(file);

                // Not the expected class
                if (clazz != null && !clazz.isInstance(vo)) return null;

                // Override update date, with file date (=deletion date)
                if (vo instanceof IUpdateDateEntity) {
                    Date lastModified = new Date(file.lastModified());
                    ((IUpdateDateEntity<?, Date>) vo).setUpdateDate(lastModified);
                }

                return (V)vo;
            } catch (Throwable t) {
                return null;
            }
        })
        .filter(Objects::nonNull)
        .findFirst();
    }
}
