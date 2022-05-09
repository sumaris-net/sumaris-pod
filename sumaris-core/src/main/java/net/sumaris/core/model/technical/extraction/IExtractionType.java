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

package net.sumaris.core.model.technical.extraction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.StatusEnum;

public interface IExtractionType<P extends IEntity<Integer>, D extends IEntity<Integer>>  {

    interface Fields extends IEntity.Fields {

        String CATEGORY = "category";
        String FORMAT = "format";
        String VERSION = "version";

        String PARENT_ID = "parentId";
        String PARENT = "parent";

        String ID = "id";
        String LABEL = "label";
        String NAME = "name";

        String RECORDER_PERSON = "recorderPerson";
        String RECORDER_DEPARTMENT = "recorderDepartment";
    }

    String LABEL_SEPARATOR = "-";

    /**
     * Compute a label, from the format AND the id (if any).
     * Example: "RDB-001"
     *
     * @param format
     * @return
     */
    static String computeLabel(@NonNull IExtractionType type) {
        return computeLabel(type.getFormat(), type.getId());
    }

    /**
     * Compute a label, from the format AND the id (if any).
     * Example: "RDB-001"
     *
     * @param format
     * @return
     */
    static String computeLabel(@NonNull String format, Integer id) {
        if (id == null) return format;
        return format + LABEL_SEPARATOR + Math.abs(id);
    }

    /**
     * If label was derived from another format, return the raw (original) format.
     * Example: "rdb-001" will return "RDB"
     *
     * @param label
     * @return
     */
    @Deprecated
    static String getRawFormatLabel(String label) {
        if (label == null) return null;
        int lastSeparatorIndex = label.lastIndexOf(LABEL_SEPARATOR);
        if (lastSeparatorIndex == -1) return label;
        return label.substring(0, lastSeparatorIndex);
    }

    static String getFormat(String label) {
        if (label == null) return null;
        int lastSeparatorIndex = label.indexOf(LABEL_SEPARATOR);
        if (lastSeparatorIndex == -1) return label;
        return label.substring(0, lastSeparatorIndex);
    }

    @Deprecated
    static ExtractionCategoryEnum getRawFormatCategory(String label) {
        String rawFormatLabel = getRawFormatLabel(label);
        return rawFormatLabel.contains(LABEL_SEPARATOR)
            ? ExtractionCategoryEnum.PRODUCT
            : ExtractionCategoryEnum.LIVE;
    }

    /**
     * Say if a label is a root format label.
     * Root label must NOT contains the character '-'.
     * @param label
     * @return true if label contains '-' (= root format)
     */
    @Deprecated
    static boolean isRootFormatLabel(String label) {
        if (label == null) return false;
        return !label.contains(LABEL_SEPARATOR);
    }

    Integer getId();

    /**
     * A unique label. Computed from '[format]-[id]'
     * @return
     */
    default String getLabel() {
        return computeLabel(getFormat(), getId());
    }

    default String getName() {
        return null;
    }

    String getFormat();

    String getVersion();
    ExtractionCategoryEnum getCategory();

    default Integer getParentId() {
        return null;
    }

    @JsonIgnore
    default IExtractionType<P, D> getParent() {
        return null;
    }

    String[] getSheetNames();

    @Deprecated
    default String getRawFormatLabel() {
        return getRawFormatLabel(getLabel());
    }

    @Deprecated
    default ExtractionCategoryEnum getRawFormatCategory() {
        return getRawFormatCategory(getLabel());
    }

    default boolean isRoot() {
        return getParent() == null && getParentId() == null;
    }
    default boolean isChild() {
        return !isRoot();
    }

    default Integer getStatusId() {
        return StatusEnum.TEMPORARY.getId(); // private by default
    }

    default D getRecorderDepartment() {
        return null;
    }

    default P getRecorderPerson() {
        return null;
    }
}
