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

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.sumaris.server.exception.InvalidPathException;

public interface RestPaths {

    String BASE_API_PATH = "/api";

    String REGISTER_CONFIRM_PATH = BASE_API_PATH + "/confirmEmail";

    String PERSON_AVATAR_PATH = BASE_API_PATH + "/avatar/{pubkey}";

    String DEPARTMENT_LOGO_PATH = BASE_API_PATH + "/logo/{label}";

    String IMAGE_PATH = BASE_API_PATH + "/image/{id}";

    String FAVICON = BASE_API_PATH + "/favicon";

    String NODE_INFO_PATH = BASE_API_PATH + "/node/info";

    String NODE_HEALTH_PATH = BASE_API_PATH + "/node/health";

    String DOWNLOAD_PATH = "/download";

    String UPLOAD_PATH = "/upload";

    String APP_SHARE_PATH = "/share/{uuid:[a-zA-Z0-9-_$.]+}";
    String APP_EMAIL_CONFIRM_PATH = "/confirm/{email}/{code}";


    static void checkSecuredPath(String path) throws InvalidPathException {
        if (!isSecuredPath(path)) throw new InvalidPathException("Invalid path: " + path);
    }

    static boolean isSecuredPath(@NonNull String path) throws InvalidPathException {
        Preconditions.checkArgument(path.trim().length() > 0);
        // Check if the file's name contains invalid characters
        return !path.contains("..");
    }
}
