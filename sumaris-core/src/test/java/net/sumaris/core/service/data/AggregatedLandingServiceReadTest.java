package net.sumaris.core.service.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.aggregatedLanding.AggregatedLandingVO;
import net.sumaris.core.vo.filter.AggregatedLandingFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * @author peck7 on 13/06/2020.
 */
@Ignore("Use only on SFA Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
@Slf4j
public class AggregatedLandingServiceReadTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb("oracle");

    @Autowired
    private AggregatedLandingService aggregatedLandingService;

    @Test
    public void findAll() {

        long start = System.currentTimeMillis();

        List<AggregatedLandingVO> aggregatedLandings = aggregatedLandingService.findAll(
            AggregatedLandingFilterVO.builder()
                .programLabel("SIH-OBSDEB")
                .locationId(119) // LS-BO
                .startDate(Dates.safeParseDate("17/09/2018", "dd/MM/yyyy"))
                .endDate(Dates.safeParseDate("23/09/2018", "dd/MM/yyyy"))
                .build()
        );

        log.info(String.format("aggregated landings loaded in %sms", System.currentTimeMillis() - start));
        Assert.assertNotNull(aggregatedLandings);
        Assert.assertEquals(37, aggregatedLandings.size());
    }
}
