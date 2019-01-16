package net.sumaris.core.dao.technical.geometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GeometryHelperTest {

	@Test
	public void getRectangleLabelByLatLong() {
		// Check label with a position inside the Atlantic sea
		String label = GeometryHelper.getRectangleLabelByLatLong(47.6f, -5.05f);
		assertEquals("24E4", label);

		// Check label with a position inside the Mediterranean sea
		label = GeometryHelper.getRectangleLabelByLatLong(42.27f, 5.4f);
		assertEquals("M24C2", label);
	}

	@Test
	public void computeGeometryFromRectangleLocationLabel() {
		Geometry geom = null;
		Coordinate[] coords = null;

		geom = GeometryHelper.computeGeometryFromRectangleLabel("27E8");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) -2, coords[0].x, 0);
		assertEquals((double) 49, coords[0].y, 0);

		geom = GeometryHelper.computeGeometryFromRectangleLabel("24E4");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) -6, coords[0].x, 0);
		assertEquals((double) 47.5, coords[0].y, 0);

		geom = GeometryHelper.computeGeometryFromRectangleLabel("M24C1");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) 4.5, coords[0].x, 0);
		assertEquals((double) 42, coords[0].y, 0);

		geom = GeometryHelper.computeGeometryFromRectangleLabel("M14F2");
		assertNotNull(geom);
		coords = geom.getCoordinates();
		assertNotNull(coords);
		assertEquals((double) 20, coords[0].x, 0);
		assertEquals((double) 37, coords[0].y, 0);
	}

}
