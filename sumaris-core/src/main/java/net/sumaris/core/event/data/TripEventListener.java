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
import net.sumaris.core.event.JmsEntityEvents;
import net.sumaris.core.vo.data.TripVO;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TripEventListener {

    @JmsListener(destination = JmsEntityEvents.DESTINATION,
        selector = "operation = 'insert' AND entityName = 'Trip'",
        containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    public void onInsertTrip(TripVO entity) {
        log.info("New trip {id: {}, recorderPerson: {id: {}}}",  entity.getId(), entity.getRecorderPerson().getId());
        // TODO send event for supervisor
    }

    @JmsListener(destination = JmsEntityEvents.DESTINATION,
        selector = "operation = 'update' AND entityName = 'Trip'",
        containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    public void onUpdateTrip(TripVO entity) {
        log.info("Updated trip {id: {}, recorderPerson: {id: {}}}",  entity.getId(), entity.getRecorderPerson().getId());
        // TODO send event for supervisor

    }
}
