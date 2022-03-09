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
import net.sumaris.core.model.referential.conversion.WeightLengthConversion;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

public class WeightLengthConversionRepositoryImpl extends SumarisJpaRepositoryImpl<WeightLengthConversion, Integer, WeightLengthConversionVO>
    implements WeightLengthConversionRepository {

    @Resource
    private LocationRepository locationRepository;

    protected WeightLengthConversionRepositoryImpl(EntityManager entityManager) {
        super(WeightLengthConversion.class, WeightLengthConversionVO.class, entityManager);
    }

    @Override
    public List<WeightLengthConversionVO> findAll(WeightLengthConversionFilterVO filter, Page page, WeightLengthConversionFetchOptions fetchOptions) {
        Specification<WeightLengthConversion> spec = filter != null ? toSpecification(filter) : null;
        TypedQuery<WeightLengthConversion> query = getQuery(spec, page, WeightLengthConversion.class);
        return streamQuery(query)
            .map(source -> this.toVO(source, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public long count(WeightLengthConversionFilterVO filter) {
        Specification<WeightLengthConversion> spec = filter != null ? toSpecification(filter) : null;
        return getCountQuery(spec, WeightLengthConversion.class).getSingleResult();
    }

    @Override
    public void toVO(WeightLengthConversion source, WeightLengthConversionVO target, boolean copyIfNull) {
        toVO(source, target, WeightLengthConversionFetchOptions.DEFAULT, copyIfNull);
    }

    protected WeightLengthConversionVO toVO(WeightLengthConversion source, WeightLengthConversionFetchOptions fetchOptions) {
        if (source == null) return null;
        WeightLengthConversionVO target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    protected void toVO(WeightLengthConversion source, WeightLengthConversionVO target, WeightLengthConversionFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        fetchOptions = fetchOptions == null ? WeightLengthConversionFetchOptions.DEFAULT : fetchOptions;

        // Status
        if (source.getStatus() != null || copyIfNull) {
            if (source.getStatus() == null) {
                target.setStatusId(null);
            }
            else {
                target.setStatusId(source.getStatus().getId());
            }
        }

        // Sex
        if (source.getSex() != null || copyIfNull) {
            if (source.getSex() == null) {
                target.setSexId(null);
            }
            else {
                target.setSexId(source.getSex().getId());
            }
        }

        // Length Parameter
        if (source.getLengthParameter() != null || copyIfNull) {
            if (source.getLengthParameter() == null) {
                target.setLengthParameterId(null);
            }
            else {
                target.setLengthParameterId(source.getLengthParameter().getId());
            }
        }

        // Length unit
        if (source.getLengthUnit() != null || copyIfNull) {
            if (source.getLengthUnit() == null) {
                target.setLengthUnitId(null);
            }
            else {
                target.setLengthUnitId(source.getLengthUnit().getId());
            }
        }

        // Reference Taxon
        if (source.getReferenceTaxon() != null || copyIfNull) {
            if (source.getReferenceTaxon() == null) {
                target.setReferenceTaxonId(null);
            }
            else {
                target.setReferenceTaxonId(source.getReferenceTaxon().getId());
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
    }

    @Override
    public void toEntity(WeightLengthConversionVO source, WeightLengthConversion target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Status
        Integer statusId = source.getStatusId();
        if (statusId != null || copyIfNull) {
            if (statusId == null) target.setStatus(null);
            else target.setStatus(getReference(Status.class, statusId));
        }

        // Location
        Integer locationId = source.getLocation() != null ? source.getLocation().getId() : source.getLocationId();
        if (locationId != null || copyIfNull) {
            if (locationId == null) target.setLocation(null);
            else target.setLocation(getReference(Location.class, locationId));
        }

        // Sex
        Integer sexId = source.getSex() != null ? source.getSex().getId() : source.getSexId();
        if (sexId != null || copyIfNull) {
            if (sexId == null) target.setSex(null);
            else target.setSex(getReference(QualitativeValue.class, sexId));
        }

        // Length parameter
        Integer lengthParameterId = source.getLengthParameter() != null ? source.getLengthParameter().getId() : source.getLengthParameterId();
        if (lengthParameterId != null || copyIfNull) {
            if (lengthParameterId == null) target.setLengthParameter(null);
            else target.setLengthParameter(getReference(Parameter.class, lengthParameterId));
        }

        // Length unit
        Integer lengthUnitId = source.getLengthUnit() != null ? source.getLengthUnit().getId() : source.getLengthUnitId();
        if (lengthUnitId != null || copyIfNull) {
            if (lengthUnitId == null) target.setLengthUnit(null);
            else target.setLengthUnit(getReference(Unit.class, lengthUnitId));
        }
    }

    protected Specification<WeightLengthConversion> toSpecification(@NonNull WeightLengthConversionFilterVO filter) {
        return BindableSpecification
            .where(inStatusIds(filter.getStatusIds()))
            .and(hasReferenceTaxonIds(filter.getReferenceTaxonIds()))
            .and(hasLocationIds(filter.getLocationIds()))
            .and(hasSexIds(filter.getSexIds()))
            .and(hasLengthParameterIds(filter.getLengthParameterIds()))
            .and(hasLengthUnitIds(filter.getLengthUnitIds()))
            .and(atDate(filter.getDate()))
            ;
    }

}
