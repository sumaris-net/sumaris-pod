package net.sumaris.core.dao.administration.programStrategy;

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
import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author peck7 on 24/08/2020.
 */
public interface ProgramRepository
    extends ReferentialRepository<Integer, Program, ProgramVO, ProgramFilterVO, ProgramFetchOptions>,
    ProgramSpecifications {

    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_READ_USER_ID, key = "#p0", unless = "#result==null")
    default List<Integer> getReadableProgramIdsByUserId(int userId) {
        return getProgramIdsByUserIdAndPrivilegeIds(userId, true, null);
    }

    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_WRITE_USER_ID, key = "#p0", unless = "#result==null")
    default List<Integer> getWritableProgramIdsByUserId(int userId) {
        return getProgramIdsByUserIdAndPrivilegeIds(userId, false, ProgramPrivilegeUtils.getWriteIds());
    }

    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_READ_USER_ID, key = "#p0", condition = "#p1==null", unless = "#result==null")
    default List<Integer> getProgramIdsByUserIdAndPrivilegeIds(@Param("userId") int userId,
                                                               @Param("programPrivilegeIds") List<Integer> programPrivilegeIds) {
        return getProgramIdsByUserIdAndPrivilegeIds(userId, CollectionUtils.isEmpty(programPrivilegeIds), programPrivilegeIds);
    }


    @Query(value = "select distinct PROGRAM.id" +
        "   from PERSON P," +
        "       PROGRAM" +
        "           left outer join PROGRAM2DEPARTMENT P2D on PROGRAM.ID = P2D.PROGRAM_FK" +
        "           left outer join PROGRAM2PERSON P2P on PROGRAM.ID = P2P.PROGRAM_FK" +
        "   where P.ID = :userId " +
        "       AND (" +
        "           (P2D.DEPARTMENT_FK = P.DEPARTMENT_FK AND (:anyProgramPrivilege OR P2D.PROGRAM_PRIVILEGE_FK in (:programPrivilegeIds))) " +
        "           OR (P2P.PERSON_FK = P.ID AND (:anyProgramPrivilege OR P2P.PROGRAM_PRIVILEGE_FK in (:programPrivilegeIds)))" +
        "       )" +
        " union" +
        "   select distinct STRATEGY.PROGRAM_FK" +
        "   from STRATEGY" +
        "       inner join STRATEGY2DEPARTMENT S2D on STRATEGY.ID = S2D.STRATEGY_FK" +
        "       inner join PERSON P on S2D.DEPARTMENT_FK = P.DEPARTMENT_FK " +
        "   where p.ID = :userId " +
        "       AND (:anyProgramPrivilege OR S2D.PROGRAM_PRIVILEGE_FK in (:programPrivilegeIds))", nativeQuery = true)
    List<Integer> getProgramIdsByUserIdAndPrivilegeIds(@Param("userId") int userId,
                                                       @Param("anyProgramPrivilege") boolean anyProgramPrivilege,
                                                       @Param("programPrivilegeIds") List<Integer> programPrivilegeIds
    );

    @Query(value = "select distinct P2D.LOCATION_FK AS LOCATION_FK" +
        "   from PERSON P inner join PROGRAM2DEPARTMENT P2D on P2D.DEPARTMENT_FK = P.DEPARTMENT_FK" +
        "   where" +
        "       P.ID = :userId" +
        "       AND P2D.PROGRAM_FK = :programId" +
        " union" +
        "   select distinct P2P.LOCATION_FK AS LOCATION_FK" +
        "   from PROGRAM2PERSON P2P" +
        "   where" +
        "       P2P.PERSON_FK = :userId" +
        "       AND P2P.PROGRAM_FK = :programId", nativeQuery = true)
    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_LOCATION_IDS_BY_USER_ID, unless = "#result==null")
    List<Integer> getProgramLocationIdsByUserId(@Param("userId") int userId, @Param("programId") int programId);

//    @Query(value = "select distinct pp from Program p inner join p.persons p2p inner join p2p.privilege pp where p.id=:id and p2p.person.id=:personId")
//    List<ReferentialVO> getAllPrivilegesByUserId(@Param("id") int id, @Param("personId") int personId);
}
