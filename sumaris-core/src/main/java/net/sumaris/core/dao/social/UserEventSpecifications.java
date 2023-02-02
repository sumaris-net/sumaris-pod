package net.sumaris.core.dao.social;

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

import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.model.technical.history.ProcessingHistory;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.social.UserEventFetchOptions;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import javax.persistence.criteria.ParameterExpression;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface UserEventSpecifications {

    default Specification<UserEvent> toSpecification(UserEventFilterVO filter) {
        if (filter == null) return null;


        return BindableSpecification
            .where(inRecipients(filter.getRecipients()))
            .and(inIssuers(filter.getIssuers()))
            .and(inLevels(filter.getLevels()))
            .and(inTypes(filter.getTypes()))
            .and(creationDateAfter(filter.getStartDate()))
            .and(includedIds(filter.getIncludedIds()))
            .and(excludeRead(filter.isExcludeRead()))
            .and(hasSource(filter.getSource()))
            ;
    }

    default Specification<UserEvent> includedIds(Integer[] includedIds) {
        if (ArrayUtils.isEmpty(includedIds)) return null;
        return BindableSpecification.<UserEvent>where((root, query, criteriaBuilder) -> {
                ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, UserEvent.Fields.ID);
                return criteriaBuilder.in(root.get(UserEvent.Fields.ID)).value(param);
            })
            .addBind(UserEvent.Fields.ID, Arrays.asList(includedIds));
    }

    default Specification<UserEvent> inIssuers(String[] issuers) {
        return inPropertyValues(UserEvent.Fields.ISSUER, issuers);

    }

    default Specification<UserEvent> hasSource(String source) {
        if (StringUtils.isBlank(source)) return null;
        return BindableSpecification.<UserEvent>where((root, query, cb) -> {
                ParameterExpression<String> param = cb.parameter(String.class, UserEvent.Fields.SOURCE);
                return cb.equal(root.get(UserEvent.Fields.SOURCE), param);
            })
            .addBind(UserEvent.Fields.SOURCE, source);
    }

    default Specification<UserEvent> inRecipients(String[] recipients) {
        return inPropertyValues(UserEvent.Fields.RECIPIENT, recipients);
    }

    default Specification<UserEvent> inTypes(String[] types) {
        return inPropertyValues(UserEvent.Fields.TYPE, types);
    }

    default Specification<UserEvent> inLevels(String[] levels) {
        return inPropertyValues(UserEvent.Fields.LEVEL, levels);
    }

    default Specification<UserEvent> creationDateAfter(Date startDate) {
        if (startDate == null) return null;
        return (root, query, cb) -> cb.greaterThan(root.get(UserEvent.Fields.CREATION_DATE), startDate);
    }

    default Specification<UserEvent> excludeRead(boolean excludeRead) {
        if (!excludeRead) return null;
        return (root, query, cb) -> cb.isNull(root.get(UserEvent.Fields.READ_SIGNATURE));
    }

    default Specification<UserEvent> inPropertyValues(String propertyName, String[] values) {
        if (ArrayUtils.isEmpty(values)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, propertyName);
            return cb.in(Daos.composePath(root, propertyName))
                .value(param);
        }).addBind(propertyName, Arrays.asList(values));
    }

    long count(UserEventFilterVO filter);

    default List<UserEventVO> findAllVO(UserEventFilterVO filter,
                                        @Nullable net.sumaris.core.dao.technical.Page page) {
        return findAllVO(filter, page, null);
    }

    List<UserEventVO> findAllVO(UserEventFilterVO filter,
                                @Nullable net.sumaris.core.dao.technical.Page page,
                                @Nullable UserEventFetchOptions fetchOptions);


    Page<UserEventVO> findAllVO(@Nullable Specification<UserEvent> spec, @Nullable Pageable pageable);
    Page<UserEventVO> findAllVO(@Nullable Specification<UserEvent> spec, net.sumaris.core.dao.technical.Page page);

    Timestamp getDatabaseCurrentTimestamp();
}
