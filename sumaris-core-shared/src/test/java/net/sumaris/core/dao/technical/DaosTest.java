package net.sumaris.core.dao.technical;

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
