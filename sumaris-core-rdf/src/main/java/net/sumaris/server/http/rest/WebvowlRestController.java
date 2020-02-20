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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.ModelType;
import net.sumaris.rdf.model.ModelURIs;
import net.sumaris.rdf.service.RdfExportOptions;
import net.sumaris.rdf.service.RdfExportService;
import net.sumaris.rdf.util.ModelUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.JenaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
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
public class WebvowlRestController {

    public static final String BASE_PATH = "/webvowl";
    public static final String SERVER_TIMESTAMP_PATH = BASE_PATH +"/serverTimeStamp";
    public static final String CONVERT_PATH = BASE_PATH + "/convert";
    public static final String LOADING_STATUS_PATH = BASE_PATH + "/loadingStatus";
    public static final String CONVERSION_DONE_PATH = BASE_PATH + "/conversionDone";


    private static final Logger log = LoggerFactory.getLogger(WebvowlRestController.class);

    @Resource
    private RdfExportService service;

    @Value( "${rdf.model.uri}" )
    private String rdfModelUriPrefix;

    @PostConstruct
    public void afterPropertySet() {
        log.info("Starting WebVOWL endpoint {{}}...", BASE_PATH);
    }


    protected Map<String, String> webvowlSessions = Maps.newConcurrentMap();
    @GetMapping(value = SERVER_TIMESTAMP_PATH, produces = {"text/plain", "application/text"})
    @ResponseBody
    public String getServerTimestamp() {
        String sessionId = generateSessionId();
        webvowlSessions.put(sessionId, String.format("* Initializing session #%s", sessionId));
        return sessionId;
    }

    @GetMapping(value = CONVERT_PATH,
            produces = {
                    RdfMediaType.APPLICATION_JSON_VALUE,
                    RdfMediaType.APPLICATION_X_JAVASCRIPT_VALUE,
                    RdfMediaType.APPLICATION_WEBVOWL_VALUE
            })
    public ResponseEntity<String> convertRdfIriToVowl(@RequestParam(name = "iri", required = false) String iri,
                                                      @RequestParam(name = "ns", required = false) String namespace,
                                                      @RequestParam(name = "sessionId", required = false) String sessionId) {

        sessionId = sessionId != null ? sessionId : generateSessionId();
        if (StringUtils.isBlank(iri) && StringUtils.isNotBlank(namespace)) {
            iri = ModelURIs.URI_BY_NAMESPACE.get(namespace);
        }
        if (StringUtils.isBlank(iri)) throw new IllegalArgumentException("Required query parameters 'iri' or 'ns'");

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

    @PostMapping(value = CONVERT_PATH,
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

    @GetMapping(value = LOADING_STATUS_PATH, produces = {"text/plain", "application/text"})
    public ResponseEntity<String> getLoadingStatus(@RequestParam(name = "sessionId", required = false) String sessionId) {

        if (StringUtils.isNotBlank(sessionId)) {
            return ResponseEntity.ok().body(webvowlSessions.get(sessionId));
        }
        else {
            return ResponseEntity.badRequest().body("Invalid session");
        }
    }

    @GetMapping(value = CONVERSION_DONE_PATH, produces = {RdfMediaType.TEXT_PLAIN_VALUE, "application/text"})
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

    protected Model getModelFromIri(String iri, String sourceFormat) {
        if (StringUtils.isBlank(iri)) {
            throw new IllegalArgumentException("Invalid IRI: " + iri);
        }

        RdfFormat format = RdfFormat.fromUserString(sourceFormat);
        log.info(String.format("Reading %s model {%s}...", format.toJenaFormat(), iri));

        // Path match /ontology/{modelType}/{class}/{id}
        String apiPath = rdfModelUriPrefix + RdfRestController.BASE_PATH;
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

    protected String generateSessionId() {
        return String.valueOf(System.currentTimeMillis());
    }
}