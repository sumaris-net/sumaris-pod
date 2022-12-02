package net.sumaris.core.dao.data.sample;

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

import net.sumaris.core.dao.data.RootDataRepository;
import net.sumaris.core.model.data.Sample;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.SampleFilterVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SampleRepository
    extends RootDataRepository<Sample, SampleVO, SampleFilterVO, SampleFetchOptions>, SampleSpecifications {

    @Modifying
    @Query("delete from Sample s where s.landing.id = :landingId")
    void deleteByLandingId(@Param("landingId") int landingId);

    @Query(value = "SELECT distinct m.alphanumericalValue " +
                   "from Sample s " +
                   "inner join s.measurements m " +
                   "where s.program.id = :programId " +
                   "and m.pmfm.id = :tagIdPmfmId " +
                   "and m.alphanumericalValue IN (:tagIds) " +
                   "and s.id NOT IN (:excludedIds)")
    Set<String> getDuplicatedTagIdsByProgramId(@Param("programId") int programId,
                                               @Param("tagIdPmfmId") Integer tagIdPmfmId,
                                               @Param("tagIds") Collection<String> tagIds,
                                               @Param("excludedIds") Collection<Integer> excludedIds);
}
