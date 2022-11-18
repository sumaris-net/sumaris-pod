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

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.JobRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.model.technical.history.ProcessingHistory;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("jobService")
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {


    private final JobRepository jobRepository;

    private final ReferentialDao referentialDao;

    private final SumarisConfiguration configuration;

    private boolean enableTechnicalTablesUpdate = false;

    private final ApplicationContext applicationContext;


    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {

        // Update technical tables (if option changed)
        if (enableTechnicalTablesUpdate != configuration.enableTechnicalTablesUpdate()) {
            enableTechnicalTablesUpdate = configuration.enableTechnicalTablesUpdate();
            if (enableTechnicalTablesUpdate) {
                // Get self (by interface) to force transaction creation
                JobService self = applicationContext.getBean(JobService.class);

                // Insert missing processing status
                self.updateProcessingStatus();

                // Insert missing processing types
                self.updateProcessingTypes();
            }
        }
    }

    @Override
    public JobVO save(@NonNull JobVO source) {
        Preconditions.checkNotNull(source.getStatus());
        Preconditions.checkNotNull(source.getType());
        Preconditions.checkNotNull(source.getName());

        return jobRepository.save(source);
    }

    @Override
    public JobVO get(int id) {
        return findById(id)
            .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.persistence.error.entityNotFound", ProcessingHistory.class.getSimpleName(), id)));
    }

    @Override
    public Optional<JobVO> findById(int id) {
        return jobRepository.findById(id)
            .map(jobRepository::toVO);
    }

    @Override
    public List<JobVO> findAll(JobFilterVO filter) {
        return jobRepository.findAll(filter);
    }

    @Override
    public Page<JobVO> findAll(JobFilterVO filter, Pageable page) {
        return jobRepository.findAll(filter, page);
    }

    public boolean updateProcessingStatus() {
        try {
            String entityName = ProcessingStatus.class.getSimpleName();
            List<String> insertedLabels = Arrays.stream(ProcessingStatusEnum.values())
                    .map(ProcessingStatusEnum::getLabel)
                    // Filter if not exists
                    .filter(label -> referentialDao.countByFilter(
                            entityName,
                            ReferentialFilterVO.builder()
                                    .label(label)
                                    .build()) == 0)
                    // Transform to new VO
                    .map(label -> ReferentialVO.builder()
                            .label(label)
                            .name(label)
                            .statusId(StatusEnum.ENABLE.getId())
                            .entityName(entityName).build())
                    // Save VO
                    .map(referentialDao::save)
                    // Update the enum id
                    .map(vo -> {
                        ProcessingStatusEnum.byLabel(vo.getLabel()).setId(vo.getId());
                        return vo.getLabel();
                    })
                    .collect(Collectors.toList());
            if (!insertedLabels.isEmpty()) {
                log.info("Technical table PROCESSING_STATUS successfully updated. (inserts: {})", insertedLabels.size());
                log.debug(" - New processing status: {}", insertedLabels);
            }
            return true;
        } catch (Throwable t) {
            log.error("Error while initializing processing status: {}", t.getMessage(), t);
            return false;
        }
    }

    public boolean updateProcessingTypes() {
        try {
            String entityName = ProcessingType.class.getSimpleName();
            List<String> insertedLabels = Arrays.stream(ProcessingTypeEnum.values())
                .map(ProcessingTypeEnum::getLabel)
                // Filter if not exists
                .filter(label -> referentialDao.countByFilter(
                    entityName,
                    ReferentialFilterVO.builder()
                        .label(label)
                        .build()) == 0)
                // Transform to new VO
                .map(label -> ReferentialVO.builder()
                    .label(label)
                    .name(label)
                    .statusId(StatusEnum.ENABLE.getId())
                    .entityName(entityName).build())
                // Save VO
                .map(referentialDao::save)
                // Update the enum id
                .map(vo -> {
                    ProcessingTypeEnum.byLabel(vo.getLabel()).setId(vo.getId());
                    return vo.getLabel();
                })
                .collect(Collectors.toList());
            if (!insertedLabels.isEmpty()) {
                log.info("Technical table PROCESSING_TYPE successfully updated. (inserts: {})", insertedLabels.size());
                log.debug(" - New processing types: {}", insertedLabels);
            }
            return true;
        } catch (Throwable t) {
            log.error("Error while initializing processing type: {}", t.getMessage(), t);
            return false;
        }
    }
}
