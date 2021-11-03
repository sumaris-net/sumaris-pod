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

package net.sumaris.server.http.rest;

public interface RestPaths {

    String BASE_PATH = "/api";

    String REGISTER_CONFIRM_PATH = BASE_PATH + "/confirmEmail";

    String PERSON_AVATAR_PATH = BASE_PATH + "/avatar/{pubkey}";

    String DEPARTMENT_LOGO_PATH = BASE_PATH + "/logo/{label}";

    String IMAGE_PATH = BASE_PATH + "/image/{id}";

    String DOWNLOAD_PATH = "/download";

    String FAVICON = BASE_PATH + "/favicon";

    String NODE_INFO_PATH = BASE_PATH + "/node/info";

}
