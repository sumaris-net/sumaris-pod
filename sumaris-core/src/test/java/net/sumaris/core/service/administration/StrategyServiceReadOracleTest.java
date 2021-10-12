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
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@Ignore("Use only Ifremer Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
@Slf4j
public class StrategyServiceReadOracleTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb("oracle");

    @Autowired
    private StrategyService service;

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
}
