package net.sumaris.server.http.geojson.extraction;

import net.sumaris.core.dao.referential.location.Locations;
import net.sumaris.core.extraction.vo.ExtractionColumnMetadataVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.http.geojson.GeoJsonGeometries;
import org.geojson.Feature;
import org.geojson.FeatureCollection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class GeoJsonExtractions {

    protected GeoJsonExtractions() {
        // Helper class
    }

    public static FeatureCollection toFeatureCollection(ExtractionResultVO result) {

        FeatureCollection features = new FeatureCollection();

        List<String> propertyNames = result.getColumns().stream()
                .map(ExtractionColumnMetadataVO::getName)
                //.map(StringUtils::underscoreToChangeCase)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        List<Feature> rows = result.getRows().stream().map(row -> {
            Feature feature = new Feature();
            Map<String, Object> properties = new HashMap<>();
            int index = 0;
            for (String property: propertyNames) {
                Object value = row[index++];
                if (value != null) {
                    properties.put(property, value);
                }
            }
            feature.setProperties(properties);

            // Geometry
            com.vividsolutions.jts.geom.Geometry geom = null;
            String square = (String)properties.get("square");
            if (square != null) {
                geom = Locations.getGeometryFromMinuteSquareLabel(square, 10, false);
                feature.setGeometry(GeoJsonGeometries.jtsGeometry(geom));
            }
            else {
                String rectangle = (String)properties.get("statisticalRectangle");
                if (rectangle != null) {
                    geom = Locations.getGeometryFromRectangleLabel(rectangle, false);
                    feature.setGeometry(GeoJsonGeometries.jtsGeometry(geom));
                }
            }

            return feature;
        }).collect(Collectors.toList());

        features.setFeatures(rows);

        return features;

    }
}
