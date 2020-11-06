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

package net.sumaris.rdf.config;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.rdf.util.RdfFormat;
import org.nuiton.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(
    prefix = "rdf",
    name = {"enabled"}
)
public class RdfAutoConfiguration {

    @Bean
    public RdfConfiguration rdfConfiguration(SumarisConfiguration configuration) {
        return new RdfConfiguration(configuration);
    }
}
