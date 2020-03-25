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
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.ModelURIs;
import net.sumaris.rdf.service.data.RdfDataExportOptions;
import net.sumaris.rdf.service.data.RdfDataExportService;
import net.sumaris.rdf.service.schema.RdfSchemaExportOptions;
import net.sumaris.rdf.service.schema.RdfSchemaExportService;
import net.sumaris.rdf.util.ModelUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@ConditionalOnBean({RdfConfiguration.class})
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

    public static final String CONVERT = ONTOLOGY_PATH + "/convert";

    private static final Logger log = LoggerFactory.getLogger(RdfRestController.class);

    @Resource
    private RdfSchemaExportService schemaExportService;

    @Resource
    private RdfDataExportService dataExportService;

    @Resource
    private RdfConfiguration config;

    private String ontologyBaseUri;

    @PostConstruct
    public void init() {
        // Compute /ontology full path
        String modelBaseUri = config.getModelBaseUri();
        if (modelBaseUri.endsWith("/")) {
            modelBaseUri = modelBaseUri.substring(0, modelBaseUri.length()-1);
        }
        this.ontologyBaseUri = modelBaseUri + ONTOLOGY_PATH;

        log.info("Starting OWL endpoint {{}}...", SCHEMA_PATH_SLASH);
        log.info("Starting OWL endpoint {{}}...", DATA_SLASH_PATH);
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
                                                    @RequestParam(name = "equivalences", defaultValue = "false", required = false) String equivalences,
                                                    final HttpServletRequest request) {

        // Find output format
        RdfFormat format = findRdfFormat(request, userFormat, RdfFormat.RDF);

        // Generate the schema ontology
        Model schema = schemaExportService.getSchemaOntology(RdfSchemaExportOptions.builder()
                .className(className)
                .withDisjoints(!"false".equalsIgnoreCase(disjoints)) // True by default
                .withEquivalences("true".equalsIgnoreCase(equivalences))
                // TODO .withInterfaces()
                .build());

        return ResponseEntity.ok()
                .contentType(format.mineType())
                .body(ModelUtils.modelToBytes(schema, format));
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
                                                 @RequestParam(name = "format", required = false) String userFormat,
                                                 final HttpServletRequest request) {

        RdfDataExportOptions options = RdfDataExportOptions.builder()
                .className(className)
                .id(objectId)
                .build();

        // Find the output format
        RdfFormat outputFormat = findRdfFormat(request, userFormat, RdfFormat.RDF);

        // Get individuals
        Model individuals = dataExportService.getIndividuals(options);

        // Add schema

        return ResponseEntity.ok()
                .contentType(outputFormat.mineType())
                .body(ModelUtils.modelToBytes(individuals, outputFormat));
    }

    @GetMapping(value = CONVERT,
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

        if (StringUtils.isBlank(uri)) {
            if (StringUtils.isBlank(prefix)) {
                throw new IllegalArgumentException("Required query parameters 'uri' or 'prefix'.");
            }

            // Split prefix string, to allow prefix=<prefix1>+<prefix2>
            uri = Arrays.stream(prefix.split("[,|+]"))
                    .map(p -> ModelURIs.RDF_URL_BY_PREFIX.get(p))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("+"));
            if (StringUtils.isBlank(uri))
                throw new IllegalArgumentException(String.format("Invalid prefix '%s'", prefix));
        }

        // Find the output format
        RdfFormat outputFormat = findRdfFormat(request, userFormat, RdfFormat.RDF);

        // Read input model
        String[] parts = uri.split("[,|+]"); // Split, to allow to passe many URI
        Model model = Arrays.stream(parts)
                .map(aUri -> {
                    if (StringUtils.isBlank(aUri)) throw new IllegalArgumentException("Invalid 'uri': " + aUri);

                    // Retrieve the source format
                    RdfFormat inputFormat;
                    if (StringUtils.isNotBlank(sourceFormat)) {
                        inputFormat = RdfFormat.fromUserString(sourceFormat)
                                .orElseThrow(() -> new IllegalArgumentException("Unknown sourceFormat: " + sourceFormat));
                    }
                    else {
                        inputFormat = RdfFormat.fromUrlExtension(request.getRequestURI())
                                .orElse(RdfFormat.RDF);
                    };

                    log.info("Converting {} model {{}} into {}...", inputFormat.toJenaFormat(), aUri, outputFormat.toJenaFormat());

                    // Read from URI
                    return loadModelByUri(aUri.trim(), inputFormat);
                })
                // Union on all models
                .reduce(ModelFactory::createUnion).orElse(null);

        return ResponseEntity.ok()
                .contentType(outputFormat.mineType())
                .body(ModelUtils.modelToBytes(model, outputFormat));
    }

    /* -- Conversion service -- */

    public Model loadModelByUri(String uri, RdfFormat uriFormat) {
        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException("Invalid IRI: " + uri);
        }
        Preconditions.checkNotNull(uriFormat);

        log.info(String.format("Reading %s model {%s}...", uriFormat.toJenaFormat(), uri));

        // If URI is on current Pod
        // Path match /ontology/{schema|data}/{class}/{id}
        if (uri.startsWith(ontologyBaseUri)) {
            URI relativeUri = URI.create(uri.substring(ontologyBaseUri.length()));
            List<String> pathParams = Splitter.on('/').omitEmptyStrings().trimResults().splitToList(relativeUri.getPath());
            if (pathParams.size() < 1 || pathParams.size() > 3) {
                throw new IllegalArgumentException("Invalid URI: " + uri);
            }

            String modelType = pathParams.get(0);
            String className = pathParams.size() > 1 ? pathParams.get(1) : null;

            switch (modelType) {
                case "schema":
                    RdfSchemaExportOptions schemaOptions = RdfSchemaExportOptions.builder()
                            .className(className)
                            .build();
                    fillOptionsUsingRequestUri(schemaOptions, relativeUri);
                    return schemaExportService.getSchemaOntology(schemaOptions);
                case "data":
                    String objectId = pathParams.size() > 2 ? pathParams.get(2) : null;
                    RdfDataExportOptions dataOptions = RdfDataExportOptions.builder()
                            .className(className)
                            .id(objectId)
                            .build();
                    return dataExportService.getIndividuals(dataOptions);
                default:
                    throw new IllegalArgumentException("Invalid URI: " + uri);
            }
        }

        // External URI
        else {
            return ModelUtils.loadModelByUri(uri, uriFormat);
        }
    }

    /* -- protected methods -- */


    protected RdfSchemaExportOptions fillOptionsUsingRequestUri(RdfSchemaExportOptions options, URI uri) {
        Preconditions.checkNotNull(options);
        Preconditions.checkNotNull(uri);
        Map<String, String> requestParams = parseQueryParams(uri);

        // With disjoint ? true by default
        String disjoints = requestParams.get("disjoints");
        if (StringUtils.isNotBlank(disjoints)) options.setWithDisjoints(! "false".equalsIgnoreCase(disjoints));

        // With equivalences ? false by default
        String equivalences = requestParams.get("equivalences");
        if (StringUtils.isNotBlank(equivalences)) options.setWithEquivalences("true".equalsIgnoreCase(equivalences)); // false by default

        // packages ? empty by default
        String packages = requestParams.get("packages");
        if (StringUtils.isNotBlank(packages)) {
            options.setPackages(Splitter.on(',').omitEmptyStrings().trimResults().splitToList(packages));
        }

        return options;
    }

    protected Map<String, String> parseQueryParams(URI uri) {
        Preconditions.checkNotNull(uri);

        Map<String, String> result = Maps.newHashMap();

        String query = uri.getQuery();
        if (StringUtils.isNotBlank(query)) {
            for (String paramStr : Splitter.on('&').omitEmptyStrings().trimResults().split(query)) {
                String[] paramParts = paramStr.split("=");
                if (paramParts.length == 1) {
                    result.put(paramParts[0], "true");
                }
                else if (paramParts.length == 2) {
                    result.put(paramParts[0], paramParts[1]);
                }
                else {
                    // Ignore
                    if (log.isInfoEnabled()) log.info("Skipping invalid IRI's query parameter: " + paramStr);
                }
            }
        }
        return result;
    }

    protected RdfFormat findRdfFormat(final HttpServletRequest request, @Nullable final String userFormat, @Nullable final RdfFormat defaultFormat) {
        if (StringUtils.isNotBlank(userFormat)) {
            return RdfFormat.fromUserString(userFormat)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown output format: " + userFormat));
        }

        // Analyse HTTP header 'Accept' content types
        else {
            return RdfFormat.fromUrlExtension(request.getRequestURI())
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
}