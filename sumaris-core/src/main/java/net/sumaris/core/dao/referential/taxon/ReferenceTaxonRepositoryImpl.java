package net.sumaris.core.dao.referential.taxon;

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

import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.referential.ReferenceTaxonVO;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.util.Optional;

public class ReferenceTaxonRepositoryImpl<O extends IFetchOptions>
        extends SumarisJpaRepositoryImpl<ReferenceTaxon, Integer, ReferenceTaxonVO>
        implements ReferenceTaxonRepository {

    @Autowired
    ReferenceTaxonRepositoryImpl(EntityManager entityManager) {
        super(ReferenceTaxon.class, ReferenceTaxonVO.class, entityManager);
    }

    @Override
    public ReferenceTaxonVO get(Integer id) {
        return toVO(this.getById(id));
    }

    @Override
    public Optional<ReferenceTaxonVO> findVOById(Integer id) {
        return findById(id, null);
    }

    public Optional<ReferenceTaxonVO> findById(Integer id, O fetchOptions) {
        return super.findById(id).map(entity -> toVO(entity, fetchOptions));
    }

    protected ReferenceTaxonVO toVO(ReferenceTaxon source, O fetchOptions) {
        if (source == null) return null;
        ReferenceTaxonVO target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    protected void toVO(ReferenceTaxon source, ReferenceTaxonVO target, O fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target);
    }
}
