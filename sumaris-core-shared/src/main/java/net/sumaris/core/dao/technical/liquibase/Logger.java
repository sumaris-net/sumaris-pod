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

import liquibase.logging.LogType;
import liquibase.logging.core.AbstractLogger;

import org.slf4j.LoggerFactory;

/**
 * Liquibase finds this class by itself by doing a custom component scan (sl4fj wasn't generic enough).
 */
public class Logger extends AbstractLogger {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger("liquibase");

    @Override
    public void severe(LogType logType, String message) {
        log.error("{} {}", logType.name(), message);
    }

    @Override
    public void severe(LogType logType, String message, Throwable e) {
        log.error("{} {}", logType.name(), message, e);
    }

    @Override
    public void warning(LogType logType, String message) {
        log.warn("{} {}", logType.name(), message);
    }

    @Override
    public void warning(LogType logType, String message, Throwable e) {
        log.warn("{} {}", logType.name(), message, e);
    }

    @Override
    public void info(LogType logType, String message) {
        log.info("{} {}", logType.name(), message);
    }

    @Override
    public void info(LogType logType, String message, Throwable e) {
        log.info("{} {}", logType.name(), message, e);
    }

    @Override
    public void debug(LogType logType, String message) {
        log.debug("{} {}", logType.name(), message);
    }

    @Override
    public void debug(LogType logType, String message, Throwable e) {
        log.debug("{} {}", logType.name(), message, e);
    }

}
