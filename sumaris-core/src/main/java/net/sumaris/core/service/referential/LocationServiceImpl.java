package net.sumaris.core.service.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.StatusRepository;
import net.sumaris.core.dao.referential.ValidityStatusRepository;
import net.sumaris.core.dao.referential.location.*;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.ValidityStatus;
import net.sumaris.core.model.referential.ValidityStatusEnum;
import net.sumaris.core.model.referential.location.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("locationService")
@Slf4j
public class LocationServiceImpl implements LocationService {

    @Autowired
    protected LocationRepository locationRepository;

    @Autowired
    protected LocationAreaRepository locationAreaRepository;

    @Autowired
    protected StatusRepository statusRepository;

    @Autowired
    protected ValidityStatusRepository validityStatusRepository;

    @Autowired
    protected LocationLevelRepository locationLevelRepository;

    @Autowired
    protected LocationClassificationRepository locationClassificationRepository;

    @Autowired
    protected ReferentialDao referentialDao;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    protected SumarisConfiguration configuration;

    private boolean enableTechnicalTablesUpdate = false;

    @Async
    @TransactionalEventListener(
        value = {ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class},
        phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    protected void onConfigurationReady(ConfigurationEvent event) {

        // Update technical tables (if option changed)
        if (enableTechnicalTablesUpdate != configuration.enableTechnicalTablesUpdate()) {
            enableTechnicalTablesUpdate = configuration.enableTechnicalTablesUpdate();
            if (enableTechnicalTablesUpdate) {
                updateLocationHierarchy();
            }
        }
    }

    @Override
    public LocationVO get(int id) {
        return locationRepository.get(id);
    }

    @Override
    public List<LocationVO> findByFilter(LocationFilterVO filter) {
        return findByFilter(filter, null, ReferentialFetchOptions.builder().build());
    }

    @Override
    public List<LocationVO> findByFilter(LocationFilterVO filter, Page page, ReferentialFetchOptions fetchOptions) {
        return locationRepository.findAll(filter, page, fetchOptions);
    }

    @Override
    public long countByFilter(LocationFilterVO filter) {
        return locationRepository.count(filter);
    }

    @Override
    public void insertOrUpdateRectangleLocations() {

        if (log.isInfoEnabled()) {
            log.info("Checking statistical rectangles in table {LOCATION}...");
        }

        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.RECTANGLE_ICES.getLabel(), "ICES rectangle")
                .put(LocationLevelEnum.RECTANGLE_GFCM.getLabel(), "CGPM/GFCM rectangle")
                .build());

        LocationLevel icesRectangleLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_ICES.getLabel());
        Objects.requireNonNull(icesRectangleLocationLevel);
        LocationLevel cgpmRquareLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_GFCM.getLabel());
        Objects.requireNonNull(cgpmRquareLocationLevel);

        Status enableStatus = statusRepository.getEnableStatus();
        ValidityStatus validStatus = validityStatusRepository.getById(ValidityStatusEnum.VALID.getId());

        // Existing rectangles
        List<LocationVO> existingLocations = locationLevels.values().stream()
                .map(level -> getLocationsByLocationLevelId(level.getId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        Map<String, LocationVO> rectangleByLabelMap = Beans.splitByProperty(existingLocations, Location.Fields.LABEL);

        int locationInsertCount = 0;
        Date creationDate = new Date();

        Set<String> labels = ImmutableSet.<String>builder()
                // ICES rectangles
                .addAll(Locations.getAllIcesRectangleLabels(resourceLoader, false))
                // GCPM/GFCM rectangles
                .addAll(Locations.getAllCgpmGfcmRectangleLabels(resourceLoader, false))
                .build();

        if (labels.size() == existingLocations.size()) {
            log.info(String.format("Statistical rectangle already loaded (%s exists)", existingLocations.size()));
            return;
        }

        log.info(String.format("Inserting statistical rectangles... (%s found, %s expected)", existingLocations.size(), labels.size()));

        for (String label: labels) {

            if (!rectangleByLabelMap.containsKey(label)) {
                Location location = new Location();
                location.setLabel(label);
                location.setName(label);
                location.setCreationDate(creationDate);
                if (label.startsWith("M")) {
                    location.setLocationLevel(cgpmRquareLocationLevel);
                }
                else {
                    location.setLocationLevel(icesRectangleLocationLevel);
                }
                location.setStatus(enableStatus);
                location.setValidityStatus(validStatus);
                locationRepository.save(location);
                locationInsertCount++;
            }
        }
        if (log.isInfoEnabled()) {
            log.info(String.format("Statistical rectangles successfully inserted (%s inserts)", locationInsertCount));
        }
    }

    @Override
    public void insertOrUpdateSquares10() {
        if (log.isInfoEnabled()) {
            log.info("Checking squares 10'x10' in table {LOCATION}...");
        }

        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.SQUARE_10.getLabel(), "Square 10'x10'")
                .build());

        LocationLevel square10LocationLevel = locationLevels.get(LocationLevelEnum.SQUARE_10.getLabel());
        Objects.requireNonNull(square10LocationLevel);

        Status enableStatus = statusRepository.getEnableStatus();
        ValidityStatus validStatus = validityStatusRepository.getById(ValidityStatusEnum.VALID.getId());

        // Get existing rectangle
        Map<String, LocationVO> rectangleByLabelMap = Beans.splitByProperty(getExistingRectangles(), Location.Fields.LABEL);

        // Get existing locations
        List<LocationVO> existingLocations = getLocationsByLocationLevelId(square10LocationLevel.getId());
        Map<String, LocationVO> locationByLabelMap = Beans.splitByProperty(existingLocations, Location.Fields.LABEL);

        int locationInsertCount = 0;
        int locationAssociationInsertCount = 0;
        Date creationDate = new Date();

        Set<String> labels = Locations.getAllSquare10Labels(resourceLoader, false);

        if (labels.size() == existingLocations.size()) {
            log.info(String.format("Square 10'x10' already loaded in {LOCATION} (%s found)", existingLocations.size()));
            return;
        }

        log.info(String.format("Inserting square 10'x10'... (%s found, %s expected)", existingLocations.size(), labels.size()));

        for (String label: labels) {

            if (!locationByLabelMap.containsKey(label)) {
                Location location = new Location();
                location.setLabel(label);
                location.setName(label);
                location.setCreationDate(creationDate);
                location.setUpdateDate(creationDate);
                location.setLocationLevel(square10LocationLevel);
                location.setStatus(enableStatus);
                location.setValidityStatus(validStatus);
                locationRepository.save(location);
                locationByLabelMap.put(label, locationRepository.toVO(location));
                locationInsertCount++;
            }
        }


        for (String label: labels) {
            String parentRectangleLabel = Locations.convertSquare10ToRectangle(label);

            // Link to parent (rectangle) if exists
            if (parentRectangleLabel != null) {
                LocationVO parentLocation = rectangleByLabelMap.get(parentRectangleLabel);
                LocationVO childLocation = locationByLabelMap.get(label);

                // Update the square parent, if need:
                if (parentLocation != null && !locationRepository.hasAssociation(childLocation.getId(), parentLocation.getId())) {
                    locationRepository.addAssociation(childLocation.getId(), parentLocation.getId(), 1d);
                    locationAssociationInsertCount++;
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Square 10'x10' successfully inserted ({} inserts, {} location associations) ",
                    locationInsertCount,
                    locationAssociationInsertCount);
        }
    }

    public void insertOrUpdateRectangleAndSquareAreas() {

        long startTime = System.currentTimeMillis();
        int srid = configuration.getGeometrySrid();
        log.info("Updating location geometries (rectangles and squares)... (SRID: {})", srid);

        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.RECTANGLE_ICES.getLabel(), "ICES rectangle")
                .put(LocationLevelEnum.RECTANGLE_GFCM.getLabel(), "CGPM/GFCM rectangle")
                .put(LocationLevelEnum.SQUARE_10.getLabel(), "Square 10' x 10'")
                .build());
        LocationLevel icesRectangleLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_ICES.getLabel());
        Objects.requireNonNull(icesRectangleLocationLevel);
        LocationLevel cgpmRectangleLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_GFCM.getLabel());
        Objects.requireNonNull(cgpmRectangleLocationLevel);
        LocationLevel square10LocationLevel = locationLevels.get(LocationLevelEnum.SQUARE_10.getLabel());
        Preconditions.checkNotNull(square10LocationLevel);

        ValidityStatus notValidStatus = validityStatusRepository.getById(ValidityStatusEnum.INVALID.getId());
        Pattern rectangleLabelPattern = Pattern.compile("[M]?[0-9]{2,3}[A-Z][0-9]");

        Map<String, LocationVO> rectangleByLabelMap = Maps.newHashMap();

        int locationInsertCount = 0;
        int locationUpdateCount = 0;
        int locationAreaInsertCount = 0;
        int locationAreaUpdateCount = 0;

        // Get existing rectangles
        List<LocationVO> rectangleLocations = getExistingRectangles();
        List<LocationVO> square10Locations = getLocationsByLocationLevelId(square10LocationLevel.getId());

        while (rectangleLocations.size() > 0) {
            for (LocationVO location : rectangleLocations) {

                Integer objectId = location.getId();
                String rectangleLabel = location.getLabel();
                if (!rectangleLabelPattern.matcher(rectangleLabel).matches()) {
                    log.warn(String.format("Invalid rectangle label {%s} No geometry will be created for this rectangle.", rectangleLabel));
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Updating geometry of rectangle {%s}", rectangleLabel));
                }

                // Load rectangle geometry
                try {
                    LocationArea locationArea = locationAreaRepository.findById(objectId).orElse(null);
                    Geometry geometry = Locations.getGeometryFromRectangleLabel(rectangleLabel, true);
                    Preconditions.checkNotNull(geometry, "No geometry found for rectangle with label:" + rectangleLabel);

                    // Set the geometry SRID
                    geometry.setSRID(srid);

                    if (locationArea == null) {
                        locationArea = new LocationArea();
                        locationArea.setId(objectId);
                        locationArea.setPosition(geometry);
                        locationAreaRepository.save(locationArea);
                        locationAreaInsertCount++;
                    } else if (locationArea.getPosition() == null
                        || !geometry.equals(locationArea.getPosition())) {
                        locationArea.setPosition(geometry);
                        locationAreaRepository.save(locationArea);
                        locationAreaUpdateCount++;
                    }

                    // Store the rectangle for a later use
                    rectangleByLabelMap.put(rectangleLabel, location);
                }
                // Location Area cannot be loaded (e.g. when geometry is invalid)
                catch (DataAccessException e) {
                    log.error("Error while loading LocationArea for id={}: {}", objectId, e.getLocalizedMessage());
                }
                // Geometry not found
                catch (NullPointerException npe) {
                    log.error(npe.getLocalizedMessage());
                }
            }

            // Reset location list (could be fill if new rectangle are found
            rectangleLocations = Lists.newArrayList();

            // Get all squares
            for (LocationVO location : square10Locations) {

                Integer objectId = location.getId();
                String squareLabel = location.getLabel();
                if (squareLabel == null || squareLabel.length() != 8) {
                    log.warn(String.format("Invalid square label: %s. No geometry will be created for this square.", squareLabel));
                    continue;
                }

                // Load square geometry
                LocationArea locationArea = locationAreaRepository.getById(objectId);
                Geometry geometry = Locations.getGeometryFromMinuteSquareLabel(squareLabel, 10, true);
                Preconditions.checkNotNull(geometry, "No geometry found for square with label:" + squareLabel);

                // Set the geometry SRID
                geometry.setSRID(srid);

                if (locationArea == null) {
                    locationArea = new LocationArea();
                    locationArea.setId(objectId);
                    locationArea.setPosition(geometry);
                    locationAreaRepository.save(locationArea);
                    locationAreaInsertCount++;
                } else if (locationArea.getPosition() == null
                        || !geometry.equals(locationArea.getPosition())) {
                    locationArea.setPosition(geometry);
                    locationAreaRepository.save(locationArea);
                    locationAreaUpdateCount++;
                }

                // Update parent (as ICES_rectangle) :
                String parentRectangleLabel = Locations.convertSquare10ToRectangle(squareLabel);
                LocationVO parentLocation = rectangleByLabelMap.get(parentRectangleLabel);

                // Create the parent ICES Rectangle if need
                if (parentLocation == null) {
                    //log.debug(String.format("Create rectangle with label %s, because the child square %s has been found.", parentRectangleLabel, location.getLabel()));
                    parentLocation = new LocationVO();
                    parentLocation.setLabel(parentRectangleLabel);
                    parentLocation.setName(parentRectangleLabel);
                    if (parentRectangleLabel.startsWith("M")) {
                        parentLocation.setLevelId(cgpmRectangleLocationLevel.getId());
                    }
                    else {
                        parentLocation.setLevelId(icesRectangleLocationLevel.getId());
                    }
                    parentLocation.setValidityStatusId(notValidStatus.getId());
                    parentLocation = locationRepository.save(parentLocation);

                    // Add this new rectangle to the list, to enable geometry creation in the next <for> iteration
                    if (!rectangleLocations.contains(parentLocation)) {
                        rectangleLocations.add(parentLocation);
                        rectangleByLabelMap.put(parentRectangleLabel, parentLocation);
                    }
                }

                // Update the square parent, if need:
                if (parentLocation != null && !locationRepository.hasAssociation(location.getId(), parentLocation.getId())) {
                    locationRepository.addAssociation(location.getId(), parentLocation.getId(), 1d);
                    locationUpdateCount++;
                }
            }

            // Reset (to disable <for> in the next <while> iteration)
            square10Locations = Lists.newArrayList();
        }

        if (log.isInfoEnabled()) {
            log.info("Updating location geometries (rectangles and squares) [OK] {} - LOCATION ({} inserts - {} updates) - LOCATION_AREA ({} inserts - {} updates)",
                Dates.elapsedTime(startTime),
                locationInsertCount, locationUpdateCount,
                locationAreaInsertCount, locationAreaUpdateCount);
        }
    }

    @Override
    public void updateLocationHierarchy() {
        if (log.isInfoEnabled()) {
            log.info("Updating location hierarchy...");
        }
        locationRepository.updateLocationHierarchy();
    }

    @Override
    public String getLocationLabelByLatLong(Number latitude, Number longitude) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("Arguments 'latitude' and 'longitude' should not be null.");
        }

        // Try to find a statistical rectangle
        String rectangleLabel = Locations.getRectangleLabelByLatLong(latitude, longitude);
        if (StringUtils.isNotBlank(rectangleLabel)) return rectangleLabel;

        // TODO: find it from spatial query ?

        // Otherwise, return null
        return null;
    }

    @Override
    public Integer getLocationIdByLatLong(Number latitude, Number longitude) {
        String locationLabel = getLocationLabelByLatLong(latitude, longitude);
        if (locationLabel == null) return null;
        Optional<ReferentialVO> location = referentialDao.findByUniqueLabel(Location.class.getSimpleName(), locationLabel);
        return location.map(ReferentialVO::getId).orElse(null);
    }

    @Override
    @Deprecated
    public void updateRectanglesAndSquares() {
        // synonym of:
        insertOrUpdateRectangleAndSquareAreas();
    }

    /* -- protected -- */

    protected Map<String, LocationLevel> createAndGetLocationLevels(Map<String, String> levels) {
        Map<String, LocationLevel> result = Maps.newHashMap();

        Date creationDate = new Date();
        LocationClassification defaultClassification = locationClassificationRepository.getById(LocationClassificationEnum.SEA.getId());

        for (String label: levels.keySet()) {
            String name = StringUtils.trimToNull(levels.get(label));

            LocationLevel locationLevel = locationLevelRepository.findByLabel(label);
            if (locationLevel == null) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Adding new LocationLevel with label {%s}", label));
                }
                locationLevel = new LocationLevel();
                locationLevel.setLabel(label);
                locationLevel.setName(name);
                locationLevel.setCreationDate(creationDate);
                locationLevel.setLocationClassification(defaultClassification);
                locationLevel.setStatus(statusRepository.getEnableStatus());
                locationLevel = locationLevelRepository.save(locationLevel);
            }
            result.put(label, locationLevel);
        }
        return result;
    }

    protected List<LocationVO> getExistingRectangles() {
        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.RECTANGLE_ICES.getLabel(), "ICES rectangle")
                .put(LocationLevelEnum.RECTANGLE_GFCM.getLabel(), "CGPM/GFCM rectangle")
                .build());

        // Get existing rectangles
        return locationLevels.values()
                .stream()
                .map(level -> getLocationsByLocationLevelId(level.getId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

    }

    protected List<LocationVO> getLocationsByLocationLevelId(int locationLevelId) {
        return locationRepository.findAll(LocationFilterVO.builder()
            .levelIds(new Integer[]{locationLevelId})
            .build());
    }
}
