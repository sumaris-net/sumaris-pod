package net.sumaris.server.http.graphql.data;

/*-
 * #%L
 * Sumaris3 Core :: Server
 * %%
 * Copyright (C) 2017 - 2020 Ifremer
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.DatabaseResource;
import net.sumaris.server.http.graphql.AbstractGraphQLServiceTest;
import net.sumaris.server.util.security.AuthTokenVO;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

@Slf4j
public class DataGraphQLServiceTest extends AbstractGraphQLServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void findAllActivityCalendars() {

         Object list = getResponse("activityCalendars", Object.class);

    }

}
