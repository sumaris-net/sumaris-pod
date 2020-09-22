package net.sumaris.core;

/*-
 * #%L
 * SUMARiS :: Sumaris Client Core
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

import net.sumaris.core.dao.data.*;
import net.sumaris.core.dao.referential.LocationWriteTest;
import net.sumaris.core.dao.referential.TaxonGroupRepositoryWriteTest;
import net.sumaris.core.dao.referential.TaxonNameDaoReadTest;
import net.sumaris.core.dao.technical.extraction.ExtractionProductDaoWriteTest;
import net.sumaris.core.dao.technical.schema.DatabaseSchemaDaoTest;
import net.sumaris.core.service.FileImportServiceTest;
import net.sumaris.core.service.administration.DepartmentServiceTest;
import net.sumaris.core.service.administration.PersonServiceTest;
import net.sumaris.core.service.administration.StrategyServiceTest;
import net.sumaris.core.service.data.*;
import net.sumaris.core.service.referential.LocationServiceReadTest;
import net.sumaris.core.service.referential.LocationServiceWriteTest;
import net.sumaris.core.service.referential.ReferentialServiceReadTest;
import net.sumaris.core.service.referential.TaxonGroupServiceWriteTest;
import net.sumaris.core.service.technical.SoftwareServiceTest;
import net.sumaris.core.service.technical.schema.DatabaseSchemaServiceTest;
import net.sumaris.core.util.crypto.CryptoUtilsTest;
import net.sumaris.core.util.crypto.MD5UtilTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by Ludovic on 02/02/2016.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // DAO : data
    BatchDaoWriteTest.class,
    SampleDaoWriteTest.class,
    TripRepositoryWriteTest.class,
    LandingRepositoryReadTest.class,
    VesselDaoImplReadTest.class,
    VesselSnapshotDaoImplReadTest.class,
    // DAO: referential
    LocationWriteTest.class,
    TaxonGroupRepositoryWriteTest.class,
    TaxonNameDaoReadTest.class,
    // DAO: technical
    DatabaseSchemaDaoTest.class,
    ExtractionProductDaoWriteTest.class,

    // Service: administration
    DepartmentServiceTest.class,
    PersonServiceTest.class,
    StrategyServiceTest.class,

    // Service: data
    ObservedLocationServiceWriteTest.class,
    OperationServiceWriteTest.class,
    TripServiceQualityTest.class,
    TripServiceWriteTest.class,
    TripServiceReadTest.class,
    VesselServiceWriteTest.class,
    LandingServiceReadTest.class,
    LandingServiceWriteTest.class,

    // Service: referential
    LocationServiceReadTest.class,
    LocationServiceWriteTest.class,
    ReferentialServiceReadTest.class,
    TaxonGroupServiceWriteTest.class,

    // Service: technical
    DatabaseSchemaServiceTest.class,
    SoftwareServiceTest.class,
    FileImportServiceTest.class,

    // Util
    CryptoUtilsTest.class,
    MD5UtilTest.class
})
public class AllTests {

    @ClassRule
    public static InitTests initTests = new InitTests();

}
