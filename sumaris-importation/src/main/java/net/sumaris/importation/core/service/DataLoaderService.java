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

package net.sumaris.importation.core.service;

import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.importation.core.exception.FileValidationException;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;

@Transactional
public interface DataLoaderService {

    /**
     * Validate file format
     * @param inputFile
     * @param fileType
     * @return
     * @throws IOException
     */
    void remove(DatabaseTableEnum table, String[] filteredColumns, Object[] filteredValues) throws IOException;

    /**
     * Validate file format
     * @param inputFile
     * @param fileType
     * @return
     * @throws IOException
     */
    @Transactional(readOnly = true)
    void validate(File inputFile, DatabaseTableEnum table) throws IOException, FileValidationException;

    /**
     * Import a file into the database
     *
     * @param table the table to fill
     * @param country the country data to import (null=all countries)
     * @param validate Should apply a validation before importation ?
     * @param appendData Should append data, or remove previous before (using the country) ?
     * @return
     * @throws IOException
     */
    void load(File inputFile, DatabaseTableEnum table, boolean validate) throws IOException,
            FileValidationException;

}
