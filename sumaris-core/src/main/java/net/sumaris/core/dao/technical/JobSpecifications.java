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

package net.sumaris.core.dao.technical;

import net.sumaris.core.dao.referential.IEntityWithJoinSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.ProcessingStatusEnum;
import net.sumaris.core.model.referential.ProcessingType;
import net.sumaris.core.model.referential.ProcessingTypeEnum;
import net.sumaris.core.model.technical.history.ProcessingHistory;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public interface JobSpecifications extends IEntityWithJoinSpecifications<Integer, ProcessingHistory> {

    default Specification<ProcessingHistory> hasIssuers(String... issuers) {
        if (issuers == null) return null;
        final List<String> cleanIssuers = Arrays.stream(issuers).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cleanIssuers)) return null;

        String paramName = ProcessingHistory.Fields.DATA_TRANSFERT_ADDRESS;

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, paramName);
            return cb.in(root.get(ProcessingHistory.Fields.DATA_TRANSFERT_ADDRESS)).value(parameter);
        }).addBind(paramName, cleanIssuers);
    }

    default Specification<ProcessingHistory> hasTypes(String... types) {
        if (ArrayUtils.isEmpty(types)) {
            types = Arrays.stream(ProcessingTypeEnum.values()).map(ProcessingTypeEnum::getLabel).toArray(String[]::new);
        }
        return hasInnerJoinValues(
            StringUtils.doting(ProcessingHistory.Fields.PROCESSING_TYPE, ProcessingType.Fields.LABEL),
            types);
    }

    default Specification<ProcessingHistory> hasJobStatus(JobStatusEnum... jobStatus) {
        if (ArrayUtils.isEmpty(jobStatus)) return null;
        List<Integer> statusIds = Arrays.stream(jobStatus)
            .map(JobStatusEnum::getProcessingStatus)
            .filter(Objects::nonNull)
            .map(ProcessingStatusEnum::getId)
            .filter(id -> id != null && id >= 0)
            .toList();
        if (CollectionUtils.isEmpty(statusIds)) return null;
        return hasInnerJoinIds(
            ProcessingHistory.Fields.PROCESSING_STATUS,
            statusIds.toArray(new Integer[0]));
    }

    default Specification<ProcessingHistory> updatedAfter(Date updateDate) {
        if (updateDate == null) return null;
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThan(
                root.get(ProcessingHistory.Fields.UPDATE_DATE),
                criteriaBuilder.literal(updateDate)
            );
    }

    default Specification<ProcessingHistory> startedBefore(Date startDate) {
        if (startDate == null) return null;
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.lessThan(
                root.get(ProcessingHistory.Fields.PROCESSING_DATE), // fixme: seems to be an end date, not a start date
                criteriaBuilder.literal(startDate)
            );
    }

    List<JobVO> findAll(JobFilterVO filter);

    List<JobVO> findAll(JobFilterVO filter, net.sumaris.core.dao.technical.Page page);

    Page<JobVO> findAll(JobFilterVO filter, Pageable pageable);
}
