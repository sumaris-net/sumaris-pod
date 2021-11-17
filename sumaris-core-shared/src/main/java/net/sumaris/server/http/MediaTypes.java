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

package net.sumaris.server.http;

import lombok.NonNull;
import net.sumaris.core.util.StringUtils;
import org.springframework.http.MediaType;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import java.util.Optional;

public class MediaTypes {

    public static Optional<MediaType> parseMediaType(@NonNull String mimeType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(mimeType);
            return Optional.of(mediaType);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // abc.zip
    // abc.pdf,..
    public static Optional<MediaType> getMediaTypeForFileName(@NonNull ServletContext servletContext, @NonNull String fileName) {
        // application/pdf
        // application/xml
        // image/gif, ...
        String mineType = servletContext.getMimeType(fileName);
        if (StringUtils.isBlank(mineType)) return Optional.empty();
        return parseMediaType(mineType);
    }

    public static MediaType getMediaTypeForFileName(@NonNull ServletContext servletContext,
                                                    @NonNull String fileName,
                                                    @Nullable MediaType defaultValue) {
        return getMediaTypeForFileName(servletContext, fileName).orElse(defaultValue);
    }

}
