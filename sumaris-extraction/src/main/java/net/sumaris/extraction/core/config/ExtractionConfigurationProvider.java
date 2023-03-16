package net.sumaris.extraction.core.config;

/*-
 * #%L
 * Quadrige3 Core :: Quadrige3 Server Core
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
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

import net.sumaris.core.config.ApplicationConfigAliasProvider;
import net.sumaris.extraction.cli.config.ExtractionConfigurationAction;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.config.ConfigActionDef;
import org.nuiton.config.ConfigOptionDef;

import java.util.Locale;

import static org.nuiton.i18n.I18n.l;

/**
 * Config provider (for site generation).
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class ExtractionConfigurationProvider implements ApplicationConfigProvider, ApplicationConfigAliasProvider {

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return "sumaris-extraction";
	}

	/** {@inheritDoc} */
	@Override
	public String getDescription(Locale locale) {
		return l(locale, "sumaris.extraction.config");
	}

	/** {@inheritDoc} */
	@Override
	public ConfigOptionDef[] getOptions() {
		return ExtractionConfigurationOption.values();
	}

	/** {@inheritDoc} */
	@Override
	public ConfigActionDef[] getActions() {
		return ExtractionConfigurationAction.values();
	}

	@Override
	public void addAlias(ApplicationConfig applicationConfig) {
		// CLI options
		applicationConfig.addAlias("--format", "--option", ExtractionConfigurationOption.EXTRACTION_CLI_OUTPUT_FORMAT.getKey());
		applicationConfig.addAlias("--frequency", "--option", ExtractionConfigurationOption.EXTRACTION_CLI_FREQUENCY.getKey());
	}
}
