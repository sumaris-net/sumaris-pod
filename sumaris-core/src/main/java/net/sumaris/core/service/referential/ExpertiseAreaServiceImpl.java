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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ExpertiseAreaRepository;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.spatial.ExpertiseAreaFetchOptions;
import net.sumaris.core.vo.referential.spatial.ExpertiseAreaVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("expertiseAreaService")
@RequiredArgsConstructor
@Slf4j
public class ExpertiseAreaServiceImpl implements ExpertiseAreaService {

    protected final ExpertiseAreaRepository expertiseAreaRepository;


    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.EXPERTISE_AREAS_ENABLED)
    public List<ExpertiseAreaVO> findAllEnabled() {
        return expertiseAreaRepository.findAll(ReferentialFilterVO.builder()
            .statusIds(new Integer[]{ StatusEnum.ENABLE.getId() })
            .build(), ExpertiseAreaFetchOptions.FULL);
    }
}
