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

package net.sumaris.cli.action.data;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.service.data.denormalize.DenormalizeTripService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.TripFilterVO;

@Slf4j
public class DenormalizeTripsAction {

    /**
     * Denormalize trips
     */
    public void run() {
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        DenormalizeTripService tripService = ServiceLocator.instance().getService("denormalizeTripService", DenormalizeTripService.class);

        // Create filter
        TripFilterVO.TripFilterVOBuilder filterBuilder = TripFilterVO.builder()
            .includedIds(config.getCliFilterTripIds().toArray(Integer[]::new))
            .operationIds(config.getCliFilterOperationIds().toArray(Integer[]::new))
            .programLabel(config.getCliFilterProgramLabel());

        Integer year = config.getCliFilterYear();
        if (year != null && year > 1970) {
            filterBuilder.startDate(Dates.getFirstDayOfYear(year))
                .endDate(Dates.getLastSecondOfYear(year));
        }

        // Execute job
        ProgressionModel progression = new ProgressionModel();
        progression.addPropertyChangeListener(IProgressionModel.Fields.MESSAGE, (event) -> log.info(progression.getMessage()));
        tripService.denormalizeByFilter(filterBuilder.build(), progression);

    }
}
