package net.sumaris.core.service.referential;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.MultiPolygon;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ValidityStatusDao;
import net.sumaris.core.dao.referential.location.*;
import net.sumaris.core.model.referential.ValidityStatus;
import net.sumaris.core.model.referential.ValidityStatusEnum;
import net.sumaris.core.model.referential.location.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("locationService")
public class LocationServiceImpl implements LocationService{

    private static final Logger log = LoggerFactory.getLogger(LocationServiceImpl.class);

    @Autowired
    protected LocationDao locationDao;

    @Autowired
    protected LocationAreaDao locationAreaDao;

    @Autowired
    protected ValidityStatusDao validityStatusDao;

    @Autowired
    protected LocationLevelDao locationLevelDao;

    @Autowired
    protected LocationClassificationDao locationClassificationDao;

    @Autowired
    protected ReferentialDao referentialDao;

    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public void insertOrUpdateRectangleLocations() {

        if (log.isInfoEnabled()) {
            log.info("Checking all statistical rectangles exists...");
        }

        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.RECTANGLE_ICES.getLabel(), "ICES rectangle")
                .put(LocationLevelEnum.RECTANGLE_CGPM_GFCM.getLabel(), "CGPM/GFCM rectangle")
                .build());

        LocationLevel icesRectangleLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_ICES.getLabel());
        Objects.requireNonNull(icesRectangleLocationLevel);
        LocationLevel cgpmRquareLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_CGPM_GFCM.getLabel());
        Objects.requireNonNull(cgpmRquareLocationLevel);

        ValidityStatus validStatus = validityStatusDao.getOne(ValidityStatusEnum.VALID.getId());

        // Existing rectangles
        List<LocationVO> existingLocations = locationLevels.values().stream()
                .map(level -> locationDao.getByLocationLevel(level.getId()))
                .flatMap(list -> list.stream())
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
            log.info(String.format("No missing rectangle detected (%s found)", existingLocations.size()));
            return;
        }

        log.info(String.format("Inserting missing rectangle (%s existing - %s expected)", existingLocations.size(), labels.size()));

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
                location.setValidityStatus(validStatus);
                locationDao.create(location);
                locationInsertCount++;
            }
        }
        if (log.isInfoEnabled()) {
            log.info(String.format("LOCATION: INSERT count: %s", locationInsertCount));
        }
    }

    @Override
    public void insertOrUpdateSquares10() {
        if (log.isInfoEnabled()) {
            log.info("Checking all squares 10'x10' exists...");
        }

        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.SQUARE_10.getLabel(), "Square 10'x10'")
                .build());

        LocationLevel square10LocationLevel = locationLevels.get(LocationLevelEnum.SQUARE_10.getLabel());
        Objects.requireNonNull(square10LocationLevel);

        ValidityStatus validStatus = validityStatusDao.getOne(ValidityStatusEnum.VALID.getId());

        // Get existing rectangle
        Map<String, LocationVO> rectangleByLabelMap = Beans.splitByProperty(getExistingRectangles(), Location.Fields.LABEL);

        // Get existing locations
        List<LocationVO> existingLocations = locationDao.getByLocationLevel(square10LocationLevel.getId());
        Map<String, LocationVO> locationByLabelMap = Beans.splitByProperty(existingLocations, Location.Fields.LABEL);

        int locationInsertCount = 0;
        int locationAssociationInsertCount = 0;
        Date creationDate = new Date();

        Set<String> labels = Locations.getAllSquare10Labels(resourceLoader, false);

        if (labels.size() == existingLocations.size()) {
            log.info(String.format("No missing square 10'x10' (%s found)", existingLocations.size()));
            return;
        }

        log.info(String.format("Inserting missing square 10'x10' (%s existing - %s expected)", existingLocations.size(), labels.size()));

        for (String label: labels) {

            if (!locationByLabelMap.containsKey(label)) {
                Location location = new Location();
                location.setLabel(label);
                location.setName(label);
                location.setCreationDate(creationDate);
                location.setUpdateDate(creationDate);
                location.setLocationLevel(square10LocationLevel);
                location.setValidityStatus(validStatus);
                location = locationDao.create(location);
                locationByLabelMap.put(label, locationDao.toLocationVO(location));
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
                if (parentLocation != null && !locationDao.hasAssociation(childLocation.getId(), parentLocation.getId())) {
                    locationDao.addAssociation(childLocation.getId(), parentLocation.getId(), 1d);
                    locationAssociationInsertCount++;
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("LOCATION: INSERT count: %s", locationInsertCount));
            log.info(String.format("LOCATION_ASSOCIATION: INSERT count: %s", locationAssociationInsertCount));
        }
    }

    public void insertOrUpdateRectangleAndSquareAreas() {

        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.RECTANGLE_ICES.getLabel(), "ICES rectangle")
                .put(LocationLevelEnum.RECTANGLE_CGPM_GFCM.getLabel(), "CGPM/GFCM rectangle")
                .put(LocationLevelEnum.SQUARE_10.getLabel(), "Square 10' x 10'")
                .build());
        LocationLevel icesRectangleLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_ICES.getLabel());
        Objects.requireNonNull(icesRectangleLocationLevel);
        LocationLevel cgpmRectangleLocationLevel = locationLevels.get(LocationLevelEnum.RECTANGLE_CGPM_GFCM.getLabel());
        Objects.requireNonNull(cgpmRectangleLocationLevel);
        LocationLevel square10LocationLevel = locationLevels.get(LocationLevelEnum.SQUARE_10.getLabel());
        Preconditions.checkNotNull(square10LocationLevel);

        ValidityStatus notValidStatus = validityStatusDao.getOne(ValidityStatusEnum.INVALID.getId());
        Pattern rectangleLabelPattern = Pattern.compile("[M]?[0-9]{2,3}[A-Z][0-9]");

        Map<String, LocationVO> rectangleByLabelMap = Maps.newHashMap();

        int locationInsertCount = 0;
        int locationUpdateCount = 0;
        int locationAreaInsertCount = 0;
        int locationAreaUpdateCount = 0;

        // Get existing rectangles
        List<LocationVO> rectangleLocations = getExistingRectangles();
        List<LocationVO> square10Locations = locationDao.getByLocationLevel(square10LocationLevel.getId());

        while (rectangleLocations.size() > 0) {
            for (LocationVO location : rectangleLocations) {

                Integer objectId = location.getId();
                String rectangleLabel = location.getLabel();
                if (!rectangleLabelPattern.matcher(rectangleLabel).matches()) {
                    log.warn(String.format("Invalid rectangle label {%s.} No geometry will be created for this rectangle.", rectangleLabel));
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Updating geometry of rectangle {%s}", rectangleLabel));
                }

                // Load rectangle geometry
                LocationArea locationArea = locationAreaDao.findById(objectId).orElse(null);
                MultiPolygon geometry = (MultiPolygon) Locations.getGeometryFromRectangleLabel(rectangleLabel, true);
                Preconditions.checkNotNull(geometry, "No geometry found for rectangle with label:" + rectangleLabel);

                if (locationArea == null) {
                    locationArea = new LocationArea();
                    locationArea.setId(objectId);
                    locationArea.setPosition(geometry);
                    locationAreaDao.save(locationArea);
                    locationAreaInsertCount++;
                } else if (locationArea.getPosition() == null
                        || !geometry.equals(locationArea.getPosition())) {
                    locationArea.setPosition(geometry);
                    locationAreaDao.save(locationArea);
                    locationAreaUpdateCount++;
                }

                // Store the rectangle for a later use
                rectangleByLabelMap.put(rectangleLabel, location);
            }

            // Reset location list (could be fiil if new rectangle are found
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
                LocationArea locationArea = locationAreaDao.getOne(objectId);
                MultiPolygon geometry = (MultiPolygon) Locations.getGeometryFromSquare10Label(squareLabel);
                Preconditions.checkNotNull(geometry, "No geometry found for square with label:" + squareLabel);

                if (locationArea == null) {
                    locationArea = new LocationArea();
                    locationArea.setId(objectId);
                    locationArea.setPosition(geometry);
                    locationAreaDao.save(locationArea);
                    locationAreaInsertCount++;
                } else if (locationArea.getPosition() == null
                        || !geometry.equals(locationArea.getPosition())) {
                    locationArea.setPosition(geometry);
                    locationAreaDao.save(locationArea);
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
                    parentLocation = (LocationVO)referentialDao.save(parentLocation);

                    // Add this new rectangle to the list, to enable geometry creation in the next <for> iteration
                    if (rectangleLocations.contains(parentLocation) == false) {
                        rectangleLocations.add(parentLocation);
                        rectangleByLabelMap.put(parentRectangleLabel, parentLocation);
                    }
                }

                // Update the square parent, if need:
                if (parentLocation != null && !locationDao.hasAssociation(location.getId(), parentLocation.getId())) {
                    locationDao.addAssociation(location.getId(), parentLocation.getId(), 1d);
                    locationUpdateCount++;
                }
            }

            // Reset (to disable <for> in the next <while> iteration)
            square10Locations = Lists.newArrayList();
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("LOCATION: INSERT count: %s", locationInsertCount));
            log.info(String.format("          UPDATE count: %s", locationUpdateCount));
            log.info(String.format("LOCATION_AREA: INSERT count: %s", locationAreaInsertCount));
            log.info(String.format("               UPDATE count: %s", locationAreaUpdateCount));
        }
    }

    @Override
    public void updateLocationHierarchy() {
        if (log.isInfoEnabled()) {
            log.info("Updating location hierarchy...");
        }
        locationDao.updateLocationHierarchy();
    }

    @Override
    public void printLocationPorts(PrintStream out, String indentation) {
        /*Preconditions.checkArgument(StringUtils.isNotBlank(indentation));
        Preconditions.checkNotNull(out);

        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelLabels.HARBOUR, "Port")
                .put(LocationLevelLabels.COUNTRY, "Country")
                .build());

        LocationLevel countryLocationLevel = locationLevels.get(LocationLevelLabels.COUNTRY);
        LocationLevel portLocationLevel = locationLevels.get(LocationLevelLabels.HARBOUR);

        List<Integer> processedPorts = Lists.newArrayList();
        List<Location> countries = locationDao.getLocationByLocationLevel(countryLocationLevel.getId());
        for (Location country: countries) {
            if ("1".equals(country.getValidityStatus().getCode())) {
                out.println(String.format("%s - %s (%s)", country.getLabel(), country.getName(), country.getId()));

                List<Location> nuts3list = locationDao.getLocationByLevelAndParent(
                        nut3LocationLevel.getId(),
                        country.getId());
                for (Location nuts3: nuts3list) {
                    if ("1".equals(nuts3.getValidityStatus().getCode())) {
                        out.println(String.format("%s%s - %s (%s)", indentation, nuts3.getLabel(), nuts3.getName(), nuts3.getId()));

                        List<Location> ports = locationDao.getLocationByLevelAndParent(
                                portLocationLevel.getId(),
                                nuts3.getId());
                        for (Location port: ports) {
                            if ("1".equals(port.getValidityStatus().getCode())) {
                                out.println(String.format("%s%s%s - %s (%s)", indentation, indentation, port.getLabel(), port.getName(), port.getId()));
                                processedPorts.add(port.getId());
                            }
                        }
                    }
                }

                // Port sans NUTS 3 :
                List<Location> ports = locationDao.getLocationByLevelAndParent(
                        portLocationLevel.getId(),
                        country.getId());
                boolean firstActivePort = true;
                for (Location port: ports) {

                    if ("1".equals(port.getValidityStatus().getCode())
                            && !processedPorts.contains(port.getId())) {
                        if (firstActivePort) {
                            out.println(String.format("%sSANS NUTS3 (ou sans nuts3 valide):", indentation));
                            firstActivePort = false;
                        }
                        out.println(String.format("%s%s%s - %s (%s)", indentation, indentation, port.getLabel(), port.getName(), port.getId()));
                        processedPorts.add(port.getId());
                    }
                }

            }
        }

        // Port sans pays :
        List<Location> ports = locationDao.getLocationByLocationLevel(
                portLocationLevel.getId());
        boolean firstActivePort = true;
        for (Location port: ports) {

            if ("1".equals(port.getValidityStatus().getCode())
                    && !processedPorts.contains(port.getId())) {
                if (firstActivePort) {
                    out.println(String.format("SANS PAYS :", indentation));
                    out.println(String.format("%sSANS NUTS3 :", indentation));
                    firstActivePort = false;
                }
                out.println(String.format("%s%s%s - %s (%s)", indentation, indentation, port.getLabel(), port.getName(), port.getId()));
                processedPorts.add(port.getId());
            }
        }*/
    }


    @Override
    public String getLocationLabelByLatLong(Number latitude, Number longitude) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("Arguments 'latitude' and 'longitude' should not be null.");
        }

        // Try to get a statistical rectangle
        String rectangleLabel = Locations.getRectangleLabelByLatLong(latitude, longitude);
        if (StringUtils.isNotBlank(rectangleLabel)) return rectangleLabel;

        // TODO: get it from spatial query ?

        // Otherwise, return null
        return null;
    }

    @Override
    public Integer getLocationIdByLatLong(Number latitude, Number longitude) {
        String locationLabel = getLocationLabelByLatLong(latitude, longitude);
        if (locationLabel == null) return null;
        ReferentialVO location = referentialDao.findByUniqueLabel(Location.class.getSimpleName(), locationLabel);
        return location == null ? null : location.getId();
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
        LocationClassification defaultClassification = locationClassificationDao.getByLabel(LocationClassificationEnum.SEA.getLabel());

        for (String label: levels.keySet()) {
            String name = StringUtils.trimToNull(levels.get(label));

            LocationLevel locationLevel = locationLevelDao.findByLabel(label);
            if (locationLevel == null) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Adding new LocationLevel with label {%s}", label));
                }
                locationLevel = new LocationLevel();
                locationLevel.setLabel(label);
                locationLevel.setName(name);
                locationLevel.setCreationDate(creationDate);
                locationLevel.setLocationClassification(defaultClassification);
                locationLevel = locationLevelDao.create(locationLevel);
            }
            result.put(label, locationLevel);
        }
        return result;
    }

    protected List<LocationVO> getExistingRectangles() {
        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelEnum.RECTANGLE_ICES.getLabel(), "ICES rectangle")
                .put(LocationLevelEnum.RECTANGLE_CGPM_GFCM.getLabel(), "CGPM/GFCM rectangle")
                .build());

        // Get existing rectangles
        return locationLevels.values()
                .stream()
                .map(level -> locationDao.getByLocationLevel(level.getId()))
                .flatMap(list -> list.stream())
                .collect(Collectors.toList());

    }
}
