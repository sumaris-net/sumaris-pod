package net.sumaris.importation.service.ices;

import net.sumaris.importation.exception.FileValidationException;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;

@Transactional
public interface IcesDataLoaderService {

    /**
     * Import a CL file (landing statistics) into the database
     *
     * @param inputFile the input data file to import
     * @param country the country data to import (null=all countries)
     * @param validate Should apply a validation before importation ?
     * @param appendData Should append data, or remove previous before (using the country) ?
     * @return
     * @throws IOException
     */
    void loadLanding(File inputFile, String country, boolean validate, boolean appendData) throws IOException,
            FileValidationException;

    /**
     * Import a TR file (trip) into the database
     *
     * @param inputFile the input data file to import
     * @param country the country data to import (null=all countries)
     * @param validate Should apply a validation before importation ?
     * @param appendData Should append data, or remove previous before (using the country) ?
     * @return
     * @throws IOException
     */
    void loadTrip(File inputFile, String country, boolean validate, boolean appendData) throws IOException,
            FileValidationException;

    /**
     * Import a data file with mixed rows (TR, HH, SL...)
     *
     * @param inputFile the input data file to import
     * @param country the country data to import (null=all countries)
     * @param validate Should apply a validation before importation ?
     * @param appendData Should append data, or remove previous before (using the country) ?
     * @return
     * @throws IOException
     */
    void loadMixed(File inputFile, String country, boolean validate, boolean appendData) throws IOException,
            FileValidationException;

    /**
     * Will detect the file format, then load the file
     * @param inputFile
     * @param country
     * @param validate
     * @param appendData
     * @throws IOException
     * @throws FileValidationException
     */
    void detectFormatAndLoad(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException;
}
