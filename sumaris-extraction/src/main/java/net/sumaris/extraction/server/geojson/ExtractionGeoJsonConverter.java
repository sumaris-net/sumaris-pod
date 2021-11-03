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

import com.google.common.collect.Maps;
import lombok.NonNull;
import net.sumaris.core.dao.referential.location.Locations;
import net.sumaris.extraction.core.specification.data.trip.AggRdbSpecification;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Geometry;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Component("extractionGeoJsonConverter")
public class ExtractionGeoJsonConverter {

    private Map<String, Function<String, Geometry>> convertersByColumnNames = Maps.newConcurrentMap();

    public FeatureCollection toFeatureCollection(final ExtractionResultVO result,
                                                 @NonNull final String spatialColumnName) {
        return toFeatureCollection(result, spatialColumnName,
                findGeometryConverterByColumnName(spatialColumnName).orElse(null));
    }

    public FeatureCollection toFeatureCollection(@NonNull final ExtractionResultVO result,
                                                 final String spatialColumnName,
                                                 @NonNull final Function<String, Geometry> geometryConverter) {

        FeatureCollection features = new FeatureCollection();

        List<String> propertyNames = result.getColumns().stream()
                .map(ExtractionTableColumnVO::getColumnName)
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

            // Map space value to geometry
            if (geometryConverter != null) {
                String spaceValue = (String) properties.get(spatialColumnName);
                feature.setGeometry(geometryConverter.apply(spaceValue));
            }

            return feature;
        }).collect(Collectors.toList());

        features.setFeatures(rows);

        return features;
    }

    public Optional<Function<String, Geometry>> findGeometryConverterByColumnName(String spatialColumnName) {

        // Replace alias, and convert to lowercase
        spatialColumnName = AggRdbSpecification.resolveColumnName(spatialColumnName);

        // Already in the map: use it
        Function<String, Geometry> converter = convertersByColumnNames.get(spatialColumnName);
        if (converter != null) return Optional.of(converter);

        // Square 10'x10'
        if (AggRdbSpecification.COLUMN_SQUARE.equals(spatialColumnName)) {

            // Create converter
            converter = (value) -> {
                if (value == null) return null;
                org.locationtech.jts.geom.Geometry geom = Locations.getGeometryFromMinuteSquareLabel(value, 10, false);
                return GeoJsonGeometries.jtsGeometry(geom);
            };

            // Add to map, for the next time
            convertersByColumnNames.put(spatialColumnName, converter);

            return Optional.of(converter);
        }

        // Statistical rectangle (ICES or CGPM)
        else if (AggRdbSpecification.COLUMN_STATISTICAL_RECTANGLE.equals(spatialColumnName)) {

            // Create converter
            converter = (value) -> {
                if (value == null) return null;
                org.locationtech.jts.geom.Geometry geom = Locations.getGeometryFromRectangleLabel(value, false);
                return GeoJsonGeometries.jtsGeometry(geom);
            };

            // Add to map, for the next time
            convertersByColumnNames.put(spatialColumnName, converter);

            return Optional.of(converter);
        }

        return Optional.empty();
    }
}
