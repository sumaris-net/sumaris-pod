package net.sumaris.server.http.geojson.extraction;

/*-
 * #%L
 * SUMARiS:: Server
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

import net.sumaris.core.dao.referential.location.Locations;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;
import net.sumaris.server.http.geojson.GeoJsonGeometries;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class GeoJsonExtractions {

    protected GeoJsonExtractions() {
        // Helper class
    }

    public static FeatureCollection toFeatureCollection(ExtractionResultVO result, String spaceColumnName) {

        Function<String, Geometry> mapper = null;
        if ("square".equalsIgnoreCase(spaceColumnName)) {
            mapper = (value) -> {
                if (value == null) return null;
                org.locationtech.jts.geom.Geometry geom = Locations.getGeometryFromMinuteSquareLabel(value, 10, false);
                return GeoJsonGeometries.jtsGeometry(geom);
            };
        }
        else if ("statistical_rectangle".equals(spaceColumnName) || "rect".equalsIgnoreCase(spaceColumnName)){
            mapper = (value) -> {
                if (value == null) return null;
                org.locationtech.jts.geom.Geometry geom = Locations.getGeometryFromRectangleLabel(value, false);
                return GeoJsonGeometries.jtsGeometry(geom);
            };
        }

        FeatureCollection features = new FeatureCollection();

        final Function<String, Geometry> finalMapper = mapper;
        List<String> propertyNames = result.getColumns().stream()
                .map(ExtractionProductColumnVO::getColumnName)
                //.map(StringUtils::underscoreToChangeCase)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        List<Feature> rows = Beans.getStream(result.getRows()).map(row -> {
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
            if (finalMapper != null) {
                String spaceValue = (String) properties.get(spaceColumnName);
                feature.setGeometry(finalMapper.apply(spaceValue));
            }

            return feature;
        }).collect(Collectors.toList());

        features.setFeatures(rows);

        return features;

    }
}
