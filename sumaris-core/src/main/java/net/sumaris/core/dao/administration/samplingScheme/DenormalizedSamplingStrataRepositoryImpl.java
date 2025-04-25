/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.administration.samplingScheme;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.administration.samplingScheme.DenormalizedSamplingStrata;
import net.sumaris.core.vo.administration.samplingScheme.DenormalizedSamplingStrataFetchOptions;
import net.sumaris.core.vo.administration.samplingScheme.DenormalizedSamplingStrataVO;
import net.sumaris.core.vo.administration.samplingScheme.SamplingStrataFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;

@Slf4j
public class DenormalizedSamplingStrataRepositoryImpl
    extends ReferentialRepositoryImpl<Integer, DenormalizedSamplingStrata, DenormalizedSamplingStrataVO, SamplingStrataFilterVO, DenormalizedSamplingStrataFetchOptions>
    implements DenormalizedSamplingStrataSpecifications {


    public DenormalizedSamplingStrataRepositoryImpl(EntityManager entityManager) {
        super(DenormalizedSamplingStrata.class,
            DenormalizedSamplingStrataVO.class,
            entityManager);
    }

    @Override
    protected Specification<DenormalizedSamplingStrata> toSpecification(@NonNull SamplingStrataFilterVO filter, DenormalizedSamplingStrataFetchOptions fetchOptions) {
        Specification<DenormalizedSamplingStrata> specification = super.toSpecification(filter, fetchOptions);
        if (filter.getId() != null) return specification;
        return specification
            .and(programId(filter.getProgramId()))
            .and(programLabel(filter.getProgramLabel()))
            ;
    }

    @Override
    public List<DenormalizedSamplingStrataVO> findAll(SamplingStrataFilterVO filter, Page page, DenormalizedSamplingStrataFetchOptions fetchOptions) {
        return super.findAll(filter, page, fetchOptions);
    }
}