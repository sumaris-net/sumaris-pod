package net.sumaris.core.service.data;

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.TripVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author peck7 on 06/12/2018.
 */
public class TripServiceQualityTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TripService service;

    @Test
    public void control() {

        TripVO trip = service.get(dbResource.getFixtures().getTripId(0));
        Assert.assertNotNull(trip);

        // Make sure control date is NOT set
        if (trip.getControlDate() != null) {
            trip.setControlDate(null);
            service.save(trip, false);
            trip = service.get(dbResource.getFixtures().getTripId(0));
            Assert.assertNotNull(trip);
        }

        Assert.assertNull(trip.getControlDate());

        trip = service.control(trip);

        Assert.assertNotNull(trip.getControlDate());

    }

    @Test
    public void validate() {

        TripVO trip = service.get(dbResource.getFixtures().getTripId(0));
        Assert.assertNotNull(trip);

        trip.setControlDate(new Date());
        Assert.assertNull(trip.getValidationDate());

        trip = service.validate(trip);

        Assert.assertNotNull(trip.getValidationDate());

    }

    @Test
    public void unvalidate() {

        TripVO trip = service.get(dbResource.getFixtures().getTripId(0));
        Assert.assertNotNull(trip);

        trip.setControlDate(new Date());
        trip.setValidationDate(new Date());

        trip = service.unvalidate(trip);

        Assert.assertNull(trip.getValidationDate());

    }


}
