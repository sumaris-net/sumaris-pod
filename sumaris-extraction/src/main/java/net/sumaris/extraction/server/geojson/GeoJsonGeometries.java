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

package net.sumaris.extraction.server.geojson;

import net.sumaris.core.exception.SumarisTechnicalException;
import org.geojson.Geometry;
import org.geojson.LngLatAlt;
import org.geojson.MultiPolygon;
import org.geojson.Polygon;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper for GeoJson conversion
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class GeoJsonGeometries {

    protected GeoJsonGeometries() {
        // helper class
    }

    public static Geometry jtsGeometry(org.locationtech.jts.geom.Geometry geom) {

        if (geom instanceof org.locationtech.jts.geom.Polygon) {
            return new Polygon(
                    Stream.of(geom.getCoordinates()).map(coordinate ->
                            new LngLatAlt(coordinate.x, coordinate.y, coordinate.z)).collect(Collectors.toList())
            );
        }
        if (geom instanceof org.locationtech.jts.geom.MultiPolygon) {
            org.locationtech.jts.geom.Geometry firstPolygon = geom.getGeometryN(0);
            return new MultiPolygon(new Polygon(
                    Stream.of(firstPolygon.getCoordinates()).map(coordinate ->
                            new LngLatAlt(coordinate.x, coordinate.y, coordinate.z)).collect(Collectors.toList())
            ));
        }

        throw new SumarisTechnicalException(String.format("GeoJson conversion from %s not implement yet !", geom.getGeometryType()));
    }

    public static LngLatAlt jtsCoordinate(org.locationtech.jts.geom.Coordinate coordinate) {
        return new LngLatAlt(coordinate.x, coordinate.y, coordinate.z);
    }
}
