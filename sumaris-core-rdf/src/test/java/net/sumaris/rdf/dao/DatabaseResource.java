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

package net.sumaris.rdf.dao;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.rdf.config.RdfConfigurationOption;
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

	public static final String MODULE_NAME = "sumaris-core-rdf";

	private final DatabaseFixtures fixtures;

	public static DatabaseResource readDb() {
		return new DatabaseResource("", false);
	}

	public static DatabaseResource readDb(String configName) {
		return new DatabaseResource(configName, false);
	}

	public static DatabaseResource writeDb() {
		return new DatabaseResource("", true);
	}

	public static DatabaseResource writeDb(String configName) {
		return new DatabaseResource(configName, true);
	}

	protected DatabaseResource(String configName, boolean writeDb) {
		super(configName, writeDb);
		fixtures = new DatabaseFixtures();
	}

	public DatabaseFixtures getFixtures() {
		return fixtures;
	}

	@Override
	public String getBuildEnvironment() {
		return "hsqldb";
	}

	@Override
	protected String getConfigFilesPrefix() {
		return MODULE_NAME +"-test";
	}

	@Override
	protected String getModuleDirectory() {
		return MODULE_NAME;
	}

	@Override
	protected String getI18nBundleName() {
		return MODULE_NAME + "-i18n";
	}

	@Override
	protected void before(Description description) throws Throwable {
		super.before(description);

		// Init the TDB2 Triple store
		initTripleStore();
	}

	protected void initTripleStore() throws Throwable {
		// Source dir, init by InitTests.main
		File sourceDirectory = new File("target/rdf");
		Assume.assumeTrue(String.format("No RDF dataset found at '%s'. Please run InitTests and retry", sourceDirectory.getPath()), sourceDirectory.exists() && sourceDirectory.isDirectory());

		SumarisConfiguration config = SumarisConfiguration.getInstance();
		if (isWriteDb()) {
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
