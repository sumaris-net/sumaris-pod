package net.sumaris.core.util;

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

import net.sumaris.core.util.type.ContentAwareHashMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class ContentAwareHashMapTest {

    @Test
    public void testContentAwareHashMap() {

        // Test with same map instance
        {   // Init a map with two elements
            Map<String, Integer> map1 = new ContentAwareHashMap<>();

            map1.put("1", 1);
            map1.put("2", 1);

            // Save the hashcode
            int hashcode1 = map1.hashCode();

            // Update the map with the same elements
            map1.put("1", 2);
            map1.put("2", 2);

            // Test if the hashcode is the same
            assertNotEquals("les hash sont pas pas identiques",
                    hashcode1, map1.hashCode());
        }
        // Test with to different maps, different content
        {
            // Init a map with two elements
            Map<String, Integer> map1 = new ContentAwareHashMap<>();
            Map<String, Integer> map2 = new ContentAwareHashMap<>();

            map1.put("1", 1);
            map1.put("2", 1);

            map2.put("1", 2);
            map2.put("2", 2);

            // Test if the hashcode is the same
            assertNotEquals("les hash sont pas pas identiques",
                    map1.hashCode(), map2.hashCode());

        }

        // Test with to different maps, same content
        {
            // Init a map with two elements
            Map<String, Integer> map1 = new ContentAwareHashMap<>();
            Map<String, Integer> map2 = new ContentAwareHashMap<>();

            map1.put("1", 1);
            map1.put("2", 1);

            map2.put("1", 1);
            map2.put("2", 1);

            // Test if the hashcode is the same
            assertEquals("les hash sont pas pas identiques",
                    map1.hashCode(), map2.hashCode());

        }
    }
}
