package net.sumaris.server.http.graphql.extraction;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.service.ExtractionService;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.server.http.rest.DownloadController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class ExtractionGraphQLService {

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private DownloadController downloadController;

    /* -- Extraction / table -- */

    @GraphQLQuery(name = "extractionTypes", description = "Get all available extraction types")
    @Transactional(readOnly = true)
    //@IsUser
    public List<ExtractionTypeVO> getAllExtractionTypes() {
        return extractionService.getAllExtractionTypes();
    }

    @GraphQLQuery(name = "extractionRows", description = "Preview some extraction rows")
    @Transactional
    //@IsUser
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

        return extractionService.executeAndRead(type, filter, offset, size, sort, direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);
    }

    @GraphQLQuery(name = "extractionFile", description = "Execute extraction to a file")
    @Transactional
    //@IsUser
    public String getExtractionFile(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                 @GraphQLArgument(name = "filter") ExtractionFilterVO filter
    ) throws IOException {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");

        File tempFile = extractionService.executeAndDump(type, filter);
        String fileServerPath = downloadController.registerFile(tempFile, true);

       return fileServerPath;
    }

}
