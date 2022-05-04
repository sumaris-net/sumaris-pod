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

package net.sumaris.extraction.server.http;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.I18nUtil;
import net.sumaris.core.util.ResourceUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.extraction.core.service.ExtractionDocumentationService;
import net.sumaris.extraction.core.service.ExtractionService;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.extraction.core.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.extraction.server.config.ExtractionWebConfigurationOption;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.server.security.IDownloadController;
import net.sumaris.extraction.server.util.QueryParamUtils;
import net.sumaris.server.http.MediaTypes;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


@RestController
@Slf4j
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
public class ExtractionRestController implements ExtractionRestPaths {

    protected static final String HTML_PREVIEW_PATH = "classpath:static/doc/preview.html";

    protected static final Collection<MediaType> HTML_MEDIA_TYPES = ImmutableList.of(
            MediaType.TEXT_HTML,
            MediaType.APPLICATION_XHTML_XML
    );

    @Autowired
    private SumarisConfiguration configuration;

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ExtractionDocumentationService extractionDocumentationService;

    @Autowired
    private ExtractionSecurityService extractionSecurityService;

    @Autowired
    private IDownloadController downloadController;

    @PostConstruct
    public void init() {
        log.info("Starting Extraction endpoint {{}}...", ExtractionRestPaths.BASE_PATH);
    }

    @GetMapping(
            value = {
                    TYPES_PATH
            },
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_JSON_UTF8_VALUE
            })
    public List<ExtractionTypeVO> getExtractionTypes() {

        // User can read all: return all types
        if (extractionSecurityService.canReadAll()) {
            return extractionService.findAll();
        }

        ExtractionTypeFilterVO filter = new ExtractionTypeFilterVO();
        filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId()});

        return extractionService.findAll(filter, null);
    }

    @GetMapping(
            value = {
                    DOC_PATH,
                    DOC_PATH + EXTENSION_PATH_PARAM,
                    DOC_WITH_VERSION_PATH,
                    DOC_WITH_VERSION_PATH + EXTENSION_PATH_PARAM,
            },
            produces = {
                    MediaType.TEXT_MARKDOWN_VALUE,
                    MediaType.TEXT_PLAIN_VALUE,
                    // Browser HTML requests
                    MediaType.APPLICATION_XHTML_XML_VALUE,
                    MediaType.TEXT_HTML_VALUE
            })
    public ResponseEntity<Resource> getDocumentation(@PathVariable(name = "category", required = true) String category,
                                                     @PathVariable(name = "label", required = true) String label,
                                                     @PathVariable(name = "version", required = false) String version,
                                                     @RequestParam(name = "format", required = false) String userFormat,
                                                     final HttpServletRequest request) {
        Preconditions.checkArgument(StringUtils.isNotBlank(category) && StringUtils.isNotBlank(label), "Invalid path. Expected: '/extraction/<lire|product>/<label>/doc'");

        // Set user locale
        Locale locale = getLocale(request);

        // Find output format (default: HTML)
        MediaType mediaType = getMediaType(request, userFormat, MediaType.TEXT_HTML);

        ExtractionTypeVO type = ExtractionTypeVO.builder()
                .category(ExtractionCategoryEnum.valueOfIgnoreCase(category))
                .label(StringUtils.changeCaseToUnderscore(label))
                .version(version)
                .build();

        try {

            Resource resource = extractionDocumentationService.find(type, locale)
                    .orElseThrow(() -> new SumarisTechnicalException(ErrorCodes.NOT_FOUND,
                            String.format("No documentation for extraction {category: '%s', label: '%s'}", category, label)));

            log.debug(String.format("Download resource {%s} as {%s}", resource.getFilename(), mediaType));

            if (mediaType.isPresentIn(HTML_MEDIA_TYPES)) {
                return createHtmlResponse(resource, mediaType);
            }

            // Return markdown (no rendering)
            return createAttachmentResponse(resource, resource.getFilename(), mediaType);

        } catch (IOException e) {
            log.error("Unable to read manual file: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping(
            value = {
                    DOWNLOAD_PATH,
                    DOWNLOAD_PATH + EXTENSION_PATH_PARAM,
                    DOWNLOAD_WITH_VERSION_PATH,
                    DOWNLOAD_WITH_VERSION_PATH + EXTENSION_PATH_PARAM,
            })
    public ResponseEntity executeAndDownload(@PathVariable(name = "category") String category,
                                   @PathVariable(name = "label") String label,
                                   @PathVariable(name = "version", required = false) String version,
                                   @PathVariable(name = "extension", required = false) String extension,
                                   @RequestParam(value = "q", required = false) String queryString,
                                   HttpServletResponse response) throws IOException {

        ExtractionTypeVO type = ExtractionTypeVO.builder()
                .category(ExtractionCategoryEnum.valueOfIgnoreCase(category))
                .label(StringUtils.changeCaseToUnderscore(label))
                .version(version)
                .build();

        ExtractionFilterVO filter;
        try {
            filter = QueryParamUtils.parseFilterQueryString(queryString);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().build();
        }

        extractionSecurityService.checkReadAccess(type);

        File tempFile = extractionService.executeAndDump(type, filter);

        // Add to file register
        String path = downloadController.registerFile(tempFile, true);

        // Redirect to path
        response.sendRedirect(path);
        return null;
    }

    /* -- protected methods -- */

    protected MediaType getMediaType(final HttpServletRequest request, @Nullable final String userFormat, @Nullable final MediaType defaultFormat) {
        // If user give a format, try to resolve it as an extension
        if (StringUtils.isNotBlank(userFormat)) {
            return MediaTypes.getMediaTypeForFileName(request.getServletContext(), "test." + userFormat)
                    .orElse(getMediaType(request, null, defaultFormat));
        }

        // Analyse URL extension
        return MediaTypes.getMediaTypeForFileName(request.getServletContext(), request.getRequestURI())
            // Or analyse HTTP header 'Accept' content types
            .orElseGet(() -> {
                Collection<String> acceptedContentTypes = Splitter.on(",").trimResults().splitToList(request.getHeader(HttpHeaders.ACCEPT));
                return acceptedContentTypes.stream()
                        .map(contentType -> MediaTypes.parseMediaType(contentType).orElse(null))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(defaultFormat);
            });
    }

    protected ResponseEntity<Resource> createAttachmentResponse(
            Resource source,
            String fileName,
            MediaType mediaType) throws IOException {
        return ResponseEntity.ok()
                // Content-Disposition
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                // Content-Type
                .contentType(mediaType)
                // Content-Length
                .contentLength(source.contentLength())
                .body(source);
    }

    protected ResponseEntity<Resource>  createHtmlResponse(Resource source,
                                                           MediaType mediaType) throws IOException {
        Resource templateResource = ResourceUtils.findResource(HTML_PREVIEW_PATH)
                .orElseThrow(() -> new SumarisTechnicalException("Missing HTML template for manual rendering"));

        String baseName = Files.getNameWithoutExtension(new File(source.getFilename()));
        String fileName = String.format("%s.%s", baseName, "html");
        File renderFile = new File(configuration.getTempDirectory(), fileName);

        boolean needRender = !renderFile.exists();

        // Force new render if source file has been updated (e.g. new Pod version)
        if (!needRender && (source.lastModified() > renderFile.lastModified())) {
            Files.deleteQuietly(renderFile);
            needRender = true;
        }

        if (needRender) {

            String htmlTemplate = ResourceUtils.readContent(templateResource, Charsets.UTF_8);
            String markdownContent = ResourceUtils.readContent(source, Charsets.UTF_8);
            String serverUrl = configuration.getApplicationConfig().getOption(ExtractionWebConfigurationOption.SERVER_URL.getKey());
            if (serverUrl.lastIndexOf('/') != serverUrl.length() -1) {
                serverUrl += "/";
            }

            htmlTemplate = htmlTemplate
                    .replace("{{markdown}}", markdownContent)
                    .replaceAll("src=\"/", "src=\"" + serverUrl)
                    .replaceAll("href=\"/", "href=\"" + serverUrl);

            FileUtils.write(renderFile, htmlTemplate, Charsets.UTF_8);
        }

        return ResponseEntity.ok()
                // Content-Type
                .contentType(mediaType)
                // Content-Length
                .contentLength(renderFile.length())
                .body(new InputStreamResource(new FileInputStream(renderFile)));
    }


    protected Locale getLocale(@NonNull final HttpServletRequest request) {
        String languages = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)
                .replaceAll("-", "_") // Change format 'fr-FR' to 'fr_FR'
                .replaceAll(";[^,]+", ""); // Remove charset
        return I18nUtil.findFirstI18nLocale(languages)
                .orElse(Locale.UK);
    }
}