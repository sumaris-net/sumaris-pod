package net.sumaris.server.http.geojson;

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

    public static Geometry jtsGeometry(com.vividsolutions.jts.geom.Geometry geom) {

        if (geom instanceof com.vividsolutions.jts.geom.Polygon) {
            return new Polygon(
                    Stream.of(geom.getCoordinates()).map(coordinate ->
                            new LngLatAlt(coordinate.x, coordinate.y, coordinate.z)).collect(Collectors.toList())
            );
        }
        if (geom instanceof com.vividsolutions.jts.geom.MultiPolygon) {
            com.vividsolutions.jts.geom.Geometry firstPolygon = geom.getGeometryN(0);
            return new MultiPolygon(new Polygon(
                    Stream.of(firstPolygon.getCoordinates()).map(coordinate ->
                            new LngLatAlt(coordinate.x, coordinate.y, coordinate.z)).collect(Collectors.toList())
            ));
        }

        throw new SumarisTechnicalException(String.format("GeoJson conversion from %s not implement yet !", geom.getGeometryType()));
    }

    public static LngLatAlt jtsCoordinate(com.vividsolutions.jts.geom.Coordinate coordinate) {
        return new LngLatAlt(coordinate.x, coordinate.y, coordinate.z);
    }
}
