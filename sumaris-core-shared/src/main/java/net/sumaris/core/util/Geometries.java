package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Geometries {

	public interface SRID {
		Integer NONE = 0;
		Integer WGS86 = 4326;
	}

	protected static final double EARTH_RADIUS = 6378288.0;
	protected static Pattern COORDINATES_PATTERN = Pattern.compile("([0-9]{4})([NS])[\\s\\xA0]+([0-9]{5})([EW])[\\s\\xA0]*");
	protected final static String WKT_POINT = "POINT(%s %s)";
	protected final static String WKT_MULTIPOINT_BASE = "MULTIPOINT(%s)";
	protected final static String WKT_MULTIPOINT_SUBPART = "(%s %s)";
	protected final static String WKT_POLYGON = "POLYGON((%s))";
	protected final static String WKT_POLYGON_SUBPART = "%s %s";
	protected final static String WKT_MULTIPOLYGON = "MULTIPOLYGON((%s))";
	protected final static String WKT_MULTIPOLYGON_SUBPART = "(%s)";

	private static final GeometryFactory geometryFactory = new GeometryFactory();

	/**
	 * <p>createPoint.</p>
	 *
	 * @param x a {@link Double} object.
	 * @param y a {@link Double} object.
	 * @return a {@link Point} object.
	 */
	public static Point createPoint(Double x, Double y) {
		return geometryFactory.createPoint(new Coordinate(x, y));
	}


	public static Point createPoint(Number longitudeX, Number latitudeY) {
		String wktString = String.format(WKT_POINT, longitudeX, latitudeY);
		return (Point) getGeometry(wktString);
	}

	/**
	 * <p>createLine.</p>
	 *
	 * @param minX a {@link Double} object.
	 * @param minY a {@link Double} object.
	 * @param maxX a {@link Double} object.
	 * @param maxY a {@link Double} object.
	 * @return a {@link LineString} object.
	 */
	public static LineString createLine(Double minX, Double minY, Double maxX, Double maxY) {
		return geometryFactory.createLineString(new Coordinate[]{
				new Coordinate(minX, minY),
				new Coordinate(maxX, maxY)});
	}

	/**
	 * <p>createPolygon.</p>
	 *
	 * @param minX a {@link Double} object.
	 * @param minY a {@link Double} object.
	 * @param maxX a {@link Double} object.
	 * @param maxY a {@link Double} object.
	 * @return a {@link Polygon} object.
	 */
	public static Polygon createPolygon(Double minX, Double minY, Double maxX, Double maxY) {
		return geometryFactory.createPolygon(new Coordinate[]{
				new Coordinate(minX, minY),
				new Coordinate(minX, maxY),
				new Coordinate(maxX, maxY),
				new Coordinate(maxX, minY),
				new Coordinate(minX, minY)});
	}

	/**
	 * <p>getGeometry.</p>
	 *
	 * @param wktString a {@link String} object.
	 * @return a {@link Geometry} object.
	 */
	public static Geometry getGeometry(String wktString) {
		try {
			return new WKTReader(geometryFactory).read(wktString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>getWKTString.</p>
	 *
	 * @param geometry a {@link Geometry} object.
	 * @return a {@link String} object.
	 */
	public static String getWKTString(Geometry geometry) {
		return new WKTWriter().write(geometry);
	}


	/**
	 * @deprecated  use getGeometry() instead
	 * @param wktGeometry
	 * @return
	 */
	@Deprecated
	public static Geometry wktToGeometry(String wktGeometry) {
		return getGeometry(wktGeometry);
	}


	public static MultiPoint createMultiPoint(Number... coords) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < coords.length; i = i + 2) {
			Number longitudeX = coords[i];
			Number latitudeY = coords[i];
			sb.append(", ");
			sb.append(String.format(WKT_MULTIPOINT_SUBPART, longitudeX, latitudeY));
		}

		String wktString = String.format(WKT_MULTIPOINT_BASE, sb.substring(2));
		Geometry geom = getGeometry(wktString);
		return (MultiPoint) geom;
	}

	public static Number getLatitudeFromCoordinates(String coordinates) throws java.text.ParseException {
		Preconditions.checkNotNull(coordinates);
		Matcher matcher = COORDINATES_PATTERN.matcher(StringUtils.trim(coordinates));
		if (!matcher.matches()) {
			throw new RuntimeException("Not a coordinate string: " + coordinates);
		}
		String latitudeString = matcher.group(1);
		String latitudeLetter = matcher.group(2);
		try {
			Number latitudeDegrees = NumberFormat.getInstance().parse(latitudeString.substring(0, 2));
			Number latitudeMinutes = NumberFormat.getInstance().parse(latitudeString.substring(2));
			double latitude = ("S".equals(latitudeLetter) ? -1 : 1) * ((double) (latitudeMinutes.doubleValue() / 60) + latitudeDegrees.doubleValue());
			return latitude;
		} catch (java.text.ParseException e) {
			throw new java.text.ParseException("Unable to convert coordinates in lat/long.", 0);
		}
	}

	public static Number getLongitudeFromCoordinates(String coordinates) throws java.text.ParseException {
		Preconditions.checkNotNull(coordinates);
		Matcher matcher = COORDINATES_PATTERN.matcher(coordinates.trim());
		if (!matcher.matches()) {
			throw new RuntimeException("Not a coordinate string:" + coordinates);
		}
		String longitudeString = matcher.group(3);
		String longitudeLetter = matcher.group(4);
		try {
			Number longitudeDegrees = NumberFormat.getInstance().parse(longitudeString.substring(0, 3));
			Number longitudeMinutes = NumberFormat.getInstance().parse(longitudeString.substring(3));
			double longitude = ("W".equals(longitudeLetter) ? -1 : 1)
					* ((double) (longitudeMinutes.doubleValue() / 60) + longitudeDegrees.doubleValue());
			return longitude;
		} catch (java.text.ParseException e) {
			throw new java.text.ParseException("Unable to convert coordinates in lat/long.", 0);
		}
	}

	/**
	 * Create a polygon from 2 points : bottom left, and top right
	 * 
	 * @param bottomLeftX
	 * @param bottomLeftY
	 * @param topRightX
	 * @param topRightY
	 * @return
	 */
	public static Geometry createRectangleGeometry(Number bottomLeftX, Number bottomLeftY, Number topRightX, Number topRightY,
			boolean returnHasMultiPolygon) {
		String wtkPoint1 = String.format(WKT_POLYGON_SUBPART, bottomLeftX, bottomLeftY);
		String wtkPoint2 = String.format(WKT_POLYGON_SUBPART, topRightX, bottomLeftY);
		String wtkPoint3 = String.format(WKT_POLYGON_SUBPART, topRightX, topRightY);
		String wtkPoint4 = String.format(WKT_POLYGON_SUBPART, bottomLeftX, topRightY);
		String wtkPolygon = wtkPoint1 + "," + wtkPoint2 + "," + wtkPoint3 + "," + wtkPoint4 + "," + wtkPoint1;
		String wktString;
		if (!returnHasMultiPolygon) {
			wktString = String.format(WKT_POLYGON, wtkPolygon);
		} else {
			wktString = String.format(WKT_MULTIPOLYGON, String.format(WKT_MULTIPOLYGON_SUBPART, wtkPolygon));
		}

		return getGeometry(wktString);
	}

	/**
	 * <p>getDistanceInMeters.</p>
	 *
	 * @param startLatitude a {@link Float} object.
	 * @param startLongitude a {@link Float} object.
	 * @param endLatitude a {@link Float} object.
	 * @param endLongitude a {@link Float} object.
	 * @return a int.
	 */
	public static int getDistanceInMeters(Number startLatitude,
										  Number startLongitude,
										  Number endLatitude,
										  Number endLongitude) {
		Preconditions.checkNotNull(startLatitude);
		Preconditions.checkNotNull(startLongitude);
		Preconditions.checkNotNull(endLatitude);
		Preconditions.checkNotNull(endLongitude);

		double sLat = startLatitude.doubleValue() * Math.PI / 180.0;
		double sLong = startLongitude.doubleValue() * Math.PI / 180.0;
		double eLat = endLatitude.doubleValue() * Math.PI / 180.0;
		double eLong = endLongitude.doubleValue() * Math.PI / 180.0;

		Double d = EARTH_RADIUS *
				(Math.PI / 2 - Math.asin(Math.sin(eLat) * Math.sin(sLat)
						+ Math.cos(eLong - sLong) * Math.cos(eLat) * Math.cos(sLat)));
		return d.intValue();
	}

	/**
	 * <p>getDistanceInMilles.</p>
	 *
	 * @param distance a {@link Float} object.
	 * @return a {@link String} object.
	 */
	public static String getDistanceInMilles(Number distance) {
		String distanceText;
		if (distance != null) {
			double distanceInMilles = distance.doubleValue() / 1852;
			distanceText = String.format("%.3d", distanceInMilles);

		} else {
			distanceText = "";
		}
		return distanceText;
	}


}
