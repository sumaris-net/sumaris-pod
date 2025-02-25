package net.sumaris.core.service.data.denormalize;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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


import lombok.NonNull;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author BLA
 * 
 *    Service in charge of trip data
 * 
 */
@Transactional
public interface DenormalizedTripService {

	@Transactional(propagation = Propagation.SUPPORTS)
	DenormalizedTripResultVO denormalizeByFilter(@NonNull TripFilterVO filter);

	@Transactional(propagation = Propagation.SUPPORTS)
	DenormalizedTripResultVO denormalizeByFilter(TripFilterVO filter, IProgressionModel progression);

	@Transactional
	DenormalizedTripResultVO denormalizeById(int tripId);
}
