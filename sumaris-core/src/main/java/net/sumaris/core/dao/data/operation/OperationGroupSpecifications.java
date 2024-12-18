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
import net.sumaris.core.dao.data.IDataSpecifications;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.referential.metier.MetierVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationGroupSpecifications
    extends IDataSpecifications<Integer, Operation> {


    List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> operationGroups);

    /**
     * @deprecated use the cacheable function getMainUndefinedOperationGroupId() instead
     * @param tripId
     * @param fetchOptions
     * @return
     */
    OperationGroupVO getMainUndefinedOperationGroup(int tripId, DataFetchOptions fetchOptions);

    void updateUndefinedOperationDates(int tripId, Date startDate, Date endDate);

    /**
     * Get metier ( = operations with same start and end date as trip)
     *
     * @param tripId trip id
     * @return metiers of trip
     */
    List<MetierVO> getMetiersByTripId(int tripId);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheConfiguration.Names.MAIN_UNDEFINED_OPERATION_GROUP_BY_TRIP_ID, key = "#root.args[0]")
            }
    )
    List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> metiers);
}
