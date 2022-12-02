package net.sumaris.core.service.technical;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2022 SUMARiS Consortium
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



import io.leangen.graphql.annotations.GraphQLArgument;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface JobService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean updateProcessingStatus();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean updateProcessingTypes();

    JobVO save(JobVO source);

    @Transactional(readOnly = true)
    JobVO get(int id);

    @Transactional(readOnly = true)
    Optional<JobVO> findById(int id);

    @Transactional(readOnly = true)
    List<JobVO> findAll(@GraphQLArgument(name = "filter") JobFilterVO filter);

    @Transactional(readOnly = true)
    Page<JobVO> findAll(@GraphQLArgument(name = "filter") JobFilterVO filter, Pageable page);
}
