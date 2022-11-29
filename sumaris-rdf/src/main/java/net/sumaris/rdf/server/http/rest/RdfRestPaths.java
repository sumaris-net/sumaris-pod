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

package net.sumaris.rdf.server.http.rest;

public interface RdfRestPaths {
    String SCHEMA = "schema"; // Should be same as ModelType.SCHEMA (but in lowercase);
    String DATA = "data"; // Should be same as ModelType.DATA (but in lowercase);

    String SCHEMA_BASE_PATH = "/" + SCHEMA;
    String DATA_BASE_PATH = "/" + DATA;

    String SCHEMA_FILES_BASE_PATH = SCHEMA_BASE_PATH + "/files";

    String SPARQL_ENDPOINT = "/sparql";
    String WEBVOWL_BASE_PATH = "/webvowl";

    String WEBVOWL_DATA_FILE_PATH = WEBVOWL_BASE_PATH + "/data/{prefix}.{format}";
}
