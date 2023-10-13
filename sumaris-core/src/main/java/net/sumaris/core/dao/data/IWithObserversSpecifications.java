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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * @author peck7 on 28/08/2020.
 */
public interface IWithObserversSpecifications<E extends IWithObserversEntity<Integer, Person>>
        extends IEntitySpecifications<Integer, E> {

    String OBSERVER_PERSON_IDS_PARAM = "observerPersonIds";


    default Specification<E> hasObserverPersonIds(Integer... observerPersonIds) {
        return hasObserverPersonIds(IWithObserversEntity.Fields.OBSERVERS, observerPersonIds);
    }

    default Specification<E> hasObserverPersonIds(String observersPath, Integer... observerPersonIds) {
        if (ArrayUtils.isEmpty(observerPersonIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {

            // Avoid duplicated entries (because of join)
            query.distinct(true);

            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, OBSERVER_PERSON_IDS_PARAM);
            return cb.in(Daos.composeJoin(root, observersPath, JoinType.INNER).get(IEntity.Fields.ID))
                .value(parameter);
        }).addBind(OBSERVER_PERSON_IDS_PARAM, Arrays.asList(observerPersonIds));
    }
}
