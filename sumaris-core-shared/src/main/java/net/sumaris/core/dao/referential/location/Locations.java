package net.sumaris.core.dao.referential.location;

/*
 * #%L
 * SIH-Adagio :: Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2014 Ifremer
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Geometries;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Location helper.</p>
 *
 * @author Tony Chemit (chemit@codelutin.com)
 * @author Benoit Lavenier (benoit.lavenier@e-is.pro)
 */
public class Locations {

    /**
     * <p>Constructor for LocationUtils.</p>
     */
    protected Locations() {
        // Should not be instantiate
    }

    /**
     * Return location label from a longitude and a latitude (in decimal degrees - WG84).
     *
     * @param latitude  a latitude (in decimal degrees - WG84)
     * @param longitude a longitude (in decimal degrees - WG84)
     * @return A label (corresponding to a statistical rectangle), or null if no statistical rectangle exists for this position
     */
    public static String getRectangleLabelByLatLong(Number latitude, Number longitude) {
        if (longitude == null || latitude == null) {
            return null;
        }
        String locationLabel = null;
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();

        // If position inside "Mediterranean and black sea" :
        if (((lon >= 0 && lon < 42) && (lat >= 30 && lat < 47.5))
                || ((lon >= -6 && lon < 0) && (lat >= 35 && lat < 40))) {

            // Number of rectangles, between the given latitude and 30°N :
            double nbdemidegreeLat = Math.floor((lat - 30)) * 2;

            // Number of rectangles, between the given longitude and 6°W :
            double nbdemidegreeLong = Math.floor((lon + 6)) * 2;

            // Letter change every 10 rectangles, starting with 'A' :
            char letter = (char) ((int) (Math.floor(nbdemidegreeLong / 10) + 65));
            int rest = (int) (nbdemidegreeLong % 10);
            locationLabel = "M" + ((int) nbdemidegreeLat) + letter + rest; //$NON-NLS-1$
        }

        // If position inside "Atlantic (nord-east)" :
        else if ((lon >= -50 && lon <= 70) && (lat >= 36 && lat <= 89)) {
            int halfDegreesNb = (int) Math.floor((lat - 36) * 2) + 1;
            double degreesNb = Math.floor(lon + 50);
            char letter = (char) ((int) (Math.floor(degreesNb / 10) + 65));
            int rest = (int) (degreesNb % 10);
            locationLabel = String.valueOf(halfDegreesNb) + letter + rest;
        }
        return locationLabel;
    }


    public static Geometry getGeometryFromRectangleLabel(String rectangleLabel) {
        return getGeometryFromRectangleLabel(rectangleLabel, false);
    }

    public static Geometry getGeometryFromRectangleLabel(String rectangleLabel, boolean useMultiPolygon) {
        Preconditions.checkNotNull(rectangleLabel);
        Preconditions.checkArgument(StringUtils.isNotBlank(rectangleLabel), "Argument 'rectangleLabel' must not be empty string.");

        Geometry geometry;

        // If rectangle inside "Mediterranean and black sea"
        if (rectangleLabel.startsWith("M")) {
            String rectangleLabelNoLetter = rectangleLabel.substring(1);
            String nbdemidegreeLat = rectangleLabelNoLetter.substring(0, 2);
            String letter = rectangleLabelNoLetter.substring(2, 3);
            String rest = rectangleLabelNoLetter.substring(3);

            double latitude = Double.parseDouble(nbdemidegreeLat) * 0.5f + 30;

            double longitude = Double.parseDouble(rest) * 0.5f + (letter.charAt(0) - 65) * 5 - 6f;
            geometry = Geometries.createRectangleGeometry(longitude, latitude, longitude + 0.5f, latitude + 0.5f, useMultiPolygon /*=MultiPolygon*/);
        }

        // If rectangle inside "Atlantic (nord-east)" :
        else {
            String nbdemidegreeLat = rectangleLabel.substring(0, 2);
            String letter = rectangleLabel.substring(2, 3);
            String rest = rectangleLabel.substring(3);

            // Special case for '102D0'
            if (rectangleLabel.length() == 5) {
                nbdemidegreeLat = rectangleLabel.substring(0, 3);
                letter = rectangleLabel.substring(3, 4);
                rest = rectangleLabel.substring(4);
            }

            double latitude = Double.parseDouble(nbdemidegreeLat) * 0.5f + 35.5f;
            double longitude = Double.parseDouble(rest) + (letter.charAt(0) - 65) * 10 - 50;

            geometry = Geometries.createRectangleGeometry(longitude, latitude, longitude + 1, latitude + 0.5f, useMultiPolygon /*=MultiPolygon*/);
        }

        return geometry;
    }

    /**
     * Compute the polygon from the 10 min x 10 min square label.
     *
     * @param square1010 10x10 minutes square label
     * @deprecated use getGeometryFromMinuteSquareLabel() instead
     */
    @Deprecated
    public static Geometry getGeometryFromSquare10Label(String square1010) {
        return getGeometryFromMinuteSquareLabel(square1010, 10, true);
    }

    public static Geometry getGeometryFromMinuteSquareLabel(String label, int minute, boolean useMultiPolygon) {
        Preconditions.checkNotNull(label);
        Preconditions.checkArgument(StringUtils.isNotBlank(label));
        if (minute < 10) {
            Preconditions.checkArgument(label.length() == 10, String.format("Invalid square format. Expected 10 characters for %s'x%s' square", minute, minute));
        }
        else {
            Preconditions.checkArgument(label.length() == 8, String.format("Invalid square format. Expected 8 characters for %s'x%s' square", minute, minute));
        }

        int cadran = Integer.parseInt(label.substring(0, 1));

        double signLongitude;
        double signLatitude;
        switch (cadran) {
            case 1: // NW
                signLatitude = 1.0;
                signLongitude = -1.0;
                break;
            case 2: // NE
                signLatitude = 1.0;
                signLongitude = 1.0;
                break;
            case 3: // SE
                signLatitude = -1.0;
                signLongitude = 1.0;
                break;
            case 4: // SW
                signLatitude = -1.0;
                signLongitude = -1.0;
                break;
            default:
                throw new IllegalArgumentException("Unable to parse quadrant");
        }

        int offset = 1;
        int nbMinuteChar = minute < 10 ? 2 : 1;

        /* Compute the latitude of the square */
        double intLatitude = Double.parseDouble(label.substring(offset, offset+2));
        offset += 2;
        double decLatitude = Double.parseDouble(label.substring(offset, offset + nbMinuteChar)) * minute / 60.0;
        offset += nbMinuteChar;
        double latitude = signLatitude * (intLatitude + decLatitude);

        /* Compute the longitude of the square */
        double intLongitude = Double.parseDouble(label.substring(offset, offset+3));
        offset += 3;
        double decLongitude = Double.parseDouble(label.substring(offset, offset+nbMinuteChar)) * minute / 60.0;
        double longitude = signLongitude * (intLongitude + decLongitude);

        return Geometries.createRectangleGeometry(
                longitude,
                latitude,
                longitude + signLongitude * minute / 60.0,
                latitude + signLatitude * minute / 60.0,
                useMultiPolygon /*=MultiPolygon*/);
    }

    /**
     * Compute the statistical rectangle from the 10x10 square.
     * (See doc: square_10.md)
     * @param squareLabel 10x10 square
     * @return null if invalid square label
     */
    public static String convertMinuteSquareToRectangle(final String squareLabel, final int minute) {
        if (squareLabel == null || squareLabel.length() != 8) {
            return null;
        }

        int cadran = Integer.parseInt(squareLabel.substring(0, 1));

        double signLongitude;
        double signLatitude;
        switch (cadran) {
            case 1: // NW
                signLatitude = 1.0;
                signLongitude = -1.0;
                break;
            case 2: // NE
                signLatitude = 1.0;
                signLongitude = 1.0;
                break;
            case 3: // SE
                signLatitude = -1.0;
                signLongitude = 1.0;
                break;
            case 4: // SW
                signLatitude = -1.0;
                signLongitude = -1.0;
                break;
            default:
                throw new IllegalArgumentException("Unable to parse quadrant");
        }

        int offset = 1;
        int nbMinuteChar = minute < 10 ? 2 : 1;
        /* Compute the latitude of the square */
        double intLatitude = Double.parseDouble(squareLabel.substring(offset, offset+2));
        offset += 2;
        double decLatitude = Double.parseDouble(squareLabel.substring(offset, offset + nbMinuteChar)) * minute / 60.0;
        offset += nbMinuteChar;
        double latitude = signLatitude * (intLatitude + decLatitude);

        /* Compute the longitude of the square */
        double intLongitude = Double.parseDouble(squareLabel.substring(offset, offset+3));
        offset += 3;
        double decLongitude = Double.parseDouble(squareLabel.substring(offset, offset + nbMinuteChar)) * minute / 60.0;
        double longitude = signLongitude * (intLongitude + decLongitude);

        return getRectangleLabelByLatLong(latitude, longitude);
    }

    /**
     * Compute the statistical rectangle from the 10x10 square.
     * (See doc: square_10.md)
     * @param square1010 10x10 square
     */
    public static String convertSquare10ToRectangle(final String square1010) {
        return convertMinuteSquareToRectangle(square1010, 10);
    }

    /**
     * Compute the list of suare 10'10' from a statistical rectangle
     *
     * @param rectangleLabel rectangle label
     */
    public static Set<String> convertRectangleToSquares10(final String rectangleLabel) {
        return convertRectangleToMinuteSquares(rectangleLabel, 10);
    }


    public static Set<String> getAllIcesRectangleLabels(ResourceLoader resourceLoader, boolean failSafe) {

        try {
            return readLines(resourceLoader.getResource("classpath:referential/ices_rectangles.txt"));
        } catch (SumarisTechnicalException e) {
            if (!failSafe) {
                throw e;
            }
        }
        // Fail safe: will generate all rectangle (not only in sea area !)
        Set<String> result = Sets.newHashSet();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 98; i++) {
            sb.setLength(0);
            sb.append(i < 10 ? ("0" + i) : i);
            for (char l = 'A'; l <= 'M'; l++) {
                sb.setLength(2);
                sb.append(l);
                for (int j = 0; j <= 8; j++) {
                    sb.setLength(3);
                    sb.append(j);
                    result.add(sb.toString());
                }
            }
        }

        return result;
    }

    public static Set<String> getAllCgpmGfcmRectangleLabels(ResourceLoader resourceLoader, boolean failSafe) {
        try {
            return readLines(resourceLoader.getResource("classpath:referential/cgpm_rectangles.txt"));
        } catch (SumarisTechnicalException e) {
            if (!failSafe) {
                throw e;
            }
        }

        // Fail safe: will generate all rectangle (not only in sea area !)
        Set<String> result = Sets.newHashSet();
        StringBuilder sb = new StringBuilder();
        sb.append("M");
        for (int i = 0; i <= 34; i++) {
            sb.setLength(1);
            sb.append(i < 10 ? ("0" + i) : i);
            for (char l = 'A'; l <= 'J'; l++) {
                sb.setLength(3);
                sb.append(l);
                for (int j = 0; j <= 8; j++) {
                    sb.setLength(4);
                    sb.append(j);
                    result.add(sb.toString());
                }
            }
        }

        return result;
    }

    /**
     * Return location label (square 10'x10' format) from a longitude and a latitude (in decimal degrees - WG84).
     *
     * @param latitude  a latitude (in decimal degrees - WG84)
     * @param longitude a longitude (in decimal degrees - WG84)
     * @return A label
     */
    public static String getSquare10LabelByLatLong(Number latitude, Number longitude) {
        return getMinuteSquareLabelByLatLong(latitude, longitude, 10);
    }

    /**
     * Return square label from a longitude and a latitude (in decimal degrees - WG84).
     *
     * @param latitude  a latitude (in decimal degrees - WG84)
     * @param longitude a longitude (in decimal degrees - WG84)
     * @param squareSize Size of the square, in minutes
     * @return A label
     */
    public static String getMinuteSquareLabelByLatLong(Number latitude, Number longitude, int squareSize) {
        if (longitude == null || latitude == null) {
            return null;
        }
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();
        StringBuilder result = new StringBuilder();

        // Quadrant
        int quadrant;
        if (lon <= 0 && lat >= 0) {
            quadrant = 1;
        }
        else if (lon > 0 && lat > 0) {
            quadrant = 2;
        } else if (lon > 0 && lat < 0) {
            quadrant = 3;
        } else {
            quadrant = 4;
        }
        result.append(quadrant);

        // Latitude
        lat = Math.abs(lat);
        int intLatitude = (int)Math.floor(lat);
        int decLatitude = (int)Math.floor((lat - intLatitude) * 60 / squareSize);
        result.append(Strings.padStart(String.valueOf(intLatitude), 2, '0'));
        if (squareSize >= 10) {
            // minute in one character
            result.append(decLatitude);
        }
        else {
            // minute in two character
            result.append(Strings.padStart(String.valueOf(decLatitude), 2, '0'));
        }

        // Longitude
        lon = Math.abs(lon);
        int intLongitude = (int)Math.floor(lon);
        int decLongitude = (int)Math.floor((lon - intLongitude) * 60 / squareSize);
        result.append(Strings.padStart(String.valueOf(intLongitude), 3, '0'));
        if (squareSize >= 10) {
            // minute in one character
            result.append(decLongitude);
        }
        else {
            // minute in two character
            result.append(Strings.padStart(String.valueOf(decLongitude), 2, '0'));
        }

        return result.toString();
    }

    public static Set<String> getAllSquare10Labels(ResourceLoader resourceLoader, boolean failSafe) {
        return ImmutableSet.<String>builder()
                .addAll(getAllIcesRectangleLabels(resourceLoader, failSafe))
                .addAll(getAllCgpmGfcmRectangleLabels(resourceLoader, failSafe))
                .build().stream()
                .flatMap(label -> Locations.convertRectangleToSquares10(label).stream())
                .collect(Collectors.toSet());
    }

    /* -- private methods -- */

    public static Set<String> convertRectangleToMinuteSquares(final String rectangleLabel, final int minute) {
        Preconditions.checkNotNull(rectangleLabel);
        Preconditions.checkArgument(StringUtils.isNotBlank(rectangleLabel), "Argument 'rectangleLabel' must not be empty string.");

        Geometry geom = getGeometryFromRectangleLabel(rectangleLabel, false);

        if (geom == null) return null;

        Coordinate startPoint = geom.getCoordinates()[0];
        Coordinate endPoint = geom.getCoordinates()[2];

        Set<String> result = Sets.newHashSet();

        for (double longitude = startPoint.x; longitude < endPoint.x; longitude += minute/60.) {
            for (double latitude = startPoint.y; latitude < endPoint.y; latitude += minute/60.) {
                String label = getSquare10LabelByLatLong(latitude, longitude);
                result.add(label);
            }
        }

        return result;
    }

    private static Set<String> readLines(Resource resource) {
        Preconditions.checkNotNull(resource);
        Preconditions.checkArgument(resource.exists());
        Set<String> result = Sets.newHashSet();
        try {
            InputStream is = resource.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                if (StringUtils.isNotBlank(line)) {
                    result.add(line.trim());
                }
                line = reader.readLine();
            }
            return result;
        } catch (IOException e) {
            throw new SumarisTechnicalException("Could not read resource: " + resource.getFilename(), e);
        }
    }


}
