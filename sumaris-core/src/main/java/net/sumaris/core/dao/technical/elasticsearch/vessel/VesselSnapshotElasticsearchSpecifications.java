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

import co.elastic.clients.elasticsearch.snapshot.Repository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public interface VesselSnapshotElasticsearchSpecifications {

    void recreate();

    Optional<Date> findMaxUpdateDate();

    List<Integer> findAllIds();

    List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
                                   Page page,
                                   VesselFetchOptions fetchOptions);

    List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
                                   int offset, int size,
                                   String sortAttribute, SortDirection sortDirection,
                                   VesselFetchOptions fetchOptions);

    boolean enableRegistrationCodeSearchAsPrefix();
}
