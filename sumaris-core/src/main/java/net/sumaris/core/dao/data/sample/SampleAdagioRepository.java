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

import net.sumaris.core.model.data.Sample;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

/**
 * @author peck7 on 01/09/2020.
 */
@Repository
@ConditionalOnProperty(
        prefix = "sumaris.persistence",
        name = {"adagio.schema"},
        havingValue = "SIH2_ADAGIO_DBA",
        matchIfMissing = true
)
public interface SampleAdagioRepository
    extends JpaRepository<Sample, Integer> {

    @Query(value = "SELECT distinct M.ALPHANUMERICAL_VALUE " +
                   "FROM SIH2_ADAGIO_DBA.SAMPLE s " +
                   "INNER JOIN SIH2_ADAGIO_DBA.SAMPLE_MEASUREMENT M on M.SAMPLE_FK = S.ID " +
                   "WHERE S.PROGRAM_FK = :programLabel " +
                   "AND M.PMFM_FK = :tagIdPmfmId " +
                   "AND M.ALPHANUMERICAL_VALUE IN (:tagIds) " +
                   "AND S.id NOT IN (:excludedIds)",
            nativeQuery = true)
    Set<String> getDuplicatedTagIdsByProgramLabel(@Param("programLabel") String programLabel,
                                                  @Param("tagIdPmfmId") Integer tagIdPmfmId,
                                                  @Param("tagIds") Collection<String> tagIds,
                                                  @Param("excludedIds") Collection<Integer> excludedIds);
}
