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

package net.sumaris.core.dao.technical.cache;

import lombok.NonNull;
import net.sumaris.core.util.StringUtils;

import javax.annotation.Nullable;
import java.time.Duration;

/**
 * Cache duration, in seconds
 */
public enum CacheTTL {


    DEFAULT(1500), // 25 min;

    SHORT(10 * 60), // 10 min
    MEDIUM(60 * 60), // 1 h
    LONG(12 * 60 * 60), // 12 h

    ETERNAL(24 * 60 * 60) // 1 day
    ;

    private Duration value;

    CacheTTL(int seconds) {
        this.value = Duration.ofSeconds(seconds);
    }

    public Duration asDuration() {
        return value;
    }

    public static CacheTTL fromString(@NonNull String name) {
        return CacheTTL.valueOf(name.toUpperCase());
    }

    public static CacheTTL nullToDefault(@Nullable CacheTTL ttl, CacheTTL defaultTtl) {
        return ttl != null ? ttl : defaultTtl;
    }

}