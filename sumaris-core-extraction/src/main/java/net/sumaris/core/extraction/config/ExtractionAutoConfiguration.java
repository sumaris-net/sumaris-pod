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

package net.sumaris.core.extraction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.technical.extraction.ExtractionProductRepository;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.extraction.dao.administration.ExtractionStrategyDao;
import net.sumaris.core.extraction.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.service.*;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import javax.cache.CacheManager;
import javax.sql.DataSource;
import java.util.Optional;

@Slf4j
@Configuration
@AutoConfigureOrder(1)
@ConditionalOnProperty(
    prefix = "sumaris.extraction",
    name = {"enabled"},
    matchIfMissing = true
)
public class ExtractionAutoConfiguration {

    public ExtractionAutoConfiguration() {
        log.info("Starting Extraction module...");
    }

    @Bean
    public ExtractionConfiguration extractionConfiguration(SumarisConfiguration configuration) {
        ExtractionConfiguration instance = new ExtractionConfiguration(configuration);
        ExtractionConfiguration.setInstance(instance);
        return instance;
    }

}
