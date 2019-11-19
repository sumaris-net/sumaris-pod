package net.sumaris.core.service.referential;

import net.sumaris.core.service.referential.location.LocationByPositionService;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintStream;

@Transactional
public interface LocationService extends LocationByPositionService  {

    void insertOrUpdateRectangleLocations();

    void insertOrUpdateSquares10();

    void insertOrUpdateRectangleAndSquareAreas();

    void updateLocationHierarchy();

    @Transactional(readOnly = true)
    void printLocationPorts(PrintStream out, String indentation);

    /**
     * @deprecated use insertOrUpdateRectangleAndSquareAreas instead
     */
    @Deprecated
    void updateRectanglesAndSquares();

}
