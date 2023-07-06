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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.config.ExtractionConfigurationOption;
import net.sumaris.extraction.core.service.ExtractionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class ExtractionJob {

    private final ExtractionService extractionService;
    private final ExtractionConfiguration configuration;
    private boolean enable = false;

    public ExtractionJob(ExtractionService extractionService,
                         ExtractionConfiguration configuration) {
        super();
        this.extractionService = extractionService;
        this.configuration = configuration;
    }

    /* -- protected functions -- */

    @EventListener({ConfigurationReadyEvent.class})
    public void onConfigurationReady(ConfigurationReadyEvent event) {
        boolean enable = configuration.enableExtractionProduct() && configuration.enableExtractionScheduling();
        if (this.enable != enable) {
            this.enable = enable;
            if (!enable) {
                log.info("Extraction jobs disabled. Use option '{}' to enable jobs", ExtractionConfigurationOption.EXTRACTION_PRODUCT_ENABLE.getKey());
            }
            else {
                // Load started
                log.info("Started Extraction jobs {{}}",
                    Arrays.stream(ProcessingFrequencyEnum.values())
                        .filter(e -> e != ProcessingFrequencyEnum.MANUALLY && e != ProcessingFrequencyEnum.NEVER)
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.hourly.cron:0 0 * * * ?}")
    protected void executeHourly(){
        if (!enable) return; // Skip
        extractionService.executeAll(ProcessingFrequencyEnum.HOURLY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.daily.cron:0 0 0 * * ?}")
    protected void executeDaily(){
        if (!enable) return; // Skip
        extractionService.executeAll(ProcessingFrequencyEnum.DAILY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.weekly.cron:0 0 0 2 * MON}")
    protected void executeWeekly(){
        if (!enable) return; // Skip
        extractionService.executeAll(ProcessingFrequencyEnum.WEEKLY);
    }

    @Scheduled(cron = "${sumaris.extraction.scheduling.monthly.cron:0 0 0 1 * ?}")
    protected void executeMonthly(){
        if (!enable) return; // Skip
        extractionService.executeAll(ProcessingFrequencyEnum.MONTHLY);
    }

    @Scheduled(cron = "${sumaris.extraction.clean.daily.cron:0 0 0 * * ?}")
    protected void dropTemporaryTables(){
        extractionService.dropTemporaryTables();
    }

}