package net.sumaris.core.dao.referential.gradient;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.model.referential.gradient.NearbySpecificArea;
import net.sumaris.core.model.referential.location.LocationHierarchyMode;
import net.sumaris.core.model.referential.spatial.SpatialItemTypeEnum;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro> on 18/20/2024
 */
@Slf4j
public class NearbySpecificAreaRepositoryImpl
    extends ReferentialRepositoryImpl<Integer, NearbySpecificArea, ReferentialVO, ReferentialFilterVO, ReferentialFetchOptions>
    implements ReferentialSpecifications<Integer, NearbySpecificArea> {

    public NearbySpecificAreaRepositoryImpl(EntityManager entityManager) {
        super(NearbySpecificArea.class, ReferentialVO.class, entityManager);
    }

    @Override
    protected Specification<NearbySpecificArea> toSpecification(
        @NonNull ReferentialFilterVO filter, ReferentialFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(inSpatialLocationIds(SpatialItemTypeEnum.NEARBY_SPECIFIC_AREA,
                LocationHierarchyMode.NONE,
                filter.getLocationIds()))
            ;
    }

}
