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

package net.sumaris.extraction.server.graphql;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.extraction.core.service.ExtractionService;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.extraction.core.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.extraction.server.http.ExtractionRestPaths;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.security.IDownloadController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@GraphQLApi
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class ExtractionGraphQLService {

    private final ExtractionService extractionService;
    private final IDownloadController downloadController;
    private final ExtractionSecurityService extractionSecurityService;
    private String documentationUrl;
    private boolean enableCache = false;

    public ExtractionGraphQLService(
        ExtractionService extractionService,
        IDownloadController downloadController,
        ExtractionSecurityService extractionSecurityService) {
        this.extractionService = extractionService;
        this.downloadController = downloadController;
        this.extractionSecurityService = extractionSecurityService;
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {

        // Prepare URL for String formatter
        String serverUrl = event.getConfiguration().getApplicationConfig().getOption("server.url");

        if (StringUtils.isNotBlank(serverUrl)) {
            documentationUrl = serverUrl + ExtractionRestPaths.DOC_PATH;
        }
        else {
            documentationUrl = null;
        }

        enableCache = event.getConfiguration().enableCache();
    }

    @GraphQLQuery(name = "extractionTypes", description = "Get all available extraction types", deprecationReason = "Use liveExtractionTypes and aggregationTypes")
    @Transactional(readOnly = true)
    public List<ExtractionTypeVO> getAllExtractionTypes(
        @GraphQLArgument(name = "filter") ExtractionTypeFilterVO filter,
        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
        @GraphQLArgument(name = "sortBy") String sort,
        @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction) {

        filter = extractionSecurityService.sanitizeFilter(filter);
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        return extractionService.findAll(filter,
            Page.builder().offset(offset).size(size).sortBy(sort).sortDirection(sortDirection).build());
    }

    @GraphQLQuery(name = "liveExtractionTypes", description = "Get all live extraction types")
    @Transactional(readOnly = true)
    public List<ExtractionTypeVO> getLiveExtractionTypes(@GraphQLArgument(name = "filter") ExtractionTypeFilterVO filter,
                                                         @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                         @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                         @GraphQLArgument(name = "sortBy") String sort,
                                                         @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction) {

        filter = extractionSecurityService.sanitizeFilter(filter);
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        return extractionService.findAll(filter,
            Page.builder().offset(offset).size(size).sortBy(sort).sortDirection(sortDirection).build());
    }

    @GraphQLQuery(name = "extractionRows", description = "Preview some extraction rows")
    @Transactional
    public ExtractionResultVO getExtractionRows(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                               @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                               @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                               @GraphQLArgument(name = "size", defaultValue = "100") Integer size,
                                               @GraphQLArgument(name = "sortBy") String sort,
                                               @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        extractionSecurityService.checkReadAccess(type);

        return extractionService.executeAndRead(type, filter, Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
            .build());
    }

    @GraphQLQuery(name = "extraction", description = "Preview some extraction")
    @Transactional
    public List<Map<String, String>> getExtraction(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                                   @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                   @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                   @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                   @GraphQLArgument(name = "sortBy") String sort,
                                                   @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                                   @GraphQLArgument(name = "cacheDuration") String cacheDuration
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        extractionSecurityService.checkReadAccess(type);

        Page page = Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction))
            .build();

        if (!enableCache && cacheDuration != null) {
            log.warn( "User try to use extraction with {cacheDuration: {}) but cache has been disabled (option '{}'). Will execute without cache",
                cacheDuration,
                SumarisConfigurationOption.CACHE_ENABLED.getKey());
            cacheDuration = null;
        }

        ExtractionResultVO resultVO;
        if (cacheDuration == null) {
            resultVO = extractionService.executeAndRead(type, filter, page);
        }
        else {
            resultVO = extractionService.executeAndReadWithCache(type, filter, page,
                CacheTTL.fromString(cacheDuration));
        }

        return toJsonArray(resultVO);
    }

    @GraphQLQuery(name = "extractionFile", description = "Execute extraction to a file")
    @Transactional(timeout = 10000000)
    public String getExtractionFile(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                    @GraphQLArgument(name = "filter") ExtractionFilterVO filter
    ) throws IOException {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");

        extractionSecurityService.checkReadAccess(type);

        File tempFile = extractionService.executeAndDump(type, filter);

        // Add to download controller
        String filePath = downloadController.registerFile(tempFile, true);

       return filePath;
    }

    @GraphQLQuery(name = "docUrl", description = "Get extraction documentation URL")
    public String getDocUrl(@GraphQLContext ExtractionTypeVO type) {
        if (type.getDocUrl() != null) return type.getDocUrl();

        if (documentationUrl != null && type.getCategory() != null && type.getLabel() != null) {
            String docUrl = documentationUrl.replaceFirst("\\{category[^\\}]*\\}", type.getCategory().name().toLowerCase())
                .replaceFirst("\\{label[^\\}]*\\}", type.getLabel().toLowerCase());
            type.setDocUrl(docUrl);
            return docUrl;
        }

        return null;
    }

    @GraphQLMutation(name = "saveExtraction", description = "Create or update a extraction")
    @Transactional
    public ExtractionTypeVO saveExtraction(@GraphQLArgument(name = "type") @NonNull ExtractionTypeVO type,
                                           @GraphQLArgument(name = "filter") ExtractionFilterVO filter
    ) {
        // WHen source extraction is a live extraction: force to clean id
        if (ExtractionCategoryEnum.LIVE.equals(type.getCategory())
            || (type.getId() != null && type.getId() < 0)) {
            type.setId(null);
        }
        boolean isNew = type.getId() == null;
        if (isNew) {
            extractionSecurityService.checkWriteAccess();
            type.setId(null);
        }
        else {
            extractionSecurityService.checkWriteAccess(type.getId());
        }

        return extractionService.save(type, filter);
    }

    /* -- protected methods -- */

    protected List<Map<String, String>> toJsonArray(ExtractionResultVO source) {
        String[] columnNames = source.getColumns()
            .stream().map(ExtractionTableColumnVO::getLabel)
            .toArray(String[]::new);
        List<Map<String, String>> target = new ArrayList<>();

        return source.getRows()
            .stream().map(row -> {
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < row.length; i++) {
                    rowMap.put(columnNames[i], row[i]);
                }
                return rowMap;
            }).collect(Collectors.toList());
    }
}
