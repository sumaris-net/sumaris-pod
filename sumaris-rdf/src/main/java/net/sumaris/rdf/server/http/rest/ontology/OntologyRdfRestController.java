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

package net.sumaris.rdf.server.http.rest.ontology;

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.ModelURIs;
import net.sumaris.rdf.core.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.core.service.RdfModelService;
import net.sumaris.rdf.core.service.data.RdfIndividualFetchOptions;
import net.sumaris.rdf.core.service.data.RdfIndividualService;
import net.sumaris.rdf.core.service.schema.RdfSchemaFetchOptions;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import net.sumaris.rdf.core.util.ModelUtils;
import net.sumaris.rdf.core.util.RdfFormat;
import net.sumaris.rdf.core.util.RdfMediaType;
import net.sumaris.rdf.server.http.rest.RdfRestPaths;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.rdf.model.Model;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Objects;


@RestController
@ConditionalOnBean({ RdfConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class OntologyRdfRestController implements RdfRestPaths {

    protected static final String EXTENSION_PATH_PARAM = ".{extension:[a-z0-9-_]+}";

    // Schema path
    public static final String SCHEMA_PATH = SCHEMA_BASE_PATH + "/" + "{vocab:[a-zA-Z]+}";
    public static final String SCHEMA_PATH_SLASH = SCHEMA_PATH + "/";
    public static final String SCHEMA_VERSION_PATH = SCHEMA_PATH_SLASH + "{version:[0-9.]+}";
    public static final String SCHEMA_VERSION_PATH_SLASH = SCHEMA_VERSION_PATH + "/";
    public static final String SCHEMA_VERSION_CLASS = SCHEMA_VERSION_PATH_SLASH + "{class:[a-zA-Z]+}";
    public static final String SCHEMA_VERSION_CLASS_SLASH = SCHEMA_VERSION_CLASS + "/";

    // Vocab conversion
    public static final String SCHEMA_CONVERT_PATH = SCHEMA_BASE_PATH + "/convert";

    // Data path
    public static final String INDIVIDUAL_PATH = DATA_BASE_PATH + "/" + "{vocab:[a-zA-Z]+}";
    public static final String INDIVIDUAL_PATH_SLASH = INDIVIDUAL_PATH + "/";
    public static final String INDIVIDUAL_CLASS_PATH = INDIVIDUAL_PATH_SLASH + "{class:[a-zA-Z]+}";
    public static final String INDIVIDUAL_CLASS_SLASH = INDIVIDUAL_CLASS_PATH + "/";
    public static final String INDIVIDUAL_CLASS_ID = INDIVIDUAL_CLASS_SLASH + "{id:[0-9a-zA-Z]+}";


    @Resource
    private RdfModelService rdfModelService;

    @Resource
    private RdfSchemaService rdfSchemaService;

    @Resource
    private RdfIndividualService rdfIndividualService;

    @Resource
    private RdfConfiguration rdfConfiguration;

    @PostConstruct
    public void init() {
        log.info("Starting Ontology endpoint {{}}...", SCHEMA_PATH_SLASH);
        log.info("Starting Ontology endpoint {{}}...", INDIVIDUAL_PATH_SLASH);
    }

    @GetMapping(
        value = {
            SCHEMA_PATH,
            SCHEMA_PATH + EXTENSION_PATH_PARAM,
            SCHEMA_PATH_SLASH,
            SCHEMA_VERSION_PATH,
            SCHEMA_VERSION_PATH + EXTENSION_PATH_PARAM,
            SCHEMA_VERSION_PATH_SLASH,
            SCHEMA_VERSION_CLASS,
            SCHEMA_VERSION_CLASS + EXTENSION_PATH_PARAM,
            SCHEMA_VERSION_CLASS_SLASH
        },
        produces = {
            RdfMediaType.APPLICATION_RDF_XML_VALUE,
            RdfMediaType.APPLICATION_XML_VALUE,
            RdfMediaType.APPLICATION_RDF_JSON_VALUE,
            RdfMediaType.APPLICATION_JSON_VALUE,
            RdfMediaType.APPLICATION_JSON_LD_VALUE,
            RdfMediaType.APPLICATION_N_TRIPLES_VALUE,
            RdfMediaType.APPLICATION_N_QUADS_VALUE,
            RdfMediaType.TEXT_N3_VALUE,
            RdfMediaType.APPLICATION_TRIG_VALUE,
            RdfMediaType.TEXT_TRIG_VALUE,
            RdfMediaType.APPLICATION_TRIX_VALUE,
            RdfMediaType.TEXT_TRIX_VALUE,
            RdfMediaType.APPLICATION_TURTLE_VALUE,
            RdfMediaType.TEXT_TURTLE_VALUE,
            // Browser HTML requests
            MediaType.APPLICATION_XHTML_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        })
    public ResponseEntity<byte[]> getSchemaOntology(@PathVariable(name = "vocab") String vocabulary,
                                                    @PathVariable(name = "version", required = false) String version,
                                                    @PathVariable(name = "class", required = false) String className,
                                                    @RequestParam(name = "extension", required = false) String extension,
                                                    @RequestParam(name = "format", required = false) String userFormat,
                                                    @RequestParam(name = "disjoints", defaultValue = "true", required = false) String disjoints,
                                                    @RequestParam(name = "equivalences", defaultValue = "true", required = false) String equivalences,
                                                    @RequestParam(name = "interfaces", defaultValue = "false", required = false) String interfaces,
                                                    final HttpServletRequest request) {

        // Find the output format
        RdfFormat outputFormat = null;
        if (StringUtils.isNotBlank(extension)) {
            outputFormat = RdfFormat.fromExtension(extension).orElse(null);
        }
        if (outputFormat == null) {
            outputFormat = findRdfFormat(request, userFormat, RdfFormat.RDFXML);
        }

        // Check version
        if (StringUtils.isBlank(version)) {
            version = rdfConfiguration.getModelVersion();
        }

        // Generate the schema ontology
        Model schema = rdfSchemaService.getOntology(RdfSchemaFetchOptions.builder()
            .vocabulary(vocabulary)
            .version(version)
            .className(className)
            .withDisjoints(!"false".equalsIgnoreCase(disjoints)) // True by default
            .withEquivalences(!"false".equalsIgnoreCase(equivalences))
            .withInterfaces(!"false".equalsIgnoreCase(interfaces))
            .build());

        return ResponseEntity.ok()
            .contentType(outputFormat.mineType())
            .body(ModelUtils.toBytes(schema, outputFormat));
    }

    @GetMapping(
        value = {
            INDIVIDUAL_CLASS_PATH,
            INDIVIDUAL_CLASS_PATH + EXTENSION_PATH_PARAM,
            INDIVIDUAL_CLASS_SLASH,
            INDIVIDUAL_CLASS_ID,
            INDIVIDUAL_CLASS_ID + EXTENSION_PATH_PARAM
        },
        produces = {
            RdfMediaType.APPLICATION_RDF_XML_VALUE,
            RdfMediaType.APPLICATION_XML_VALUE,
            RdfMediaType.APPLICATION_RDF_JSON_VALUE,
            RdfMediaType.APPLICATION_JSON_VALUE,
            RdfMediaType.APPLICATION_JSON_LD_VALUE,
            RdfMediaType.APPLICATION_N_TRIPLES_VALUE,
            RdfMediaType.APPLICATION_N_QUADS_VALUE,
            RdfMediaType.TEXT_N3_VALUE,
            RdfMediaType.APPLICATION_TRIG_VALUE,
            RdfMediaType.TEXT_TRIG_VALUE,
            RdfMediaType.APPLICATION_TRIX_VALUE,
            RdfMediaType.TEXT_TRIX_VALUE,
            RdfMediaType.APPLICATION_TURTLE_VALUE,
            RdfMediaType.TEXT_TURTLE_VALUE,
            // Browser HTML requests
            MediaType.APPLICATION_XHTML_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        })
    public ResponseEntity<byte[]> getIndividuals(@PathVariable(name = "vocab") String vocabulary,
                                                 @PathVariable(name = "class") String className,
                                                 @PathVariable(name = "id", required = false) String objectId,
                                                 @PathVariable(name = "extension", required = false) String extension,
                                                 @RequestParam(name = "schema", required = false) String schema,
                                                 @RequestParam(name = "format", required = false) String userFormat,
                                                 @RequestParam(name = "from", required = false, defaultValue = "0") int offset,
                                                 @RequestParam(name = "size", required = false, defaultValue = "100") int size,
                                                 final HttpServletRequest request) {

        RdfIndividualFetchOptions options = RdfIndividualFetchOptions.builder()
            .vocabulary(vocabulary)
            .className(className)
            .id(objectId)
            .maxDepth(1)
            .page(Page.builder()
                .offset(objectId == null ? offset : 0)
                .size(objectId == null ? size : 1)
                .build())
            .build();

        boolean withSchema = "".equalsIgnoreCase(schema) || "true".equalsIgnoreCase(schema);
        if (!withSchema) options.setReasoningLevel(ReasoningLevel.NONE);

        // Find the output format
        RdfFormat outputFormat = null;
        if (StringUtils.isNotBlank(extension)) {
            outputFormat = RdfFormat.fromExtension(extension).orElse(null);
        }
        if (outputFormat == null) {
            outputFormat = findRdfFormat(request, userFormat, RdfFormat.RDFXML);
        }

        // Get individuals
        Model individuals = rdfIndividualService.getIndividuals(options);

        // Add schema
        //individuals = ModelFactontology/convertry.createInfModel(ReasonerRegistry.getOWLReasoner(), schemaExportService.getOntology(className, extension, ));

        return ResponseEntity.ok()
            .contentType(outputFormat.mineType())
            .body(ModelUtils.toBytes(individuals, outputFormat));
    }

    @GetMapping(value = SCHEMA_CONVERT_PATH,
        produces = {
            MediaType.APPLICATION_XML_VALUE,
            RdfMediaType.APPLICATION_RDF_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_UTF8_VALUE,
            RdfMediaType.APPLICATION_RDF_JSON_VALUE,
            RdfMediaType.APPLICATION_JSON_LD_VALUE,
            RdfMediaType.APPLICATION_N_TRIPLES_VALUE,
            RdfMediaType.APPLICATION_N_QUADS_VALUE,
            RdfMediaType.TEXT_N3_VALUE,
            RdfMediaType.TEXT_TRIG_VALUE,
            RdfMediaType.TEXT_TRIX_VALUE,
            RdfMediaType.TEXT_TURTLE_VALUE
        })
    public ResponseEntity<byte[]> convertFromUri(@RequestParam(name = "uri", required = false) String uri,
                                                 @RequestParam(name = "prefix", required = false) String prefix,
                                                 @RequestParam(name = "sourceFormat", required = false) String sourceFormat,
                                                 @RequestParam(name = "format", required = false) String userFormat,
                                                 final HttpServletRequest request) {

        // Find the input format
        RdfFormat inputFormat = null;
        if (StringUtils.isNotBlank(sourceFormat)) {
            inputFormat = RdfFormat.fromUserString(sourceFormat)
                .orElseThrow(() -> new IllegalArgumentException("Unknown sourceFormat: " + sourceFormat));
        }

        // Find the output format
        RdfFormat outputFormat = findRdfFormat(request, userFormat, RdfFormat.RDFXML);

        String[] iris;
        if (StringUtils.isNotBlank(uri)) {
            // Split, to allow uri=<uri1>+<uri2>
            iris = uri.split("[,|+]");
        }
        else {
            if (StringUtils.isBlank(prefix)) throw new IllegalArgumentException("Required query parameters 'uri' or 'prefix'.");

            // Split prefix (allow prefix=<prefix1>+<prefix2>)
            String[] prefixes = prefix.split("[,|+]");
            iris = ModelURIs.getModelUrlByPrefix(prefixes).toArray(new String[0]);

            if (ArrayUtils.isEmpty(iris)) throw new IllegalArgumentException(String.format("Invalid prefix '%s'", prefix));
        }

        // Read then convert IRI models
        byte[] content = rdfModelService.unionThenConvert(iris, inputFormat, outputFormat);

        return ResponseEntity.ok()
            .contentType(outputFormat.mineType())
            .body(content);
    }

    /* -- protected methods -- */

    protected RdfFormat findRdfFormat(final HttpServletRequest request, @Nullable final String userFormat, @Nullable final RdfFormat defaultFormat) {
        if (StringUtils.isNotBlank(userFormat)) {
            return RdfFormat.fromUserString(userFormat)
                .orElseThrow(() -> new IllegalArgumentException("Unknown output format: " + userFormat));
        }

        // Analyse URL extension
        return RdfFormat.fromUrlExtension(request.getRequestURI())
            // Or analyse HTTP header 'Accept' content types
            .orElseGet(() -> {
                Collection<String> acceptedContentTypes = Splitter.on(",").trimResults().splitToList(request.getHeader(HttpHeaders.ACCEPT));
                return acceptedContentTypes.stream()
                    .map(contentType -> RdfFormat.fromContentType(contentType).orElse(null))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(defaultFormat);
            });
    }
}