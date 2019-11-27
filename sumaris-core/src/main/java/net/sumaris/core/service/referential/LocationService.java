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
