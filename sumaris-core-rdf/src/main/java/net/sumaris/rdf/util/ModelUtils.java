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

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.google.common.base.Preconditions;
import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.model.reasoner.ReasoningLevel;
import net.sumaris.server.http.rest.RdfFormat;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.apache.jena.vocabulary.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

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
        return RdfFormat.fromUserString(userFormat)
                .map(RdfFormat::toJenaFormat)
                .orElse(userFormat.toUpperCase());
    }
    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static String modelToString(Model model, RdfFormat format) {
        return modelToString(model, format.getLabel());
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
     * Serialize model into byte array, using the expected format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a byte array representation of the model
     */
    public static byte[] modelToBytes(Model model, RdfFormat format) {
        return modelToBytes(model, format.getName());
    }

    /**
     * Serialize model into byte array, using the expected format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a byte array representation of the model
     */
    public static byte[] modelToBytes(Model model, @Nullable String format) {

        format = toJenaFormat(format);

        // Special case for VOWL format
        if ("VOWL".equalsIgnoreCase(format)) {
            try (
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            ) {
                model.write(bos);
                bos.flush();

                return new Owl2Vowl(new ByteArrayInputStream(bos.toByteArray())).getJsonAsString().getBytes();
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
                return os.toByteArray();
            } catch (IOException e) {
                throw new SumarisTechnicalException("Unable to serialize model to string: " + e.getMessage(), e);
            }
        }
    }
    /**
     * Serialize model into a file, using the expected format
     *
     * @param file  output file
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     */
    public static void modelToFile(File file, Model model, @Nullable String format) {
        modelToFile(file, model, RdfFormat.fromUserString(format).orElse(null));
    }

    /**
     * Serialize model into a file, using the expected format
     *
     * @param file  output file
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     */
    public static void modelToFile(File file, Model model, @Nullable RdfFormat format) {

        // Special case for VOWL format
        if (format == RdfFormat.VOWL) {
            try (
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            ) {
                model.write(bos);
                bos.flush();

                new Owl2Vowl(new ByteArrayInputStream(bos.toByteArray())).writeToFile(file);
            } catch (Exception e) {
                throw new SumarisTechnicalException("Error converting model into VOWL", e);
            }
        }

        // Other case
        else {
            try (FileOutputStream fos = new FileOutputStream(file);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);) {
                if (format == null) {
                    model.write(bos);
                } else {
                    model.write(bos, format.toJenaFormat());
                }
                bos.flush();
            } catch (IOException e) {
                throw new SumarisTechnicalException("Unable to serialize model to file: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Serialize model into byte array, using the expected format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a byte array representation of the model
     */
    public static byte[] datasetToBytes(Dataset dataset, RdfFormat format) {
        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            if (format == null) {
                RDFDataMgr.write(os, dataset, RdfFormat.TURTLE);
            } else {
                RDFDataMgr.write(os, dataset, format);
            }
            os.flush();
            os.close();
            return os.toByteArray();
        } catch (IOException e) {
            throw new SumarisTechnicalException("Unable to serialize model to string: " + e.getMessage(), e);
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
    public static Model loadModelByUri(String iri, RdfFormat format) {

        // Special case for OWL file format (not supported by Jena)
        if (format == RdfFormat.OWL) {
            try {
                OntologyManager ontManager = OntManagers.createONT();
                Ontology onto = ontManager.loadOntology(IRI.create(iri));
                com.github.owlcs.ontapi.jena.model.OntModel ontModel = onto.asGraphModel();
                //ontModel.write(System.out);
                return ontModel;
            } catch(OWLOntologyCreationException e) {
                throw new SumarisTechnicalException("Cannot parse model from IRI: " + iri, e);
            }
        }

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
    public static Model loadModelByUri(String iri, @Nullable  String format) {
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

    public static OntModel createOntologyModel(String prefix, String namespace, ReasoningLevel reasoningLevel) {
        return createOntologyModel(prefix, namespace, reasoningLevel, null);
    }

    public static OntModel createOntologyModel(String prefix, String namespace, ReasoningLevel reasoningLevel, Model schema) {

        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(namespace);
        Preconditions.checkNotNull(reasoningLevel);

        OntModel ontology;
        switch (reasoningLevel) {
            case NONE:
                ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
                break;
            case RDFS:
                ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF);
                if (schema != null) {
                    ontology.getReasoner().bindSchema(schema.getGraph());
                }
                break;
            case OWL:
            default:
                Reasoner reasoner = ReasonerRegistry.getOWLMicroReasoner();
                InfModel infModel = schema != null ?
                        ModelFactory.createInfModel(reasoner, schema, ModelFactory.createDefaultModel()) :
                        ModelFactory.createInfModel(reasoner, ModelFactory.createDefaultModel());
                ontology = ModelFactory.createOntologyModel(
                        OntModelSpec.OWL_MEM_MICRO_RULE_INF
                        //OntModelSpec.OWL_DL_MEM
                        , infModel);
        }

        ontology.setNsPrefix(prefix, namespace);

        ontology.setNsPrefix("rdf", RDF.getURI());
        ontology.setNsPrefix("rdfs", RDFS.getURI());
        ontology.setNsPrefix("xsd", XSD.getURI());
        ontology.setNsPrefix("owl", OWL.getURI());

        ontology.setNsPrefix("dc", DC.getURI()); // http://purl.org/dc/elements/1.1/
        ontology.setNsPrefix("dcterms", DCTerms.getURI()); // http://purl.org/dc/terms/

        ontology.setStrictMode(true);

        return ontology;
    }
}
