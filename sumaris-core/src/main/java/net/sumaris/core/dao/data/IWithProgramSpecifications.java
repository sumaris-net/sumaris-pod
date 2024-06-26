package net.sumaris.core.dao.data;

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

import net.sumaris.core.dao.referential.IEntitySpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.data.IWithProgramEntity;
import net.sumaris.core.model.data.IWithVesselEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author peck7 on 28/08/2020.
 */
public interface IWithProgramSpecifications<ID extends Serializable, E extends IWithProgramEntity<ID, Program>>
        extends IEntitySpecifications<ID, E> {

    String PROGRAM_LABEL_PARAM = "programLabel";
    String PROGRAM_IDS_PARAM = "programIds";

    default Specification<E> hasProgramLabel(String programLabel) {
        if (programLabel == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<String> param = cb.parameter(String.class, PROGRAM_LABEL_PARAM);
            return cb.equal(root.get(E.Fields.PROGRAM).get(IItemReferentialEntity.Fields.LABEL), param);
        }).addBind(PROGRAM_LABEL_PARAM, programLabel);
    }

    default Specification<E> hasProgramIds(Integer[] programIds) {
        if (ArrayUtils.isEmpty(programIds)) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, PROGRAM_IDS_PARAM);
            return cb.in(root.get(E.Fields.PROGRAM).get(IItemReferentialEntity.Fields.ID)).value(param);
        }).addBind(PROGRAM_IDS_PARAM, Arrays.asList(programIds));
    }
}
