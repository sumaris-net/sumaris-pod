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

package net.sumaris.extraction.server.job;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.history.ProcessingFrequency;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.config.ExtractionConfigurationOption;
import net.sumaris.extraction.core.service.ExtractionManager;
import net.sumaris.extraction.core.service.ExtractionProductService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class ExtractionJob {

    @Autowired
    private ExtractionManager extractionManager;

    @Autowired
    private ExtractionConfiguration configuration;

    @Autowired(required = false)
    private Executor extractionExecutor;

    private boolean enable = false;


    public ExtractionJob() {
        super();
    }

    public ExtractionJob(ExtractionManager extractionManager,
                         ExtractionConfiguration configuration) {
        super();
        this.extractionManager = extractionManager;
        this.configuration = configuration;
        this.enable = configuration.enableExtractionProduct();
    }

   

    /* -- protected functions -- */

    @EventListener({ConfigurationReadyEvent.class})
    protected void onConfigurationReady(ConfigurationReadyEvent event) {
        if (!this.enable) {
            boolean enable = configuration.enableExtractionProduct() && extractionExecutor != null;

            if (!enable) {
                log.debug("Extraction jobs disabled.");
                log.debug("To enable extraction jobs, please set configuration option '{}=true'", ExtractionConfigurationOption.EXTRACTION_PRODUCT_ENABLE.getKey());
                return;
            }
            else {
                // Load started
                log.info("Started Extraction jobs, for frequencies {{}}",
                    Arrays.stream(ProcessingFrequencyEnum.values())
                        .filter(e -> e != ProcessingFrequencyEnum.MANUALLY && e != ProcessingFrequencyEnum.NEVER)
                        .map(Enum::name)
                        .collect(Collectors.joining(",")));
                this.enable = true;
            }
        }
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.hourly.cron:0 0 * * * ?}")
    @Async
    protected void executeHourly(){
        if (!enable) return; // Skip
        extractionManager.executeAll(ProcessingFrequencyEnum.HOURLY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.daily.cron:0 0 0 * * ?}")
    @Async
    protected void executeDaily(){
        if (!enable) return; // Skip
        extractionManager.executeAll(ProcessingFrequencyEnum.DAILY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.weekly.cron:0 0 0 2 * MON}")
    @Async
    protected void executeWeekly(){
        if (!enable) return; // Skip
        extractionManager.executeAll(ProcessingFrequencyEnum.WEEKLY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.monthly.cron:0 0 0 1 * ?}")
    @Async
    protected void executeMonthly(){
        if (!enable) return; // Skip
        extractionManager.executeAll(ProcessingFrequencyEnum.MONTHLY);
    }


}