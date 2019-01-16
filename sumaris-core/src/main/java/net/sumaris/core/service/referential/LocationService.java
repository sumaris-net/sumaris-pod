package net.sumaris.core.service.referential;

import java.io.PrintStream;

public interface LocationService {

    void updateRectanglesAndSquares();

    void updateLocationHierarchy();

    void printLocationPorts(PrintStream out, String indentation);
}
