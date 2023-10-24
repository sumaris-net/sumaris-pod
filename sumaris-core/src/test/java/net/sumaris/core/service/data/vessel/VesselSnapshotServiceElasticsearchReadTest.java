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

package net.sumaris.core.service.data.vessel;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.elasticsearch.ElasticsearchResource;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.UpdateVesselSnapshotsResultVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.List;

@ActiveProfiles({"test", "elasticsearch"})
@TestPropertySource(locations = "classpath:application-test-elasticsearch.properties")
public class VesselSnapshotServiceElasticsearchReadTest extends VesselSnapshotServiceAbstractReadTest {

    @ClassRule
    public static final ElasticsearchResource nodeResource = new ElasticsearchResource();

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Value("${spring.elasticsearch.enabled:false}")
    private boolean enable;

    @Before
    public void setup() {
        // Index vessels
        UpdateVesselSnapshotsResultVO result = service.indexVesselSnapshots(createFilterBuilder().build());
        Assume.assumeNotNull(result);
        Assume.assumeTrue(result.getVessels() > 0);
    }

    @Test
    public void countByFilter() {
        super.countByFilter();
    }

    @Test
    public void findAll() {
        super.findAll();
    }

}
