package net.sumaris.core.service.referential;

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

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.service.referential.location.LocationByPositionService;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Future;

@Transactional
public interface LocationService extends LocationByPositionService  {

    @Transactional(readOnly = true)
    LocationVO get(int id);

    @Transactional(readOnly = true)
    List<LocationVO> findByFilter(LocationFilterVO filter);

    @Transactional(readOnly = true)
    List<LocationVO> findByFilter(LocationFilterVO filter, Page page, ReferentialFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    long countByFilter(LocationFilterVO filter);

    void insertOrUpdateRectangleLocations();

    void insertOrUpdateSquares10();

    void insertOrUpdateRectangleAndSquareAreas();

    void updateLocationHierarchy();
    @Async("jobTaskExecutor")
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    Future<Void> asyncUpdateLocationHierarchy(@Nullable IProgressionModel progressionModel);


    /**
     * @deprecated use insertOrUpdateRectangleAndSquareAreas instead
     */
    @Deprecated
    void updateRectanglesAndSquares();

}
