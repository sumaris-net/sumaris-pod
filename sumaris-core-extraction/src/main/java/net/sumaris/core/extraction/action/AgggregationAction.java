package net.sumaris.core.extraction.action;

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
import net.sumaris.core.action.ActionUtils;
import net.sumaris.core.extraction.config.ExtractionConfiguration;
import net.sumaris.core.extraction.exception.UnknownFormatException;
import net.sumaris.core.extraction.format.ProductFormatEnum;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.service.ExtractionProductService;
import net.sumaris.core.extraction.service.ExtractionService;
import net.sumaris.core.extraction.util.ExtractionFormats;
import net.sumaris.core.extraction.vo.AggregationTypeVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.service.data.ProductService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>DatabaseChangeLogAction class.</p>
 *
 */
@Slf4j
public class AgggregationAction {

    /**
     * <p>execute.</p>
     */
    public void execute() {
        ExtractionConfiguration config = ExtractionConfiguration.instance();
        AggregationService aggregationService = ServiceLocator.instance().getService("aggregationService", AggregationService.class);
        ExtractionProductService productService = ServiceLocator.instance().getService("extractionProductService", ExtractionProductService.class);

        String formatLabel = config.getExtractionCliOutputFormat();
        AggregationTypeVO type = null;
        try {
            IExtractionFormat format = ProductFormatEnum.valueOf(formatLabel);
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
        ActionUtils.logConnectionProperties();


    }

    /**
     * <p>refresh.</p>
     */
    public void refresh() {
        ExtractionConfiguration config = ExtractionConfiguration.instance();
        AggregationService aggregationService = ServiceLocator.instance().getService("aggregationService", AggregationService.class);
        ExtractionProductService productService = ServiceLocator.instance().getService("extractionProductService", ExtractionProductService.class);

        List<ExtractionProductVO> products = productService.findByFilter(ExtractionProductFilterVO.builder()
                        .statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
                        .build(),
                ExtractionProductFetchOptions.builder().build());

        if (CollectionUtils.isEmpty(products)) {
            log.info("No product found in database. Nothing to refresh");
            return;
        }

        log.info("Refreshing aggregation products...");
        ActionUtils.logConnectionProperties();

        for (ExtractionProductVO product: products) {
            log.info("Updating product {{}}...", product.getLabel());

            aggregationService.refresh(product.getId());

            try {
                Thread.sleep(10000); // Waiting 10s, to avoid HSQLDB 'generale error'
            }
            catch (InterruptedException e) {
                // Stop
                return;
            }
        }
    }
}
