package net.sumaris.core.dao.data.trip;

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

import net.sumaris.core.dao.data.RootDataRepository;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.vo.data.TripFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface TripRepository extends
    RootDataRepository<Trip, TripVO, TripFilterVO, TripFetchOptions>,
    TripSpecifications {

    @Query("select p.id from Trip t inner join t.program p where t.id = :id")
    int getProgramIdById(@Param("id") int id);

    @Modifying
    @Query("delete from Trip t where t.id in (select l.trip.id from Landing l where l.id = :landingId)")
    void deleteByLandingId(@Param("landingId") int landingId);

    @Modifying
    @Query("delete from Trip t where t.id in (select l.trip.id from Landing l where l.id in (:landingIds))")
    void deleteByLandingIds(@Param("landingIds") Collection<Integer> landingIds);
}
