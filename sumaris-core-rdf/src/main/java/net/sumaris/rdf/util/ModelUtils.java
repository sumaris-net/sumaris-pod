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

import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.http.rest.RdfFormat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.JenaException;

import javax.annotation.Nullable;
import java.io.*;

public class ModelUtils {



    protected ModelUtils() {
        // Helper class
    }


    /**
     * Convert to Jena format, and use XML/RDF by default
     * @param userFormat
     * @return
     */
    public static String toJenaFormat(String userFormat) {
        if (StringUtils.isBlank(userFormat)) {
            return RdfFormat.RDF.toJenaFormat();
        }
        try {
            return RdfFormat.fromUserString(userFormat).toJenaFormat();
        }
        catch(IllegalArgumentException e) {
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
    public static String modelToString(Model model, RdfFormat format) {
        return modelToString(model, format.name());
    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static String modelToString(Model model, @Nullable String format) {

        format = toJenaFormat(format);

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

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static void writeModel(Model model, @Nullable String format, OutputStream os) {

        format = toJenaFormat(format);

        // Special case for VOWL format
        if ("VOWL".equalsIgnoreCase(format)) {
            try (
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            ) {
                model.write(bos);
                bos.flush();

                String jsonString = new Owl2Vowl(new ByteArrayInputStream(bos.toByteArray())).getJsonAsString();
                os.write(jsonString.getBytes("UTF8"));
            } catch (Exception e) {
                throw new SumarisTechnicalException("Error when writing model into VOWL", e);
            }
        }

        // Other case
        else {
            try (final BufferedOutputStream bos = new BufferedOutputStream(os)) {
                if (format == null) {
                    model.write(bos);
                } else {
                    model.write(bos, format);
                }
                bos.flush();
            } catch (IOException e) {
                throw new SumarisTechnicalException("Error when writing model to stream: " + e.getMessage(), e);
            }
        }
    }

    /**
     *
     * @param iri The IRI to parse
     * @param format input format if null then output to RDF/XML
     * @return a ontology model
     */
    public static Model readModel(String iri, RdfFormat format) {
        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(iri, format.toJenaFormat());
            return model;
        } catch(JenaException e) {
            throw new SumarisTechnicalException("Cannot parse model from IRI: " + iri, e);
        }
    }

    /**
     *
     * @param iri The IRI to parse
     * @param format input format if null then output to RDF/XML
     * @return a ontology model
     */
    public static Model readModel(String iri, @Nullable  String format) {
        format = toJenaFormat(format);

        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(iri, format);
            return model;
        } catch(JenaException e) {
            throw new SumarisTechnicalException("Cannot parse model from IRI: " + iri, e);
        }
    }

    /**
     *
     * @param is The Input model
     * @param format input format if null then output to RDF/XML
     * @return a ontology model
     */
    public static Model readModel(InputStream is, @Nullable  RdfFormat format) {
        format = format != null ? format : RdfFormat.RDF;

        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(is, format.toJenaFormat());
            return model;
        } catch(JenaException e) {
            throw new SumarisTechnicalException("Read model error: " + e.getMessage(), e);
        }
    }
}
