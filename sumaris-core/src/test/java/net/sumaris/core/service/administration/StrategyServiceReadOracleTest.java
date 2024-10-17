package net.sumaris.core.service.administration;

/*-
 * #%L
 * SUMARiS:: Core
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.PeriodVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@Ignore("Use only on Ifremer Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-test-oracle.properties")
@Slf4j
public class StrategyServiceReadOracleTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb("oracle");

    @Autowired
    private StrategyService service;

    @Autowired
    private ProgramService programService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {

        configurationService.applySoftwareProperties();
    }

    @Test
    public void findByFilterWithFetch() {

        Page page = Page.builder().size(100).build();

        StrategyFilterVO filter = StrategyFilterVO.builder()
            .programLabels(new String[]{"SIH-PARAM-BIO"})
            .build();
        StrategyFetchOptions fetchOptions = StrategyFetchOptions.builder()
            .withTaxonNames(true)
            .build();

        // First call
        long duration1 = 0l;
        {
            long time = System.currentTimeMillis();
            List<StrategyVO> strategies = service.findByFilter(filter, page, fetchOptions);
            duration1 = System.currentTimeMillis() - time;
            Assert.assertNotNull(strategies);
            Assert.assertTrue(CollectionUtils.isNotEmpty(strategies));

            strategies.forEach(s -> {
                Assert.assertTrue(CollectionUtils.isNotEmpty(s.getTaxonNames()));
            });

            log.info("Strategies fetched (first call) in {}ms", duration1);
        }

        // Second call (should be cached
        long duration2 = 0l;
        {
            long time = System.currentTimeMillis();
            service.findByFilter(filter, page, fetchOptions);
            duration2 = System.currentTimeMillis() - time;

            // Should be x2 more efficient
            log.info("Strategies fetched (second call) in {}ms", duration2);
            Assert.assertTrue( duration2 <= duration1 / 2);

        }
    }

    @Test
    public void findByFilter() {
        // Load SIH-OBSMER program
        ProgramVO program = programService.getByLabel("SIH-OBSMER", ProgramFetchOptions.MINIMAL);
        // Load FRA country
        LocationVO country = getLocationByLabelAndLevel("FRA", LocationLevelEnum.COUNTRY.getId());
        // Load an FRA harbour (XDZ - Douarnenez)
        LocationVO harbour = getLocationByLabelAndLevel("XDZ", LocationLevelEnum.HARBOUR.getId());

        Page page = Page.builder().size(10).build();

        // Filter by program
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                .programIds(new Integer[]{program.getId()})
                .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(4, strategies.size());
        }

        // Filter by country location, and dates (startDate only
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                .programIds(new Integer[]{program.getId()})
                .locationIds(new Integer[]{country.getId()})
                .periods(new PeriodVO[]{PeriodVO.builder()
                    .startDate(Dates.safeParseDate("2020-11-23 00:00:00", "yyyy-MM-dd HH:mm:ss"))
                    .build(),
                })
                .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
            Assert.assertEquals("OBSMER démarrage le 23/11/2020", strategies.get(0).getName());
        }

        // Filter by country location, and dates (startDate AND endDate)
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                .programIds(new Integer[]{program.getId()})
                .locationIds(new Integer[]{country.getId()})
                .periods(new PeriodVO[]{PeriodVO.builder()
                    .startDate(Dates.safeParseDate("2020-11-23 00:00:00", "yyyy-MM-dd HH:mm:ss"))
                    .endDate(Dates.safeParseDate("2021-03-31", "yyyy-MM-dd"))
                    .build(),
                })
                .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
            Assert.assertEquals("OBSMER démarrage le 23/11/2020", strategies.get(0).getName());
        }


        // Filter by location, and dates (startDate AND endDate)
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                .programIds(new Integer[]{program.getId()})
                .locationIds(new Integer[]{harbour.getId()})
                .periods(new PeriodVO[]{PeriodVO.builder()
                    .startDate(Dates.safeParseDate("2020-11-23 00:00:00", "yyyy-MM-dd HH:mm:ss"))
                    .build(),
                })
                .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
            Assert.assertEquals("OBSMER démarrage le 23/11/2020", strategies.get(0).getName());
        }
    }

    private LocationVO getLocationByLabelAndLevel(String label, int levelId) {
        List<LocationVO> locations = locationService.findByFilter(LocationFilterVO.builder()
                .label(label)
            .levelIds(new Integer[]{levelId})
            .build());
        Assume.assumeNotNull(locations);
        Assume.assumeTrue(locations.size() == 1);
        return CollectionUtils.extractSingleton(locations);
    }
}
