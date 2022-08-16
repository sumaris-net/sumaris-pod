package net.sumaris.extraction.cli.action;

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
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.extraction.core.service.ExtractionService;
import net.sumaris.extraction.core.service.ExtractionServiceLocator;

/**
 * <p>DatabaseChangeLogAction class.</p>
 *
 */
@Slf4j
public class ExtractionUpdateAction {

    /**
     * <p>Update a product (execute extraction or aggregation).</p>
     */
    public void run() throws InterruptedException {

        // Create the job
        ExtractionService manager = ExtractionServiceLocator.extractionManager();

        // Run it !
        ProcessingFrequencyEnum frequency = ExtractionConfiguration.instance().getExtractionCliFrequency();
        manager.executeAll(frequency);

        // Waiting 10s, to let DB drop tables (asynchronously)
        Thread.sleep(10000);
    }

}
