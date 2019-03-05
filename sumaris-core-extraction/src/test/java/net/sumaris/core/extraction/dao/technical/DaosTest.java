package net.sumaris.core.extraction.dao.technical;

import net.sumaris.core.util.Dates;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class DaosTest {

    @Test
    public void getSqlToDate() {

        Date date = Dates.getFirstDayOfYear(2019);
        String sql = Daos.getSqlToDate(date);
        Assert.assertNotNull(sql);
        Assert.assertEquals("TO_DATE('2019-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS')", sql);
    }
}
