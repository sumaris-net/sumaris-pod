package net.sumaris.core.service.data;

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.aggregatedLanding.AggregatedLandingVO;
import net.sumaris.core.vo.filter.AggregatedLandingFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * @author peck7 on 13/06/2020.
 */
@TestPropertySource(locations = "classpath:sumaris-core-test-oracle.properties")
public class AggregatedLandingServiceReadTest extends AbstractServiceTest {

    private static final Logger log = LoggerFactory.getLogger(AggregatedLandingServiceReadTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

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