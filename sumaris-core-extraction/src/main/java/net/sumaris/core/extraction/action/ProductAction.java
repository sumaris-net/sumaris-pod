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
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.service.ExtractionProductService;
import net.sumaris.core.extraction.service.ExtractionService;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.history.ProcessingFrequency;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * <p>DatabaseChangeLogAction class.</p>
 *
 */
@Slf4j
public class ProductAction {

    /**
     * <p>Update a product (execute extraction or aggregation).</p>
     */
    public void update() {
        // Get beans
        ExtractionConfiguration config = ExtractionConfiguration.instance();
        ExtractionProductService productService = ServiceLocator.instance().getService("extractionProductService", ExtractionProductService.class);
        ExtractionService extractionService = ServiceLocator.instance().getService("extractionService", ExtractionService.class);
        AggregationService aggregationService = ServiceLocator.instance().getService("aggregationService", AggregationService.class);

        // Get execution frequency
        ProcessingFrequencyEnum frequency = config.getExtractionCliFrequency();

        if (frequency == ProcessingFrequencyEnum.NEVER) {
            log.error("Products with frequency '{}' cannot be updated!", frequency);
        }

        long now = System.currentTimeMillis();
        log.info("Updating products... {frequency: '{}'}", frequency);
        ActionUtils.logConnectionProperties();

        // Get products to refresh
        List<ExtractionProductVO> products = productService.findByFilter(ExtractionProductFilterVO.builder()
                // Filter on public or private products
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
                // With the expected frequency
                .searchJoin(ExtractionProduct.Fields.PROCESSING_FREQUENCY)
                .searchAttribute(ProcessingFrequency.Fields.LABEL)
                .searchText(frequency.getLabel())
            .build(),
            ExtractionProductFetchOptions.builder().build());

        if (CollectionUtils.isEmpty(products)) {
            log.info("No product found.");
            return;
        }

        for (ExtractionProductVO product: products) {
            log.info("Updating product {{}}...", product.getLabel());

            try {
                aggregationService.updateProduct(product.getId());
                Thread.sleep(10000); // Waiting 10s, to let DB drop tables (asynchronously)
            }
            catch (InterruptedException e) {
                // Stop
                return;
            }
        }

        log.info("Updating products... {frequency: '{}'}", frequency);
    }


}
