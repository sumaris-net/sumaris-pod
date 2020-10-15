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

package net.sumaris.server.http.vowl;

import com.google.common.collect.Maps;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.exception.JenaExceptions;
import net.sumaris.rdf.model.ModelURIs;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.server.http.rest.RdfFormat;
import net.sumaris.server.http.rest.RdfMediaType;
import net.sumaris.server.http.rest.RdfRestController;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@ConditionalOnBean({WebMvcConfigurer.class})
public class WebvowlRestController {

    public static final String BASE_PATH = "/webvowl";
    public static final String SERVER_TIMESTAMP_PATH = BASE_PATH +"/serverTimeStamp";
    public static final String CONVERT_PATH = BASE_PATH + "/convert";
    public static final String LOADING_STATUS_PATH = BASE_PATH + "/loadingStatus";
    public static final String CONVERSION_DONE_PATH = BASE_PATH + "/conversionDone";


    private static final Logger log = LoggerFactory.getLogger(WebvowlRestController.class);

    @Resource
    private RdfRestController rdfRestController;

    @PostConstruct
    public void start() {
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
                                                      @RequestParam(name = "prefix", required = false) String prefix,
                                                      @RequestParam(name = "format", required = false) String format,
                                                      @RequestParam(name = "sessionId", required = false) String optionalSessionId,
                                                      final HttpServletRequest request) {

        final String sessionId = optionalSessionId != null ? optionalSessionId : generateSessionId();
        if (StringUtils.isBlank(iri)) {
            if (StringUtils.isBlank(prefix)) {
                throw new IllegalArgumentException("Required query parameters 'iri' or 'prefix'.");
            }
            iri = Arrays.stream(prefix.split("[,|+]"))
                    .map(p -> ModelURIs.RDF_URL_BY_PREFIX.get(p))
                    .filter(Objects::nonNull).collect(Collectors.joining("+"));
            if (StringUtils.isBlank(iri))
                throw new IllegalArgumentException(String.format("Unknown prefix '%s'. Try using the 'iri' param.", prefix));
        }

        try {
            String[] parts = iri.split("[,|+]");
            List<Model> models = Arrays.stream(parts)
                .map(aUri -> {
                    if (StringUtils.isBlank(aUri)) throw new IllegalArgumentException("Invalid 'iri': " + aUri);

                    // Retrieve the source format
                    RdfFormat sourceFormat = RdfFormat.fromUrlExtension(aUri.trim()).orElse(RdfFormat.RDF);

                    // Read from IRI
                    log.info("Reading {} model {{}}..", sourceFormat.getName(), aUri);
                    webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Reading {%s} %s model...", aUri, sourceFormat.toJenaFormat()));
                    return rdfRestController.loadModelByUri(aUri.trim(), sourceFormat);
                })
                    .collect(Collectors.toList());
            Model model;
            if (models.size() == 1) {
                model = models.get(0);
            }
            else {
                // Union on all models
                model = models.stream()
                        .reduce(ModelFactory::createUnion)
                        .orElse(null);
            }


            // Convert to WebVOWL
            RdfFormat targetFormat = RdfFormat.fromUserString(format).orElse(RdfFormat.VOWL);
            log.info(String.format("Converting model {%s} to %s...", iri, targetFormat.toJenaFormat()));
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Converting model to %s...", targetFormat.toJenaFormat()));

            return ResponseEntity.ok()
                    .contentType(targetFormat.mineType())
                    .body(ModelUtils.modelToString(model, targetFormat));
        }
        catch(Exception e) {
            Throwable cause = JenaExceptions.findJenaRootCause(e).orElse(e);
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* ERROR: %s", cause.getMessage()));
            throw e;
        }
    }

    @PostMapping(value = CONVERT_PATH,
            produces = {
                RdfMediaType.APPLICATION_JSON_VALUE,
                RdfMediaType.APPLICATION_X_JAVASCRIPT_VALUE,
                RdfMediaType.APPLICATION_WEBVOWL_VALUE
        })
    public ResponseEntity<byte[]> convertToVowl(@RequestParam(name = "ontology") MultipartFile file,
                                                @RequestParam(name = "sessionId") String sessionId) {

        String contentType = file.getContentType() != null ? file.getContentType() : RdfFormat.RDF.mineType().toString();
        RdfFormat sourceFormat = RdfFormat.fromContentType(contentType).orElse(RdfFormat.RDF);

        log.info(String.format("Reading %s model from uploaded file {%s} ...", file.getOriginalFilename(), sourceFormat.toJenaFormat()));
        webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Analyzing %s model...", sourceFormat.toJenaFormat()));

        try (InputStream is = new ByteArrayInputStream(file.getBytes());) {

            Model model = ModelUtils.readModel(is, sourceFormat);
            is.close();

            // Convert to WebVOWL
            RdfFormat targetFormat = RdfFormat.VOWL;
            log.info(String.format("Converting model into {%s}...", targetFormat.toJenaFormat()));
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* Converting model to %s...", targetFormat.toJenaFormat()));

            return ResponseEntity.ok()
                    .contentType(targetFormat.mineType())
                    .body(ModelUtils.modelToBytes(model, targetFormat));
        }
        catch(IOException e) {
            Throwable cause = JenaExceptions.findJenaRootCause(e).orElse(e);
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* ERROR: %s", cause.getMessage()));
            throw new SumarisTechnicalException(e);
        }
        catch(Exception e) {
            Throwable cause = JenaExceptions.findJenaRootCause(e).orElse(e);
            webvowlSessions.computeIfPresent(sessionId, (key, value) -> String.format("* ERROR: %s", cause.getMessage()));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(RdfMediaType.APPLICATION_JSON)
                    .body(String.format("{\"error\": \"%s\"}", cause.getMessage()).getBytes());
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


    protected String generateSessionId() {
        return String.valueOf(System.currentTimeMillis());
    }
}