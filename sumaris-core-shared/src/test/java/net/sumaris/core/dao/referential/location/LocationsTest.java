package net.sumaris.core.dao.referential.location;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class LocationsTest {


	@Autowired
	ResourceLoader resourceLoader;

	@Test
	public void getRectangleLabelByLatLong() {
		// Check label with a position inside the Atlantic sea
		String label = Locations.getRectangleLabelByLatLong(47.6f, -5.05f);
		assertEquals("24E4", label);

		// Check label with a position inside the Mediterranean sea
		label = Locations.getRectangleLabelByLatLong(42.27f, 5.4f);
		assertEquals("M24C2", label);
	}

	@Test
	public void getGeometryFromRectangleLabel() {
		Geometry geom;
		Coordinate[] coords;

		geom = Locations.getGeometryFromRectangleLabel("27E8");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) -2, coords[0].x, 0);
		assertEquals((double) 49, coords[0].y, 0);

		geom = Locations.getGeometryFromRectangleLabel("24E4");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) -6, coords[0].x, 0);
		assertEquals((double) 47.5, coords[0].y, 0);

		geom = Locations.getGeometryFromRectangleLabel("M24C1");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) 4.5, coords[0].x, 0);
		assertEquals((double) 42, coords[0].y, 0);

		geom = Locations.getGeometryFromRectangleLabel("M14F2");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) 20, coords[0].x, 0);
		assertEquals((double) 37, coords[0].y, 0);
	}

	@Test
	public void getAllIcesRectangleLabels() {
		Set<String> labels = Locations.getAllIcesRectangleLabels(resourceLoader, false);
		assertNotNull(labels);
		assertEquals(labels.size(), 7246);
	}

	@Test
	public void getAllCgpmGfcmRectangleLabels() {
		Set<String> labels = Locations.getAllCgpmGfcmRectangleLabels(resourceLoader, false);
		assertNotNull(labels);
		assertEquals(1545, labels.size());
	}

	@Test
	public void convertSquareToRectangle() {
		// Check ICES rectangle(NW quadrant)
		String rectangleLabel = Locations.convertSquare10ToRectangle("14250013");// 42°5'N 1°3'W
		assertNotNull(rectangleLabel);
		assertEquals("14E8", rectangleLabel);

		// Check ICES rectangle (NE quadrant)
		rectangleLabel = Locations.convertSquare10ToRectangle("25140033");
		assertNotNull(rectangleLabel);
		assertEquals("32F3", rectangleLabel);

		// Check CGPM rectangle (NE quadrant)
		rectangleLabel = Locations.convertSquare10ToRectangle("24200051");
		assertNotNull(rectangleLabel);
		assertEquals("M24C2", rectangleLabel);

	}

	@Test
	public void convertRectangleToSquares10() {
		{
			Set<String> squares = Locations.convertRectangleToSquares10("14E1");
			assertNotNull(squares);
			assertEquals(21, squares.size());
			assertTrue(squares.contains("14230090"));
		}

		{
			Set<String> squares = Locations.convertRectangleToSquares10("M24C2");
			assertNotNull(squares);
			assertEquals(9 /* 3x3 */, squares.size());
			assertTrue(squares.contains("24200051"));
		}

	}

	@Test
	public void getSquare10LabelByLatLong() {
		{
			String squareLabel = Locations.getSquare10LabelByLatLong(35.3, 3.6);
			assertNotNull(squareLabel);
			assertEquals("23510033", squareLabel);
		}

		{
			String rectLabel = "14E1";
			Geometry geom = Locations.getGeometryFromRectangleLabel(rectLabel);
			String squareLabel = Locations.getSquare10LabelByLatLong(geom.getCoordinate().y, geom.getCoordinate().x);
			assertNotNull(squareLabel);
			assertEquals("14230090", squareLabel);

			// Inverse conversion
			String convertedRectLabel = Locations.convertSquare10ToRectangle(squareLabel);
			assertNotNull(squareLabel);
			assertEquals(rectLabel, convertedRectLabel);
		}
	}

	@Test
	public void getAllSquare10Labels() {

		Set<String> squares = Locations.getAllSquare10Labels(resourceLoader, true);
		assertNotNull(squares);
		assertTrue(squares.size() > 0);
		assertTrue(squares.contains("14230090"));
		assertTrue(squares.contains("25140033"));
		assertTrue(squares.contains("24200051"));
		assertEquals(138501, squares.size());
	}

	@Test
	public void getGeometryFromSquare10Label() {

		Double lat = 49.9d;
		Double lon = 0.01d;

		String square10Label = Locations.getSquare10LabelByLatLong(lat,lon);
		Assert.assertEquals("24950000", square10Label);

		// Check other case
		Geometry geometry = Locations.getGeometryFromSquare10Label("24950000");
		assertNotNull(geometry);

		Coordinate startPoint = geometry.getCoordinates()[0];
		Assert.assertTrue(startPoint.x <= lon);
		Assert.assertTrue(startPoint.y <= lat);

		Coordinate endPoint = geometry.getCoordinates()[2];
		Assert.assertTrue(endPoint.x >= lon);
		Assert.assertTrue(endPoint.y >= lat);

	}
}
