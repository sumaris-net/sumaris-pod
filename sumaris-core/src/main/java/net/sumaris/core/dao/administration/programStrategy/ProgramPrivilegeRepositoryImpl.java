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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilege;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.cache.annotation.Cacheable;

import javax.persistence.EntityManager;

/**
 * @author blavenie
 */
@Slf4j
public class ProgramPrivilegeRepositoryImpl
    extends ReferentialRepositoryImpl<ProgramPrivilege, ReferentialVO, ReferentialFilterVO, ReferentialFetchOptions> {

    public ProgramPrivilegeRepositoryImpl(EntityManager entityManager) {
        super(ProgramPrivilege.class, ReferentialVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_PRIVILEGE_BY_ID, unless = "#result==null")
    public ReferentialVO get(int id) {
        return super.get(id);
    }

}
