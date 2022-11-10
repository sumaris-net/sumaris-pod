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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.history.ProcessingHistoryRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.referential.ProcessingType;
import net.sumaris.core.model.referential.ProcessingTypeEnum;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {


    private final ProcessingHistoryRepository processingHistoryRepository;

    private final ReferentialDao referentialDao;

    private final SumarisConfiguration configuration;

    private boolean enableTechnicalTablesUpdate = false;


    @PostConstruct
    protected void init() {

        // Update technical tables (if option changed)
        if (this.enableTechnicalTablesUpdate != configuration.enableTechnicalTablesUpdate()) {
            this.enableTechnicalTablesUpdate = configuration.enableTechnicalTablesUpdate();

            // Insert missing processing types
            if (this.enableTechnicalTablesUpdate) initProcessingTypes();
        }
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        init();
    }

    @Override
    public JobVO save(@NonNull JobVO source) {
        Preconditions.checkNotNull(source.getStatus());
        Preconditions.checkNotNull(source.getType());
        Preconditions.checkNotNull(source.getName());

        return processingHistoryRepository.save(source);
    }

    @Override
    public JobVO get(int id) {
        return processingHistoryRepository.toVO(processingHistoryRepository.getById(id));
    }

    @Override
    public Optional<JobVO> findById(int id) {
        return processingHistoryRepository.findById(id)
            .map(processingHistoryRepository::toVO);
    }

    @Override
    public List<JobVO> findAll(JobFilterVO filter) {
        return processingHistoryRepository.findAll(filter);
    }

    @Override
    public Page<JobVO> findAll(JobFilterVO filter, Pageable page) {
        return processingHistoryRepository.findAll(filter, page);
    }

    @Transactional
    protected boolean initProcessingTypes() {
        try {
            String entityName = ProcessingType.class.getSimpleName();
            List<String> newTypeLabels = Arrays.stream(ProcessingTypeEnum.values())
                .map(ProcessingTypeEnum::getLabel)
                // Filter if not exists
                .filter(label -> referentialDao.countByFilter(
                    ProcessingType.class.getSimpleName(),
                    ReferentialFilterVO.builder()
                        .label(label)
                        .build()) == 0)
                // Transform to new VO
                .map(label -> ReferentialVO.builder().label(label).entityName(entityName).build())
                // Save VO
                .map(referentialDao::save)
                // Update the enum id
                .map(vo -> {
                    ProcessingTypeEnum.byLabel(vo.getLabel()).setId(vo.getId());
                    return vo.getLabel();
                })
                .collect(Collectors.toList());
            if (!newTypeLabels.isEmpty()) {
                log.info("Adding {} processing types: {}", newTypeLabels.size(), newTypeLabels);
            }
            return true;
        } catch (Throwable t) {
            log.error("Error while initializing processing type: {}", t.getMessage(), t);
            return false;
        }
    }
}
