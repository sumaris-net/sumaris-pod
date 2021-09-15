/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.scheduler;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.extraction.config.ExtractionConfiguration;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.service.ExtractionProductService;
import net.sumaris.core.extraction.service.ExtractionServiceLocator;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.history.ProcessingFrequency;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ExtractionJob {

    private ExtractionConfiguration config;
    private ExtractionProductService productService;
    private AggregationService aggregationService;
    private boolean ready = false;

    @Autowired
    public ExtractionJob(ExtractionConfiguration config,
                         ExtractionProductService productService,
                         AggregationService aggregationService) {
        super();
        this.config = config;
        this.productService = productService;
        this.aggregationService = aggregationService;
    }

    public ExtractionJob() {
        super();
        this.config = ExtractionConfiguration.instance();
        this.productService = ExtractionServiceLocator.extractionProductService();
        this.aggregationService = ExtractionServiceLocator.aggregationService();
        this.ready = true;
    }

    public void execute(@NonNull ProcessingFrequencyEnum frequency) {
        if (!ready) return; // SKip
        if (frequency == ProcessingFrequencyEnum.NEVER) {
            throw new IllegalArgumentException(String.format("Cannot update products with frequency '%s'", frequency));
        }

        long now = System.currentTimeMillis();
        log.info("Updating {} extractions...", frequency.name().toLowerCase());

        // Get products to refresh
        List<ExtractionProductVO> products = productService.findByFilter(ExtractionProductFilterVO.builder()
                // Filter on public or private products
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
                // With the expected frequency
                .searchJoin(ExtractionProduct.Fields.PROCESSING_FREQUENCY)
                .searchAttribute(ProcessingFrequency.Fields.LABEL)
                .searchText(frequency.getLabel())
                .build(),
            ExtractionProductFetchOptions.MINIMAL);

        if (CollectionUtils.isEmpty(products)) {
            log.info("No {} extraction found.", frequency.name().toLowerCase());
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        for (ExtractionProductVO product: products) {
            try {
                log.debug("Updating extraction {id: {}, label: '{}'}...", product.getId(), product.getLabel());
                aggregationService.updateProduct(product.getId());
                successCount++;
            }
            catch(Throwable e) {
                log.error("Error while updating extraction {id: {}, label: '{}'}: {}", product.getId(), product.getLabel(), e.getMessage(), e);
                errorCount++;

                // TODO: add to processing history
            }
        }

        log.info("Updating {} extractions [OK] in {}ms (success: {}, errors: {})",
            frequency.name().toLowerCase(),
            System.currentTimeMillis() - now,
            successCount, errorCount);
    }

    /* -- protected functions -- */

    @EventListener({ConfigurationReadyEvent.class})
    protected void onConfigurationReady(ConfigurationReadyEvent event) {
        if (!this.ready) {
            // Load started
            log.info("Started extraction jobs, with frequency {{}}",
                Arrays.stream(ProcessingFrequencyEnum.values())
                    .filter(e -> e != ProcessingFrequencyEnum.MANUALLY && e != ProcessingFrequencyEnum.NEVER)
                    .map(Enum::name)
                    .collect(Collectors.joining(",")));
            this.ready = true;
        }
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.hourly.cron:0 0 * * * ?}")
    @Async
    protected void executeHourly(){
        execute(ProcessingFrequencyEnum.HOURLY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.daily.cron:0 0 0 * * ?}")
    @Async
    protected void executeDaily(){
        execute(ProcessingFrequencyEnum.DAILY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.daily.cron:0 0 0 2 * MON}")
    @Async
    protected void executeWeekly(){
        execute(ProcessingFrequencyEnum.WEEKLY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.hourly.cron:0 0 0 1 * ?}")
    @Async
    protected void executeMonthly(){
        execute(ProcessingFrequencyEnum.MONTHLY);
    }



}