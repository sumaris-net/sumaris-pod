package net.sumaris.importation;

/*-
 * #%L
 * SUMARiS :: Sumaris Client Core
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.extraction.dao.DatabaseResource;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class InitTests extends net.sumaris.core.test.InitTests {

    private static final Logger log = LoggerFactory.getLogger(InitTests.class);

    private String[] args;

    public static void main(String[] args) {

        InitTests initTests = new InitTests();
        initTests.args = args;
        try {

            // Force replacement
            initTests.setReplaceDbIfExists(true);

            initTests.before();
        } catch (Throwable ex) {
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public String getTargetDbDirectory() {
        return "target/db";
    }

    @Override
    protected String getModuleName() {
        return DatabaseResource.MODULE_NAME;
    }

    @Override
    protected SumarisConfiguration createConfig() {
        SumarisConfiguration config = super.createConfig();
        config.getApplicationConfig().setOption(SumarisConfigurationOption.SEQUENCE_START_WITH.getKey(),
                String.valueOf(100));
        return config;
    }

    @Override
    protected void beforeInsert(Connection conn) throws SQLException {
        super.beforeInsert(conn);
    }

    @Override
    protected void afterInsert(Connection conn) throws SQLException {
        super.afterInsert(conn);
    }

    protected String[] getConfigArgs() {
        if (ArrayUtils.isNotEmpty(args)) {
            return args;
        }
        return super.getConfigArgs();
    }
}
