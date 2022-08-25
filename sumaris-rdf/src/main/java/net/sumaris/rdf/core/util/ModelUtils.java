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

package net.sumaris.rdf.core.util;

import com.google.common.base.Preconditions;
import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.reasoner.ReasoningLevel;
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
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nullable;
import java.io.*;
import java.util.function.Function;

public class ModelUtils {

    protected ModelUtils() {
        // Helper class
    }

    public static boolean isNotEmpty(Model m) {
        return m != null && !m.isEmpty();
    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static String toString(Model model, @Nullable String format) {
        return toString(model, toRdfFormat(format));
    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static String toString(Model model, @Nullable RdfFormat format) {
        try {
            byte[] bytes = toBytes(model, format);
            return new String(bytes, "UTF8");
        } catch (IOException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /**
     * Serialize model into byte array, using the expected format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a byte array representation of the model
     */
    public static byte[] toBytes(Model model, String format) {
        return toBytes(model, toRdfFormat(format));
    }

    /**
     * Serialize model into a file, using the expected format
     *
     * @param file  output file
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     */
    public static void write(Model model, @Nullable String format, File file) throws IOException {
        write(model, toRdfFormat(format), file);
    }

    /**
     * Serialize model into a file, using the expected format
     *
     * @param file  output file
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     */
    public static void write(Model model, @Nullable RdfFormat format, File file) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            write(model, format, os);
        }
    }


    /**
     * Serialize model into byte array, using the expected format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a byte array representation of the model
     */
    public static byte[] toBytes(Model model, @Nullable RdfFormat format) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(1024)) {
            write(model, format, bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SumarisTechnicalException("Unable to serialize model: " + e.getMessage(), e);
        }
    }

    /**
     * Serialize model into byte array, using the expected format
     *
     * @param dataset  input dataset
     * @param format output format if null then output to RDF/XML
     * @return a byte array representation of the model
     */
    public static byte[] toBytes(Dataset dataset, RdfFormat format) {
        Preconditions.checkArgument(format == RdfFormat.TRIG || format == RdfFormat.NTRIPLES, "Only TRIG and NTRIPLES format are allowed");

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            write(dataset, format, os);
            return os.toByteArray();
        } catch (IOException e) {
            throw new SumarisTechnicalException("Unable to serialize model: " + e.getMessage(), e);
        }
    }

    public static void write(Dataset dataset, RdfFormat format, OutputStream os) {
        if (format == null) {
            format = RdfFormat.TRIG;
        }
        else {
            Preconditions.checkArgument(format == RdfFormat.TRIG || format == RdfFormat.NTRIPLES, "Only TRIG and NTRIPLES format are allowed");
        }

        try (final BufferedOutputStream bos = new BufferedOutputStream(os)) {
            RDFDataMgr.write(os, dataset, format);
            bos.flush();
            os.flush();
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
    public static void write(Model model, @Nullable String format, OutputStream os) {
        write(model, toRdfFormat(format), os);
    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static void write(Model model, @Nullable RdfFormat format, OutputStream os) {

        try (final BufferedOutputStream bos = new BufferedOutputStream(os)) {

            // OWL
            if (format == RdfFormat.OWL) {
                writeOwl(model, bos);
            }

            // VOWL
            else if (format == RdfFormat.VOWL) {
                writeVowl(model, bos);
            }

            // Other Jena format
            else {
                if (format == null) {
                    model.write(bos);
                }
                else {
                    model.write(bos, format.toJenaFormat());
                }
            }

            bos.flush();
            os.flush();
        } catch (IOException e) {
            throw new SumarisTechnicalException("Error when writing model to stream: " + e.getMessage(), e);
        }
    }

    /**
     *
     * @param iri The IRI to parse
     * @param format input format if null then output to RDF/XML
     * @return a ontology model
     */
    public static Model read(String iri, @Nullable String format) {
        return read(iri, toRdfFormat(format));
    }

    /**
     *
     * @param iri The IRI to parse
     * @param format input format if null then output to RDF/XML
     * @return a ontology model
     */
    public static Model read(String iri, RdfFormat format) {

        // Try to get format, by the extension
        if (format == null) {
            format = RdfFormat.fromUrlExtension(iri).orElse(null);
        }

        // OWL
        if (format == RdfFormat.OWL) {
            return readOwl(iri);
        }

        // VOWL
        else if (format == RdfFormat.VOWL) {
            throw new IllegalArgumentException("Reading VOWL model format is not supported");
        }

        // Other Jena format
        try {
            Model model = ModelFactory.createDefaultModel();
            if (format == null) {
                model.read(iri);
            }
            else {
                model.read(iri, format.toJenaFormat());
            }

            return model;
        } catch (JenaException e) {
            throw new SumarisTechnicalException("Cannot parse model from IRI: " + iri, e);
        }
    }

    /**
     *
     * @param file The Input model file
     * @param format input format if null then output to RDF/XML
     * @return a ontology model
     */
    public static Model read(File file, @Nullable  RdfFormat format) {
        // If missing, try to get format from file extension
        if (format == null) {
            format = RdfFormat.fromUrlExtension(file.getName()).orElse(null);
        }

        try (FileInputStream is = new FileInputStream(file)) {
            return read(is, format);
        }
        catch(IOException e) {
            throw new SumarisTechnicalException("Cannot parse model file:" + file.getAbsolutePath(), e);
        }
    }

    /**
     *
     * @param is The Input model
     * @param format input format if null then output to RDF/XML
     * @return a ontology model
     */
    public static Model read(InputStream is, @Nullable  RdfFormat format) {

        // OWL
        if (format == RdfFormat.OWL) {
            return readOwl(is);
        }

        // VOWL
        else if (format == RdfFormat.VOWL) {
            throw new IllegalArgumentException("Reading VOWL model format is not supported");
        }

        try {
            Model model = ModelFactory.createDefaultModel();
            if (format == null) {
                model.read(is, RdfFormat.RDFXML.toJenaFormat());
            }
            else {
                model.read(is, format.toJenaFormat());
            }
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

    /* -- protected functions -- */

    /**
     * Convert RDF format, and use XML/RDF by default
     * @param userFormat
     * @return
     */
    protected static RdfFormat toRdfFormat(String userFormat) {
        if (StringUtils.isBlank(userFormat)) {
            return RdfFormat.RDFXML;
        }
        return RdfFormat.fromUserString(userFormat)
            .orElse(RdfFormat.RDFXML);
    }


    protected static Model readOwl(String iri) {

        String rdfFilenameFromUri = iri.replaceAll("(http|https)", "")
            .replaceAll("[:]+", "")
            .replaceAll("[/]+", "_")
            .replace("^[_]+", "")
            .replace(".owl$", ".rdf");
        File pivotRdfFile = new File(RdfConfiguration.instance().getRdfDirectory(), rdfFilenameFromUri);

        // Reuse the pivot file if exists
        if (pivotRdfFile.exists()) {
            return read(pivotRdfFile, RdfFormat.RDFXML);
        }

        // Read OWL using loader, and the given pivot file
        // Next conversion will be able to reuse the pivot file
        return readOwl(manager -> {
            try {
                return manager.loadOntologyFromOntologyDocument(IRI.create(iri));
            } catch(Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }, pivotRdfFile);
    }

    protected static Model readOwl(InputStream is) {
        return readOwl(manager -> {
            try {
                return manager.loadOntologyFromOntologyDocument(is);
            } catch(Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }, null);
    }

    protected static Model readOwl(Function<OWLOntologyManager, OWLOntology> loader, File pivotRdfFile) {

        RdfConfiguration config = RdfConfiguration.instance();

        try {
            // Init the pivot file, if need
            pivotRdfFile = pivotRdfFile != null ? pivotRdfFile :
                File.createTempFile("owl2rdf", "tmp", config.getRdfDirectory());
            if (pivotRdfFile.exists() && !pivotRdfFile.delete()) {
                throw new SumarisTechnicalException("Pivot RDF file already exists, and cannot be deleted!");
            }

            // Read OWL and write as RDF/XML
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = loader.apply(manager);
            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), IRI.create(pivotRdfFile));
            manager.removeOntology(ontology);

            // Read pivot RDF file
            return read(pivotRdfFile, RdfFormat.RDFXML);

        } catch (OWLException | IOException e) {
            throw new SumarisTechnicalException("Cannot parse OWL model", e);
        }
    }

    public static void writeOwl(Model model, OutputStream os) throws IOException {

        File tempRdfFile = null;
        try {
            tempRdfFile = Files.createTempFile("rdf2owl",
                RdfConfiguration.instance().getTempDirectory());

            // Write OWL
            write(model, RdfFormat.RDFXML, tempRdfFile);

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(tempRdfFile);
            manager.saveOntology(ontology, new OWLXMLDocumentFormat(), os);
            manager.removeOntology(ontology);
        }
        catch (OWLException e) {
            throw new IOException(e);
        }
        finally {
            if (tempRdfFile != null && tempRdfFile.exists()) {
                Files.deleteQuietly(tempRdfFile);
            }
        }
    }

    protected static void writeVowl(Model model, OutputStream os) throws IOException {
        Owl2Vowl converter = createOwl2Vowl(model);
        os.write(converter.getJsonAsString().getBytes("UTF8"));
    }


    protected static Owl2Vowl createOwl2Vowl(Model model) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(1024)) {
            // Dump model as RDF/XML
            model.write(bos, RdfFormat.RDFXML.toJenaFormat());
            bos.flush();

            // Then create the converter
            return new Owl2Vowl(new ByteArrayInputStream(bos.toByteArray()));
        } catch (Exception e) {
            throw new SumarisTechnicalException("Error when writing model into VOWL", e);
        }
    }
}
