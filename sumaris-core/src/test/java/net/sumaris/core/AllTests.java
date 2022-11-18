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

import net.sumaris.core.dao.administration.UserSettingsRepositoryWriteTest;
import net.sumaris.core.dao.administration.UserTokenRepositoryWriteTest;
import net.sumaris.core.dao.data.*;
import net.sumaris.core.dao.referential.*;
import net.sumaris.core.dao.technical.extraction.ExtractionProductRepositoryWriteTest;
import net.sumaris.core.dao.technical.schema.DatabaseSchemaDaoTest;
import net.sumaris.core.service.data.vessel.VesselServiceWriteTest;
import net.sumaris.core.service.technical.ConfigurationServiceTest;
import net.sumaris.core.service.technical.FileImportServiceTest;
import net.sumaris.core.service.administration.*;
import net.sumaris.core.service.data.*;
import net.sumaris.core.service.referential.*;
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
    // DAO: referential
    LocationRepositoryReadTest.class,
    LocationRepositoryWriteTest.class,
    MetierRepositoryReadTest.class,
    ParameterRepositoryWriteTest.class,
    PmfmRepositoryReadTest.class,
    PmfmRepositoryWriteTest.class,
    TaxonGroupRepositoryWriteTest.class,
    TaxonNameRepositoryReadTest.class,
    // DAO: administration
    UserTokenRepositoryWriteTest.class,
    UserSettingsRepositoryWriteTest.class,
    // DAO : data
    BatchRepositoryWriteTest.class,
    LandingRepositoryReadTest.class,
    SampleRepositoryWriteTest.class,
    TripRepositoryWriteTest.class,
    VesselDaoImplReadTest.class,
    VesselSnapshotDaoImplReadTest.class,
    // DAO: technical
    DatabaseSchemaDaoTest.class,
    ExtractionProductRepositoryWriteTest.class,

    // Service: referential
    LocationServiceReadTest.class,
    LocationServiceWriteTest.class,
    PmfmServiceReadTest.class,
    PmfmServiceWriteTest.class,
    ReferentialServiceReadTest.class,
    ReferentialServiceWriteTest.class,
    ReferentialExternalServiceReadTest.class,
    TaxonGroupServiceWriteTest.class,
    // Service: administration
    DepartmentServiceTest.class,
    PersonServiceTest.class,
    ProgramServiceReadTest.class,
    ProgramServiceWriteTest.class,
    StrategyServiceReadTest.class,
    StrategyServiceWriteTest.class,
    StrategyPredocServiceReadTest.class,
    // Service: data
    ObservedLocationServiceReadTest.class,
    ObservedLocationServiceWriteTest.class,
    LandingServiceReadTest.class,
    LandingServiceWriteTest.class,
    AggregatedLandingServiceReadTest.class,
    TripServiceQualityTest.class,
    TripServiceWriteTest.class,
    TripServiceReadTest.class,
    OperationServiceWriteTest.class,
    PacketServiceWriteTest.class,
    VesselServiceWriteTest.class,
    PhysicalGearServiceReadTest.class,

    // Service: technical
    DatabaseSchemaServiceTest.class,
    ConfigurationServiceTest.class,
    SoftwareServiceTest.class,
    FileImportServiceTest.class,

    // Util
    CryptoUtilsTest.class,
    MD5UtilTest.class,
    MiscTest.class
})
public class AllTests {

    @ClassRule
    public static InitTests initTests = new InitTests();

}
