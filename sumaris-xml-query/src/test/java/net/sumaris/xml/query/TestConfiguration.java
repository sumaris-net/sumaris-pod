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

package net.sumaris.xml.query;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
    exclude = {
        LiquibaseAutoConfiguration.class,
        FreeMarkerAutoConfiguration.class,
        JmsAutoConfiguration.class
    },
    scanBasePackages = {
        "net.sumaris.xml.query"
    }
)
public class TestConfiguration extends net.sumaris.core.test.TestConfiguration {

    public static final String MODULE_NAME = "sumaris-xml-query";
    public static final String DATASOURCE_PLATFORM = "hsqldb";
    public static final String CONFIG_FILE_PREFIX = "application";
    public static final String CONFIG_FILE_NAME = CONFIG_FILE_PREFIX + ".properties";
    public static final String I18N_BUNDLE_NAME = MODULE_NAME + "-i18n";

    @Override
    protected String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    protected String getI18nBundleName() {
        return I18N_BUNDLE_NAME;
    }
}
