/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.model.referential.spatial;

import com.google.common.base.Joiner;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.spatial.ExpertiseAreaVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExpertiseAreaUtils {

    protected ExpertiseAreaUtils() {
        // Helper class
    }

    public static String serialize(Collection<ExpertiseAreaVO> values) {
        return Beans.getStream(values)
            .map(item -> {
                if (item == null || StringUtils.isBlank(item.getName())) return null; // Skip if no name

                List<Integer> locationIds = Beans.collectIds(item.getLocations());
                if (CollectionUtils.isEmpty(locationIds)) return null; // Skip if no location

                return item.getName() + "|" + Joiner.on(";").join(locationIds);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));
    }
}
