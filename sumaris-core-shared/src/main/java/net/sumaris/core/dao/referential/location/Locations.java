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
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import net.sumaris.core.util.Geometries;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.util.Set;

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
	 * @param latitude a latitude (in decimal degrees - WG84)
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
			geometry = Geometries.createRectangleGeometry(longitude, latitude, longitude + 0.5f, latitude + 0.5f, true /*=MultiPolygon*/);
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

			geometry = Geometries.createRectangleGeometry(longitude, latitude, longitude + 1, latitude + 0.5f, true /*=MultiPolygon*/);
		}

		return geometry;
	}

	/**
	 * Compute the polygon from the 10x10 square label.
	 *
	 * @param square1010    10x10 square label
	 */
	public static Geometry getGeometryFromSquareLabel(String square1010) {
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

		return Geometries.createRectangleGeometry(
				longitude,
				latitude,
				longitude + signLongitude * 10 / 60.0,
				latitude + signLatitude * 10.0 / 60.0,
				true /*=MultiPolygon*/);
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


	public static Set<String> getAllIcesRectangleLabels(ResourceLoader resourceLoader, boolean failSafe) {

		try {
			return readLines(resourceLoader.getResource("classpath:referential/ices_rectangles.txt"));
		}
		catch(SumarisTechnicalException e) {
			if (failSafe) {
				// continue
			}
			else {
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
		}
		catch(SumarisTechnicalException e) {
			if (failSafe) {
				// continue
			}
			else {
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

	/* -- private methods -- */

	private static Set<String> readLines(Resource resource){
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
		}
		catch(IOException e) {
			throw new SumarisTechnicalException("Could not read resource: " + resource.getFilename(), e);
		}
	}
}
