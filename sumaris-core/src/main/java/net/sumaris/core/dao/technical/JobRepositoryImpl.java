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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.annotation.EntityEnums;
import net.sumaris.core.model.referential.ProcessingStatus;
import net.sumaris.core.model.referential.ProcessingStatusEnum;
import net.sumaris.core.model.referential.ProcessingType;
import net.sumaris.core.model.referential.ProcessingTypeEnum;
import net.sumaris.core.model.technical.history.ProcessingHistory;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author peck7 on 21/08/2020.
 */
@Slf4j
public class JobRepositoryImpl
    extends SumarisJpaRepositoryImpl<ProcessingHistory, Integer, JobVO>
    implements JobSpecifications {

    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    protected JobRepositoryImpl(EntityManager entityManager,
                                ObjectMapper objectMapper) {
        super(ProcessingHistory.class, JobVO.class, entityManager);
        this.objectMapper = objectMapper;
        xmlMapper = new XmlMapper();
        setLockForUpdate(false);
        setCheckUpdateDate(false);
    }

    @Override
    public List<JobVO> findAll(JobFilterVO filter) {
        try (Stream<ProcessingHistory> stream = super.streamAll(toSpecification(filter))) {
            return stream.map(this::toVO).toList();
        }
    }

    @Override
    public List<JobVO> findAll(JobFilterVO filter, net.sumaris.core.dao.technical.Page page) {
        TypedQuery<ProcessingHistory> query = getQuery(toSpecification(filter), page, ProcessingHistory.class);

        try (Stream<ProcessingHistory> stream = streamQuery(query)) {
            return stream.map(this::toVO).toList();
        }
    }

    @Override
    public Page<JobVO> findAll(JobFilterVO filter, Pageable pageable) {
        return super.findAll(toSpecification(filter), pageable)
            .map(this::toVO);
    }

    protected Specification<ProcessingHistory> toSpecification(JobFilterVO filter) {
        return BindableSpecification
            .where(id(filter.getId(), Integer.class))
            .and(hasIssuers(filter.getIssuer(), filter.getIssuerEmail()))
            .and(hasTypes(filter.getTypes()))
            .and(hasJobStatus(filter.getStatus()))
            .and(includedIds(filter.getIncludedIds()))
            .and(excludedIds(filter.getExcludedIds()))
            .and(updatedAfter(filter.getLastUpdateDate()))
            .and(startedBefore(filter.getStartedBefore()))
        ;
    }

    @Override
    public void toVO(ProcessingHistory source, JobVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        target.setIssuer(source.getDataTransfertAddress());

        // Status
        ProcessingStatusEnum statusEnum = ProcessingStatusEnum.valueOf(source.getProcessingStatus().getId());
        JobStatusEnum targetStatus = JobStatusEnum.byProcessingStatus(statusEnum);
        target.setStatus(targetStatus);

        // Type - we map only well known types (and replace others with UNKNOWN label)
        ProcessingTypeEnum typeEnum = ProcessingTypeEnum.byId(source.getProcessingType().getId())
            .orElse(ProcessingTypeEnum.UNKNOWN);
        target.setType(typeEnum.getLabel());

        // Start date
        target.setStartDate(Dates.min(source.getUpdateDate(), source.getProcessingDate()));

        // End date
        if (ProcessingStatusEnum.isFinished(statusEnum)) {
            target.setEndDate(Dates.max(source.getUpdateDate(), source.getProcessingDate()));
        }
        else {
            target.setEndDate(null); // Running or pending = no end date
        }

        // Configuration
        if (StringUtils.isNotBlank(source.getXmlConfiguration())) {
            try {
                JsonNode node = xmlMapper.readTree(source.getXmlConfiguration().getBytes());
                target.setConfiguration(objectMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert XML to JSON", e);
                if (log.isDebugEnabled()) {
                    log.debug("XML to convert: {}", source.getConfiguration());
                }
            }
        }
        else if (StringUtils.isNotBlank(source.getConfiguration())) {
            target.setConfiguration(source.getConfiguration());
        }

        // Report
        if (StringUtils.isNotBlank(source.getXmlReport())) {
            try {
                JsonNode node = xmlMapper.readTree(source.getXmlReport().getBytes());
                target.setReport(objectMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert XML to JSON", e);
                if (log.isDebugEnabled()) {
                    log.debug("XML to deserialize: {}", source.getXmlReport());
                }
            }
        }
    }

    @Override
    public JobVO save(@NonNull JobVO source) {

        // Call the inherited method
        JobVO result = super.save(source);

        return result;
    }

    @Override
    public void toEntity(JobVO source, ProcessingHistory target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Issuer
        target.setDataTransfertAddress(source.getIssuer());

        // Type
        Integer processingTypeId = ProcessingTypeEnum.byLabelOrName(source.getType())
            .filter(EntityEnums::isResolved) // Skip if unresolved
            .map(ProcessingTypeEnum::getId)
            .orElseGet(() -> {
                if (target.getProcessingType() != null && target.getProcessingType().getId() >= 0) {
                    return target.getProcessingType().getId(); // Keep existing
                }
                return ProcessingTypeEnum.UNKNOWN.getId();
            });
        if (EntityEnums.isUnresolvedId(processingTypeId)) {
            throw new DataNotFoundException("Cannot resolved ProcessingType from job type: " + source.getType());
        }
        target.setProcessingType(getReference(ProcessingType.class, processingTypeId));

        // Status
        ProcessingStatusEnum targetStatus = source.getStatus().getProcessingStatus();
        if (targetStatus == null || targetStatus.getId() == -1 /*not resolved*/) {
            targetStatus = ProcessingStatusEnum.WAITING_EXECUTION;
        }
        target.setProcessingStatus(getReference(ProcessingStatus.class, targetStatus.getId()));

        // Job is finished: fill processing date with end date
        if (ProcessingStatusEnum.isFinished(targetStatus)) {
            Date processingDate = source.getEndDate();
            if (processingDate == null) processingDate = getDatabaseCurrentDate();
            target.setProcessingDate(processingDate);
        }
        // Job is pending or running: fill processing date with start date
        else {
            Date processingDate = source.getStartDate();
            if (processingDate == null) processingDate = this.getDatabaseCurrentTimestamp();
            target.setProcessingDate(processingDate);
        }

        // Configuration
        if (StringUtils.isNotBlank(source.getConfiguration())) {
            try {
                JsonNode node = objectMapper.readTree(source.getConfiguration().getBytes());
                target.setXmlConfiguration(xmlMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert JSON to XML", e);
                if (log.isDebugEnabled()) {
                    log.debug("JSON to convert: {}", source.getConfiguration());
                }
            }
        }

        // Report
        if (StringUtils.isNotBlank(source.getReport())) {
            try {
                JsonNode node = objectMapper.readTree(source.getConfiguration().getBytes());
                target.setXmlReport(xmlMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert JSON to XML", e);
                if (log.isDebugEnabled()) {
                    log.debug("JSON to deserialize: {}", source.getReport());
                }
            }
        }
    }

    @Override
    public void deleteById(Integer id) {
        super.deleteById(id);
    }

}
