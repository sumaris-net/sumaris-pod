package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.elasticsearch.vessel.VesselSnapshotElasticsearchRepository;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.elasticsearch.ElasticsearchResource;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 06/11/2019.
 */
@Slf4j
@ActiveProfiles({"test"})
public class VesselElasticsearchRepositoryReadTest extends VesselSnapshotRepositoryAbstractReadTest<VesselSnapshotElasticsearchRepository> {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @ClassRule
    public static final ElasticsearchResource nodeResource = new ElasticsearchResource();

    @Autowired
    protected VesselSnapshotRepository databaseRepository;

    @Autowired
    protected ProgramRepository programRepository;

    @Autowired
    protected VesselSnapshotElasticsearchRepository elasticsearchRepository;

    @Before
    public void setUp() throws Exception {
        super.setUp(elasticsearchRepository);

        // Disable replicas
        elasticsearchRepository.setNumberOfReplicas(0);

        // Fill index
        VesselFilterVO filter = createFilterBuilder().build();
        List<VesselSnapshotVO> vessels = databaseRepository.findAll(filter, 0, 100, "id", SortDirection.ASC, VesselFetchOptions.builder()
                .withBasePortLocation(true)
                .build());
        Assume.assumeNotNull(vessels);
        Assume.assumeTrue(!vessels.isEmpty());

        ProgramVO filteredProgram = Optional.ofNullable(filter.getProgramLabel())
            .map(programRepository::getByLabel)
            .orElse(null);
        // Prepare for indexation
        vessels.forEach(v -> {
            if (v.getEndDate() == null) {
                v.setEndDate(VesselSnapshotElasticsearchRepository.DEFAULT_END_DATE);
            }
            v.setProgram(filteredProgram);
        });
        elasticsearchRepository.bulkIndex(vessels);

        // Wait end of storage
        Thread.sleep(5000);
        nodeResource.waitClusterYellowStatus();
    }

    @Test
    public void count() {
        super.count();
    }

    @Test
    public void findByFilter_defaultSearchAttributes() {
        super.findByFilter_defaultSearchAttributes();
    }

    @Test
    public void findByFilter_registrationCode() {
        super.findByFilter_registrationCode();
    }

    @Test
    public void findByFilter_name() {
        super.findByFilter_name();
    }

    @Test
    public void findByFilter_otherCriteria() {
        super.findByFilter_otherCriteria();
    }

    @Test
    public void findAllVesselFeaturesIdsByFilter() throws ParseException {
        Date minUpdateDate = Dates.parseDateStrictly("01/01/2018", "dd/MM/yyyy");
        List<Integer> vesselFeaturesIds = elasticsearchRepository.findAllVesselFeaturesIdsByFilter(
            VesselFilterVO.builder().minUpdateDate(minUpdateDate)
                .build()
        );
        Assert.assertTrue(CollectionUtils.isNotEmpty(vesselFeaturesIds));

        Date latestUpdateDate = Dates.parseDateStrictly("01/01/2023", "dd/MM/yyyy");
        List<Integer> latestVesselFeaturesIds = elasticsearchRepository.findAllVesselFeaturesIdsByFilter(
            VesselFilterVO.builder().minUpdateDate(latestUpdateDate)
                .build()
        );
        Assert.assertTrue(CollectionUtils.isNotEmpty(latestVesselFeaturesIds));

        // Compare each other
        Assert.assertTrue(latestVesselFeaturesIds.size() < vesselFeaturesIds.size());
    }
}
