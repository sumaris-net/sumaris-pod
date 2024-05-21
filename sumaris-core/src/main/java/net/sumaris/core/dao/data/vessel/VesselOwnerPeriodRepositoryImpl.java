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

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.data.*;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.VesselOwnerPeriodVO;
import net.sumaris.core.vo.filter.VesselOwnerFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class VesselOwnerPeriodRepositoryImpl
    extends SumarisJpaRepositoryImpl<VesselOwnerPeriod, VesselOwnerPeriodId, VesselOwnerPeriodVO>
    implements VesselOwnerPeriodSpecifications {

    private final VesselOwnerRepository vesselOwnerRepository;

    @Autowired
    public VesselOwnerPeriodRepositoryImpl(EntityManager entityManager,
                                           VesselOwnerRepository vesselOwnerRepository) {
        super(VesselOwnerPeriod.class, VesselOwnerPeriodVO.class, entityManager);
        this.vesselOwnerRepository = vesselOwnerRepository;
    }

    @Override
    public Specification<VesselOwnerPeriod> toSpecification(VesselOwnerFilterVO filter) {
        return BindableSpecification.where(vesselId(filter.getVesselId()))
                .and(vesselOwnerId(filter.getVesselOwnerId()))
                .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
                // Program
                .and(hasProgramLabel(filter.getProgramLabel()))
                .and(hasProgramIds(filter.getProgramIds()));
    }

    @Override
    public List<VesselOwnerPeriodVO> findAll(VesselOwnerFilterVO filter, Page page) {

        TypedQuery<VesselOwnerPeriod> query = getQuery(toSpecification(filter), page, VesselOwnerPeriod.class);

        try (Stream<VesselOwnerPeriod> stream = streamQuery(query)) {
            return stream.map(this::toVO).toList();
        }
    }

    public Optional<VesselOwnerPeriodVO> findLastByVesselId(int vesselId) {
        return findByVesselIdAndDate(vesselId, null).map(this::toVO);
    }

    @Override
    public Optional<VesselOwnerPeriod> findByVesselIdAndDate(int vesselId, Date date) {

        Specification<VesselOwnerPeriod> specification = vesselId(vesselId).and(atDate(date));
        TypedQuery<VesselOwnerPeriod> query = getQuery(specification, 0, 1, StringUtils.doting(VesselOwnerPeriod.Fields.ID, VesselOwnerPeriodId.Fields.START_DATE), SortDirection.DESC, VesselOwnerPeriod.class);
        try (Stream<VesselOwnerPeriod> stream = query.getResultStream()) {
            Optional<VesselOwnerPeriod> result = stream.findFirst();

            // Nothing found: retry without a date, if not already the case
            if (result.isEmpty() && date != null) {
                return findByVesselIdAndDate(vesselId, null);
            }

            return result;
        }
    }

    @Override
    public void toVO(VesselOwnerPeriod source, VesselOwnerPeriodVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Vessel
        if (source.getId() != null || copyIfNull) {
            if (source.getId() == null) {
                target.setId(null);
            }
            else {
                target.setId(source.getId().copy());
            }
        }

        // Vessel owner
        if (source.getVesselOwner() != null) {
            target.setVesselOwner(vesselOwnerRepository.toVO(source.getVesselOwner()));
        }
    }

    @Override
    public void toEntity(VesselOwnerPeriodVO source, VesselOwnerPeriod target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Vessel
        Integer vesselId = source.getVesselId() != null ? source.getVesselId() : (source.getVessel() != null ? source.getVessel().getId() : null);
        if (copyIfNull || vesselId != null) {
            if (vesselId == null ) {
                target.setVessel(null);
            } else {
                target.setVessel(getReference(Vessel.class, vesselId));
            }
        }

        // Vessel Owner
        Integer vesselOwnerId = source.getVesselOwnerId() != null ? source.getVesselOwnerId() : (source.getVesselOwner() != null ? source.getVesselOwner().getId() : null);
        if (copyIfNull || vesselOwnerId != null) {
            if (vesselOwnerId == null) {
                target.setVesselOwner(null);
            } else {
                target.setVesselOwner(getReference(VesselOwner.class, vesselOwnerId));
            }
        }
    }

    @Override
    protected List<Expression<?>> toSortExpressions(CriteriaQuery<?> query, Root<VesselOwnerPeriod> root, CriteriaBuilder cb, String property) {

        // Rename sort on 'startDate' into 'id.startDate'
        if (property == null || property.endsWith(VesselOwnerPeriod.Fields.START_DATE)) {
            property = StringUtils.doting(VesselOwnerPeriod.Fields.ID, VesselOwnerPeriod.Fields.START_DATE);
        }

        return super.toSortExpressions(query, root, cb, property);
    }
}
