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
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.samplingScheme.SamplingStrata;
import net.sumaris.core.model.data.IWithSamplingStrataEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 2.10.0
 */
public interface IWithSamplingStrataSpecifications<ID extends Serializable, E extends IWithSamplingStrataEntity<ID, SamplingStrata>>
    extends IEntitySpecifications<ID, E> {

    String SAMPLING_STRATA_IDS_PARAM = "samplingStrataIds";

    default Specification<E> hasSamplingStrataIds(Integer[] samplingStrataIds) {
        if (ArrayUtils.isEmpty(samplingStrataIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, SAMPLING_STRATA_IDS_PARAM);
            return cb.in(root.get(IWithSamplingStrataEntity.Fields.SAMPLING_STRATA).get(IEntity.Fields.ID)).value(param);
        }).addBind(SAMPLING_STRATA_IDS_PARAM, Arrays.asList(samplingStrataIds));
    }
}
