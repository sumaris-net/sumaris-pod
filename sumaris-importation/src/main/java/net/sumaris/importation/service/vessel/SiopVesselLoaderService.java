package net.sumaris.importation.service.vessel;

/*-
 * #%L
 * SUMARiS:: Core Importation
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

import net.sumaris.importation.exception.FileValidationException;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;

@Transactional
public interface SiopVesselLoaderService {

    /**
     * Import a CL file (landing statistics) into the database
     *
     * @param inputFile the input data file to import
     * @param format the file format
     * @param validate Should apply a validation before importation ?
     * @param appendData Should append data, or remove previous before (using the country) ?
     * @return
     * @throws IOException
     */
    void loadFromFile(File inputFile, String format, boolean validate, boolean appendData) throws IOException,
            FileValidationException;
}
