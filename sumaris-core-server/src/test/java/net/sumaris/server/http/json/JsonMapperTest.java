package net.sumaris.server.http.json;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.server.service.BaseServiceTest;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.Date;

public class JsonMapperTest extends BaseServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testTripSerialization() throws JsonProcessingException, UnsupportedEncodingException {
        TripVO trip = new TripVO();
        trip.setUpdateDate(new Timestamp(System.currentTimeMillis()));

        String json = new String(objectMapper.writeValueAsBytes(trip), "UTF8");
        Assert.assertNotNull(json);
    }
}
