package net.sumaris.core.service.technical;

/*-
 * #%L
 * Quadrige3 Core :: Server API
 * %%
 * Copyright (C) 2017 - 2022 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import fr.ifremer.quadrige3.core.dao.BindableSpecification;
import fr.ifremer.quadrige3.core.dao.system.JobRepository;
import fr.ifremer.quadrige3.core.model.IEntity;
import fr.ifremer.quadrige3.core.model.administration.user.User;
import fr.ifremer.quadrige3.core.model.enumeration.JobStatusEnum;
import fr.ifremer.quadrige3.core.model.enumeration.JobTypeEnum;
import fr.ifremer.quadrige3.core.model.option.NoFetchOptions;
import fr.ifremer.quadrige3.core.model.option.SaveOptions;
import fr.ifremer.quadrige3.core.model.system.Job;
import fr.ifremer.quadrige3.core.service.AbstractEntityServiceImpl;
import fr.ifremer.quadrige3.core.service.referential.ReferentialSpecifications;
import fr.ifremer.quadrige3.core.util.Dates;
import fr.ifremer.quadrige3.core.util.StringUtils;
import fr.ifremer.quadrige3.core.vo.system.JobFilterVO;
import fr.ifremer.quadrige3.core.vo.system.JobVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JobServiceImpl
    extends AbstractEntityServiceImpl<Job, Integer, JobRepository, JobVO, JobFilterVO, NoFetchOptions, SaveOptions>
    implements JobService {

    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    public JobServiceImpl(EntityManager entityManager, JobRepository repository, ObjectMapper objectMapper) {
        super(entityManager, repository, Job.class, JobVO.class);
        this.objectMapper = objectMapper;
        xmlMapper = new XmlMapper();
        setEmitEvent(true);
    }

    @Override
    protected void toVO(Job source, JobVO target, NoFetchOptions fetchOptions) {
        super.toVO(source, target, fetchOptions);

        target.setType(JobTypeEnum.byId(source.getTypeName()));
        target.setStatus(JobStatusEnum.byId(source.getStatus()));
        target.setUserId(Optional.ofNullable(source.getUser()).map(User::getId).orElse(null));

        if (source.getConfiguration() != null) {
            try {
                JsonNode node = xmlMapper.readTree(source.getConfiguration().getBytes());
                target.setConfiguration(objectMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert XML to JSON", e);
                if (log.isDebugEnabled()) {
                    log.debug("XML to convert: {}", source.getConfiguration());
                }
            }
        }

        if (source.getReport() != null) {
            try {
                JsonNode node = xmlMapper.readTree(source.getReport().getBytes());
                target.setReport(objectMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert XML to JSON", e);
                if (log.isDebugEnabled()) {
                    log.debug("XML to deserialize: {}", source.getReport());
                }
            }
        }
    }

    @Override
    protected void toEntity(JobVO source, Job target, SaveOptions saveOptions) {
        super.toEntity(source, target, saveOptions);

        target.setTypeName(source.getType().getId());
        target.setStatus(source.getStatus().getId());
        target.setUser(Optional.ofNullable(source.getUserId()).map(id -> load(User.class, id)).orElse(null));

        if (source.getConfiguration() != null) {
            try {
                JsonNode node = objectMapper.readTree(source.getConfiguration().getBytes());
                target.setConfiguration(xmlMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert JSON to XML", e);
                if (log.isDebugEnabled()) {
                    log.debug("JSON to convert: {}", source.getConfiguration());
                }
            }
        }

        if (source.getReport() != null) {
            try {
                JsonNode node = objectMapper.readTree(source.getReport().getBytes());
                target.setReport(xmlMapper.writeValueAsString(node));
            } catch (IOException e) {
                log.error("Unable to convert JSON to XML", e);
                if (log.isDebugEnabled()) {
                    log.debug("JSON to convert: {}", source.getReport());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected BindableSpecification<Job> toSpecification(JobFilterVO filter) {
        if (filter == null)
            return null;

        BindableSpecification<Job> specification = BindableSpecification
            .where(ReferentialSpecifications.hasValue(StringUtils.doting(Job.Fields.USER, IEntity.Fields.ID), filter.getUserId()))
            .and(ReferentialSpecifications.withCollectionValues(
                Job.Fields.TYPE_NAME,
                CollectionUtils.emptyIfNull(filter.getTypes()).stream().map(JobTypeEnum::getId).collect(Collectors.toList())
            ))
            .and(ReferentialSpecifications.withCollectionValues(
                Job.Fields.STATUS,
                CollectionUtils.emptyIfNull(filter.getStatus()).stream().map(JobStatusEnum::getId).collect(Collectors.toList())
            ));

        if (filter.getLastUpdateDate() != null) {
            specification.and(Specification.where((root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(
                    root.get(Job.Fields.UPDATE_DATE),
                    criteriaBuilder.literal(Dates.addMilliseconds(filter.getLastUpdateDate(), 1))
                ))
            );
        }
        if (filter.getStartedBefore() != null) {
            specification.and(Specification.where((root, query, criteriaBuilder) -> criteriaBuilder.lessThan(
                    root.get(Job.Fields.START_DATE),
                    criteriaBuilder.literal(filter.getStartedBefore())
                ))
            );
        }

        return specification;
    }
}
