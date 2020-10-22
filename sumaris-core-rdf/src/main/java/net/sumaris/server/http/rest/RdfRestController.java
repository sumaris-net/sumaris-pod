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

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.ModelURIs;
import net.sumaris.rdf.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.service.RdfModelService;
import net.sumaris.rdf.service.data.RdfDataExportOptions;
import net.sumaris.rdf.service.data.RdfDataExportService;
import net.sumaris.rdf.service.schema.RdfSchemaOptions;
import net.sumaris.rdf.service.schema.RdfSchemaService;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.rdf.util.RdfFormat;
import net.sumaris.rdf.util.RdfMediaType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.rdf.model.Model;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Objects;


@RestController
@ConditionalOnBean({WebMvcConfigurer.class, RdfConfiguration.class})
@Slf4j
public class RdfRestController {

    protected static final String EXTENSION_PATH_PARAM = ".{extension:[a-z0-9-_]+}";

    // Schema path
    public static final String ONTOLOGY_PATH = "/ontology";
    public static final String SCHEMA_PATH = ONTOLOGY_PATH + "/schema";
    public static final String SCHEMA_PATH_SLASH = SCHEMA_PATH + "/";
    public static final String SCHEMA_BY_CLASS = SCHEMA_PATH_SLASH + "{class:[a-zA-Z]+}";
    public static final String SCHEMA_BY_CLASS_SLASH = SCHEMA_BY_CLASS + "/";

    // Data path
    public static final String DATA_PATH = ONTOLOGY_PATH + "/data";
    public static final String DATA_SLASH_PATH = DATA_PATH + "/";
    public static final String DATA_BY_CLASS_PATH = DATA_SLASH_PATH + "{class:[a-zA-Z]+}";
    public static final String DATA_BY_CLASS_SLASH_PATH = DATA_BY_CLASS_PATH + "/";
    public static final String DATA_BY_OBJECT_PATH = DATA_BY_CLASS_SLASH_PATH + "{id:[0-9a-zA-Z]+}";


    public static final String CONVERT_PATH = ONTOLOGY_PATH + "/convert";

    @Resource
    private RdfModelService  modelService;

    @Resource
    private RdfSchemaService schemaExportService;

    @Resource
    private RdfDataExportService dataExportService;

    @Resource
    private RdfConfiguration config;


    @PostConstruct
    public void init() {
        log.info("Starting Ontology endpoint {{}}...", SCHEMA_PATH_SLASH);
        log.info("Starting Ontology endpoint {{}}...", DATA_SLASH_PATH);
    }

    @GetMapping(
            value = {
                    SCHEMA_PATH,
                    SCHEMA_PATH + EXTENSION_PATH_PARAM,
                    SCHEMA_PATH_SLASH,
                    SCHEMA_BY_CLASS,
                    SCHEMA_BY_CLASS + EXTENSION_PATH_PARAM,
                    SCHEMA_BY_CLASS_SLASH
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
    public ResponseEntity<byte[]> getSchemaOntology(@PathVariable(name = "class", required = false) String className,
                                                    @RequestParam(name = "extension", required = false) String extension,
                                                    @RequestParam(name = "format", required = false) String userFormat,
                                                    @RequestParam(name = "disjoints", defaultValue = "true", required = false) String disjoints,
                                                    @RequestParam(name = "equivalences", defaultValue = "true", required = false) String equivalences,
                                                    final HttpServletRequest request) {

        // Find output format
        RdfFormat format = findRdfFormat(request, userFormat, RdfFormat.RDFXML);

        // Generate the schema ontology
        Model schema = schemaExportService.getOntology(RdfSchemaOptions.builder()
                .className(className)
                .withDisjoints(!"false".equalsIgnoreCase(disjoints)) // True by default
                .withEquivalences(!"false".equalsIgnoreCase(equivalences))
                .build());

        return ResponseEntity.ok()
                .contentType(format.mineType())
                .body(ModelUtils.toBytes(schema, format));
    }

    @GetMapping(
            value = {
                    DATA_BY_CLASS_PATH,
                    DATA_BY_CLASS_PATH + EXTENSION_PATH_PARAM,
                    DATA_BY_CLASS_SLASH_PATH,
                    DATA_BY_OBJECT_PATH,
                    DATA_BY_OBJECT_PATH + EXTENSION_PATH_PARAM
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
    public ResponseEntity<byte[]> getIndividuals(@PathVariable(name = "class") String className,
                                                 @PathVariable(name = "id", required = false) String objectId,
                                                 @PathVariable(name = "extension", required = false) String extension,
                                                 @RequestParam(name = "schema", required = false) String schema,
                                                 @RequestParam(name = "format", required = false) String userFormat,
                                                 @RequestParam(name = "from", required = false, defaultValue = "0") int offset,
                                                 @RequestParam(name = "size", required = false, defaultValue = "100") int size,
                                                 final HttpServletRequest request) {

        RdfDataExportOptions options = RdfDataExportOptions.builder()
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
        Model individuals = dataExportService.getIndividuals(options);

        // Add schema
        //individuals = ModelFactontology/convertry.createInfModel(ReasonerRegistry.getOWLReasoner(), schemaExportService.getOntology(className, extension, ));

        return ResponseEntity.ok()
                .contentType(outputFormat.mineType())
                .body(ModelUtils.toBytes(individuals, outputFormat));
    }

    @GetMapping(value = CONVERT_PATH,
            produces = {
                    MediaType.APPLICATION_XML_VALUE,
                    RdfMediaType.APPLICATION_RDF_XML_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
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
                                                 @RequestParam(name = "format", defaultValue = "RDF") String userFormat,
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
        byte[] content = modelService.unionThenConvert(iris, inputFormat, outputFormat);

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