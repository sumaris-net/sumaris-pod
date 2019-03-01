package net.sumaris.core.dao.referential.location;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

}
