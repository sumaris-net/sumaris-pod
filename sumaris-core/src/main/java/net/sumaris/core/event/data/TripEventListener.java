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
import net.sumaris.core.event.entity.EntityEventService;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.TripVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean({EntityEventService.class})
@Slf4j
public class TripEventListener {

    public TripEventListener(EntityEventService entityEventService) {

        entityEventService.registerListener(new EntityEventService.Listener() {
            @Override
            public void onUpdate(EntityUpdateEvent event) {
                Object data = event.getData();
                if (data instanceof TripVO) {
                    onUpdateTrip((TripVO)data);
                }
            }

            @Override
            public void onInsert(EntityInsertEvent event) {
                Object data = event.getData();
                if (data instanceof TripVO) {
                    onInsertTrip((TripVO) data);
                }
            }
        }, Trip.class);
    }

    public void onInsertTrip(TripVO entity) {
        log.info("New Trip#{} {recorderPerson: {id: {}}}",  entity.getId(), entity.getRecorderPerson().getId());
        // TODO send event for supervisor
    }

    public void onUpdateTrip(TripVO entity) {
        log.info("Update Trip#{} {updateDate: '{}', recorderPerson: {id: {}}}",  entity.getId(),
            Dates.toISODateTimeString(entity.getUpdateDate()),
            entity.getRecorderPerson().getId());
        // TODO send event for supervisor
    }
}
