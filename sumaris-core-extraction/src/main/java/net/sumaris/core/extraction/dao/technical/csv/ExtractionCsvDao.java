package net.sumaris.core.extraction.dao.technical.csv;

/*-
 * #%L
 * Quadrige3 Core :: Client API
 * %%
 * Copyright (C) 2017 - 2018 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionCsvDao {

    /**
     * Constant <code>UTF8_BOM=0xFEFF</code>
     */
    int UTF8_BOM = 0xFEFF;

    /**
     * <p>dumpQueryToCSV.</p>
     *
     * @param file              the output file
     * @param query             the query string
     * @param fieldNamesByAlias map the output column name by aliased field name
     * @param dateFormats       map the output format for dates by aliased field name
     * @param decimalFormats    map the output format for decimals by aliased field name
     * @param ignoredFields     list of fields to ignore
     */
    void dumpQueryToCSV(File file,
                        String query,
                        Map<String, String> fieldNamesByAlias,
                        Map<String, String> dateFormats,
                        Map<String, String> decimalFormats,
                        List<String> ignoredFields) throws IOException;

}
