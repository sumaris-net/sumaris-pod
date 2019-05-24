package net.sumaris.server;

/*-
 * #%L
 * SUMARiS :: Sumaris Server Core
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


/**
 * To be able to manage database connection for unit test.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class DatabaseResource extends net.sumaris.core.test.DatabaseResource {

	public static final String MODULE_NAME = "sumaris-core-server";

	protected DatabaseResource(String configName, boolean writeDb) {
		super(configName, writeDb);
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
	protected void initConfiguration(String configFilename) {
		String[] configArgs = getConfigArgs();
		SumarisConfiguration config = new SumarisConfiguration(configFilename, configArgs);
		SumarisConfiguration.setInstance(config);
	}

}
