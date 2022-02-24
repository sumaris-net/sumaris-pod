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

package net.sumaris.core.event.data;


import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.JmsConfiguration;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.data.DenormalizedBatchService;
import net.sumaris.core.service.data.OperationService;
import net.sumaris.core.vo.data.batch.BatchVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@ConditionalOnProperty(
    name = "sumaris.persistence.denormalizedBatch.enabled",
    havingValue = "true"
)
@Slf4j
public class BatchEventListener {

    @Resource
    private DenormalizedBatchService denormalizedBatchService;

    public BatchEventListener() {
        log.info("Listening Batch save event, to execute denormalization on each changes");
    }

    @JmsListener(destination = "updateBatch", containerFactory = JmsConfiguration.CONTAINER_FACTORY_NAME)
    public void onUpdateBatch(BatchVO batch) {

        BatchVO catchBatch = batch != null && batch.getParent() == null && batch.getParentId() == null ? batch : null;
        if (catchBatch == null) return; // Skip if not a catch batch


        if (catchBatch.getOperationId() != null) {
            denormalizedBatchService.denormalizeAndSaveByOperationId(catchBatch.getOperationId(), null);
        }
        else if (catchBatch.getSaleId() != null) {
            // TODO: compute options
            denormalizedBatchService.denormalizeAndSaveBySaleId(catchBatch.getSaleId(), null);
        }
        else {
            log.warn("Invalid catch batch update event: no parent found! Expected one of operation or sale.");
        }
    }
}
