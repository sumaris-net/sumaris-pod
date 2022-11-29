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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import net.sumaris.extraction.core.service.ExtractionTypeService;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.server.http.ExtractionRestPaths;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.server.http.graphql.GraphQLApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@GraphQLApi
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class ExtractionTypeGraphQLService {

    private final ExtractionSecurityService extractionSecurityService;
    private final ExtractionTypeService extractionTypeService;
    private String documentationUrl;

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
    }

    @GraphQLQuery(name = "extractionTypes", description = "Get all available extraction types", deprecationReason = "Use liveExtractionTypes and aggregationTypes")
    public List<ExtractionTypeVO> findAllByFilter(
        @GraphQLArgument(name = "filter") ExtractionTypeFilterVO filter,
        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
        @GraphQLArgument(name = "sortBy") String sort,
        @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction) {

        filter = extractionSecurityService.sanitizeFilter(filter);
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        return extractionTypeService.findAllByFilter(filter,
            Page.builder().offset(offset).size(size).sortBy(sort).sortDirection(sortDirection).build());
    }

    @GraphQLQuery(name = "docUrl", description = "Get extraction documentation URL")
    public String getDocUrl(@GraphQLContext ExtractionTypeVO type) {
        if (type.getDocUrl() != null) return type.getDocUrl();

        if (documentationUrl != null && type.getFormat() != null) {
            String docUrl = documentationUrl.replaceFirst("\\{label[^\\}]*\\}", type.getLabel());
            type.setDocUrl(docUrl);
            return docUrl;
        }

        return null;
    }

}
