package net.sumaris.core.service.data;

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

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author ludovic.pecquot@e-is.pro
 * <p>
 * Service in charge of operation group beans
 * TODO: build unit tests
 */
@Transactional
public interface OperationGroupService {

    @Transactional(readOnly = true)
    List<MetierVO> getMetiersByTripId(int tripId);

    @Transactional(readOnly = true)
    List<OperationGroupVO> findAllByTripId(int tripId, DataFetchOptions options);

    List<OperationGroupVO> findAllByTripId(int tripId, Page page, DataFetchOptions options);

    @Transactional(readOnly = true)
    OperationGroupVO get(int id);

    /**
     * @deprecated Use cacheable method getMainUndefinedOperationGroupId() instead
     * @param tripId
     * @return
     */
    @Transactional(readOnly = true)
    OperationGroupVO getMainUndefinedOperationGroup(int tripId);

    @Transactional(readOnly = true)
    Optional<Integer> getMainUndefinedOperationGroupId(int tripId);

    List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> metiers);

    void updateUndefinedOperationDates(int tripId, Date startDate, Date endDate);

    List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> operations);

    void delete(int id);

    void delete(List<Integer> ids);

}
