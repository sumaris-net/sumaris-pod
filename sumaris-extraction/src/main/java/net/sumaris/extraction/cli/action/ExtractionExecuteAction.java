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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.util.*;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.exception.UnknownFormatException;
import net.sumaris.extraction.core.service.*;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;

import java.io.File;
import java.io.IOException;
import java.util.Date;
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
        ExtractionService service = ExtractionServiceLocator.extractionService();

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
        File outputFile;
        File outputDirectory;
        {
            outputDirectory = SumarisConfiguration.getInstance().getCliOutputFile();
            if (outputDirectory != null && outputDirectory.exists() && outputDirectory.isDirectory()) {
                outputFile = new File(outputDirectory, String.format("%s-%s.zip", type.getFormat(), Dates.formatDate(new Date(), Dates.CSV_DATE_TIME)));
            } else {
                outputFile = ActionUtils.checkAndGetOutputFile(false, ExtractionExecuteAction.class);
                outputDirectory = null;
            }
        }

        // Execute the extraction
        long startTime = System.currentTimeMillis();
        File tempFile;
        try {
            boolean isLiveTrips = ExtractionTypes.isLive(type) && ExtractionTypes.isOnTrips(type);
            if (isLiveTrips) {
                LiveExtractionTypeEnum liveFormat = LiveExtractionTypeEnum.valueOf(type.getFormat());
                ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
                filter.setProgramLabel(config.getCliFilterProgramLabel());
                filter.setIncludedIds(config.getCliFilterTripIds().toArray(Integer[]::new));
                filter.setOperationIds(config.getCliFilterOperationIds().toArray(Integer[]::new));
                Integer year = config.getCliFilterYear();
                if (year != null && year > 1970) {
                    filter.setStartDate(Dates.getFirstDayOfYear(year));
                    filter.setEndDate(Dates.getLastSecondOfYear(year));
                }
                tempFile = service.executeAndDumpTrips(liveFormat, filter);
            }
            else {
                tempFile = service.executeAndDump(type, null, null);
            }
            if (tempFile != null && !tempFile.exists()) {
                log.error("No data");
                return;
            }
        } catch (IOException e) {
            log.error("Error during extraction: " + e.getMessage(), e);
            return;
        }

        // Move temp file to expected output file
        if (outputDirectory == null) {
            try {
                Files.moveFile(tempFile, outputFile);
            }
            catch (IOException e) {
                log.error("Error while creating output file: " + e.getMessage(), e);
                return;
            }
        }

        // Extract temp file to expected output directory
        else {
            try {
                ZipUtils.uncompressFileToPath(tempFile, outputDirectory.getAbsolutePath(), true);
            }
            catch (IOException e) {
                log.error("Error while creating output file: " + e.getMessage(), e);
                return;
            }
        }

        // Success log
        log.info("{} extraction finished, in {} - output: {}",
            StringUtils.capitalize(type.getFormat().toLowerCase()),
            TimeUtils.printDurationFrom(startTime),
            outputFile.getAbsolutePath());
    }
}
