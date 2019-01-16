package net.sumaris.core.dao.technical.geometry;

import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeometryHelper {

	protected static Pattern COORDINATES_PATTERN = Pattern.compile("([0-9]{4})([NS])[\\s\\xA0]+([0-9]{5})([EW])[\\s\\xA0]*");
	protected final static String WKT_POINT = "POINT(%s %s)";
	protected final static String WKT_MULTIPOINT_BASE = "MULTIPOINT(%s)";
	protected final static String WKT_MULTIPOINT_SUBPART = "(%s %s)";
	protected final static String WKT_POLYGON = "POLYGON((%s))";
	protected final static String WKT_POLYGON_SUBPART = "%s %s";
	protected final static String WKT_MULTIPOLYGON = "MULTIPOLYGON((%s))";
	protected final static String WKT_MULTIPOLYGON_SUBPART = "(%s)";

	public static Geometry wktToGeometry(String wktPoint) {
		WKTReader fromText = new WKTReader();
		Geometry geom = null;
		try {
			geom = fromText.read(wktPoint);
			geom.setSRID(4326);
		} catch (ParseException e) {
			throw new RuntimeException("Not a WKT string: " + wktPoint);
		}
		return geom;
	}

	public static Point createPoint(Number longitudeX, Number latitudeY) {
		String wktString = String.format(WKT_POINT, longitudeX, latitudeY);
		Geometry geom = wktToGeometry(wktString);
		return (Point) geom;
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
		Geometry geom = wktToGeometry(wktString);
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
	 * Compute the statistical rectangle from the 10x10 square.
	 *
	 * @param square1010    10x10 square
	 */
	public static String convertSquareToRectangle(final String square1010) {
		String calculRectangle = "";

		if (square1010 == null || square1010.length() != 8) {
			return calculRectangle;
		}

		String cadran = square1010.substring(0, 1);
		double signLongitude = 1.0;
		double signLatitude = 1.0;

		if (cadran.equals("1")) {
			signLongitude = -1.0;
		} else if (cadran.equals("3")) {
			signLongitude = -1.0;
		} else if (cadran.equals("4")) {
			signLongitude = -1.0;
			signLatitude = -1.0;
		}

		/* Compute the longitude of the square */
		double intLongitude = Double.parseDouble(square1010.substring(4, 7));
		double decLongitude = Double.parseDouble(square1010.substring(7, 8)) * 10.0 / 60.0 + 0.01;
		double longitude = signLongitude * (intLongitude + decLongitude);

		/* Compute the latitude of the square */
		double intLatitude = Double.parseDouble(square1010.substring(1, 3));
		double decLatitude = Double.parseDouble(square1010.substring(3, 4)) * 10.0 / 60.0 + 0.01;
		double latitude = signLatitude * (intLatitude + decLatitude);

		/* Compute the rectangle from the position :
		 * longitude must be between -50 and +70 and latitude must be between +36 and +89 */
		if (longitude >= -50 && longitude <= 70 && latitude >= 36 && latitude <= 89) {
			int nbdemidegre = (int) (Math.floor((latitude - 36) * 2) + 1);
			double nbdegre = Math.floor(longitude + 50);
			char lettre = (char) (Math.floor(nbdegre / 10) + 65);
			int reste = (int) (nbdegre % 10);

			calculRectangle = (nbdemidegre < 10 ? "0" : "") + nbdemidegre + "" + lettre + "" + reste;
		}

		return calculRectangle;
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
		String wktString = null;
		if (!returnHasMultiPolygon) {
			wktString = String.format(WKT_POLYGON, wtkPolygon);
		} else {
			wktString = String.format(WKT_MULTIPOLYGON, String.format(WKT_MULTIPOLYGON_SUBPART, wtkPolygon));
		}

		Geometry geom = wktToGeometry(wktString);
		return geom;
	}

	/**
	 * Return location label from a longitude and a latitude (in decimal degrees - WG84).
	 * 
	 * @param latitude a latitude (in decimal degrees - WG84)
	 * @param longitude a longitude (in decimal degrees - WG84)
	 * @return A location label (corresponding to a statistical rectangle), or null if no statistical rectangle exists for this position
	 */
	public static String getRectangleLabelByLatLong(Float latitude, Float longitude) {
		if (longitude == null || latitude == null) {
			return null;
		}
		String locationLabel = null;

		// If position inside "Mediterranean and black sea" :
		if (((longitude >= 0 && longitude < 42) && (latitude >= 30 && latitude < 47.5))
				|| ((longitude >= -6 && longitude < 0) && (latitude >= 35 && latitude < 40))) {

			// Number of rectangles, between the given latitude and 30°N :
			double nbdemidegreeLat = Math.floor((latitude - 30)) * 2;

			// Number of rectangles, between the given longitude and 6°W :
			double nbdemidegreeLong = Math.floor((longitude + 6)) * 2;

			// Letter change every 10 rectangles, starting with 'A' :
			char letter = new Character((char) ((int) (Math.floor(nbdemidegreeLong / 10) + 65))).charValue();
			int rest = (int) (nbdemidegreeLong % 10);
			locationLabel = "M" + String.valueOf((int) nbdemidegreeLat) + letter + String.valueOf(rest); //$NON-NLS-1$
		}

		// If position inside "Atlantic (nord-east)" :
		else if ((longitude >= -50 && longitude <= 70) && (latitude >= 36 && latitude <= 89)) {
			int halfDegreesNb = (int) Math.floor((latitude - 36) * 2) + 1;
			double degreesNb = Math.floor(longitude + 50);
			char letter = new Character((char) ((int) (Math.floor(degreesNb / 10) + 65))).charValue();
			int rest = (int) (degreesNb % 10);
			locationLabel = String.valueOf(halfDegreesNb) + letter + String.valueOf(rest);
		}
		return locationLabel;
	}

	public static Geometry computeGeometryFromRectangleLabel(String rectangleLabel) {
		Preconditions.checkNotNull(rectangleLabel);
		Preconditions.checkArgument(StringUtils.isNotBlank(rectangleLabel), "Argument 'rectangleLabel' must not be empty string.");

		Geometry geometry = null;

		// If rectangle inside "Mediterranean and black sea"
		if (rectangleLabel.startsWith("M")) {
			String rectangleLabelNoLetter = rectangleLabel.substring(1);
			String nbdemidegreeLat = rectangleLabelNoLetter.substring(0, 2);
			String letter = rectangleLabelNoLetter.substring(2, 3);
			String rest = rectangleLabelNoLetter.substring(3);

			double latitude = Double.parseDouble(nbdemidegreeLat) * 0.5f + 30;

			double longitude = Double.parseDouble(rest) * 0.5f + (letter.charAt(0) - 65) * 5 - 6f;

			geometry = GeometryHelper.createRectangleGeometry(longitude, latitude, longitude + 0.5f, latitude + 0.5f, true /*=MultiPolygon*/);
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

			geometry = GeometryHelper.createRectangleGeometry(longitude, latitude, longitude + 1, latitude + 0.5f, true /*=MultiPolygon*/);
		}

		return geometry;
	}

	/**
	 * Compute the polygon from the 10x10 square label.
	 *
	 * @param square1010    10x10 square label
	 */
	public static Geometry computeGeometryFromSquareLabel(String square1010) {
		Preconditions.checkNotNull(square1010);
		Preconditions.checkArgument(StringUtils.isNotBlank(square1010));
		Preconditions.checkArgument(square1010.length() == 8);

		String cadran = square1010.substring(0, 1);
		double signLongitude = 1.0;
		double signLatitude = 1.0;

		if (cadran.equals("1")) {
			signLongitude = -1.0;
		} else if (cadran.equals("3")) {
			signLongitude = -1.0;
		} else if (cadran.equals("4")) {
			signLongitude = -1.0;
			signLatitude = -1.0;
		}

		/* Compute the longitude of the square */
		double intLongitude = Double.parseDouble(square1010.substring(4, 7));
		double decLongitude = Double.parseDouble(square1010.substring(7, 8)) * 10.0 / 60.;
		double longitude = signLongitude * (intLongitude + decLongitude);

		/* Compute the latitude of the square */
		double intLatitude = Double.parseDouble(square1010.substring(1, 3));
		double decLatitude = Double.parseDouble(square1010.substring(3, 4)) * 10.0 / 60.0;
		double latitude = signLatitude * (intLatitude + decLatitude);

		return GeometryHelper.createRectangleGeometry(
				longitude,
				latitude,
				longitude + signLongitude * 10 / 60.0,
				latitude + signLatitude * 10.0 / 60.0,
				true /*=MultiPolygon*/);
	}

}
