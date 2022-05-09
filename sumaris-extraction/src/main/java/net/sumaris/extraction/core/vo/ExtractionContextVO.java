package net.sumaris.extraction.core.vo;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.technical.extraction.ExtractionTableVO;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.*;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionContextVO implements IExtractionType {

    Integer id;
    String label;

    String format;
    String version;
    ExtractionCategoryEnum category;

    ExtractionFilterVO filter;

    String tableNamePrefix;

    Date updateDate;

    @FieldNameConstants.Exclude
    Map<String, String> tableNames = new LinkedHashMap<>();

    @FieldNameConstants.Exclude
    Set<String> rawTableNames = new HashSet<>();

    @FieldNameConstants.Exclude
    Map<String, Set<String>> hiddenColumnNames = new LinkedHashMap<>();


    @FieldNameConstants.Exclude
    Set<String> tableNameWithDistinct = new HashSet<>();

    public ExtractionContextVO() {
        // Generate a unique, positive i
        this.id = Math.abs(UUID.randomUUID().hashCode());
    }

    protected ExtractionContextVO(ExtractionContextVO source) {
        this.id = source.id;
        this.format = source.format;
        this.version = source.version;
        this.category = source.category;
        this.updateDate = source.updateDate;
        this.tableNames.putAll(source.tableNames);
        this.hiddenColumnNames.putAll(source.hiddenColumnNames);
        this.tableNameWithDistinct.addAll(source.tableNameWithDistinct);
    }

    public void setType(IExtractionType type) {
        this.category = type.getCategory();
        this.format = type.getFormat();
        this.version = type.getVersion();
    }

    /**
     * Register a table (with rows inside)
     * @param tableName
     * @param sheetName
     */
    public void addTableName(String tableName, String sheetName) {
        addTableName(tableName, sheetName, null, false);
    }

    /**
     * Register a table (with rows inside)
     * @param tableName
     * @param sheetName
     * @param hiddenColumnNames
     */
    public void addTableName(String tableName, String sheetName,
                             Set<String> hiddenColumnNames,
                             boolean enableDistinct) {
        tableNames.put(tableName, sheetName);
        if (CollectionUtils.isNotEmpty(hiddenColumnNames)) {
            this.hiddenColumnNames.put(tableName, hiddenColumnNames);
        }
        if (enableDistinct) {
            this.tableNameWithDistinct.add(tableName);
        }
    }

    /**
     * Register a raw table (to be able to remove later)
     * @param tableName
     */
    public void addRawTableName(String tableName) {
        rawTableNames.add(tableName);
    }

    public String getSheetName(String tableName) {
        String otherName = tableNames.get(tableName);
        return (otherName!=null) ? otherName : tableName;
    }

    public Set<String> getTableNames() {
        return tableNames.keySet();
    }

    @Override
    public String[] getSheetNames() {
        return tableNames.values().toArray(new String[tableNames.size()]);
    }

    public Set<String> getRawTableNames() {
        return rawTableNames;
    }

    public String getTableNameBySheetName(String sheetName) {
        return findTableNameBySheetName(sheetName)
                .orElse(null);
    }

    public Optional<String> findTableNameBySheetName(@NonNull String sheetName) {
        return tableNames.entrySet().stream()
            .filter(e -> sheetName.equalsIgnoreCase(e.getValue()))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public boolean hasSheet(String sheetName) {
        Preconditions.checkNotNull(sheetName);
        return tableNames.containsValue(sheetName);
    }

    public boolean hasRawTable(String rawTableName) {
        Preconditions.checkNotNull(rawTableName);
        return rawTableNames.contains(rawTableName);
    }
    /**
     * Return the hidden columns of the given table
     * @param tableName
     */
    public Set<String> getHiddenColumns(String tableName) {
        return hiddenColumnNames.get(tableName);
    }


    /**
     * Return is distinct is enable on the table
     * @param tableName
     */
    public boolean isDistinctEnable(String tableName) {
        return tableNameWithDistinct.contains(tableName);
    }
}
