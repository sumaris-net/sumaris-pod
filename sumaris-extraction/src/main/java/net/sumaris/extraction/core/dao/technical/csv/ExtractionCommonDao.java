package net.sumaris.extraction.core.dao.technical.csv;

/*-
 * #%L
 * Quadrige3 Core :: Client API
 * %%
 * Copyright (C) 2017 - 2018 Ifremer
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

import net.sumaris.extraction.core.vo.ExtractionContextVO;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionCommonDao {

    /**
     * Constant <code>UTF8_BOM=0xFEFF</code>
     */
    int UTF8_BOM = 0xFEFF;

    /**
     * <p>dumpQueryToCSV.</p>
     *
     * @param file              the output file
     * @param query             the query string
     * @param aliasByColumnMap  output column alias, by column name
     * @param dateFormats       output format, by column name
     * @param decimalFormats    output decimal format, by column name
     * @param excludeColumnNames set of columns to exclude
     */
    void dumpQueryToCSV(File file,
                        String query,
                        @Nullable Map<String, String> aliasByColumnMap,
                        @Nullable Map<String, String> dateFormats,
                        @Nullable Map<String, String> decimalFormats,
                        @Nullable Set<String> excludeColumnNames) throws IOException;

    void clean(ExtractionContextVO context);
}
