package net.sumaris.core.config;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
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



import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.config.ConfigActionDef;
import org.nuiton.config.ConfigOptionDef;

import java.util.Locale;

import static org.nuiton.i18n.I18n.l;

/**
 * Config provider (for site generation).
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public class SumarisConfigurationProvider implements ApplicationConfigProvider {

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return "sumaris-core-shared";
	}

	/** {@inheritDoc} */
	@Override
	public String getDescription(Locale locale) {
		return l(locale, "sumaris.config");
	}

	/** {@inheritDoc} */
	@Override
	public ConfigOptionDef[] getOptions() {
		return SumarisConfigurationOption.values();
	}

	/** {@inheritDoc} */
	@Override
	public ConfigActionDef[] getActions() {
		return new ConfigActionDef[0];
	}	
}
