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

package net.sumaris.extraction.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class InitTests extends net.sumaris.core.test.InitTests {

    private String[] args;

    public static void main(String[] args) {

        InitTests initTests = new InitTests();
        initTests.args = args;
        try {

            // Force replacement
            //initTests.setReplaceDbIfExists(true);

            initTests.before();
        } catch (Throwable ex) {
            log.error(ex.getLocalizedMessage(), ex);
        }
    }
    public InitTests() {
        super();
    }

    public InitTests(String datasourcePlatform) {
        super(datasourcePlatform);
    }

    @Override
    public String getTargetDbDirectory() {
        return "../sumaris-core/target/db";
    }

    @Override
    protected String getModuleName() {
        return TestConfiguration.MODULE_NAME;
    }

    @Override
    protected  String getConfigFileName(){
        return TestConfiguration.CONFIG_FILE_PREFIX + "-" + this.datasourcePlatform + ".properties";
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
