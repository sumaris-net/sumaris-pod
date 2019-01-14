package net.sumaris.importation.service;

import net.sumaris.core.model.SumarisTable;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.vo.ValidationErrorVO;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;

@Transactional
public interface FileImportService {
    /**
     * Import a file into thedatabase
     *
     * @param userId
     *            id of connected user
     * @param country the country data to import (null=all countries)
     * @return
     * @throws IOException
     */
    void importFile(int userId, File inputFile, SumarisTable table, String country, boolean validate, boolean appendData) throws IOException,
            FileValidationException;

    /**
     * Validate file format
     * @param userId
     * @param inputFile
     * @param fileType
     * @return
     * @throws IOException
     */
    ValidationErrorVO[] validateFile(int userId, File inputFile, SumarisTable table) throws IOException;
}
