package net.sumaris.cli.action;

/*
 * #%L
 * SIH-Adagio :: Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2014 Ifremer
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.service.technical.schema.DatabaseSchemaService;
import org.nuiton.i18n.I18n;
import org.nuiton.version.Version;

import java.io.File;

/**
 * <p>DatabaseChangeLogAction class.</p>
 *
 */
@Slf4j
public class DatabaseGenerateChangeLogAction {

    /**
     * <p>run.</p>
     */
    public void run() {
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        DatabaseSchemaService service = ServiceLocator.instance().getDatabaseSchemaService();

        log.info("Starting change log file generation...");

        // Check if database is well loaded
        if (!service.isDbLoaded()) {
            log.warn("Could not generate changelog file: database seems to be empty !");
            return;
        }

        try {
            Version actualDbVersion = service.getSchemaVersion().orElse(null);
            if (actualDbVersion != null) {
                log.info(I18n.t("sumaris.persistence.schemaVersion", actualDbVersion.toString()));
            }

            Version modelVersion = config.getVersion();
            log.info(I18n.t("sumaris.persistence.modelVersion", modelVersion.toString()));
        } catch (SumarisTechnicalException e) {
            log.error("Error while getting versions.", e);
        }

        File outputFile = ActionUtils.checkAndGetOutputFile(false,
                this.getClass());
        
        try {
            log.info("Launching changelog file generation...");
            service.generateDiffChangeLog(outputFile);
            if (outputFile != null) {
                log.info(String.format("Database changelog file successfully generated at %s", outputFile));
            }
            else {
                log.info("Database changelog file successfully generated.");
            }
        } catch (SumarisTechnicalException e) {
            log.error("Error while generating changelog file.", e);
        }
    }
}
