package net.sumaris.core.service;

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

import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.data.OperationService;
import net.sumaris.core.service.data.TripService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.technical.schema.DatabaseSchemaService;
import net.sumaris.core.service.technical.SoftwareService;

/**
 * Locates and provides all available application services.
 */
public class CoreServiceLocator {

    /**
     * <p>getDatabaseSchemaService.</p>
     *
     * @return a {@link DatabaseSchemaService} object.
     */
    public static DatabaseSchemaService getDatabaseSchemaService() {
        return ServiceLocator.instance().getService("databaseSchemaService", DatabaseSchemaService.class);
    }

    /**
     * <p>getTripService.</p>
     *
     * @return a {@link TripService} object.
     */
    public static TripService getTripService() {
        return ServiceLocator.instance().getService("tripService", TripService.class);
    }

    /**
     * <p>getTripService.</p>
     *
     * @return a {@link TripService} object.
     */
    public static OperationService getOperationService() {
        return ServiceLocator.instance().getService("operationService", OperationService.class);
    }

    /**
     * <p>getPersonService.</p>
     *
     * @return a {@link PersonService} object.
     */
    public static PersonService getPersonService() {
        return ServiceLocator.instance().getService("personService", PersonService.class);
    }

    /**
     * <p>getDepartmentService.</p>
     *
     * @return a {@link DepartmentService} object.
     */
    public static DepartmentService getDepartmentService() {
        return ServiceLocator.instance().getService("departmentService", DepartmentService.class);
    }

    /**
     * <p>getReferentialService.</p>
     *
     * @return a {@link ReferentialService} object.
     */
    public static ReferentialService getReferentialService() {
        return ServiceLocator.instance().getService("referentialService", ReferentialService.class);
    }

    /**
     * <p>getDatabaseSchemaService.</p>
     *
     * @return a {@link DatabaseSchemaService} object.
     */
    public static SoftwareService getPodConfigService() {
        return ServiceLocator.instance().getService("podConfigService", SoftwareService.class);
    }
}
