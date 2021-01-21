package net.sumaris.core.dao.data.fishingArea;

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


import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.data.FishingArea;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.referential.DepthGradient;
import net.sumaris.core.model.referential.DistanceToCoastGradient;
import net.sumaris.core.model.referential.NearbySpecificArea;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.data.FishingAreaVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 09/06/2020.
 */
public class FishingAreaRepositoryImpl
    extends SumarisJpaRepositoryImpl<FishingArea, Integer, FishingAreaVO>
    implements FishingAreaSpecifications {

    private static final Logger log =
        LoggerFactory.getLogger(FishingAreaRepositoryImpl.class);

    private final LocationRepository locationRepository;
    private final ReferentialDao referentialDao;
    private final SumarisConfiguration config;

    @Autowired
    @Lazy
    private FishingAreaRepository self;

    @Autowired
    public FishingAreaRepositoryImpl(EntityManager entityManager,
                                     SumarisConfiguration config,
                                     LocationRepository locationRepository,
                                     ReferentialDao referentialDao) {
        super(FishingArea.class, FishingAreaVO.class, entityManager);
        this.config = config;
        this.locationRepository = locationRepository;
        this.referentialDao = referentialDao;
    }

    @Override
    public List<FishingAreaVO> findAllVO(Specification<FishingArea> spec) {
        return super.findAll(spec).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void toVO(FishingArea source, FishingAreaVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        target.setLocation(locationRepository.toVO(source.getLocation()));

        if (source.getDistanceToCoastGradient() != null)
            target.setDistanceToCoastGradient(referentialDao.toVO(source.getDistanceToCoastGradient()));
        if (source.getDepthGradient() != null)
            target.setDepthGradient(referentialDao.toVO(source.getDepthGradient()));
        if (source.getNearbySpecificArea() != null)
            target.setNearbySpecificArea(referentialDao.toVO(source.getNearbySpecificArea()));

        if (source.getOperation() != null)
            target.setOperationId(source.getOperation().getId());
    }

    @Override
    public void toEntity(FishingAreaVO source, FishingArea target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        if (copyIfNull || source.getLocation() != null) {
            if (source.getLocation() == null || source.getLocation().getId() == null) {
                target.setLocation(null);
            } else {
                target.setLocation(load(Location.class, source.getLocation().getId()));
            }
        }

        if (copyIfNull || source.getDistanceToCoastGradient() != null) {
            if (source.getDistanceToCoastGradient() == null || source.getDistanceToCoastGradient().getId() == null) {
                target.setDistanceToCoastGradient(null);
            } else {
                target.setDistanceToCoastGradient(load(DistanceToCoastGradient.class, source.getDistanceToCoastGradient().getId()));
            }
        }

        if (copyIfNull || source.getDepthGradient() != null) {
            if (source.getDepthGradient() == null || source.getDepthGradient().getId() == null) {
                target.setDepthGradient(null);
            } else {
                target.setDepthGradient(load(DepthGradient.class, source.getDepthGradient().getId()));
            }
        }

        if (copyIfNull || source.getNearbySpecificArea() != null) {
            if (source.getNearbySpecificArea() == null || source.getNearbySpecificArea().getId() == null) {
                target.setNearbySpecificArea(null);
            } else {
                target.setNearbySpecificArea(load(NearbySpecificArea.class, source.getNearbySpecificArea().getId()));
            }
        }

        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // parent operation
        Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        source.setOperationId(operationId);
        if (copyIfNull || (operationId != null)) {
            if (operationId == null) {
                target.setOperation(null);
            } else {
                target.setOperation(load(Operation.class, operationId));
            }
        }

    }

    @Override
    public List<FishingAreaVO> getAllVOByOperationId(int operationId) {
        return self.getAllByOperationId(operationId).stream()
            .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<FishingAreaVO> saveAllByOperationId(int operationId, @Nonnull List<FishingAreaVO> sources) {

        // Filter on non null objects
        sources = sources.stream().filter(Objects::nonNull).collect(Collectors.toList());

        // Set parent link
        sources.forEach(fa -> fa.setOperationId(operationId));

        // Get existing fishing areas
        Set<Integer> existingIds = self.getAllIdsByOperationId(operationId);

        // Save
        sources.forEach(fishingArea -> {
            save(fishingArea);
            existingIds.remove(fishingArea.getId());
        });

        // Delete remaining objects
        existingIds.forEach(this::deleteById);

        return sources;
    }
}
