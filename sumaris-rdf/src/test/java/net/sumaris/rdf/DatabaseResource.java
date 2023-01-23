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

package net.sumaris.rdf;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.rdf.core.config.RdfConfigurationOption;
import org.apache.commons.io.FileUtils;
import org.junit.Assume;
import org.junit.runner.Description;

import java.io.File;

/**
 * To be able to manage database connection for unit test.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class DatabaseResource extends net.sumaris.core.test.DatabaseResource {


	public static DatabaseResource readDb() {
		return new DatabaseResource("", true);
	}

	public static DatabaseResource readDb(String configName) {
		return new DatabaseResource(configName, true);
	}

	public static DatabaseResource writeDb() {
		return new DatabaseResource("", false);
	}

	public static DatabaseResource writeDb(String configName) {
		return new DatabaseResource(configName, false);
	}

	protected DatabaseResource(String configName, boolean readOnly) {
		this(configName, null, readOnly);
	}

	protected DatabaseResource(String configFileSuffix, String datasourceType, boolean readOnly) {
		super(configFileSuffix, datasourceType, readOnly);
	}

	@Override
	public String getDatasourcePlatform() {
		return TestConfiguration.DATASOURCE_PLATFORM;
	}

	@Override
	protected String getConfigFilesPrefix() {
		return TestConfiguration.CONFIG_FILE_PREFIX;
	}

	@Override
	protected String getDbModuleDirectory() {
		return TestConfiguration.MODULE_NAME;
	}

	@Override
	protected void before(Description description) throws Throwable {
		super.before(description);

		// Init the TDB2 Triple store
		initTripleStore();
	}

	protected String getRdfDirectory() {
		return "target/rdf";
	}

	protected void initTripleStore() throws Throwable {
		// Source dir, init by InitTests.main
		File sourceDirectory = new File(getRdfDirectory());
		Assume.assumeTrue(String.format("No RDF dataset found at '%s'. Please run InitTests and retry", sourceDirectory.getPath()), sourceDirectory.exists() && sourceDirectory.isDirectory());

		SumarisConfiguration config = SumarisConfiguration.getInstance();
		if (canWrite()) {
			File targetDirectory = getResourceDirectory("data/rdf");
			config.getApplicationConfig().setOption(RdfConfigurationOption.RDF_DIRECTORY.getKey(), targetDirectory.getCanonicalPath());

			if (!targetDirectory.getParentFile().exists()) {
				FileUtils.forceMkdir(targetDirectory.getParentFile());
			}
			FileUtils.copyDirectory(sourceDirectory, targetDirectory);
		}
		// Readonly: use existing source dir
		else {
			config.getApplicationConfig().setOption(RdfConfigurationOption.RDF_DIRECTORY.getKey(), sourceDirectory.getCanonicalPath());
		}
	}
}
