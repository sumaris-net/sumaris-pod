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

package net.sumaris.server.service.data;


import net.sumaris.core.service.data.DenormalizedBatchService;
import net.sumaris.core.vo.data.batch.BatchVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;

import javax.annotation.Resource;

@Configuration
@ConditionalOnProperty(
        prefix = "sumaris.persistence",
        name = {"denormalizedBatch.enabled"}

        // TODO BLA Mettre à 'true' quand l'élévation sera finit
        //, matchIfMissing = true
)
public class DenormalizedBatchConfiguration {

    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(DenormalizedBatchConfiguration.class);

    @Resource
    private DenormalizedBatchService denormalizedBatchService;

    @JmsListener(destination = "updateBatch", containerFactory = "jmsListenerContainerFactory")
    public void onUpdateBatch(BatchVO batch) {

        BatchVO catchBatch = batch != null && batch.getParent() == null && batch.getParentId() == null ? batch : null;
        if (catchBatch == null) return; // Skip if not a catch batch

        if (catchBatch.getOperationId() != null) {
            denormalizedBatchService.saveAllByOperationId(catchBatch.getOperationId(), catchBatch);
        }
        else if (catchBatch.getSaleId() != null) {
            denormalizedBatchService.saveAllBySaleId(catchBatch.getSaleId(), catchBatch);
        }
        else {
            log.warn("Invalid catch batch update event: no parent found! Expected one of operation or sale.");
        }
    }
}
