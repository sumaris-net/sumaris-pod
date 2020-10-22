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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.OperationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("trashService")
@Slf4j
public class TrashServiceImpl implements TrashService {

    private static final String CLASS_FILE_NAME = "class.info";
    private static final String FILE_PREFIX_PARENT = "%s#%s_";
    private static final String JSON_FILE_EXTENSION = "json";

    private boolean enable;
    private File trashDirectory;

    private ObjectMapper objectMapper;

    @Autowired
    public TrashServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <V> Page<V> findAll(String entityName, Pageable pageable, Class<? extends V> clazz) {
        Preconditions.checkNotNull(entityName);
        Preconditions.checkNotNull(pageable);

        // Make sure sort attribute is updateDate
        // This is because we don't want to deserialize all files, then sort, but we prefer sort on file date,
        // then only deserialize files from the current page
        String sortAttribute = IUpdateDateEntityBean.Fields.UPDATE_DATE;
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            sortAttribute = pageable.getSort().stream().map(o -> o.getProperty()).findFirst().orElse(IUpdateDateEntityBean.Fields.UPDATE_DATE);
        }
        Preconditions.checkArgument(IUpdateDateEntityBean.Fields.UPDATE_DATE.equals(sortAttribute),
                String.format("Trash data can only be sorted on '%s'", IUpdateDateEntityBean.Fields.UPDATE_DATE));

        // Get sort direction
        boolean isDescending = SortDirection.fromSort(pageable.getSort()).orElse(SortDirection.DESC) == SortDirection.DESC;

        File directory = new File(trashDirectory, entityName);
        if (!directory.isDirectory()) return Page.empty(); // If not exists = empty
        if (!directory.canRead()) throw new SumarisTechnicalException("Cannot read the trash directory " + entityName);

        // Get all files in trash
        List<File> files = FileUtils.listFiles(directory, new String[]{JSON_FILE_EXTENSION}, false).stream()
            // Sort by date
            .sorted((f1, f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()) * (isDescending ? 1 : -1))
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
            List<ObjectReader> readers = classes.stream()
                    .map(c -> objectMapper.reader().forType(c))
                    .collect(Collectors.toList());

            // Try to deserialize, using readers, to return the first valid object.
            // Keep null values (e.g. when cannot deserialize), because of page's total
            result = fileStream.map(file -> readers.stream().map(reader -> {
                try {
                    // Deserialize file content
                    Object vo = reader.readValue(file);

                    // Override update date, with file date (=deletion date)
                    if (vo instanceof IUpdateDateEntityBean) {
                        Date lastModified = new Date(file.lastModified());
                        ((IUpdateDateEntityBean<?, Date>) vo).setUpdateDate(lastModified);
                    }
                    return vo;
                }
                catch(Throwable t) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .filter(obj -> obj instanceof IValueObject)
            .map(obj -> (V)obj)
            .findFirst()
            .orElse(null));
        }
        return new PageImpl<>(result.collect(Collectors.toList()), pageable, total);
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
        boolean enable = event.getConfig().enableEntityTrash();
        boolean changed = enable != this.enable;
        this.trashDirectory = event.getConfig().getTrashDirectory();
        this.enable = enable;

        if (this.enable) {
            try {
                FileUtils.forceMkdir(this.trashDirectory);
                checkTrashDirectory();
                if (changed) log.info(String.format("Started trash service at {%s}", this.trashDirectory.getAbsolutePath()));
            } catch (Exception e) {
                log.error("Cannot enable trash service: " + e.getMessage());
                this.enable = false;
            }
        }
        else if (changed) {
            log.info("Stopped trash service");
        }

    }

    @JmsListener(destination = "deleteTrip", containerFactory = "jmsListenerContainerFactory")
    @JmsListener(destination = "deleteOperation", containerFactory = "jmsListenerContainerFactory")
    protected void onEntityDeleted(Serializable entity) throws IOException {
        Preconditions.checkNotNull(entity);

        if (!this.enable || !(entity instanceof IValueObject)) return; // Skip

        IValueObject data = (IValueObject)entity;
        String entityName = data.getClass().getSimpleName();
        if (entityName.lastIndexOf("VO") == entityName.length() - 2) {
            entityName = entityName.substring(0, entityName.length() - 2);
        }

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

        String filename = new StringBuilder()
                .append(StringUtils.trimToEmpty(getFilePrefix(data)))
                .append(entityName.toLowerCase())
                .append('#')
                .append(data.getId())
                .append('.').append(JSON_FILE_EXTENSION)
                .toString();
        File file = new File(directory, filename);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Add %s#%s to trash {%s/%s}", entityName, data.getId(), entityName, filename));
        }

        try (FileWriter writer = new FileWriter(file)) {
            objectMapper.writeValue(writer, data);
        }
    }

    protected void checkTrashDirectory() throws SumarisTechnicalException {
        if (trashDirectory == null || !trashDirectory.isDirectory()) {
            throw new SumarisTechnicalException("Invalid trash directory");
        }
        checkCanRead(trashDirectory);
    }

    protected void checkCanRead(File directory) {
        if (!directory.canRead()) throw new SumarisTechnicalException("Cannot read directory: " + directory);
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
}
