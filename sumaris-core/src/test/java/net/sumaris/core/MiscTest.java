package net.sumaris.core;

/*-
 * #%L
 * SUMARiS:: Core
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

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author peck7 on 30/10/2019.
 */
public class MiscTest {

    @Test
    public void testMapByRegEx() {

        Map<String, String> source = ImmutableMap.<String, String>builder()
                .put("sumaris.userProfile.ADMIN.label", "ALLEGRO_ADMINISTRATEUR")
                .put("sumaris.userProfile.USER.label", "ALLEGRO_UTILISATEUR")
                .put("sumaris.userProfile.SUPERVISOR.label", "ALLEGRO_SUPER_UTILISATEUR")
                .put("sumaris.userProfile.GUEST.label", "SIH_AUTRE")
                .build();

        Map<String, String> target = new HashMap<>();

        Pattern pattern = Pattern.compile("sumaris.userProfile.(\\w+).label");
        source.forEach((key, value) -> {
            Matcher matcher = pattern.matcher(key);
            if (matcher.find())
                target.put(matcher.group(1), value);
        });

        System.out.println(target);

    }
}
