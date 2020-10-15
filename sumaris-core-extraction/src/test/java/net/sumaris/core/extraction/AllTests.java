package net.sumaris.core.extraction;

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

import net.sumaris.core.extraction.dao.technical.DaosTest;
import net.sumaris.core.extraction.service.AggregationServiceTest;
import net.sumaris.core.extraction.service.ExtractionServiceTest;
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
    AggregationServiceTest.class,
    ExtractionServiceTest.class
})
public class AllTests {

    @ClassRule
    public static InitTests initTests = new InitTests();

}
