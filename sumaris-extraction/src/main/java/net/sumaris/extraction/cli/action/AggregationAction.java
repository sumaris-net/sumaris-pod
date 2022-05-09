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
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.exception.UnknownFormatException;
import net.sumaris.extraction.core.service.ExtractionManager;
import net.sumaris.extraction.core.service.ExtractionProductService;
import net.sumaris.extraction.core.service.ExtractionServiceLocator;
import net.sumaris.extraction.core.specification.data.trip.AggSpecification;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * <p>DatabaseChangeLogAction class.</p>
 *
 */
@Slf4j
public class AggregationAction {

    /**
     * <p>run.</p>
     */
    public void run() {
        ExtractionConfiguration config = ExtractionConfiguration.instance();
        ExtractionProductService productService = ExtractionServiceLocator.extractionProductService();
        ExtractionManager extractionManager = ExtractionServiceLocator.extractionManager();

        String formatLabel = config.getExtractionCliOutputFormat();
        if (formatLabel != null && !formatLabel.toUpperCase().startsWith(AggSpecification.FORMAT_PREFIX)) {
            formatLabel = AggSpecification.FORMAT_PREFIX + formatLabel.toUpperCase();
        }

        IExtractionType type = null;
        try {
            type = extractionManager.getByExample(AggExtractionTypeEnum.valueOf(formatLabel));
        } catch (UnknownFormatException | IllegalArgumentException e) {
            String availableProducts = productService.findByFilter(ExtractionTypeFilterVO.builder()
                    .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                    .build(),
                    ExtractionProductFetchOptions.DOCUMENTATION)
                    .stream()
                    .map(p -> " - " + p.getLabel())
                    .collect(Collectors.joining("\n"));
            log.error("Unknown aggregation product '{}'.\nExisting products:\n{}",
                    formatLabel,
                    availableProducts);
            System.exit(1);
        }

        log.info("Starting {} aggregation {{}}...",
                StringUtils.capitalize(type.getCategory().name().toLowerCase()),
                type.getLabel());

        // Check output file
        File outputFile = ActionUtils.checkAndGetOutputFile(false, AggregationAction.class);

        // Execute the extraction
        long startTime = System.currentTimeMillis();
        File tempFile;
        try {
            tempFile = extractionManager.executeAndDump(type, null, null);
            if (!tempFile.exists()) {
                log.error("No data");
                return;
            }
        } catch (IOException e) {
            log.error("Error during aggregation: " + e.getMessage(), e);
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
        log.info("{} aggregation {{}} finished, in {} - output: {}",
            StringUtils.capitalize(type.getCategory().name().toLowerCase()),
            type.getLabel(),
            TimeUtils.printDurationFrom(startTime),
            outputFile.getAbsolutePath());
    }

}
