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

package net.sumaris.server.graphql;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.service.ExtractionService;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.server.config.ExtractionWebAutoConfiguration;
import net.sumaris.server.security.ExtractionSecurityService;
import net.sumaris.server.security.IDownloadController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@ConditionalOnBean({ExtractionWebAutoConfiguration.class})
public class ExtractionGraphQLService {

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private IDownloadController downloadController;

    @Autowired
    private ExtractionSecurityService securityService;

    @GraphQLQuery(name = "extractionTypes", description = "Get all available extraction types", deprecationReason = "Use liveExtractionTypes and aggregationTypes")
    @Transactional(readOnly = true)
    public List<ExtractionTypeVO> getAllExtractionTypes(@GraphQLArgument(name = "filter") ExtractionTypeFilterVO filter) {

        filter = securityService.sanitizeFilter(filter);

        return extractionService.findByFilter(filter);
    }

    @GraphQLQuery(name = "liveExtractionTypes", description = "Get all live extraction types")
    @Transactional(readOnly = true)
    public List<ExtractionTypeVO> getLiveExtractionTypes(@GraphQLArgument(name = "filter") ExtractionTypeFilterVO filter) {

        filter = securityService.sanitizeFilter(filter);

        return extractionService.findByFilter(filter);
    }

    @GraphQLQuery(name = "extractionRows", description = "Preview some extraction rows")
    @Transactional
    public ExtractionResultVO getExtractionRows(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                               @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                               @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                               @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                               @GraphQLArgument(name = "sortBy") String sort,
                                               @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        securityService.checkReadAccess(type);

        return extractionService.executeAndRead(type, filter, offset, size, sort, direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);
    }

    @GraphQLQuery(name = "extraction", description = "Preview some extraction")
    @Transactional
    public List<Map<String, String>> getExtraction(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                                            @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                            @GraphQLArgument(name = "sortBy") String sort,
                                                            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        securityService.checkReadAccess(type);

        List<Map<String, String>> results = new ArrayList<>();

        ExtractionResultVO resultVO = extractionService.executeAndRead(type, filter, offset, size, sort, direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);

        resultVO.getRows().forEach(row -> {
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < row.length; i++) {
                String columnName = resultVO.getColumns().get(i).getLabel();
                rowMap.put(columnName, row[i]);
            }
            results.add(rowMap);
        });

        return results;
    }

    @GraphQLQuery(name = "extractionFile", description = "Execute extraction to a file")
    @Transactional
    public String getExtractionFile(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                    @GraphQLArgument(name = "filter") ExtractionFilterVO filter
    ) throws IOException {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");

        securityService.checkReadAccess(type);

        File tempFile = extractionService.executeAndDump(type, filter);

        // Add to download controller
        String path = downloadController.registerFile(tempFile, true);

       return path;
    }

}
