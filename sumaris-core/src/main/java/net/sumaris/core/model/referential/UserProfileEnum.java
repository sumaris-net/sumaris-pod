package net.sumaris.core.model.referential;

/*-
 * #%L
 * SUMARiS:: Core
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

import java.util.Arrays;
import java.util.Optional;

public enum UserProfileEnum {

  ADMIN(1, "ADMIN"),
  USER(2, "USER"),
  SUPERVISOR(3, "SUPERVISOR"),
  GUEST(4, "GUEST");

  public final int id;
  public final String label;

  UserProfileEnum(int id, String label) {
      this.id = id;
      this.label = label;
  }

  public static String byId(int id) {
      Optional<UserProfileEnum> enumValue = Arrays.stream(values()).filter(userProfileEnum -> userProfileEnum.id == id).findFirst();
      return enumValue.isPresent() ? enumValue.toString() : null;
  }
}
