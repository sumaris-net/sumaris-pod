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

package net.sumaris.core.dao.referential.conversion;

import lombok.NonNull;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.conversion.RoundWeightConversion;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

public class RoundWeightConversionRepositoryImpl extends SumarisJpaRepositoryImpl<RoundWeightConversion, Integer, RoundWeightConversionVO>
    implements RoundWeightConversionRepository {

    private final LocationRepository locationRepository;

    @Autowired
    public RoundWeightConversionRepositoryImpl(EntityManager entityManager,
                                                  LocationRepository locationRepository) {
        super(RoundWeightConversion.class, RoundWeightConversionVO.class, entityManager);
        this.locationRepository = locationRepository;
    }

    @Override
    public List<RoundWeightConversionVO> findAll(RoundWeightConversionFilterVO filter, Page page, RoundWeightConversionFetchOptions fetchOptions) {
        Specification<RoundWeightConversion> spec = filter != null ? toSpecification(filter) : null;
        TypedQuery<RoundWeightConversion> query = getQuery(spec, page, RoundWeightConversion.class);
        return streamQuery(query)
            .map(source -> this.toVO(source, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public long count(RoundWeightConversionFilterVO filter) {
        Specification<RoundWeightConversion> spec = filter != null ? toSpecification(filter) : null;
        return getCountQuery(spec, RoundWeightConversion.class).getSingleResult();
    }

    @Override
    public void toVO(RoundWeightConversion source, RoundWeightConversionVO target, boolean copyIfNull) {
        toVO(source, target, RoundWeightConversionFetchOptions.DEFAULT, copyIfNull);
    }

    protected RoundWeightConversionVO toVO(RoundWeightConversion source, RoundWeightConversionFetchOptions fetchOptions) {
        if (source == null) return null;
        RoundWeightConversionVO target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    protected void toVO(RoundWeightConversion source, RoundWeightConversionVO target, RoundWeightConversionFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        fetchOptions = fetchOptions == null ? RoundWeightConversionFetchOptions.DEFAULT : fetchOptions;

        // Taxon Group
        if (source.getTaxonGroup() != null || copyIfNull) {
            if (source.getTaxonGroup() == null) {
                target.setTaxonGroupId(null);
            }
            else {
                target.setTaxonGroupId(source.getTaxonGroup().getId());
            }
        }

        // Location
        Integer locationId = source.getLocation() != null ? source.getLocation().getId() : null;
        if (locationId != null || copyIfNull) {
            if (locationId == null) {
                target.setLocationId(null);
                target.setLocation(null);
            }
            else {
                target.setLocationId(locationId);
                if (fetchOptions.isWithLocation()) {
                    // Use get(), to use cache
                    target.setLocation(locationRepository.get(locationId));
                }
            }
        }

        // Dressing
        if (source.getDressing() != null || copyIfNull) {
            if (source.getDressing() == null) {
                target.setDressingId(null);
            }
            else {
                target.setDressingId(source.getDressing().getId());
            }
        }

        // Preserving
        if (source.getPreserving() != null || copyIfNull) {
            if (source.getPreserving() == null) {
                target.setPreservingId(null);
            }
            else {
                target.setPreservingId(source.getPreserving().getId());
            }
        }

        // Status
        if (source.getStatus() != null || copyIfNull) {
            if (source.getStatus() == null) {
                target.setStatusId(null);
            }
            else {
                target.setStatusId(source.getStatus().getId());
            }
        }
    }

    @Override
    public void toEntity(RoundWeightConversionVO source, RoundWeightConversion target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Taxon Group
        Integer taxonGroupId = source.getTaxonGroup() != null ? source.getTaxonGroup().getId() : source.getTaxonGroupId();
        if (taxonGroupId != null || copyIfNull) {
            if (taxonGroupId == null) target.setTaxonGroup(null);
            else target.setTaxonGroup(getReference(TaxonGroup.class, taxonGroupId));
        }

        // Location
        Integer locationId = source.getLocation() != null ? source.getLocation().getId() : source.getLocationId();
        if (locationId != null || copyIfNull) {
            if (locationId == null) target.setLocation(null);
            else target.setLocation(getReference(Location.class, locationId));
        }

        // Dressing
        Integer dressingId = source.getDressing() != null ? source.getDressing().getId() : source.getDressingId();
        if (dressingId != null || copyIfNull) {
            if (dressingId == null) target.setDressing(null);
            else target.setDressing(getReference(QualitativeValue.class, dressingId));
        }

        // Preserving
        Integer preservingId = source.getPreserving() != null ? source.getPreserving().getId() : source.getPreservingId();
        if (preservingId != null || copyIfNull) {
            if (preservingId == null) target.setPreserving(null);
            else target.setPreserving(getReference(QualitativeValue.class, preservingId));
        }

        // Status
        Integer statusId = source.getStatusId();
        if (statusId != null || copyIfNull) {
            if (statusId == null) target.setStatus(null);
            else target.setStatus(getReference(Status.class, statusId));
        }

    }

    protected Specification<RoundWeightConversion> toSpecification(@NonNull RoundWeightConversionFilterVO filter) {
        return BindableSpecification
            .where(inStatusIds(filter.getStatusIds()))
            .and(hasTaxonGroupIds(filter.getTaxonGroupIds()))
            .and(hasLocationIds(filter.getLocationIds()))
            .and(hasDressingIds(filter.getDressingIds()))
            .and(hasPreservingIds(filter.getPreservingIds()))
            .and(atDate(filter.getDate()))
            ;
    }
}
