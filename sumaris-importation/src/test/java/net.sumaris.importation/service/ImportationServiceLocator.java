package net.sumaris.importation.service;

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

import net.sumaris.core.service.CoreServiceLocator;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.data.OperationService;
import net.sumaris.core.service.data.TripService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.schema.DatabaseSchemaService;

/**
 * Locates and provides all available application services.
 */
public class ImportationServiceLocator extends CoreServiceLocator {

    /**
     * <p>getIcesFileImportService.</p>
     *
     * @return a {@link FileImportService} object.
     */
    public static FileImportService getIcesFileImportService() {
        return ServiceLocator.instance().getService("icesFileImportService", FileImportService.class);
    }
}
