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
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
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

    String findIdsByUserIdQuery = "select distinct(PROGRAM.id) " +
            "           from PERSON P," +
            "                PROGRAM" +
            "                   left join PROGRAM2DEPARTMENT P2D on PROGRAM.ID = P2D.PROGRAM_FK" +
            "                   left join PROGRAM2PERSON P2P on PROGRAM.ID = P2P.PROGRAM_FK" +
            "           where P.ID = :id " +
            "               AND (P2D.DEPARTMENT_FK = P.DEPARTMENT_FK OR P2P.PERSON_FK = :id)" +
            "       union" +
            "           select distinct(PROGRAM_FK)" +
            "           from STRATEGY" +
            "               inner join STRATEGY2DEPARTMENT S2D on STRATEGY.ID = S2D.STRATEGY_FK" +
            "               inner join PERSON P on S2D.DEPARTMENT_FK = P.DEPARTMENT_FK" +
            "           where p.ID = :id";
    @Query(value = findIdsByUserIdQuery, nativeQuery = true)
    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_USER_ID, key="#p0", unless="#result==null")
    List<Integer> getProgramIdsByUserId(@Param("id") int id);

//    @Query(value = "select distinct pp from Program p inner join p.persons p2p inner join p2p.privilege pp where p.id=:id and p2p.person.id=:personId")
//    List<ReferentialVO> getAllPrivilegesByUserId(@Param("id") int id, @Param("personId") int personId);
}
