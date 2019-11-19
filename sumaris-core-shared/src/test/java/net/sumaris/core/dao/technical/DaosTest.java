package net.sumaris.core.dao.technical;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author peck7 on 11/10/2019.
 */
public class DaosTest {

    @Test
    public void getDbms() {
        Assert.assertEquals("hsqldb", Daos.getDbms("jdbc:hsqldb:hsql://localhost/sumaris"));
        Assert.assertEquals("oracle", Daos.getDbms("jdbc:oracle:thin:@localhost:1523/orcl"));
        Assert.assertEquals("postgresql", Daos.getDbms("jdbc:postgresql://localhost:5432/quadrige"));
    }
}