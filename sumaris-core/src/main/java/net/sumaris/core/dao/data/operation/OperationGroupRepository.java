package net.sumaris.core.dao.data.operation;

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

import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.data.DataRepository;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationGroupRepository
    extends DataRepository<Operation, OperationGroupVO, OperationGroupFilterVO, DataFetchOptions>, OperationGroupSpecifications {

    @Cacheable(cacheNames = CacheConfiguration.Names.MAIN_UNDEFINED_OPERATION_GROUP_BY_TRIP_ID, key="#p0", unless="#result==null")
    @Query("select min(o.id) from Operation o inner join o.trip t where o.trip.id=:tripId and o.startDateTime = t.departureDateTime and o.endDateTime = t.returnDateTime")
    Optional<Integer> getMainUndefinedOperationGroupId(@Param("tripId") int tripId);


}
