package net.sumaris.extraction.cli.action;

/*
 * #%L
 * SIH-Adagio :: Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2014 Ifremer
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.cli.action.ActionUtils;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.exception.UnknownFormatException;
import net.sumaris.extraction.core.service.*;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * <p>DatabaseChangeLogAction class.</p>
 *
 */
@Slf4j
public class ExtractionExecuteAction {

    /**
     * <p>execute.</p>
     */
    public void run() {
        ExtractionConfiguration config = ExtractionConfiguration.instance();
        ExtractionTypeService extractionTypeService = ExtractionServiceLocator.extractionTypeService();
        ExtractionManager service = ExtractionServiceLocator.extractionManager();

        String format = config.getExtractionCliOutputFormat();
        IExtractionType type;
        try {
            type = ExtractionTypes.getByFormat(format);
        } catch (UnknownFormatException e) {
            log.error("Unknown format: " + format);
            String availableTypes = extractionTypeService.getLiveTypes()
                .stream()
                .map(p -> " - " + p.getLabel())
                .collect(Collectors.joining("\n"));
            log.error("Unknown extraction format '{}'.\nAvailable formats:\n{}",
                format,
                availableTypes);
            return;
        }

        log.info("Starting {} extraction...",
                StringUtils.capitalize(type.getFormat().toLowerCase()));

        // Check output file
        File outputFile = ActionUtils.checkAndGetOutputFile(false, ExtractionExecuteAction.class);

        // Execute the extraction
        long startTime = System.currentTimeMillis();
        File tempFile;
        try {
            tempFile = service.executeAndDump(type, null, null);
            if (!tempFile.exists()) {
                log.error("No data");
                return;
            }
        } catch (IOException e) {
            log.error("Error during extraction: " + e.getMessage(), e);
            return;
        }

        // Move temp file to expected output file
        try {
            Files.moveFile(tempFile, outputFile);

        }
        catch (IOException e) {
            log.error("Error while creating output file: " + e.getMessage(), e);
            return;
        }

        // Success log
        log.info("{} extraction finished, in {} - output: {}",
            StringUtils.capitalize(type.getFormat().toLowerCase()),
            TimeUtils.printDurationFrom(startTime),
            outputFile.getAbsolutePath());
    }
}
