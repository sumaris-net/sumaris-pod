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

package net.sumaris.rdf.util;

import com.google.common.base.Preconditions;
import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import org.apache.jena.rdf.model.Model;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ModelUtils {

    protected ModelUtils() {
        // Helper class
    }


    public static String toModelWriteFormat(String userFormat) {
        if (StringUtils.isBlank(userFormat)) {
            return "RDF/XML";
        }

        switch(userFormat.toLowerCase()) {
            case "rdf":
                return "RDF/XML";
            case "json":
                return "RDF/JSON";
            case "n3":
                return "N3";
            case "trig":
                return "TriG";
            case "trix":
                return "TriX";
            case "jsonld":
            case "json-ld":
                return "JSON-LD";
            case "ttl":
                return "TURTLE";
            default:
                return userFormat.toUpperCase();
        }
    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static String modelToString(Model model, String format) {

        format = toModelWriteFormat(format);

        // Special case for VOWL format
        if ("VOWL".equalsIgnoreCase(format)) {
            try (
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            ) {
                model.write(bos);
                bos.flush();

                return new Owl2Vowl(new ByteArrayInputStream(bos.toByteArray())).getJsonAsString();
            } catch (Exception e) {
                throw new SumarisTechnicalException("Error converting model into VOWL", e);
            }
        }

        // Other case
        else {
            try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                if (format == null) {
                    model.write(os);
                } else {
                    model.write(os, format);
                }
                os.flush();
                os.close();
                return new String(os.toByteArray(), "UTF8");
            } catch (IOException e) {
                throw new SumarisTechnicalException("Unable to serialize model to string: " + e.getMessage(), e);
            }
        }
    }
}
