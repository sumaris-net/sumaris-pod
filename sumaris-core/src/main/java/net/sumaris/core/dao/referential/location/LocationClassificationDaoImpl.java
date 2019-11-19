package net.sumaris.core.dao.referential.location;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.LocationClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

@Repository("locationClassificationDao")
public class LocationClassificationDaoImpl extends HibernateDaoSupport implements LocationClassificationDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(LocationClassificationDaoImpl.class);

    @Override
    public LocationClassification create(LocationClassification classification) {
        Preconditions.checkNotNull(classification);
        //Preconditions.checkNotNull(classification.getId());
        Preconditions.checkNotNull(classification.getLabel());
        Preconditions.checkNotNull(classification.getName());

        // Default value
        if (classification.getStatus() == null) {
            classification.setStatus(load(Status.class, StatusEnum.ENABLE.getId()));
        }

        getEntityManager().persist(classification);

        return classification;
    }

    @Override
    public LocationClassification findByLabel(final String label) {

        try {
            return getByLabel(label);
        }
        catch(NoResultException e) {
            return null;
        }
        catch(Exception e) {
            return null;
        }
    }

    @Override
    public LocationClassification getByLabel(final String label) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<LocationClassification> query = builder.createQuery(LocationClassification.class);
        Root<LocationClassification> root = query.from(LocationClassification.class);

        ParameterExpression<String> labelParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(LocationClassification.Fields.LABEL), labelParam));

        TypedQuery<LocationClassification> q = getEntityManager().createQuery(query)
                .setParameter(labelParam, label);
        return q.getSingleResult();
    }

    /* -- protected methods -- */

}
