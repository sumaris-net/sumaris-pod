package net.sumaris.core;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.util.I18nUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author peck7 on 05/12/2018.
 */

@org.springframework.boot.test.context.TestConfiguration
public abstract class TestConfiguration extends net.sumaris.core.test.TestConfiguration {

    public static final String MODULE_NAME = "sumaris-core";
    public static final String CONFIG_FILE_PREFIX = "application-test";
    public static final String CONFIG_FILE_NAME = CONFIG_FILE_PREFIX + ".properties";
    public static final String I18N_BUNDLE_NAME = MODULE_NAME + "-i18n";

    @Bean
    public DatabaseFixtures databaseFixtures() {
        return new DatabaseFixtures();
    }


    @Override
    protected String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    protected String getI18nBundleName() {
        return I18N_BUNDLE_NAME;
    }


    @Bean
    @ConditionalOnMissingBean({ObjectMapper.class})
    public ObjectMapper jacksonObjectMapper() {
        return new ObjectMapper();
    }
}
