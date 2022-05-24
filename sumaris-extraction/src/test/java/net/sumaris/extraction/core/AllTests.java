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

package net.sumaris.extraction.core;

import net.sumaris.extraction.core.dao.technical.DaosTest;
import net.sumaris.extraction.core.service.ExtractionProductServiceTest;
import net.sumaris.extraction.core.service.ExtractionTypeServiceReadTest;
import net.sumaris.extraction.core.service.hsqldb.ExtractionManagerHsqlDbTest;
import net.sumaris.extraction.core.service.hsqldb.ExtractionProductServiceHsqlDbTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by Ludovic on 02/02/2016.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // DAO
    DaosTest.class,
    // Service
    ExtractionTypeServiceReadTest.class,
    ExtractionProductServiceHsqlDbTest.class,
    ExtractionManagerHsqlDbTest.class
})
public class AllTests {

    @ClassRule
    public static InitTests initTests = new InitTests();

}
