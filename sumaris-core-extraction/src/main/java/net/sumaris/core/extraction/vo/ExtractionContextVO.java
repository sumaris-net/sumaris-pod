package net.sumaris.core.extraction.vo;

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
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionContextVO {

    long id;

    String label;
    String formatName;
    String formatVersion;
    ExtractionFilterVO filter;

    @FieldNameConstants.Exclude
    Map<String, String> tableNames = new LinkedHashMap<>();

    @FieldNameConstants.Exclude
    Map<String, Set<String>> hiddenColumnNames = new LinkedHashMap<>();


    @FieldNameConstants.Exclude
    Set<String> tableNameWithDistinct = new HashSet<>();

    public ExtractionContextVO() {

    }

    protected ExtractionContextVO(ExtractionContextVO source) {

        this.id = source.id;
        this.label = source.label;
        this.formatName = source.formatName;
        this.formatVersion = source.formatVersion;
        this.tableNames.putAll(source.tableNames);
        this.hiddenColumnNames.putAll(source.hiddenColumnNames);
        this.tableNameWithDistinct.addAll(source.tableNameWithDistinct);
    }


    public String getLabel() {
        return label != null ? label : (this.formatName != null ? this.formatName.toLowerCase() : null);
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

    public String getSheetName(String tableName) {
        String otherName = tableNames.get(tableName);
        return (otherName!=null) ? otherName : tableName;
    }

    public Set<String> getTableNames() {
        return tableNames.keySet();
    }

    public String getTableNameBySheetName(String sheetName) {
        Preconditions.checkNotNull(sheetName);
        return tableNames.entrySet().stream()
                .filter(e -> sheetName.equalsIgnoreCase(e.getValue()))
                .map(e -> e.getKey())
                .findFirst()
                .orElse(null);
    }

    public boolean hasSheet(String sheetName) {
        Preconditions.checkNotNull(sheetName);
        return tableNames.containsValue(sheetName);
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
