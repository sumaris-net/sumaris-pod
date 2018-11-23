package net.sumaris.core.config;

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

import net.sumaris.core.action.DatabaseGenerateChangeLogAction;
import net.sumaris.core.action.DatabaseCreateSchemaAction;
import net.sumaris.core.action.DatabaseUpdateSchemaAction;
import net.sumaris.core.action.HelpAction;
import org.nuiton.config.ConfigActionDef;

/**
 * <p>
 * BatchesServerConfigurationAction class.
 * </p>
 */
public enum SumarisCoreConfigurationAction implements ConfigActionDef {

    HELP(HelpAction.class.getName() + "#show", "Shows help", "-h", "--help"),

    DB_CREATE(DatabaseCreateSchemaAction.class.getName() + "#run", "Create new database", "--schema-create"),

    DB_UPDATE_SCHEMA(DatabaseUpdateSchemaAction.class.getName() + "#run", "Update an existing database", "--schema-update"),

    DB_CHANGELOG(DatabaseGenerateChangeLogAction.class.getName() + "#run", "Update an existing database", "--schema-changelog");

    public final String action;
    public final String description;
    public final String[] aliases;

    SumarisCoreConfigurationAction(String action, String description, String... aliases) {
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
