package net.sumaris.importation.service;

import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.importation.exception.FileValidationException;
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
