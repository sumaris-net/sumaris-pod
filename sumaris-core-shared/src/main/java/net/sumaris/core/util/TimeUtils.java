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

package net.sumaris.core.util;

import java.time.Duration;
import java.util.Date;

public class TimeUtils {

    protected TimeUtils() {
    }

    /**
     * Human readable format of a period of time<br/>
     * Example:<ul>
     *     <li>'0.150s'</li>
     *     <li>'2m 0.150s'</li>
     *     <li>'1h 2m 0.150s'</li>
     * </ul>
     *
     * @param durationInMs period of time in millisecond
     * @return pretty print
     */
    public static String printDuration(long durationInMs) {
        return Duration.ofMillis(durationInMs).toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();
    }

    /**
     * Human readable format of a period from a given date
     *
     * @param millis period of time in millisecond
     * @return pretty print
     */
    public static String printDurationFrom(long time) {
        return printDuration(System.currentTimeMillis() - time);
    }

    /**
     * Human readable format of a period from a given date
     * @param date
     * @return pretty print
     */
    public static String printDurationFrom(Date date) {
        return printDuration(System.currentTimeMillis() - date.getTime());
    }
}
