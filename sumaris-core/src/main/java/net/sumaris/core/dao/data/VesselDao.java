package net.sumaris.core.dao.data;

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

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.filter.VesselFilterVO;

import java.util.List;

public interface VesselDao {

    VesselVO get(int id);

    List<VesselVO> findByFilter(VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    Long countByFilter(VesselFilterVO filter);

    List<VesselFeaturesVO> getFeaturesByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection);

    List<VesselRegistrationPeriodVO> getRegistrationsByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection);

    VesselVO save(VesselVO vessel, boolean checkUpdateDate);

    void delete(int id);

}
