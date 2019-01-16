package net.sumaris.core.service.referential;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.dao.referential.location.LocationLevelDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.geometry.GeometryHelper;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevelLabels;
import net.sumaris.core.vo.referential.LocationVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service("locationService")
public class LocationServiceImpl implements LocationService{

    private static final Log log = LogFactory.getLog(ReferentialServiceImpl.class);

    @Autowired
    protected LocationDao locationDao;

    @Autowired
    protected LocationLevelDao locationLevelDao;

    @Autowired
    protected ReferentialDao referentialDao;

    @Override
    public void updateRectanglesAndSquares() {

        // Retrieve location levels
        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelLabels.RECTANGLE_ICES, "ICES rectangle")
                .put(LocationLevelLabels.SQUARE_10, "Square 10x10 Degrees")
                .build());
        LocationLevel rectangleLocationLevel = locationLevels.get(LocationLevelLabels.RECTANGLE_ICES);
        Preconditions.checkNotNull(rectangleLocationLevel);
        LocationLevel squareLocationLevel = locationLevels.get(LocationLevelLabels.SQUARE_10);
        Preconditions.checkNotNull(squareLocationLevel);

        //ValidityStatus notValidStatus = validityStatusDao.load(ValidityStatusCode.NOT_VALID.getValue());
        Pattern rectangleLabelPattern = Pattern.compile("[M]?[0-9]{2,3}[A-Z][0-9]");

        Map<String, LocationVO> rectangleByLabelMap = Maps.newHashMap();

        int locationInsertCount = 0;
        int locationUpdateCount = 0;
        int locationAreaInsertCount = 0;
        int locationAreaUpdateCount = 0;

        // Get all rectangles
        List<LocationVO> rectangleLocations = locationDao.getByLocationLevel(rectangleLocationLevel.getId());
        List<LocationVO> squareLocations = locationDao.getByLocationLevel(squareLocationLevel.getId());

        while (rectangleLocations.size() > 0) {
            for (LocationVO location : rectangleLocations) {

                Integer objectId = location.getId();
                String rectangleLabel = location.getLabel();
                if (!rectangleLabelPattern.matcher(rectangleLabel).matches()) {
                    log.warn(String.format("Invalid rectangle label: %s. No geometry will be created for this rectangle.", rectangleLabel));
                    continue;
                }
                if (log.isDebugEnabled()) {
                    //log.debug(String.format("Updating geomtry of the rectangle %s", rectangleLabel));
                }

                // Load rectangle geometry
                /*LocationArea locationArea = locationAreaDao.load(objectId);
                MultiPolygon geometry = (MultiPolygon) GeometryHelper.computeGeometryFromRectangleLabel(rectangleLabel);
                Preconditions.checkNotNull(geometry, "No geometry found for rectangle with label:" + rectangleLabel);

                if (locationArea == null) {
                    locationArea = LocationArea.Factory.newInstance();
                    locationArea.setId(objectId);
                    locationArea.setPosition(geometry);
                    locationAreaDao.create(locationArea);
                    locationAreaInsertCount++;
                } else if (locationArea.getPosition() == null
                        || !geometry.equals(locationArea.getPosition())) {
                    locationArea.setPosition(geometry);
                    locationAreaDao.update(locationArea);
                    locationAreaUpdateCount++;
                }*/

                // Store the rectangle for a later use
                rectangleByLabelMap.put(rectangleLabel, location);
            }

            // Reset location list (could be fiil if new rectangle are found
            rectangleLocations = Lists.newArrayList();

            // Get all square
            for (LocationVO location : squareLocations) {

                Integer objectId = location.getId();
                String squareLabel = location.getLabel();
                if (squareLabel == null || squareLabel.length() != 8) {
                    log.warn(String.format("Invalid square label: %s. No geometry will be created for this square.", squareLabel));
                    continue;
                }

                // Load rectangle geometry
                /*LocationArea locationArea = locationAreaDao.load(objectId);
                MultiPolygon geometry = (MultiPolygon) GeometryHelper.computeGeometryFromSquareLabel(squareLabel);
                Preconditions.checkNotNull(geometry, "No geometry found for square with label:" + squareLabel);

                if (locationArea == null) {
                    locationArea = new LocationArea();
                    locationArea.setId(objectId);
                    locationArea.setPosition(geometry);
                    locationAreaDao.create(locationArea);
                    locationAreaInsertCount++;
                } else if (locationArea.getPosition() == null
                        || !geometry.equals(locationArea.getPosition())) {
                    locationArea.setPosition(geometry);
                    locationAreaDao.update(locationArea);
                    locationAreaUpdateCount++;
                }*/

                // Update parent (as ICES_rectangle) :
                String parentRectangleLabel = GeometryHelper.convertSquareToRectangle(squareLabel);
                LocationVO parentLocation = rectangleByLabelMap.get(parentRectangleLabel);

                // Create the parent ICES Rectangle if need
                if (parentLocation == null) {
                    //log.debug(String.format("Create rectangle with label %s, because the child square %s has been found.", parentRectangleLabel, location.getLabel()));
                    parentLocation = new LocationVO();
                    parentLocation.setLabel(parentRectangleLabel);
                    parentLocation.setName(parentRectangleLabel);
                    parentLocation.setLevelId(rectangleLocationLevel.getId());
                    //parentLocation.setValidityStatus(notValidStatus);
                    parentLocation = (LocationVO)referentialDao.save(parentLocation);

                    // Add this new rectangle to the list, to enable geometry creation in the next <for> iteration
                    if (rectangleLocations.contains(parentLocation) == false) {
                        rectangleLocations.add(parentLocation);
                        rectangleByLabelMap.put(parentRectangleLabel, parentLocation);
                    }
                }

                // Update the square parent, if need:
                /*if (parentLocation != null
                        && (location.getParent() == null
                        || !location.getParent().equals(parentLocation))) {
                    location.setParent(parentLocation);
                    locationDao.update(location);
                    locationUpdateCount++;
                }*/
            }

            // Reset (to disable <for> in the next <while> iteration)
            squareLocations = Lists.newArrayList();
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("LOCATION: INSERT count: %s", locationInsertCount));
            log.info(String.format("          UPDATE count: %s", locationUpdateCount));
            log.info(String.format("LOCATION_AREA: INSERT count: %s", locationAreaInsertCount));
            log.info(String.format("               UPDATE count: %s", locationAreaUpdateCount));
        }

        // Update location hierarchy
        //updateLocationHierarchy();
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
                .put(LocationLevelLabels.PORT, "Port")
                .put(LocationLevelLabels.COUNTRY, "Country")
                .build());

        LocationLevel countryLocationLevel = locationLevels.get(LocationLevelLabels.COUNTRY);
        LocationLevel portLocationLevel = locationLevels.get(LocationLevelLabels.PORT);

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

    /* -- protected -- */

    protected Map<String, LocationLevel> createAndGetLocationLevels(Map<String, String> levels) {
        Map<String, LocationLevel> result = Maps.newHashMap();

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
                locationLevel = locationLevelDao.create(locationLevel);
            }
            result.put(label, locationLevel);
        }
        return result;
    }
}
