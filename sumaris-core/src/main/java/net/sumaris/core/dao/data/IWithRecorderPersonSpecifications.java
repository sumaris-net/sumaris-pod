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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;

/**
 * @author peck7 on 28/08/2020.
 */
public interface IWithRecorderPersonSpecifications<ID extends Serializable, E extends IWithRecorderPersonEntity<ID, Person>> {

    String RECORDER_PERSON_ID_PARAM = "recorderPersonId";

    default Specification<E> hasRecorderPersonId(Integer recorderPersonId) {
        if (recorderPersonId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, RECORDER_PERSON_ID_PARAM);
            return cb.equal(root.get(E.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), param);
        }).addBind(RECORDER_PERSON_ID_PARAM, recorderPersonId);
    }
}
