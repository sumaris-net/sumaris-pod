package net.sumaris.rdf;

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

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.rdf.cli.Application;
import net.sumaris.rdf.cli.action.RdfDatasetAction;
import net.sumaris.rdf.core.config.RdfConfigurationOption;
import net.sumaris.rdf.core.service.store.RdfDatasetService;
import org.nuiton.i18n.I18n;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;

@Slf4j
public class InitTests extends net.sumaris.core.test.InitTests {

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

    public String getRdfDirectory() {
        return "target/rdf";
    }

    protected String[] getConfigArgs() {
        return new String[]{
                // Data source
                "--option", SumarisConfigurationOption.DB_DIRECTORY.getKey(), getTargetDbDirectory(),
                "--option", SumarisConfigurationOption.JDBC_URL.getKey(), SumarisConfigurationOption.JDBC_URL.getDefaultValue(),
                // Enable RDF, and TDB2
                "--option", RdfConfigurationOption.RDF_ENABLED.getKey(), Boolean.TRUE.toString(),
                "--option", RdfConfigurationOption.RDF_DIRECTORY.getKey(), getRdfDirectory(),
                "--option", RdfConfigurationOption.RDF_TDB2_ENABLED.getKey(), Boolean.TRUE.toString(),
                // Disable auto-load
                "--option", RdfConfigurationOption.RDF_DATA_IMPORT_ENABLED.getKey(), Boolean.FALSE.toString(),
                // Disable load external data (Sandre, MNHN, etc)
                "--option", RdfConfigurationOption.RDF_DATA_IMPORT_EXTERNAL.getKey(), Boolean.FALSE.toString(),
        };
    }

    @Override
    protected String getModuleName() {
        return TestConfiguration.MODULE_NAME;
    }

    @Override
    protected void before() throws Throwable {

        super.before();

        loadRdfDataset();
    }

    protected void loadRdfDataset() {

        Long startTime = System.currentTimeMillis();
        String jdbcUrl = config.getJdbcURL();
        boolean isFileDatabase = Daos.isFileDatabase(jdbcUrl);
        if (isFileDatabase) setDatabaseReadonly(false);

        // Delete old data
        File rdfDirectory = new File(getRdfDirectory());
        if (rdfDirectory.exists() && rdfDirectory.isDirectory()) {
            log.info(I18n.t("sumaris.persistence.newEmptyDatabase.deleteDirectory", getRdfDirectory()));
            Files.deleteQuietly(rdfDirectory);
        }

        // Set log levels
        System.setProperty("logging.level.root", "error");
        System.setProperty("logging.level.net.sumaris", "warn");
        System.setProperty("logging.level." + RdfDatasetService.class.getName(), "info");
        System.setProperty("logging.level.net.sumaris.core", "error");
        System.setProperty("logging.level.org.hibernate", "error");
        System.setProperty("logging.level.Hibernate Types", "error");

        String[] args = ImmutableList.<String>builder()
                .add(RdfDatasetAction.LOAD_ALIAS)
                .addAll(Arrays.asList(getConfigArgs()))
                .build().toArray(new String[0]);
        Application.run(args, getModuleName() + "-test.properties");

        log.info("Test {TDB2} triple store has been loaded, in {}", TimeUtils.printDurationFrom(startTime));
        if (isFileDatabase) {
            try {
                Daos.shutdownDatabase(config.getConnectionProperties());
            } catch (SQLException e) {
                // Already shutdown ?
            }
            setDatabaseReadonly(true);
        }
    }
}
