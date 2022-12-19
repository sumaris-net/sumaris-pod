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

package net.sumaris.extraction.server.http;

public interface ExtractionRestPaths {

    // Extensions
    String EXTENSION_PATH_PARAM = ".{extension:[a-z0-9-_]+}";
    String GEOJSON_EXTENSION = ".geojson";

    String BASE_PATH = "/api/extraction";

    // Get types paths
    String TYPES_PATH = BASE_PATH + "/types";

    // Download file paths
    String DOWNLOAD_BASE_PATH = BASE_PATH + "/download";
    String DOWNLOAD_PATH = DOWNLOAD_BASE_PATH + "/{label:[a-zA-Z0-9-_]+}";
    String DOWNLOAD_WITH_VERSION_PATH = DOWNLOAD_PATH + "/{version}";

    // Get documentation paths
    String DOC_BASE_PATH = BASE_PATH + "/doc";
    String DOC_PATH = DOC_BASE_PATH + "/{label:[a-zA-Z0-9-_]+}";
    String DOC_WITH_VERSION_PATH = DOC_PATH + "/{version}";


    // Product paths
    String PRODUCT_BASE_PATH = ExtractionRestPaths.BASE_PATH + "/product";
    String GEOJSON_LABEL_PATH = PRODUCT_BASE_PATH + "/{label:[a-zA-Z0-9-_]+}";
    String GEOJSON_LABEL_WITH_SPACE_PATH = GEOJSON_LABEL_PATH + "/{space}";



}
