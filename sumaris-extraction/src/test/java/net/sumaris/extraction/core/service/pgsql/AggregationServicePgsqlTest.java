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

package net.sumaris.extraction.core.service.pgsql;

import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.service.*;
import org.junit.*;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author peck7 on 17/12/2018.
 */
@ActiveProfiles("pgsql")
public class AggregationServicePgsqlTest extends AbstractAggregationServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("pgsql");

}
