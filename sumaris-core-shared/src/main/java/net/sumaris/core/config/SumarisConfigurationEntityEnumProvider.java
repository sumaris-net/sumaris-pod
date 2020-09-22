package net.sumaris.core.config;


import net.sumaris.core.dao.technical.model.annotation.EntityEnums;
import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.config.ConfigActionDef;
import org.nuiton.config.ConfigOptionDef;

import java.util.Locale;

import static org.nuiton.i18n.I18n.l;

/**
 * Config provider for model enumeration (enum with ModelEnum annotation)
 *
 * @author Benoit Lavenier (benoit.lavenier@e-is.pro)
 */
public class SumarisConfigurationEntityEnumProvider implements ApplicationConfigProvider {

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return "sumaris-core-shared-db-enumerations";
	}

	/** {@inheritDoc} */
	@Override
	public String getDescription(Locale locale) {
		return l(locale, "sumaris-db-enumerations.config");
	}

	/** {@inheritDoc} */
	@Override
	public ConfigOptionDef[] getOptions() {
		// Add options from model enumerations
		return EntityEnums.getEntityEnumAsOptions(null);
	}

	/** {@inheritDoc} */
	@Override
	public ConfigActionDef[] getActions() {
		return new ConfigActionDef[0];
	}

}
