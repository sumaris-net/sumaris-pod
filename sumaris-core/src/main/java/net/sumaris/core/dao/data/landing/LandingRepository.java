package net.sumaris.core.dao.data.landing;

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
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LandingRepository extends
    RootDataRepository<Landing, LandingVO, LandingFilterVO, LandingFetchOptions>,
    LandingSpecifications {

    Optional<Landing> findFirstByTripId(Integer tripId);

    @Query("select distinct l.id from Landing l where l.observedLocation.id = (:observedLocationId)")
    List<Integer> findAllIdsByObservedLocationId(@Param("observedLocationId") int observedLocationId);

    @Modifying
    @Query("delete from Landing l where l.id in (:ids)")
    void deleteByIds(@Param("ids") Collection<Integer> ids);
}
