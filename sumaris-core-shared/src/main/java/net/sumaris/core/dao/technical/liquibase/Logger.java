package net.sumaris.core.dao.technical.liquibase;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.logging.core.AbstractLogger;

import org.slf4j.LoggerFactory;

/**
 * Liquibase finds this class by itself by doing a custom component scan (sl4fj wasn't generic enough).
 */
public class Logger extends AbstractLogger {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("liquibase");
    private String name = "";

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void severe(String message) {
        LOGGER.error("{} {}", name, message);
    }

    @Override
    public void severe(String message, Throwable e) {
        LOGGER.error("{} {}", name, message, e);
    }

    @Override
    public void warning(String message) {
        LOGGER.warn("{} {}", name, message);
    }

    @Override
    public void warning(String message, Throwable e) {
        LOGGER.warn("{} {}", name, message, e);
    }

    @Override
    public void info(String message) {
        LOGGER.info("{} {}", name, message);
    }

    @Override
    public void info(String message, Throwable e) {
        LOGGER.info("{} {}", name, message, e);
    }

    @Override
    public void debug(String message) {
        LOGGER.debug("{} {}", name, message);
    }

    @Override
    public void debug(String message, Throwable e) {
        LOGGER.debug("{} {}", message, e);
    }

    @Override
    public void setLogLevel(String logLevel, String logFile) {
    }

    @Override
    public void setChangeLog(DatabaseChangeLog databaseChangeLog) {
    }

    @Override
    public void setChangeSet(ChangeSet changeSet) {
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
