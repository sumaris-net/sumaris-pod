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

package net.sumaris.rdf.cli.config;

import net.sumaris.rdf.cli.action.RdfDatasetAction;
import org.nuiton.config.ConfigActionDef;

/**
 * <p>
 * BatchesServerConfigurationAction class.
 * </p>
 */
public enum RdfConfigurationAction implements ConfigActionDef {

    LOAD(RdfDatasetAction.class.getName() + "#init", "Init Triple store", RdfDatasetAction.INIT_ALIAS),

    DUMP(RdfDatasetAction.class.getName() + "#dump", "Dump Triple store", RdfDatasetAction.DUMP_ALIAS)

    ;

    public final String action;
    public final String description;
    public final String[] aliases;

    RdfConfigurationAction(String action, String description, String... aliases) {
        this.action = action;
        this.description = description;
        this.aliases = aliases;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAction() {
        return action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAliases() {
        return aliases;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
