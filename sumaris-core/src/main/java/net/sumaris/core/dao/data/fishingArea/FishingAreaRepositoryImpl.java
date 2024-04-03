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


import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.data.FishingArea;
import net.sumaris.core.model.data.GearUseFeatures;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.referential.DepthGradient;
import net.sumaris.core.model.referential.DistanceToCoastGradient;
import net.sumaris.core.model.referential.NearbySpecificArea;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.FishingAreaVO;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author peck7 on 09/06/2020.
 */
@Slf4j
public class FishingAreaRepositoryImpl
    extends SumarisJpaRepositoryImpl<FishingArea, Integer, FishingAreaVO>
    implements FishingAreaSpecifications {

    private final LocationRepository locationRepository;
    private final ReferentialDao referentialDao;
    private final SumarisConfiguration configuration;

    private final ApplicationContext applicationContext;

    public FishingAreaRepositoryImpl(EntityManager entityManager,
                                     SumarisConfiguration configuration,
                                     LocationRepository locationRepository,
                                     ReferentialDao referentialDao,
                                     ApplicationContext applicationContext) {
        super(FishingArea.class, FishingAreaVO.class, entityManager);
        this.configuration = configuration;
        this.locationRepository = locationRepository;
        this.referentialDao = referentialDao;
        this.applicationContext = applicationContext;
        setCheckUpdateDate(false);
        setLockForUpdate(false);
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

        if (source.getSale() != null)
            target.setSaleId(source.getSale().getId());

        if (source.getGearUseFeatures() != null)
            target.setGearUseFeaturesId(source.getGearUseFeatures().getId());
    }

    @Override
    public void toEntity(FishingAreaVO source, FishingArea target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        if (copyIfNull || source.getLocation() != null) {
            if (source.getLocation() == null || source.getLocation().getId() == null) {
                target.setLocation(null);
            } else {
                target.setLocation(getReference(Location.class, source.getLocation().getId()));
            }
        }

        if (copyIfNull || source.getDistanceToCoastGradient() != null) {
            if (source.getDistanceToCoastGradient() == null || source.getDistanceToCoastGradient().getId() == null) {
                target.setDistanceToCoastGradient(null);
            } else {
                target.setDistanceToCoastGradient(getReference(DistanceToCoastGradient.class, source.getDistanceToCoastGradient().getId()));
            }
        }

        if (copyIfNull || source.getDepthGradient() != null) {
            if (source.getDepthGradient() == null || source.getDepthGradient().getId() == null) {
                target.setDepthGradient(null);
            } else {
                target.setDepthGradient(getReference(DepthGradient.class, source.getDepthGradient().getId()));
            }
        }

        if (copyIfNull || source.getNearbySpecificArea() != null) {
            if (source.getNearbySpecificArea() == null || source.getNearbySpecificArea().getId() == null) {
                target.setNearbySpecificArea(null);
            } else {
                target.setNearbySpecificArea(getReference(NearbySpecificArea.class, source.getNearbySpecificArea().getId()));
            }
        }

        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(getReference(QualityFlag.class, configuration.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(getReference(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Operation
        Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        source.setOperationId(operationId);
        if (copyIfNull || (operationId != null)) {
            if (operationId == null) {
                target.setOperation(null);
            } else {
                target.setOperation(getReference(Operation.class, operationId));
            }
        }

        // Sale
        Integer saleId = source.getSaleId() != null ? source.getSaleId() : (source.getSale() != null ? source.getSale().getId() : null);
        source.setSaleId(saleId);
        if (copyIfNull || (saleId != null)) {
            if (saleId == null) {
                target.setSale(null);
            } else {
                target.setSale(getReference(Sale.class, saleId));
            }
        }

        // Gear Use Features
        Integer gufId = source.getGearUseFeaturesId() != null ? source.getGearUseFeaturesId() : (source.getGearUseFeatures() != null ? source.getGearUseFeatures().getId() : null);
        source.setGearUseFeaturesId(gufId);
        if (copyIfNull || (gufId != null)) {
            if (gufId == null) {
                target.setGearUseFeatures(null);
            } else {
                target.setGearUseFeatures(getReference(GearUseFeatures.class, gufId));
            }
        }

    }

    @Override
    public List<FishingAreaVO> getAllByOperationId(int operationId) {
        return getRepository().getFishingAreaByOperationId(operationId)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FishingAreaVO> getAllByGearUseFeaturesId(int gufId) {
        return getRepository().getFishingAreaByGearUseFeaturesId(gufId)
            .stream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<FishingAreaVO> saveAllByOperationId(int operationId, @Nonnull List<FishingAreaVO> sources) {

        // Filter on non null objects
        sources = sources.stream().filter(Objects::nonNull).toList();

        // Set parent link
        sources.forEach(fa -> {
            fa.setOperationId(operationId);
            fa.setGearUseFeaturesId(null);
        });

        // Get existing fishing areas
        Operation operation = getById(Operation.class, operationId);

        return saveAllByList(sources, operation.getFishingAreas());
    }

    @Override
    public List<FishingAreaVO> saveAllBySaleId(int saleId, @Nonnull List<FishingAreaVO> sources) {

        // Filter on non null objects
        sources = sources.stream().filter(Objects::nonNull).toList();

        // Set parent link
        sources.forEach(fa -> {
            fa.setSaleId(saleId);
            fa.setGearUseFeaturesId(null);
        });

        // Get existing fishing areas
        Sale sale = getById(Sale.class, saleId);

        return saveAllByList(sources, sale.getFishingAreas());
    }

    @Override
    public List<FishingAreaVO> saveAllByGearUseFeaturesId(int gearUseFeaturesId, List<FishingAreaVO> sources) {

        // Filter on non null objects
        sources = Beans.getStream(sources).filter(Objects::nonNull).toList();

        // Set parent link
        sources.forEach(fa -> {
            fa.setGearUseFeaturesId(gearUseFeaturesId);
            fa.setOperationId(null);
        });

        // Get existing fishing areas
        GearUseFeatures guf = getById(GearUseFeatures.class, gearUseFeaturesId);

        return saveAllByList(sources, guf.getFishingAreas());
    }

    private FishingAreaRepository repository;

    protected FishingAreaRepository getRepository() {
        if (repository == null) {
            repository = this.applicationContext.getBean(FishingAreaRepository.class);
        }
        return repository;
    }

    protected List<FishingAreaVO> saveAllByList(@Nonnull List<FishingAreaVO> sources, @Nonnull List<FishingArea> target) {


        // Get existing fishing areas
        List<Integer> existingIds = Beans.collectIds(target);

        // Save
        sources.forEach(fishingArea -> {
            boolean exists = fishingArea.getId() != null && existingIds.remove(fishingArea.getId());
            // Reset the id, because a fishingArea could have been deleted by changes on trip.metiers (fix IMAGINE-436)
            if (!exists) fishingArea.setId(null);
        });

        // Delete remaining objects (BEFORE to save, because of unique key)
        existingIds.forEach(this::deleteById);

        // Save
        sources.forEach(this::save);

        return sources;
    }
}
