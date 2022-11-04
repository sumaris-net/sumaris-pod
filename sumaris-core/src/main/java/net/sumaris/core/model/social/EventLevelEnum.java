package net.sumaris.core.model.social;

/*-
 * #%L
 * Quadrige3 Core :: Model Shared
 * %%
 * Copyright (C) 2017 - 2022 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.io.Serializable;
import java.util.Arrays;

public enum EventLevelEnum implements Serializable {

    ERROR("ERROR"),
    WARNING("WARNING"),
    INFO("INFO"),
    DEBUG("DEBUG");

    private final String id;

    EventLevelEnum(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static EventLevelEnum byId(final String id) {
        return Arrays.stream(values()).filter(enumValue -> enumValue.getId().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown EventLevelEnum: " + id));
    }

    public static EventLevelEnum byIdOrNull(final String id) {
        return Arrays.stream(values()).filter(enumValue -> enumValue.getId().equals(id)).findFirst().orElse(null);
    }

}
