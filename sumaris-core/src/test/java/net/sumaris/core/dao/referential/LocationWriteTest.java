package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.location.LocationAreaDao;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.model.referential.location.LocationArea;
import net.sumaris.core.util.Geometries;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author peck7 on 15/10/2019.
 */
public class LocationWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();
//    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private LocationAreaDao locationAreaDao;

    @Test
    public void testGeometry() {

        LocationArea area = new LocationArea();
        area.setId(1);
        area.setLocation(locationDao.get(1)); // France
        area.setPosition(Geometries.createPoint(-55,20));
        locationAreaDao.saveAndFlush(area);
    }
}
