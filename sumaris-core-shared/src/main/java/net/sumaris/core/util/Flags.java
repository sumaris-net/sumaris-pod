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

public class Flags {
    protected Flags() {
        // Helper class : do not instantiate
    }

    public static int addFlag(int flags, int newFlag) {
        return flags | newFlag;
    }

    public static int removeFlag(int flags, int newFlag) {
        return flags - (flags & newFlag);
    }

    public static boolean hasFlag(int flags, int newFlag) {
        return (flags & newFlag) == newFlag;
    }

    public static boolean hasNotFlag(int flags, int aFlag) {
        return (flags & aFlag) == 0;
    }
}
