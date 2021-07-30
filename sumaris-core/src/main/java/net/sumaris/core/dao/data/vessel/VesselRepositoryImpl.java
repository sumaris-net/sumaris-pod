package net.sumaris.core.dao.data.vessel;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;

@Slf4j
public class VesselRepositoryImpl
    extends RootDataRepositoryImpl<Vessel, VesselVO, VesselFilterVO, DataFetchOptions>
    implements VesselSpecifications {

    @Autowired
    public VesselRepositoryImpl(EntityManager entityManager) {
        super(Vessel.class, VesselVO.class, entityManager);
    }

    @Override
    public Specification<Vessel> toSpecification(VesselFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(id(filter.getVesselId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(vesselFeatures(filter.getVesselFeaturesId()))
            .and(statusIds(filter.getStatusIds()))
            .and(searchText(filter.getSearchAttributes(), filter.getSearchText()));
    }

    @Override
    public void toVO(Vessel source, VesselVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);
    }

    @Override
    public void toEntity(VesselVO source, Vessel target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);
    }

    @Override
    protected void onAfterSaveEntity(VesselVO vo, Vessel savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
    }
}
