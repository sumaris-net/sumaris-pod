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
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.exception.UnknownFormatException;
import net.sumaris.extraction.core.format.AggregationFormatEnum;
import net.sumaris.extraction.core.service.AggregationService;
import net.sumaris.extraction.core.service.ExtractionProductService;
import net.sumaris.extraction.core.service.ExtractionServiceLocator;
import net.sumaris.extraction.core.vo.AggregationTypeVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFilterVO;

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
        AggregationService aggregationService = ExtractionServiceLocator.aggregationService();

        String formatLabel = config.getExtractionCliOutputFormat();
        AggregationTypeVO type = null;
        try {
            IExtractionFormat format = AggregationFormatEnum.valueOf(formatLabel);
            type = aggregationService.getTypeByFormat(format);
        } catch (UnknownFormatException | IllegalArgumentException e) {
            String availableProducts = productService.findByFilter(ExtractionProductFilterVO.builder()
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
    }

}
