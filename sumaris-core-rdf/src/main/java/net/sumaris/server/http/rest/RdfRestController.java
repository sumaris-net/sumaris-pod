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
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.ModelType;
import net.sumaris.rdf.service.RdfExportOptions;
import net.sumaris.rdf.service.RdfExportService;
import net.sumaris.rdf.util.ModelUtils;
import org.apache.coyote.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shared.JenaException;
import org.nuiton.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;


@RestController
@ConditionalOnBean({RdfConfiguration.class})
public class RdfRestController {

    private static final Logger log = LoggerFactory.getLogger(RdfRestController.class);

    @Resource
    private RdfExportService service;

    @Value( "${server.url}" )
    private String serverUrl;

    @Value( "${rdf.model.uri}" )
    private String rdfModelUri;

    @PostConstruct
    public void afterPropertySet() {
        log.info(String.format("Starting RDF rest controller at {%s}...", RdfRestPaths.BASE_PATH));
    }

    @GetMapping(
            value = {
                    RdfRestPaths.ONTOLOGY,
                    RdfRestPaths.ONTOLOGY_BY_CLASS,
                    RdfRestPaths.ONTOLOGY_BY_OBJECT
            },
            produces = {
                    MediaType.APPLICATION_XML_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
                    RdfMediaType.APPLICATION_RDF_XML_VALUE,
                    RdfMediaType.APPLICATION_JSON_LD_VALUE,
                    RdfMediaType.APPLICATION_N_TRIPLES_VALUE,
                    RdfMediaType.TEXT_N_TRIPLES_VALUE,
                    RdfMediaType.TEXT_TRIG_VALUE,
                    RdfMediaType.TEXT_TRIX_VALUE,
                    RdfMediaType.TEXT_TURTLE_VALUE
            })
    public ResponseEntity<String> getOwlModel(@PathVariable(name = "type") String userModelType,
                                              @PathVariable(name = "class", required = false) String className,
                                              @PathVariable(name = "id", required = false) String objectId,
                                              @RequestParam(defaultValue = "false") String disjoints,
                                              @RequestParam(name ="format", defaultValue = "RDF", required = false) String userFormat,
                                              @RequestParam(name ="schema", defaultValue = "true", required = false) String withSchema,
                                              @RequestParam(required = false) String packages) {

        RdfExportOptions options = buildOptions(className, objectId, disjoints, withSchema, packages);
        RdfFormat format = RdfFormat.fromUserString(userFormat);
        ModelType modelType = ModelType.fromUserString(userModelType);

        String content = getModelAsString(modelType, options, format);

        return ResponseEntity.ok()
                .contentType(format.mineType())
                .body(content);
    }

    @GetMapping(value = RdfRestPaths.CONVERT,
            produces = {
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                RdfMediaType.APPLICATION_RDF_XML_VALUE,
                RdfMediaType.APPLICATION_JSON_LD_VALUE,
                RdfMediaType.APPLICATION_N_TRIPLES_VALUE,
                RdfMediaType.TEXT_N_TRIPLES_VALUE,
                RdfMediaType.TEXT_TRIG_VALUE,
                RdfMediaType.TEXT_TRIX_VALUE,
                RdfMediaType.TEXT_TURTLE_VALUE
            })
    public ResponseEntity<String> getOwlModelFromIri(@RequestParam(name = "iri") String iri,
                                                     @RequestParam(name = "sourceFormat", defaultValue = "RDF") String sourceFormat,
                                                     @RequestParam(name = "format", defaultValue = "RDF") String userFormat) {
        if (StringUtils.isBlank(iri)) {
            throw new IllegalArgumentException("Invalid IRI: " + iri);
        }

        RdfFormat format = RdfFormat.fromUserString(userFormat);
        Model model = getModelFromIri(iri, sourceFormat);

        log.info(String.format("Converting ontology {%s} into {%s}...", iri, format.toJenaFormat()));

        String content = ModelUtils.modelToString(model, format);
        return ResponseEntity.ok()
                .contentType(format.mineType())
                .body(content);
    }

    /* -- WebVOWL service -- */

    public Model getModelFromIri(String iri, String sourceFormat) {
        if (StringUtils.isBlank(iri)) {
            throw new IllegalArgumentException("Invalid IRI: " + iri);
        }

        RdfFormat format = RdfFormat.fromUserString(sourceFormat);
        log.info(String.format("Reading %s model {%s}...", format.toJenaFormat(), iri));

        // Path match /ontology/{modelType}/{class}/{id}
        String apiPath = rdfModelUri + RdfRestPaths.BASE_PATH;
        if (iri.startsWith(apiPath)) {
            URI relativeUri = URI.create(iri.substring(apiPath.length()));
            List<String> pathParams = Splitter.on('/').omitEmptyStrings().trimResults().splitToList(relativeUri.getPath());
            if (pathParams.size() < 1 || pathParams.size() > 3) {
                throw new IllegalArgumentException("Invalid IRI: " + iri);
            }

            RdfExportOptions options = buildOptions(relativeUri);
            ModelType modelType = ModelType.fromUserString(pathParams.get(0));
            String className = pathParams.size() > 1 ? pathParams.get(1) : null;
            options.setClassName(className);
            String objectId = pathParams.size() > 2 ? pathParams.get(2) : null;
            options.setId(objectId);

            return getModel(modelType, options);
        }

        // External IRI
        else {
            return ModelUtils.readModel(iri, sourceFormat);
        }
    }

    protected Map<String, String> webvowlSessions = Maps.newConcurrentMap();
    @GetMapping(value = "/webvowl/serverTimeStamp", produces = {"text/plain", "application/text"})
    @ResponseBody
    public String getServerTimestamp() {
        String sessionId = String.valueOf(System.currentTimeMillis());
        webvowlSessions.put(sessionId, String.format("* Initializing session #%s", sessionId));
        return sessionId;
    }

    @GetMapping(value = "/webvowl/convert", produces = {
            RdfMediaType.APPLICATION_JSON_VALUE,
            RdfMediaType.APPLICATION_X_JAVASCRIPT_VALUE,
            RdfMediaType.APPLICATION_WEBVOWL_VALUE
    })
    public ResponseEntity<String> convertRdfIriToVowl(@RequestParam(name = "iri") String iri,
                                                      @RequestParam(name = "sessionId") String sessionId) {
        if (StringUtils.isBlank(iri)) {
            throw new IllegalArgumentException("Invalid IRI: " + iri);
        }

        try {
            // Read from IRI
            RdfFormat sourceFormat = RdfFormat.RDF;
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Analyzing %s model...", sourceFormat.toJenaFormat()));
            Model model = getModelFromIri(iri, sourceFormat.name());

            // Convert to WebVOWL
            RdfFormat targetFormat = RdfFormat.VOWL;
            log.info(String.format("Converting model {%s} to %s...", iri, targetFormat.toJenaFormat()));
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Converting model to %s...", targetFormat.toJenaFormat()));
            String content = ModelUtils.modelToString(model, targetFormat);

            return ResponseEntity.ok()
                    .contentType(targetFormat.mineType())
                    .body(content);
        }
        catch(Exception e) {
            Throwable rootCause = getJenaExceptionCauseOrSelf(e);
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* ERROR: %s", rootCause.getMessage()));
            throw e;
        }
    }

    @PostMapping(value = "/webvowl/convert",
            produces = {
                RdfMediaType.APPLICATION_JSON_VALUE,
                RdfMediaType.APPLICATION_X_JAVASCRIPT_VALUE,
                RdfMediaType.APPLICATION_WEBVOWL_VALUE
        })
    public ResponseEntity<String> convertRdfFileToVowl(@RequestParam(name = "ontology") MultipartFile file,
                                                      @RequestParam(name = "sessionId") String sessionId) {

        String contentType = file.getContentType() != null ? file.getContentType() : RdfFormat.RDF.mineType().toString();
        RdfFormat sourceFormat = RdfFormat.fromContentType(contentType);

        log.info(String.format("Reading %s model from uploaded file {%s} ...", file.getOriginalFilename(), sourceFormat.toJenaFormat()));
        webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Analyzing %s model...", sourceFormat.toJenaFormat()));

        try (InputStream is = new ByteArrayInputStream(file.getBytes());) {

            Model model = ModelUtils.readModel(is, sourceFormat);
            is.close();

            // Convert to WebVOWL
            RdfFormat targetFormat = RdfFormat.VOWL;
            log.info(String.format("Converting model into {%s}...", targetFormat.toJenaFormat()));
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Converting model to %s...", targetFormat.toJenaFormat()));
            String content = ModelUtils.modelToString(model, targetFormat);

            return ResponseEntity.ok()
                    .contentType(targetFormat.mineType())
                    .body(content);
        }
        catch(IOException e) {
            Throwable rootCause = getJenaExceptionCauseOrSelf(e);
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* ERROR: %s", rootCause.getMessage()));
            throw new SumarisTechnicalException(e);
        }
        catch(Exception e) {
            Throwable rootCause = getJenaExceptionCauseOrSelf(e);
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* ERROR: %s", rootCause.getMessage()));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(RdfMediaType.APPLICATION_JSON)
                    .body(String.format("{\"error\": \"%s\"}", rootCause.getMessage()));
        }
    }


    @GetMapping(value = "/webvowl/loadingStatus", produces = {"text/plain", "application/text"})
    public ResponseEntity<String> getLoadingStatus(@RequestParam(name = "sessionId", required = false) String sessionId) {

        if (StringUtils.isNotBlank(sessionId)) {
            return ResponseEntity.ok().body(webvowlSessions.get(sessionId));
        }
        else {
            return ResponseEntity.badRequest().body("Invalid session");
        }
    }


    @GetMapping(value = "/webvowl/conversionDone", produces = {"text/plain", "application/text"})
    @ResponseBody
    public String getVowlConversionDone(@RequestParam(defaultValue = "sessionId") String sessionId) {
        String message = webvowlSessions.remove(sessionId);
        return message + " * Conversion succeed";
    }

    /* -- protected methods -- */

    protected Model getModel(ModelType modelType,
                             RdfExportOptions options) {
        Preconditions.checkNotNull(modelType);
        Preconditions.checkNotNull(options);

        switch (modelType) {
            case SCHEMA:
                return service.getOntologyModel(options);
            case DATA:
                return service.getDataModel(options);
            default:
                throw new IllegalArgumentException("Invalid modelType: " + modelType);
        }
    }

    protected String getModelAsString(ModelType modelType,
                                      RdfExportOptions options,
                                      RdfFormat format) {
        Preconditions.checkNotNull(format);

        Model model = getModel(modelType, options);
        return ModelUtils.modelToString(model, format);
    }


    protected RdfExportOptions buildOptions(String className, String objectId, String disjoints, String withSchema, String packages) {

        return RdfExportOptions.builder()
                .className(className)
                .id(objectId)
                .withDisjoints("true".equalsIgnoreCase(disjoints))
                .withSchema("true".equalsIgnoreCase(withSchema))
                .packages(StringUtils.isNotBlank(packages) ? Splitter.on(',').omitEmptyStrings().trimResults().splitToList(packages) : null)
                .build();
    }

    protected RdfExportOptions buildOptions(URI uri) {
        Preconditions.checkNotNull(uri);
        Map<String, String> requestParams = parseQueryParams(uri);
        String disjoints = requestParams.get("disjoints");
        String methods = requestParams.get("methods");
        String packages = requestParams.get("packages");
        RdfExportOptions options = buildOptions(null, null, disjoints, methods, packages);
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

    protected JenaException getJenaExceptionCause(Throwable e) {
        if (e instanceof JenaException) {
            return (JenaException) e;
        }

        Throwable cause = e.getCause();
        if (cause != null) {
            // Loop
            return getJenaExceptionCause(cause);
        }

        return null;
    }

    protected Throwable getJenaExceptionCauseOrSelf(Throwable e) {
        JenaException jeanCause = getJenaExceptionCause(e);
        if (jeanCause != null) return jeanCause;
        return e;
    }
}