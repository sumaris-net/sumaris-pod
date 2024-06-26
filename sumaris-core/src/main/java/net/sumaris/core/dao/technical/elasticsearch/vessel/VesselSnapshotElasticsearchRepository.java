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

package net.sumaris.core.dao.technical.elasticsearch.vessel;

import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnBean({ElasticsearchRestTemplate.class})
public interface VesselSnapshotElasticsearchRepository
    extends ElasticsearchRepository<VesselSnapshotVO, Integer>,
    VesselSnapshotElasticsearchSpecifications {

    Date DEFAULT_END_DATE = Dates.safeParseDate("2100-01-01 00:00:00", Dates.CSV_DATE_TIME);


    Pageable SORT_BY_START_DATE_DESC = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("startDate")));


    @Query("{\"bool\": {\"filter\": [{\"term\": {\"vesselId\": ?0}}]}}")
    List<VesselSnapshotVO> findByVesselId(int vesselId, Pageable pageable);

    @Query("{\"bool\": {\"filter\": [{\"term\": {\"vesselId\": ?0}}, {\"range\": {\"startDate\": {\"lte\": ?1}}}, {\"range\": {\"endDate\": {\"gte\": ?1}}}]}}")
    List<VesselSnapshotVO> findByVesselIdAtDate(int vesselId,
                                                Long timeInMs,
                                                Pageable pageable);


    default Optional<VesselSnapshotVO> findByVesselIdAtDate(int vesselId, @Nullable Date date) {

        List<VesselSnapshotVO> result;
        if (date == null) {
            result = findByVesselId(vesselId, SORT_BY_START_DATE_DESC);
        }
        else {
            result = findByVesselIdAtDate(vesselId, date.getTime(), SORT_BY_START_DATE_DESC);
        }

        // OK: found it !
        if (!result.isEmpty()) return Optional.of(result.get(0));

        // Retry without date
        if (date != null) return findByVesselIdAtDate(vesselId, null);

        return Optional.empty();
    }
}