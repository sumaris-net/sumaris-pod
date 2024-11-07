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
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.VesselOwner;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.data.vessel.VesselOwnerVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.location.LocationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class VesselOwnerRepositoryImpl
    extends SumarisJpaRepositoryImpl<VesselOwner, Integer, VesselOwnerVO>
    implements VesselOwnerSpecifications {

    private final LocationRepository locationRepository;
    private final ProgramRepository programRepository;

    @Autowired
    public VesselOwnerRepositoryImpl(EntityManager entityManager,
                                     LocationRepository locationRepository,
                                     ProgramRepository programRepository) {
        super(VesselOwner.class, VesselOwnerVO.class, entityManager);
        this.locationRepository = locationRepository;
        this.programRepository = programRepository;
    }

    public VesselOwnerVO get(int id) {
        return toVO(this.getById(id));
    }

    public VesselOwner getById(int id) {
        return super.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unable to load entity " + getDomainClass().getName() + " with identifier '" + id + "': not found in database."));
    }

    @Override
    public Specification<VesselOwner> toSpecification(VesselFilterVO filter) {
        return BindableSpecification.where(searchText(filter));
    }

    @Override
    public List<VesselOwnerVO> findAll(VesselFilterVO filter, Page page) {

        TypedQuery<VesselOwner> query = getQuery(toSpecification(filter), page, VesselOwner.class);

        try (Stream<VesselOwner> stream = streamQuery(query)) {
            return stream.map(this::toVO).toList();
        }
    }

    public long count(VesselFilterVO filter) {
        return count(toSpecification(filter));
    }

    @Override
    public void toVO(VesselOwner source, VesselOwnerVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Country location
        if (copyIfNull || source.getCountryLocation() != null) {
            if (source.getCountryLocation() == null) {
                target.setCountryLocation(null);
            }
            else {
                LocationVO countryLocation = locationRepository.toVO(source.getCountryLocation());
                target.setCountryLocation(countryLocation);
            }
        }

        // Program
        if (copyIfNull || source.getProgram() != null) {
            if (source.getProgram() == null) {
                target.setProgram(null);
            }
            else {
                target.setProgram(programRepository.toVO(source.getProgram(), ProgramFetchOptions.MINIMAL));
            }
        }
    }

    @Override
    public void toEntity(VesselOwnerVO source, VesselOwner target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Country location
        if (copyIfNull || source.getCountryLocation() != null) {
            if (source.getCountryLocation() == null || source.getCountryLocation().getId() == null) {
                target.setCountryLocation(null);
            } else {
                target.setCountryLocation(getReference(Location.class, source.getCountryLocation().getId()));
            }
        }

        // Program
        Integer programId = source.getProgram() != null ? source.getProgram().getId() : null;
        if (copyIfNull || programId != null) {
            if (programId == null) {
                target.setProgram(null);
            }
            else {
                target.setProgram(getReference(Program.class, programId));
            }
        }
    }

}
